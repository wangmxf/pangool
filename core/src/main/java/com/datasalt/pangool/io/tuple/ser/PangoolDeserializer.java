/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datasalt.pangool.io.tuple.ser;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.VIntWritable;
import org.apache.hadoop.io.VLongWritable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.util.ReflectionUtils;

import com.datasalt.pangool.CoGrouperConfig;
import com.datasalt.pangool.Schema;
import com.datasalt.pangool.SerializationInfo;
import com.datasalt.pangool.io.Buffer;
import com.datasalt.pangool.io.Serialization;
import com.datasalt.pangool.io.tuple.DatumWrapper;
import com.datasalt.pangool.io.tuple.ITuple;
import com.datasalt.pangool.io.tuple.Tuple;


public class PangoolDeserializer implements Deserializer<DatumWrapper<ITuple>> {

	private CoGrouperConfig coGrouperConf;
	private SerializationInfo serInfo;
	private DataInputStream in;
	private Serialization ser;
	private boolean isRollup;
	private boolean multipleSources;
	private Map<String, Enum<?>[]> cachedEnums = new HashMap<String, Enum<?>[]>();

	private Buffer tmpInputBuffer = new Buffer();
	private ITuple commonTuple;
	private List<ITuple> specificTuples=new ArrayList<ITuple>();
	private List<ITuple> sourceTuples = new ArrayList<ITuple>();

	public PangoolDeserializer(Serialization ser, CoGrouperConfig grouperConfig) {
		this.coGrouperConf = grouperConfig;
		this.serInfo = coGrouperConf.getSerializationInfo();
		this.ser = ser;
		this.cachedEnums = PangoolSerialization.getEnums(grouperConfig);
		this.isRollup = coGrouperConf.getRollupFrom() != null && !coGrouperConf.getRollupFrom().isEmpty();
		this.multipleSources = coGrouperConf.getNumSources() >= 2;
		Schema commonSchema = serInfo.getCommonSchema();
		//this comon tuple needs to have texts initialized
		this.commonTuple = new Tuple(commonSchema,true); 
		for (Schema sourceSchema : coGrouperConf.getSourceSchemas()){
			sourceTuples.add(new Tuple(sourceSchema));
		}
		
		if (multipleSources){
			for(Schema specificSchema : serInfo.getSpecificSchemas()){
				specificTuples.add(new Tuple(specificSchema,true)); //texts initialized
			}
		} 
	}

	@Override
	public void open(InputStream in) throws IOException {
		if (in instanceof DataInputStream){
			this.in = (DataInputStream)in;
		} else {
			this.in = new DataInputStream(in);
		}
	}
	
	@Override
	public DatumWrapper<ITuple> deserialize(DatumWrapper<ITuple> t) throws IOException {
		if(t == null) {
			t = new DatumWrapper<ITuple>();
		}
		if(isRollup) {
			t.doDoubleBuffering();
		}
		
		ITuple tuple = (multipleSources) ? deserializeMultipleSources() : deserializeOneSource(t.currentDatum());
		t.datum(tuple);
		return t;
	}
	
	
	private ITuple deserializeMultipleSources() throws IOException {
		readFields(commonTuple,in);
		int sourceId = WritableUtils.readVInt(in);
		ITuple specificTuple = specificTuples.get(sourceId);
		readFields(specificTuple,in);
		ITuple result = sourceTuples.get(sourceId);
		mixIntermediateIntoResult(commonTuple,specificTuple,result,sourceId);
		return result;
	}
	
	private void mixIntermediateIntoResult(ITuple commonTuple,ITuple specificTuple,ITuple result,int sourceId){
		int[] commonTranslation = serInfo.getSerializationTranslation().commonTranslation.get(sourceId);
		for (int i =0 ; i < commonTranslation.length ; i++){
			int destPos = commonTranslation[i];
			result.set(destPos,commonTuple.get(i));
		}
		
		int[] specificTranslation = serInfo.getSerializationTranslation().particularTranslation.get(sourceId);
		for (int i =0 ; i < specificTranslation.length ; i++){
			int destPos = specificTranslation[i];
			result.set(destPos,specificTuple.get(i));
		}
	}
	
	private ITuple deserializeOneSource(ITuple reuse) throws IOException {
		readFields(commonTuple,in);
		if (reuse == null){
			reuse = sourceTuples.get(0);
		}
		int[] commonTranslation = serInfo.getSerializationTranslation().commonTranslation.get(0);
		for (int i =0 ; i < commonTranslation.length ; i++){
			int destPos = commonTranslation[i];
			reuse.set(destPos,commonTuple.get(i));
		}
		return reuse;
	}

	public void readFields(ITuple tuple, DataInput input) throws IOException {
		Schema schema = tuple.getSchema();
		for(int index = 0; index < schema.getFields().size(); index++) {
			Class<?> fieldType = schema.getField(index).getType();
			String fieldName = schema.getField(index).name();
			if(fieldType == VIntWritable.class) {
				tuple.set(index,WritableUtils.readVInt(input));
			} else if(fieldType == VLongWritable.class) {
				tuple.set(index,WritableUtils.readVLong(input));
			} else if(fieldType == Integer.class) {
				tuple.set(index,input.readInt());
			} else if(fieldType == Long.class) {
				tuple.set(index,input.readLong());
			} else if(fieldType == Double.class) {
				tuple.set(index,input.readDouble());
			} else if(fieldType == Float.class) {
				tuple.set(index,input.readFloat());
			} else if(fieldType == String.class) {
				((Text)tuple.get(index)).readFields(input);
			} else if(fieldType == Boolean.class) {
				byte b = input.readByte();
				tuple.set(index,(b != 0));
			} else if(fieldType.isEnum()) {
				int ordinal = WritableUtils.readVInt(input);
				try {
					Enum<?>[] enums = cachedEnums.get(fieldName);
					if(enums == null) {
						throw new IOException("Field " + fieldName + " is not a enum type");
					}
					tuple.set(index,enums[ordinal]);
				} catch(ArrayIndexOutOfBoundsException e) {
					throw new RuntimeException(e);
				}
			} else {
				int size = WritableUtils.readVInt(input);
				if(size != 0) {
					tmpInputBuffer.setSize(size);
					input.readFully(tmpInputBuffer.getBytes(), 0, size);
					if(tuple.get(index) == null) {
						tuple.set(index, ReflectionUtils.newInstance(fieldType, null));
					}
					Object ob = ser.deser(tuple.get(index), tmpInputBuffer.getBytes(), 0, size);
					tuple.set(index, ob);
				}
			} // end for
		}
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}
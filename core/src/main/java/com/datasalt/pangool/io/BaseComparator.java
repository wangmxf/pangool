/**
 * Copyright [2012] [Datasalt Systems S.L.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datasalt.pangool.io;

import java.io.IOException;
import java.io.Serializable;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.RawComparator;

import com.datasalt.pangool.cogroup.TupleMRConfig;
import com.datasalt.pangool.cogroup.TupleMRException;
import com.datasalt.pangool.serialization.tuples.SingleFieldDeserializer;

@SuppressWarnings("serial")
public abstract class BaseComparator<T> implements RawComparator<T>, Serializable, Configurable {

	private Configuration conf;
	private SingleFieldDeserializer fieldDeser1;
	private SingleFieldDeserializer fieldDeser2;
	private final Class<?> type;
  private T object1 = null;
  private T object2 = null;
  
	public BaseComparator(Class<?> type) {
		this.type = type;
	}
	
	@Override
	public void setConf(Configuration conf) {
		try {
	    fieldDeser1 = new SingleFieldDeserializer(conf, TupleMRConfig.get(conf), type);
	    fieldDeser2 = new SingleFieldDeserializer(conf, TupleMRConfig.get(conf), type);
	    	    
    } catch(IOException e) {
    	throw new RuntimeException(e);
    } catch(TupleMRException e) {
    	throw new RuntimeException(e);
    }
	}
	
	@Override
  public Configuration getConf() {
		return conf;
  }

	@Override
  public abstract int compare(T o1, T o2);
	
	@SuppressWarnings("unchecked")
  @Override
  public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
		try {

	    object1 = (T) fieldDeser1.deserialize(b1, s1);	    
	    object2 = (T) fieldDeser2.deserialize(b2, s2);

		} catch(IOException e) {
			throw new RuntimeException(e);
    }

	  return compare(object1, object2);
  }	
}

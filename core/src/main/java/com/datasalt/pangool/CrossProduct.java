package com.datasalt.pangool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.VIntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import com.datasalt.pangool.CoGrouper;
import com.datasalt.pangool.CoGrouperException;
import com.datasalt.pangool.Criteria.Order;
import com.datasalt.pangool.Schema;
import com.datasalt.pangool.Schema.Field;
import com.datasalt.pangool.SortBy;
import com.datasalt.pangool.api.GroupHandler;
import com.datasalt.pangool.api.InputProcessor;
import com.datasalt.pangool.api.GroupHandler.CoGrouperContext;
import com.datasalt.pangool.api.GroupHandler.Collector;
import com.datasalt.pangool.commons.HadoopUtils;
import com.datasalt.pangool.io.tuple.ITuple;
import com.datasalt.pangool.io.tuple.Tuple;

/**
 * Code for solving the URL Resolution CoGroup Problem in Pangool.
 * <p>
 * The URL Resolution CoGroup Problem is: We have one file with URL Registers: {url timestamp ip} and another file with
 * canonical URL mapping: {url canonicalUrl}. We want to obtain the URL Registers file with the url substituted with the
 * canonical one according to the mapping file: {canonicalUrl timestamp ip}.
 */
public class CrossProduct {

	@SuppressWarnings("serial")
	public static class UrlProcessor extends InputProcessor<LongWritable, Text> {
		private Tuple tuple;

		@Override
		public void process(LongWritable key, Text value, CoGrouperContext context, Collector collector)
		    throws IOException, InterruptedException {

			if(tuple == null) {
				tuple = new Tuple(context.getCoGrouperConfig().getSourceSchema("urlRegister"));
			}
			String[] fields = value.toString().split("\t");
			tuple.set("url", fields[0]);
			tuple.set("timestamp", Long.parseLong(fields[1]));
			tuple.set("ip", fields[2]);
			collector.write(tuple);
		}
	}

	@SuppressWarnings("serial")
	public static class UrlMapProcessor extends InputProcessor<LongWritable, Text> {

		private Tuple tuple;

		@Override
		public void process(LongWritable key, Text value, CoGrouperContext context, Collector collector)
		    throws IOException, InterruptedException {
			if(tuple == null) {
				tuple = new Tuple(context.getCoGrouperConfig().getSourceSchema("urlMap"));
			}

			String[] fields = value.toString().split("\t");
			tuple.set("url", fields[0]);
			tuple.set("canonicalUrl", fields[1]);
			collector.write(tuple);
		}
	}

	@SuppressWarnings("serial")
	public static class Handler extends GroupHandler<Text, NullWritable> {

		private Text result;
		private int numSources;
		
		public void setup(CoGrouperContext coGrouperContext, Collector collector)
    throws IOException, InterruptedException, CoGrouperException {
			this.numSources = coGrouperContext.getCoGrouperConfig().getNumSources();
			
		}
		

		@Override
		public void onGroupElements(ITuple group, Iterable<ITuple> tuples, CoGrouperContext context, Collector collector)
		    throws IOException, InterruptedException, CoGrouperException {
			if(result == null) {
				result = new Text();
			}
			//String cannonicalUrl = null;
			List<ITuple> cachedTuples = new ArrayList<ITuple>();

		}
	}

	public Job getJob(Configuration conf, String input1, String input2, String output) throws CoGrouperException,
	    IOException {
		List<Field> urlRegisterFields = new ArrayList<Field>();
		urlRegisterFields.add(new Field("url", String.class));
		urlRegisterFields.add(new Field("timestamp", Long.class));
		urlRegisterFields.add(new Field("ip", String.class));

		List<Field> urlMapFields = new ArrayList<Field>();
		urlMapFields.add(new Field("url", String.class));
		urlMapFields.add(new Field("canonicalUrl", String.class));

		CoGrouper grouper = new CoGrouper(conf,"CrossProduct");
		Schema urlMapSchema = new Schema("urlMap", urlMapFields);
		Schema urlRegisterSchema = new Schema("urlRegister",urlRegisterFields);
		grouper.addSourceSchema(urlMapSchema);
		grouper.addSourceSchema(urlRegisterSchema);
		
		grouper.setGroupByFields("url");
		grouper.setOrderBy(new SortBy().add("url", Order.ASC).addSourceOrder(Order.ASC));

		grouper.setGroupHandler(new Handler());
		grouper.setOutput(new Path(output), TextOutputFormat.class, Text.class, NullWritable.class);
		grouper.addInput(new Path(input1), TextInputFormat.class, new UrlMapProcessor());
		
		//create a new output for every source
		{
			List<Field> newUrlMapFields = new ArrayList<Field>();
			newUrlMapFields.add(new Field("CROSS_PRODUCT_GROUP",VIntWritable.class));
			newUrlMapFields.addAll(urlMapSchema.getFields());
			Schema extraUrlMapSchema = new Schema("EXTRA_" + urlMapSchema.getName(),newUrlMapFields);
			grouper.addNamedTupleOutput("EXTRA_" + extraUrlMapSchema.getName(),extraUrlMapSchema);
		}
		{
			List<Field> newUrlRegisterFields = new ArrayList<Field>();
			newUrlRegisterFields.add(new Field("CROSS_PRODUCT_GROUP",VIntWritable.class));
			newUrlRegisterFields.addAll(urlRegisterSchema.getFields());
			Schema extraUrlMapSchema = new Schema("",newUrlRegisterFields);
			grouper.addNamedTupleOutput("EXTRA_" + extraUrlMapSchema.getName(),extraUrlMapSchema);
		}
		
		
		
		grouper.addInput(new Path(input2), TextInputFormat.class, new UrlProcessor());
		return grouper.createJob();
	}

	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException,
	    CoGrouperException {

		Configuration conf = new Configuration();
		FileSystem fS = FileSystem.get(conf);
		String input1 = args[0];
		String input2 = args[1];
		String output = args[2];
		HadoopUtils.deleteIfExists(fS, new Path(output));
		new CrossProduct().getJob(conf, input1, input2, output).waitForCompletion(true);
	}
}

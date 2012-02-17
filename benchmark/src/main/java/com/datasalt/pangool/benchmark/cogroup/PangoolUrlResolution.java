package com.datasalt.pangool.benchmark.cogroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
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
public class PangoolUrlResolution {

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

		@Override
		public void onGroupElements(ITuple group, Iterable<ITuple> tuples, CoGrouperContext context, Collector collector)
		    throws IOException, InterruptedException, CoGrouperException {
			if(result == null) {
				result = new Text();
			}
			String cannonicalUrl = null;
			for(ITuple tuple : tuples) {
				if("urlMap".equals(tuple.getSchema().getName())) {
					cannonicalUrl = tuple.get("canonicalUrl").toString();
				} else {
					result.set(cannonicalUrl + "\t" + tuple.get("timestamp") + "\t" + tuple.get("ip"));
					collector.write(result, NullWritable.get());
				}
			}
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

		CoGrouper grouper = new CoGrouper(conf);
		grouper.addSourceSchema(new Schema("urlMap", urlMapFields));
		grouper.addSourceSchema(new Schema("urlRegister", urlRegisterFields));
		

		grouper.setGroupByFields("url");
		grouper.setOrderBy(new SortBy().add("url", Order.ASC).addSourceOrder(Order.ASC));
		
		//grouper.setSecondaryOrderBy("urlRegister", new SortBy().add("timestamp", Order.DESC));

		grouper.setGroupHandler(new Handler());
		grouper.setOutput(new Path(output), TextOutputFormat.class, Text.class, NullWritable.class);
		grouper.addInput(new Path(input1), TextInputFormat.class, new UrlMapProcessor());
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
		new PangoolUrlResolution().getJob(conf, input1, input2, output).waitForCompletion(true);
	}
}
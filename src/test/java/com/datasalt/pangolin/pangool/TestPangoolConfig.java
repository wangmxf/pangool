package com.datasalt.pangolin.pangool;

import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import com.datasalt.pangolin.grouper.io.tuple.ITuple.InvalidFieldException;
import com.datasalt.pangolin.pangool.Schema.Field;
import com.datasalt.pangolin.pangool.SortCriteria.SortOrder;

public class TestPangoolConfig {

	@Test
	public void testCommonOrderedSchema() throws CoGrouperException {
		PangoolConfigBuilder configBuilder = new PangoolConfigBuilder();

		configBuilder.addSchema(0, Schema.parse("url:string, date:long, fetched:long, content:string"));
		configBuilder.addSchema(1, Schema.parse("fetched:long, url:string, name:string"));
		configBuilder.setSorting(Sorting.parse("url asc, fetched desc"));
		configBuilder.setGroupByFields("url");
		PangoolConfig config = configBuilder.build();
		
		Assert.assertEquals(Schema.parse("url:string, fetched:long").toString(), config.getCommonOrderedSchema().toString());
	}
	
	@Test
	public void testCommonOrderedSchemaWithSourceId() throws InvalidFieldException, CoGrouperException {
		PangoolConfigBuilder configBuilder = new PangoolConfigBuilder();

		configBuilder.addSchema(0, Schema.parse("url:string, date:long, fetched:long, content:string"));
		configBuilder.addSchema(1, Schema.parse("fetched:long, url:string, name:string"));
		
		configBuilder.setSorting(new SortingBuilder()
			.add("url", SortOrder.ASC)
			.add("fetched", SortOrder.DESC)
			.addSourceId(SortOrder.ASC)
			.buildSorting()
		);

		configBuilder.setGroupByFields("url");
		PangoolConfig config = configBuilder.build();
		
		Assert.assertEquals(Schema.parse("url:string, fetched:long, " + Field.SOURCE_ID_FIELD + ":vint").toString(), config.getCommonOrderedSchema().toString());
	}
	
	@Test
	public void testParticularPartialOrderedSchemas() throws CoGrouperException {
		PangoolConfigBuilder configBuilder = new PangoolConfigBuilder();

		configBuilder.addSchema(0, Schema.parse("url:string, date:long, fetched:long, content:string"));
		configBuilder.addSchema(1, Schema.parse("fetched:long, url:string, name:string"));
		configBuilder.setSorting(Sorting.parse("url asc, fetched desc"));
		configBuilder.setGroupByFields("url");
		PangoolConfig config = configBuilder.build();
		
		Map<Integer, Schema> partialOrderedSchemas = config.getParticularPartialOrderedSchemas();

		Assert.assertEquals(Schema.parse("content:string, date:long").toString(), partialOrderedSchemas.get(0).toString()); 
		Assert.assertEquals(Schema.parse("name:string").toString(), partialOrderedSchemas.get(1).toString());
	}
	
	@Test
	public void testSerDeEquality() throws JsonGenerationException, JsonMappingException, IOException, CoGrouperException, InvalidFieldException {
		PangoolConfigBuilder configBuilder = new PangoolConfigBuilder();

		SchemaBuilder builder1 = new SchemaBuilder();
		builder1
			.add("url", String.class)
			.add("date", Long.class)
			.add("content", String.class);

		SchemaBuilder builder2 = new SchemaBuilder();
		builder2
			.add("url", String.class)
			.add("date", Long.class)
			.add("name", String.class);

		SortingBuilder builder = new SortingBuilder();
		Sorting sorting = 
			builder
			.add("url", SortOrder.ASC)
			.add("date", SortOrder.DESC)
			.addSourceId(SortOrder.ASC)
			.secondarySort(1)
				.add("content", SortOrder.ASC)
			.secondarySort(2)
				.add("name", SortOrder.ASC)
			.buildSorting();

<<<<<<< HEAD
		configBuilder.addSchema(1, builder1.createSchema());
		configBuilder.addSchema(2, builder2.createSchema());
		configBuilder.setSorting(sorting);
		configBuilder.setRollupFrom("url");
		configBuilder.setGroupByFields("url", "date");
		configBuilder.setCustomPartitionerFields("url");
		PangoolConfig config = configBuilder.build();
=======
		config.addSchema(1, builder1.createSchema());
		config.addSchema(2, builder2.createSchema());
		config.setSorting(sorting);
		config.setRollupFrom("url");
		config.setGroupByFields("url", "date");
>>>>>>> 2ee1283d3a4512bfb45ef475700a0785935d46ef

		ObjectMapper mapper = new ObjectMapper();
		String jsonConfig = config.toStringAsJSON(mapper);		
		PangoolConfig config2 = PangoolConfigBuilder.fromJSON(jsonConfig, mapper);

		Assert.assertEquals(jsonConfig, config2.toStringAsJSON(mapper));
	}
}

//package com.datasalt.avrool.mapreduce;
//
//import java.io.IOException;
//
//import org.codehaus.jackson.JsonGenerationException;
//import org.codehaus.jackson.map.JsonMappingException;
//import org.junit.Assert;
//import org.junit.Test;
//
//import com.datasalt.avrool.CoGrouperException;
//import com.datasalt.avrool.io.tuple.ITuple;
//import com.datasalt.avrool.io.tuple.ITuple.InvalidFieldException;
//import com.datasalt.avrool.mapreduce.PangoolGroupComparator;
//
//public class TestGroupComparator extends ComparatorsBaseTest {
//
//	@Test
//	public void testObjectComparison() throws CoGrouperException, JsonGenerationException, JsonMappingException, IOException, InvalidFieldException {
//		PangoolGroupComparator comparator = new PangoolGroupComparator();
//		setConf(comparator);
//		
//		// source 1
//		Assert.assertEquals(0, comparator.compare(getTuple1(true, 10, "a"), getTuple1(true, 10, "a")));
//		Assert.assertTrue(0 > comparator.compare(getTuple1(false, 10, "a"), getTuple1(true, 10, "a")));
//		Assert.assertTrue(0 < comparator.compare(getTuple1(true, 10, "a"), getTuple1(false, 10, "a")));
//		Assert.assertTrue(0 < comparator.compare(getTuple1(true, 1, "a"), getTuple1(true, 10, "a")));
//		Assert.assertTrue(0 > comparator.compare(getTuple1(true, 10, "a"), getTuple1(true, 1, "a")));
//		
//		// source 2
//		Assert.assertEquals(0, comparator.compare(getTuple2(true, 10, 0), getTuple2(true, 10, 0)));
//		Assert.assertTrue(0 > comparator.compare(getTuple2(false, 10, 0), getTuple2(true, 10, 0)));
//		Assert.assertTrue(0 < comparator.compare(getTuple2(true, 10, 0), getTuple2(false, 10, 0)));
//		Assert.assertTrue(0 < comparator.compare(getTuple2(true, 1, 0), getTuple2(true, 10, 0)));
//		Assert.assertTrue(0 > comparator.compare(getTuple2(true, 10, 0), getTuple2(true, 1, 0)));
//	}
//}
package org.rx.trace.util;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;
import org.rx.rtrace.util.StringUtils;

public class StringUtilsTest {
	
	@Test
	public void testJoin() {
		String[] s = "this is a join test".split(" ");
		Assert.assertEquals("is a", StringUtils.join(" ", s, 1, 3));
	}
	
	@Test
	public void testStripEnd01() {
		Assert.assertEquals("trailing spaces",
					StringUtils.stripEnd("trailing spaces   "));
	}
	
	@Test
	public void testStripEnd02() {
		Assert.assertEquals("ok", StringUtils.stripEnd("ok"));
	}
	
	@Test
	public void testStripStart01() {
		Assert.assertEquals("leading spaces",
					StringUtils.stripStart("   leading spaces"));
	}
	
	@Test
	public void testStripStart02() {
		Assert.assertEquals("ok", StringUtils.stripStart("ok"));
	}
	
	@Test
	public void testStrip01() {
		Assert.assertEquals("", StringUtils.strip("        "));
	}
	
	@Test
	public void testStrip02() {
		Assert.assertEquals("trailing & leading spaces",
					StringUtils.strip("   trailing & leading spaces ", ' '));
	}
	
	@Test
	public void testFlatten01() {
		Assert.assertTrue(Arrays.equals(
				new String[]{"1", "21", "22", "3"},
				StringUtils.flatten("1", new String[]{"21", "22"}, "3")));
	}
	
	@Test
	public void testFlattenNesting01() {
		Assert.assertTrue(Arrays.equals(
				new String[]{"1", "11", "121", "122", "3"},
				StringUtils.flatten(
						new Object[]{"1", new Object[]{"11", 
								new String[]{"121", "122"}
						}, "3"})));
	}
	
}

package org.rx.trace.util;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;
import org.rx.rtrace.util.CommandLineUtils;

public class CommandLineUtilsTest {

	@Test
	public void testJoinArgsSimple01() {
		checkJoinArgs("this is a test", "this", "is", "a", "test");
	}
	
	@Test
	public void testJoinArgsSimple02() {
		checkJoinArgs("John: \"Hi!\" Alex: \"Hi, John!\"",
				"John:", "\"Hi!\"",
				"Alex:", "\"Hi,", "John!\"");
	}
	
	@Test
	public void testJoinArgsComplex01() {
		checkJoinArgs("this\\_is\\_a\\_single\\_argument",
				"this is a single argument");
	}
	
	@Test
	public void testJoinArgsComplex02() {
		checkJoinArgs("a\\_multi\\_word\\_argument 2 another\\_'multi_arg'",
				"a multi word argument", "2", "another \'multi_arg\'");
	}
	
	@Test
	public void testSplitArgumentsSimple01() {
		checkSplitArgs("this is a test", "this", "is", "a", "test");
	}
	
	@Test
	public void testSplitArgumentsSimple02() {
		checkSplitArgs("John: \"Hi!\" Alex: \"Hi, John!\"",
				"John:", "\"Hi!\"",
				"Alex:", "\"Hi,", "John!\"");
	}
	
	@Test
	public void testSplitArgsComplex01() {
		checkSplitArgs("this\\_is\\_a\\_single\\_argument",
				"this is a single argument");
	}
	
	@Test
	public void testSplitArgumentsTricky01() {
		checkSplitArgs("");
	}
	
	@Test
	public void testSplitArgumentsTricky02() {
		checkSplitArgs(" ");
	}
	
	@Test
	public void testSplitArgumentsTricky03() {
		checkSplitArgs("          ");
	}
	
	@Test
	public void testSplitArgumentsTricky04() {
		checkSplitArgs("  a b", "a", "b");
	}
	
	private static void checkJoinArgs(String expJoined, String... arg) {
		Assert.assertEquals(expJoined, CommandLineUtils.joinArguments(arg));
	}
	
	private static void checkSplitArgs(String s, String... expArg) {
		String[] exp = expArg, act = CommandLineUtils.splitArguments(s);
		if (!Arrays.equals(exp, act)) {
			Assert.fail("Exp: " + Arrays.toString(exp) +
					"\nAct: " + Arrays.toString(act));
		}
	}
}

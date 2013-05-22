package org.rx.rtrace.util;

import java.util.Arrays;

public abstract class StringUtils {
	
	public static final String[] EMPTY_STRING_ARRAY = new String[]{};
	
	private static final char[] DEFAULT_STRIP_CHARS = new char[]{' '};
	
	private static final char[] DEFAULT_STRIP_CHARS_SORTED;
	
	static {
		DEFAULT_STRIP_CHARS_SORTED = DEFAULT_STRIP_CHARS.clone();
		Arrays.sort(DEFAULT_STRIP_CHARS_SORTED);
	}
	
	public static String join(String sep, String... s) {
		return join0(sep, s, 0, s.length);
	}
	
	public static String join(String sep, String[] s, int from) {
		return join(sep, s, from, s.length);
	}
	
	public static String join(String sep, String[] s, int from, int to) {
		checkRange(s, from, to);
		return join0(sep, s, from, to);
	}
	
	public static int lengthSum(String... s) { 
		return lengthSum0(s, 0, s.length);
	}
	
	public static int lengthSum(String[] s, int from, int to) {
		checkRange(s, from, to);
		int len = 0;
		while (from < to)
			len += s[from++].length();
		return len;
	}
	
	public static String stripEnd(String s, char... stripChars) {
		if (stripChars.length == 0) {
			stripChars = DEFAULT_STRIP_CHARS_SORTED;
		} else if (stripChars.length > 1) {
			stripChars = stripChars.clone();
			Arrays.sort(stripChars);
		}
		return stripEndSorted(s, stripChars);
	}
	
	public static String stripStart(String s, char... stripChars) {
		if (stripChars.length == 0) {
			stripChars = DEFAULT_STRIP_CHARS_SORTED;
		} else if (stripChars.length > 1) {
			stripChars = stripChars.clone();
			Arrays.sort(stripChars);
		}
		return stripStartSorted(s, stripChars);
	}
	
	public static String strip(String s, char... stripChars) {
		if (stripChars.length == 0) {
			stripChars = DEFAULT_STRIP_CHARS_SORTED;
		} else if (stripChars.length > 1) {
			stripChars = stripChars.clone();
			Arrays.sort(stripChars);
		}
		return stripStartSorted(stripEndSorted(s, stripChars), stripChars);
	}
	
	public static String repeat(Object o, int n) {
		if (o == null)
			throw new IllegalArgumentException("o must not be null");
		checkNonNegative(n);
		if (n == 0)
			return "";
		
		String s = o.toString();
		StringBuilder sb = new StringBuilder(s.length() * n);
		while (n-- > 0)
			sb.append(s);
		return sb.toString();
	}
	
	public static String repeat(char c, int n) {
		checkNonNegative(n);
		if (n == 0)
			return "";
		
		StringBuilder sb = new StringBuilder(n);
		while (n-- > 0)
			sb.append(c);
		return sb.toString();
	}
	
	public static String[] flatten(Object... arg) {
		return flatten(arg, 0, arg.length);
	}
	
	public static String[] flatten(Object[] args, int from, int to) {
		checkRange(args, from, to);
		return flatten0(args, from, to);
	}
	
	private static String join0(String sep, String[] s, int from, int to) {
		int range_len = to - from;
		if (range_len == 0)
			return "";
		if (range_len == 1)
			return s[from];
		StringBuilder sb = new StringBuilder(lengthSum(s, from, to) +
				sep.length() * (range_len - 1));
		sb.append(s[from]);
		while (++from < to)
			sb.append(sep).append(s[from]);
		return sb.toString();
	}
	
	private static int lengthSum0(String[] s, int from, int to) {
		int len = 0;
		while (from < to)
			len += s[from++].length();
		return len;
	}
	
	private static String stripEndSorted(String s, char... stripChars) {
		int to = s.length();
		while (--to >= 0 &&
				Arrays.binarySearch(stripChars, s.charAt(to)) >= 0) {
		}
		return s.substring(0, ++to);
	}
	
	private static String stripStartSorted(String s, char... stripChars) {
		int from = -1, to = s.length();
		while (++from < to && 
				Arrays.binarySearch(stripChars, s.charAt(from)) >= 0) {
		}
		return s.substring(from, to);
	}
	
	private static String[] flatten0(Object[] args, int from, int to) {
		if (args.length == 0)
			return EMPTY_STRING_ARRAY;
		String[] a = new String[getFlatArrayLength(args, from, to)];
		int aInd = 0;
		for (int i = from; i < to; ++i) {
			if (args[i] == null)
				a[aInd++] = null;
			else if (!args[i].getClass().isArray())
				a[aInd++] = args[i].toString();
			else
				aInd = flattenRecursive((Object[]) args[i], a, aInd);
		}
		assert(aInd == a.length);
		return a;
	}
	
	private static int flattenRecursive(Object[] src, String[] dst, int i) {
		for (Object arg : src) {
			if (arg == null)
				dst[i++] = null;
			else if (!arg.getClass().isArray())
				dst[i++] = arg.toString();
			else
				i = flattenRecursive((Object[]) arg, dst, i);
		}
		return i;
	}
	
	private static int getFlatArrayLength(Object[] args, int from, int to) {
		int len = 0;
		for (int i = from; i < to; ++i) {
			len += (args[i] == null || !args[i].getClass().isArray() ? 1
					: getFlatArrayLength((Object[]) args[i]));
		}
		return len;
	}
	
	private static int getFlatArrayLength(Object[] args) {
		int len = 0;
		for (Object arg : args) {
			len += (arg == null || !arg.getClass().isArray() ? 1
				: getFlatArrayLength((Object[]) arg));
		}
		return len;
	}
	
	private static void checkRange(Object[] a, int from, int to) {
		if (from < 0 || to > a.length)
			throw new IndexOutOfBoundsException();
		if (from > to)
			throw new IllegalArgumentException("from > to");
	}
	
	private static void checkNonNegative(int n) {
		if (n < 0)
			throw new IllegalArgumentException("n must be non-negative");
	}
}

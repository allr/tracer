package org.rx.rtrace.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Utility class for arrays.
 * 
 * @author Leo Osvald
 *
 */
public class ArrayUtils {

	public static <T> Collection<T> asCollection(T[] a, int from, int to) {
		checkArrayBounds(a, from, to);
		if (from == to)
			return Collections.emptyList();
		
		Collection<T> c = new ArrayList<T>(to - from + 1);
		while (from < to)
			c.add(a[from++]);
		return c;
	}
	
	public static <T> Collection<T> asCollection(T[] a, int from) {
		return asCollection(a, from, a.length);
	}
	
	private static <T> void checkArrayBounds(T[] a, int from, int to) {
		if (from < 0 || from > a.length || from > to)
			throw new IllegalArgumentException();
	}
	
	private ArrayUtils() {
	}
}

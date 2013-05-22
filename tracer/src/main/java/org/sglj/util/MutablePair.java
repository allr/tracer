package org.sglj.util;

/**
 * An extension of the {@link Pair} utility class which allows its contents
 * to be mutable and provides two additional methods for setting them, 
 * {@link #setFirst(Object)} and {@link #setSecond(Object)}.
 *  
 * @author Leo Osvald
 *
 * @param <T1>
 * @param <T2>
 */
public class MutablePair<T1, T2> extends Pair<T1, T2> {

	public MutablePair() {
	}
	
	public MutablePair(T1 first, T2 second) {
		super(first, second);
	}
	
	public void setFirst(T1 first) {
		super.setFirst(first);
	}
	
	public void setSecond(T2 second) {
		super.setSecond(second);
	}

}

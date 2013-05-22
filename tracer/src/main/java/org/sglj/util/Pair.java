/*
 * Pair.java
 * 
 * Copyright (C) 2010 Leo Osvald <leo.osvald@gmail.com>
 * 
 * This library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sglj.util;

/**
 * Java equivalent to C++ std::pair<T1, T2>.
 * 
 * @author Leo Osvald
 *
 * @param <T1> the type of the first element
 * @param <T2> the type of the second element
 */
public class Pair<T1, T2> implements Comparable< Pair<T1, T2> > {
	
	private T1 first;
	private T2 second;
	
	public Pair(T1 first, T2 second) {
		this.first = first;
		this.second = second;
	}
	
	Pair() {
	}
	
	public T1 first() {
		return first;
	}
	
	public T2 second() {
		return second;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public int compareTo(Pair<T1, T2> o) {
		Comparable<? super T1> compFirst; 
		Comparable<? super T2> compSecond;
		if(!(first instanceof Comparable<?>)) return 0; 
		if(!(second instanceof Comparable<?>)) return 0;
		
		compFirst = (Comparable) first;
		compSecond = (Comparable) second;
		int cmp = compFirst.compareTo(o.first);
		if(cmp != 0) return cmp;
		return compSecond.compareTo(o.second);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Pair<?,?>)) return false;
		return first.equals(((Pair<?, ?>)obj).first)
		&& second.equals(((Pair<?, ?>)obj).second);
	}
	
	@Override
	public int hashCode() {
		return first.hashCode() ^ (second.hashCode()<<16);
	}
	
	void setFirst(T1 first) {
		this.first = first;
	}
	
	void setSecond(T2 second) {
		this.second = second;
	}
	
}

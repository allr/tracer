package org.rx.analyser;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StatContext {
	Map<String, Integer> values = new LinkedHashMap<String, Integer>();
	protected final boolean strict;
	final public static boolean STRICT = Boolean.parseBoolean(System.getProperty("analyser.strict", "false"));
	
	public StatContext() {
		this(STRICT);
	}
	public StatContext(boolean strict){
		this.strict = strict;
	}
	
	public void increment(String key){
		Integer value = values.get(key);
		if(value == null){
			if(strict)
				return;
			value = 0;
		}
		values.put(key, value + 1);
	}
	public void decrement(String key){
		Integer value = values.get(key);
		if(value == null){
			if(strict)
				return;
			value = 0;
		}
		values.put(key, value - 1);
	}
	public void setValue(String key, int value){
		if(!values.containsKey(key))
			if(strict)
				throw new RuntimeException("Not monitoring key: "+key);
		values.put(key, value);
	}
	public void add(String key, int add){
		Integer value = values.get(key);
		if(value == null){
			if(strict)
				return;
			value = 0;
		}
		values.put(key, value + add);
	}
	public int get(String key){
		Integer value = values.get(key);
		if(value == null){
			if(strict)
				throw new RuntimeException("Not monitoring key: "+key);
			value = 0;
		}
		return value;
	}
	public void reset(){
		for (String key: values.keySet())
			values.put(key, 0);
	}
	
	public void clear(){
		values.clear();
	}
	public Set<String> key_set(){
		return values.keySet();
	}
	public void merge(StatContext other){
		for (String key: other.key_set())
			add(key, other.get(key));
	}
	public void print(PrintStream out) {
		int total = 0;
		for(String key: values.keySet()){
			int ccount = values.get(key);
			total += ccount;
			out.println(key+": "+ccount);
		}
		out.println("TOTAL: "+total);
	}
	
	static public class GlobalStatContext extends StatContext{
		public int nb_merge = 0;
		public GlobalStatContext() {
			this(STRICT);
		}
		public GlobalStatContext(boolean strict){
			super(strict);
		}
		public void merge(StatContext other){
			super.merge(other);
			nb_merge ++;
		}
	}

}

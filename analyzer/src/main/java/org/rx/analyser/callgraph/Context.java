package org.rx.analyser.callgraph;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Context<E> {
	Context<E> parent;
	Map<String, Set<E>> vars;
	
	public Context<E> get_parent(){ return parent; }
	public Set<String> get_vars(){ return vars.keySet(); }
	
	
	public void add_var(String name, E item){
		Set<E> set = new LinkedHashSet<E>();
		set.add(item);
		vars.put(name, set);
	}

	public void add_var(String name, Set<E> item_set){
		vars.put(name, item_set);
	}

	public Context<E> sub_context(){
		return new Context<E>(this);
	}
	
	public Context<E>[] alt_context(int nb){
		Context<E>[] contexts = new Context[nb];
		while(nb > 0)
			contexts[-- nb] = sub_context();
		return contexts;
	}
	
	public void merge_contexts(Context<E>[] contexts){
		// This method destroy it's parameter
		Set<E> alt_values = new LinkedHashSet<E>();
		for(int i = 0 ; i < contexts.length ; i ++){
			Context<E> context = contexts[i];
			alt_values.clear();
			for(String var: context.get_vars()){
				Set<E> found_in_other;
				for(int j = i+1 ; j < contexts.length; j ++){
					Context<E> other = contexts[j];
					if((found_in_other = other.remove_var(var)) != null)
						alt_values.addAll(found_in_other);
				}
				merge(var, alt_values);
			}
		}
	}
	
	private void merge(String var, Set<E> found_in_other) {
		Set<E> set = vars.get(var);
		set.addAll(found_in_other);
	}
	public boolean has_var(String name){
		return vars.containsKey(name);
	}
	public Set<E> remove_var(String name){
		return vars.remove(name);
	}
	
	public Context<E> lookup(String name){
		if(has_var(name))
			return this;
		if(parent == null)
			return null;
		return parent.lookup(name);
	}
	
	public void dump(PrintStream out){
		out.println("Dump of context :");
		out.println("\tinit: "+vars.toString());		
	}
	public Context(){
		vars = new LinkedHashMap<String, Set<E>>();
	}
	private Context(Context<E> prev) { this(); parent = prev; }
	
//	public static Context<E> global(){ return global_context;	}
//	public final static Context<E> global_context = new Context();
}
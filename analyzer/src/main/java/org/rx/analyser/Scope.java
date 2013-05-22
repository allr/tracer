package org.rx.analyser;

import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;
import org.rx.FileTools;

abstract public class Scope extends BasicContextProcessor {
	protected StatContext stats;

	public Scope(TreeNodeStream input) {
		this(input, new RecognizerSharedState());
	}
	public Scope(TreeNodeStream input, RecognizerSharedState state) {
		super(input, state);
        stats = new StatContext(false);
	}
	
	public void add_fun(String name){
		ScopeContext ctx = (ScopeContext)get_context();
		ctx.add_var(name, true);
	}
	public void add_var(String name){
		ScopeContext ctx = (ScopeContext)get_context();
		ctx.add_var(name, false);
	}
	protected void close_context() {
		ScopeContext ctx = get_context();
		ScopeContext parent = (ScopeContext)ctx.get_parent();
		Map<String, Boolean> is_fun = ctx.is_function;
		for(String var: ctx.vars){
			boolean local_is_fun = is_fun.get(var);
			ScopeContext found = (ScopeContext)parent.lookup(var);
			if(found == null){
				stats.increment(local_is_fun ? "NEWFUN" : "NEWVAR");;
				continue;
			}
			if(local_is_fun != found.is_function.get(var)){
				stats.increment(local_is_fun ? "FUN0VAR" : "VAR0FUN");
				System.err.println((local_is_fun ? "FUN0VAR" : "VAR0FUN")+": " + var);
			}else
				stats.increment(local_is_fun ? "FUN0FUN" : "VAR0VAR");
		}
		super.close_context();
	}
	public boolean is_function(String name){
		return ((ScopeContext) get_context()).is_function(name);
	}
	public void display_status(String name){
		System.out.println("accessing "+name+": it is a "+(get_context().is_function(name) ? "function" : "variable"));
	}
	
	public void load_context(String fname){
		try{
			new FileTools.ListFileReader(fname) {
				@Override
				protected void parse_line(String line) {
					add_fun(line);
				}
			};
		}
		catch(EOFException e){}
		catch(Exception e){
			System.err.println("Unable to open file global name file: "+fname);
		}
	}
	////////////////////////////////////////////////////////////////////////////
	public ScopeContext get_context(){
		return (ScopeContext) context;
	}
	public ScopeContext sub_context(Context ctx) {
		return new ScopeContext(ctx);
	}
	public class ScopeContext extends Context{
		Map<String, Boolean> is_function = new LinkedHashMap<String, Boolean>();
		public ScopeContext(Context ctx) {
			super(ctx);
		}
		public Context add_var(String name){
			throw new RuntimeException("Cannot use add_var directly");
		}
		public void add_var(String name, boolean is_fun){
			Boolean previous = is_function.get(name);
			if(previous != null && is_fun != previous)
				stats.increment(is_fun ? "FUN0VAR_LOCAL" : "VAR0FUN_LOCAL");
			super.add_var(name); // FIXME ce que c'est moche le java
			is_function.put(name, is_fun);
		}
		public boolean is_function(String name){
			ScopeContext ctx = (ScopeContext) lookup(name);
			if(ctx == null)
				return true; // By default we assume that's the name is a global function
			Boolean b = ctx.is_function.get(name);
			assert b != null;
			return b;
		}
	}
}
package org.rx.analyser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.TreeNodeStream;

abstract public class Unused extends BasicTreeVisitor implements ASTProcessor {
	int skip = 0;
	public Unused(TreeNodeStream input) {
		super(input);
	}

	public Unused(TreeNodeStream input, RecognizerSharedState state) {
		super(input, state);
	}

	public void process_tree(CommonTree tree) throws Exception {
		downup(tree,false);
		report();
	}
	
	public void read_var(String var){
		System.out.println("read var "+var+ ":"+skip);
		if(skip != 0){
			skip --;
			return;
		}
		UnusedContext ctx = (UnusedContext)get_context().lookup(var);
		if(ctx != null)
			ctx.increment(var);
		// else ; // TODO what to do if the variable is read but never assigned, probably warn
	}

	public void write_var(String var, boolean super_assign){
		skip ++;
		System.out.println("write var "+var+ ":"+skip);
		if(super_assign)
			get_context().super_add_var(var);
		else
			get_context().add_var(var);
	}
	
	public void report(){
		((UnusedContext)get_context()).report();
	}
	
	public UnusedContext sub_context(Context ctx) {
		return new UnusedContext(ctx);
	}
	public UnusedContext getContext(Context ctx) {
		return (UnusedContext)(context);
	}
	
	class UnusedContext extends Context{
		Map<String, Integer> used;
		UnusedContext(Context ctx) {
			super(ctx);
		}
		
		protected void init_context(){
			used = new LinkedHashMap<String, Integer>();
			vars = used.keySet();
		}

		void increment(String var) {
			Integer vv = used.get(var);
			int v = 1;
			if(vv != null)
				v += vv;
			used.put(var, v);
		}
		protected void report(){
			for(Entry<String, Integer> e: used.entrySet()){
				int v = e.getValue();
				if(v == 0)
					System.out.println(e.getKey());
			}
		}
		public UnusedContext add_var(String name){ 
			used.put(name, 0);
			return this;
		}
	}
}

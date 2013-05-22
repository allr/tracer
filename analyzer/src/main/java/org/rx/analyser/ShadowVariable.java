package org.rx.analyser;

import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;

public abstract class ShadowVariable extends BasicContextProcessor {
	private final boolean DEBUG = Boolean.parseBoolean(System.getProperty("analyser.shadower.debug", "false"));
	protected StatContext stats;

	public ShadowVariable(TreeNodeStream input) {
        this(input, new RecognizerSharedState());
    }
    public ShadowVariable(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
        stats = new StatContext(false);
    }
	protected ShadowContext get_context() { return (ShadowContext)super.get_context(); }
	public ShadowContext global(){ return (ShadowContext) global_context;	}
	protected ShadowContext init_global_context(){
		return new FunctionContext(null);
	}
    protected void add_var(String var){
		if(DEBUG)
			System.err.println("Add: "+var);
    	get_context().add_var(var);
    }
    protected void access_var(String var){
		boolean is_shadowed = ((ShadowContext)context).is_shadowed(var);
		if(is_shadowed)
			stats.increment("shadowed");
		else
			stats.increment("unshadowed");
		if(DEBUG)
			System.out.println("status of "+var+": "+(is_shadowed ? "shadowed" : "static"));
	}
    protected ShadowContext function_context(){
    	ShadowContext ctx = new FunctionContext(get_context());
    	set_context(ctx);
    	return ctx;
    }
    protected CompositeContext if_context(){
    	CompositeContext ctx = new CompositeContext((ShadowContext)get_context(), 2);
    	ctx.next();
    	return ctx;
    }
    protected CompositeContext repeat_context(){
    	CompositeContext ctx = new SureContext((ShadowContext)get_context());
    	set_context(ctx);
    	return ctx;
    }
    protected ShadowContext while_context(){
    	UnsureLoopContext ctx = new UnsureLoopContext((ShadowContext)get_context());
    	set_context(ctx);
    	return ctx;
    }
    protected class ShadowContext extends Context {
		Set<String> unintialized_vars;
		public ShadowContext(ShadowContext shadowContext) {
			super(shadowContext);
		}

		public Set<String> get_uninitialized_vars(){ return unintialized_vars; }

		public boolean is_shadowed(String name){
			if(unintialized_vars.contains(name))
				return true;
			else if(has_var(name))
				return false;
			else if (parent == null)
				return false;
			return ((ShadowContext)parent).is_shadowed(name);
		}
		public Context add_var(String name){ 
			unintialized_vars.remove(name);
			vars.add(name);
			return this;
		}
		protected void merge_context(Context context){
			unintialized_vars.addAll(((ShadowContext) context).get_uninitialized_vars());
		}
		protected void init_context(){
			super.init_context();
			unintialized_vars = new LinkedHashSet<String>();
		}
		public void dump(PrintStream out){
			super.dump(out);
			out.println("\tuninit: "+unintialized_vars.toString());
		}
		public void escape() {
			if(parent != null)
				((ShadowContext)parent).escape();
		}
	}
    protected class SureContext extends CompositeContext {
		public SureContext(ShadowContext shadowContext) {
			super(shadowContext, 0);
		}

		@Override
		public void escape() {
			if(current == 0){
				subs = alt_context(2);
				next();
			}else
				next_extend();
		}
    }
    protected class UnsureLoopContext extends ShadowContext {
		public UnsureLoopContext(ShadowContext shadowContext) {
			super(shadowContext);
		}
		protected ShadowContext end_context(){
			unintialized_vars.addAll(vars);
			vars.clear();
			return (ShadowContext)parent;
		}
		@Override
		public void escape() {
			// Nothing to do, since the whole ctx is already unsure
		}
	}
    protected class FunctionContext extends ShadowContext {
		public FunctionContext(ShadowContext shadowContext) {
			super(shadowContext);
		}
		protected ShadowContext end_context(){
			unintialized_vars.clear();
			vars.clear();
			return (ShadowContext)parent;
		}
	}
    protected class CompositeContext extends ShadowContext {
    	ShadowContext[] subs;
		int current = 0;
		public CompositeContext(ShadowContext shadowContext, int nb_subs) {
			super(shadowContext);
			subs = alt_context(nb_subs);
			set_context(this);
		}
		public void next(){
			assert current < subs.length;
			set_context(subs[current++]);
		}
		public void next_extend(){
			if(current == subs.length){
				ShadowContext[] new_subs = new ShadowContext[current]; // TODO maybe it's not efficients , but this case are rare
				System.arraycopy(subs, 0, new_subs, 0, current - 1);
				new_subs[current] = new ShadowContext(this);
				subs = new_subs;
			}
			next();
		}
		protected ShadowContext[] alt_context(int nb){
			ShadowContext[] contexts = new ShadowContext[nb];
			while(nb > 0)
				contexts[-- nb] = new ShadowContext(this);
			return contexts;
		}
		@Override
		public ShadowContext end_context(){
			merge_contexts(subs);
			return (ShadowContext)parent;
		}
		protected void merge_contexts(ShadowContext[] contexts){
			// This method destroy it's parameter
			for(int i = 0 ; i < contexts.length ; i ++){
				ShadowContext context = contexts[i];
				for(String var: context.get_vars()){
					boolean found = true;
					for(int j = i+1 ; j < contexts.length; j ++){
						Context other = contexts[j]; 
						if(!other.remove_var(var))
							found = false;
					}
					
					found_in_other(found, var);
				}
				merge_context(context);
			}
		}
		protected void found_in_other(boolean found, String var){
			if(found){
				unintialized_vars.remove(var);
				vars.add(var);
			} else if(!vars.contains(var)) // FIXME BIG doubt on this rule
				unintialized_vars.add(var);
		}
	}
}

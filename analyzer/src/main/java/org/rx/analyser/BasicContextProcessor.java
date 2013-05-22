package org.rx.analyser;

import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;

public abstract class BasicContextProcessor extends BasicTreeParser implements ASTProcessor {
	public final Context global_context = init_global_context();
	protected Context context = global();
	
	public BasicContextProcessor(TreeNodeStream input) {
		this(input, new RecognizerSharedState());
	}
	public BasicContextProcessor(TreeNodeStream input, RecognizerSharedState state) {
		super(input, state);
	}
	
	// BEGIN fix for changeset f85028b9f7e6
	public Context sub_context(Context ctx) {
		return new Context(ctx);
	}

	protected void open_context() {
		Context ctx = sub_context(get_context());
		set_context(ctx);
	}

	protected void close_context() {
		Context ctx = get_context();
		ctx.close_context();
	}
	// END fix for changeset f85028b9f7e6
	
	protected Context get_context() { return context; }
	public Context global(){ return global_context;	}
	protected Context init_global_context(){
		return sub_context(null);
	}
	protected void set_context(Context new_context) {
		context = new_context;
	}
	public class Context {
		Context parent;
		Set<String> vars;
		
		public Context get_parent(){ return parent; }
		public Set<String> get_vars(){ return vars; }

		
		public boolean is_global() { return this == global_context;}
		
		public Context add_var(String name){ // FIXME this method must be visible just by the enclosing class !!!
//			if(is_global() && has_var(name))
//				stats.increment("COLLAPSE");
			vars.add(name);
			return this;
		}
		
		public Context super_add_var(String name){ // FIXME this method must be visible just by the enclosing class !!!
			Context found = null;
			if(parent != null)
				found = parent.lookup(name);
//			if(found != null && found.is_global())
//				stats.increment("COLLAPSE");
			if(found == null)
				found = global_context;
			found.add_var(name);
			return found;
		}
		
		public void close_context(){
			Context ctx = end_context();
			ctx.merge_context(this);
			set_context(ctx);
		}
		protected void merge_context(Context context){
		}
		protected Context end_context(){
			return parent;
		}
		public boolean has_var(String name){
			return vars.contains(name);
		}
		public boolean remove_var(String name){
			return vars.remove(name);
		}
		public Context lookup(String name){
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
		protected Context(Context ctx){
			init_context();
			parent = ctx;
		}
		protected void init_context(){
			vars = new LinkedHashSet<String>();
		}
		//protected Context(Context prev) { this(); parent = prev; }
	}
}

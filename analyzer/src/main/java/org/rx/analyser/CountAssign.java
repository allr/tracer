package org.rx.analyser;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.rx.FileTools;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;

abstract public class CountAssign extends BasicContextProcessor {
	protected final boolean DEBUG = Boolean.parseBoolean(System.getProperty("analyser.countassign.debug", "false"));
	protected Set<String> calls_counted = new LinkedHashSet<String>();
	protected Context ctx = global();
	protected StatContext stats;
	
	public CountAssign(TreeNodeStream input) {
        this(input, new RecognizerSharedState());
    }
    public CountAssign(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
        stats = new StatContext(false);
    }
    public boolean add_assign(String name){
    	return add_assign(name, get_context());
    }
    public boolean add_assign(String name, Context super_ctx){
    	boolean ok = calls_counted.contains(name);
    	
    	if(ok){
    	/*	Context ctx = (super_ctx == null) ? null : super_ctx.lookup(name);
    		if(ctx == null || ctx.is_global()){*/
    			stats.increment(name);
    			return true;
/*    		} else {
    			stats.increment("HIDDEN");
    		}*/
    	}
    	return false;
    }    
	@Override
	public void initialize_processor(String[] opts) throws Exception {
		if(DEBUG)
			System.out.println("Loading map file: "+opts[0]);
		try{
			calls_counted.add("IGNORED");
			new FileTools.ListFileReader(opts[0]) {
				@Override
				protected void parse_line(String line) {
					calls_counted.add(line);
				}
			};
		}catch(IOException e){
			System.err.println("Cannot load counter mapfile: "+opts[0]);
		}
		if(DEBUG)
			System.out.println("Map file loaded");
	}
}

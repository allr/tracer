package org.rx.analyser;

import java.io.EOFException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;
import org.rx.FileTools;

public abstract class CountCall extends BasicContextProcessor {
	protected final boolean DEBUG = Boolean.parseBoolean(System.getProperty("analyser.countcall.debug", "false"));
	protected Set<String> calls_counted = new LinkedHashSet<String>();
	protected Context ctx = global();
	protected StatContext stats;

	public CountCall(TreeNodeStream input) {
        this(input, new RecognizerSharedState());
    }
    public CountCall(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
        stats = new StatContext(false);
    }
    public boolean add_call(String name){
    	return add_call(name, get_context());
    }
    public boolean add_call(String name, Context super_ctx){
    	boolean ok = calls_counted.contains(name);
    	
    	if(ok){
    		Context ctx = (super_ctx == null) ? null : super_ctx.lookup(name);
    		if(ctx == null || ctx.is_global()){
    			stats.increment(name);
    			return true;
    		} else {
    			stats.increment("HIDDEN");
    		}
    	}
    	return false;
    }    
    public void add_contains_call(boolean contains){
    	if(contains)
    		stats.increment("HAS");
    	else
    		stats.increment("HAS_NOT");
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
		}catch(EOFException e){}
		if(DEBUG)
			System.out.println("Map file loaded");
	}
}

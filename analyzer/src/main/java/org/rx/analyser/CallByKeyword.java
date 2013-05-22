package org.rx.analyser;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;

public abstract class CallByKeyword extends TreeStatParser{
	public static final int MAX_PARAM = Integer.parseInt(System.getProperty("analyser.callbykeyword.maxparam", "20"));
	protected final boolean DEBUG = Boolean.parseBoolean(System.getProperty("analyser.callbykeyword.debug", "false"));
		
	protected int keyword_args, normal_args;
 	
	public CallByKeyword(TreeNodeStream input) {
        this(input, new RecognizerSharedState());
    }
    public CallByKeyword(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
    }

    protected void add_call(int nb_keywords, int nb_params, boolean by_name){
    	if(nb_keywords >  MAX_PARAM)
    		nb_keywords = MAX_PARAM;
    	if(nb_keywords > 0)
    		stats.increment("CallWithKeyword");
		stats.add("TotalKeyword", nb_keywords);
		if(nb_params - nb_keywords > 0)
    		stats.increment("CallWithPosition");			
		stats.add("TotalPosition", nb_params);
		stats.increment("Calls");
		if(by_name)
			stats.increment("CallsByName");

    	stats.increment(Integer.toString(nb_keywords));
    }
    public void finalize_processor(){
    	print_stats(System.out);
    }
}

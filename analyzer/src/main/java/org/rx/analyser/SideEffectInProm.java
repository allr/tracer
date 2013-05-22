package org.rx.analyser;

import java.util.HashSet;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.TreeNodeStream;
import org.rx.analyser.parser.RParser;

public abstract class SideEffectInProm extends BasicTreeVisitor {
	long in_call = 0;
	 HashSet<String> functions = new HashSet<String>();

	public SideEffectInProm(TreeNodeStream input) {
		this(input, new RecognizerSharedState());
	}
    public SideEffectInProm(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
    }

    protected void init(){
    }
    public void finalize_processor(){
    	System.out.println(functions);
    	System.out.println("SideEffectInCall: "+in_call);
    }
    
    protected void may_add_assign(CommonTree node){
    	CommonTree n = (CommonTree)(node.getParent()); // Grrr the true node
    	int type = -1;
    	while(n != null && (
    			(type = n.getType()) != RParser.CALL) &&
    			type != RParser.FUNCTION){
    		n = (CommonTree)n.getParent();
    	}
    	if(n == null) return;
    	if(type == RParser.CALL){
    		in_call ++;
    		functions.add(n.getChild(0).getText());
    	}
    }
}

tree grammar TokenCounter;


options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.BasicTreeVisitor';
  filter = true;
}

@header {
package org.rx.analyser.parser;
  
import org.rx.analyser.*;
}
@members {
	protected StatContext stats;

	public void process_tree(CommonTree tree) throws Exception {
	    if(stats == null)
	    	stats = new StatContext(false);
		downup(tree,false);
	}
}
@rulecatch {
	catch(RecognitionException re){
		throw re; // Stop at first error ??? Doesn't work at all ??? why ??
	}
}

topdown
	: v=. { stats.increment(tokenNames[$v.getType()]); if(false) throw new RecognitionException(); }
	;

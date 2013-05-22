tree grammar SideEffectInProm;


options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.SideEffectInProm';
  filter = true;
}

@header {
package org.rx.analyser.parser;
  
import org.rx.analyser.*;
}
@members {
	public void process_tree(CommonTree tree) throws Exception {
		init();
		downup(tree, false);
	}
}
@rulecatch {
	catch(RecognitionException re){
		throw re; // Stop at first error ??? Doesn't work at all ??? why ??
	}
}

topdown
	: ass
	;
bottomup
	:
	;

ass
	: ^(ARROW a=. . {may_add_assign($a);})
	| ^(SUPER_ARROW b=. . {may_add_assign($b);})
	;
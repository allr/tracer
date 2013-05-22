tree grammar ResolvName;

options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.ResolvName';
  filter = true;
}

@header {
package org.rx.analyser.parser;
  
import org.rx.analyser.*;
}
@members {
	public void process_tree(CommonTree tree) throws Exception {
		setSourceName();
		//script();
		downup(tree, false);
	}
}
@rulecatch {
	catch(RecognitionException re){
		throw re; // Stop at first error ??? Doesn't work at all ??? why ??
	}
}

topdown
	: assign
	| function
	;
bottomup
	:
	;

assign
	: ^(ARROW i=ID ^(fun=FUNCTION .*) { System.out.println("found "+$i.text);  if(isTopLevel($fun)) add_function($i.text, $fun);})
	| ^(SUPER_ARROW i=ID ^(fun=FUNCTION .*) { add_function($i.text, $fun);})
	;
	
function
	: ^(fun=FUNCTION   { System.out.println("found lambda"); add_function("lambda", $fun);} .*)
	;
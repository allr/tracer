tree grammar ClassHierarchy;

options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.ClassHierarchy';
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
	: call
	| assign_fun
	| assign_class
	;
bottomup
	:
	;

assign_fun
	: ^(ARROW i=ID {add_name($i.text);}.*)
	| ^(SUPER_ARROW si=ID {add_name($si.text);}.*)
	;
assign_class
	: ^(ARROW ^(CALL classcall=ID .) classname=. {add_class($classcall.text, $classname);})
	| ^(SUPER_ARROW ^(CALL sclasscall=ID .) sclassname=. {add_class($sclasscall.text, $sclassname);})
	;
call
	: ^(CALL i=ID {start_call($i.text);} arg* {end_call($i.text);})
	;
	
arg	: ^(KW id=(ID|STRING) kw=. {add_kw($id.text, kw);})
	| v=. {add_arg(v);}
	;
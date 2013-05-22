tree grammar Unused;

options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.Unused';
  filter = true;
}

@header {
package org.rx.analyser.parser;
  
import org.rx.analyser.*;
}
@members {
}
@rulecatch {
	catch(RecognitionException re){
		throw re; // Stop at first error ??? Doesn't work at all ??? why ??
	}
}

topdown
	: ^(SEQUENCE { open_context(); } .*  { report(); close_context() ; } )
	| ID { read_var($ID.text) ; }
    | ^(ARROW ID {  write_var($ID.text, false); } .* )
    | ^(SUPER_ARROW ID { write_var($ID.text, false); } .* ) 	
	;

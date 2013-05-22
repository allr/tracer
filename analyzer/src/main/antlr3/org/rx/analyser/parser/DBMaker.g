tree grammar DBMaker;

options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.DBMaker';
  filter = false;
}

@header {
package org.rx.analyser.parser;
  
import java.sql.SQLException;
import org.rx.analyser.*;
}
@members {
	public void process_tree(CommonTree tree) throws Exception {
		setSourceName();
		script();
	}
}
@rulecatch {
	catch(RecognitionException re){
		throw re; // Stop at first error ??? Doesn't work at all ??? why ??
	}
}


script: expr+;

expr: id
    | assign 
    | unary_expression
    | binary_expression
    | if_expr
    | function
   	| NEXT
   	| BREAK
   	| NUMBER
   	| TRUE
   	| FALSE
   	| field
   	| at
   	| braket
   	| call
   	| while_expr
   	| for_expr
   	| repeat_expr
   	| sequence
   	| DD
   	| NULL
    ;
id	: ID
	| VARIATIC
	| STRING
	| ^(NS_GET id id)
	| ^(NS_GET_INT id id)
	;
assign
	: (^(ARROW ID ^(FUNCTION .*)))=>
		^(ARROW i=ID { set_fn_name($i.text); } value=function) // {close_context();}) 
	| (^(ARROW STRING ^(FUNCTION .*)))=>
		^(ARROW s=STRING { set_fn_name($s.text);} value=function) // {close_context();}) 
	| (^(ARROW . ^(FUNCTION .*)))=>
		^(ARROW e=expr  { set_fn_name("["+$e.start+"]") ;} value=function) // {close_context();})
	| ^(ARROW expr value=expr) 
	| ^(SUPER_ARROW expr value=expr)
	;
field
	: ^(FIELD expr expr)
	;
at	: ^(AT expr expr)
	;
braket
	: ^((BRAKET|LBB) expr expr_list*) 
	;
expr_list
	: expr
	| ^(KW id expr)
	;
function
	:	^(loc=FUNCTION {emit_fun($loc);}
			parms=param* {open_named("");} body=expr {close_context();})
	;
param
	: ^(i=ID {open_function_kw($i);} value=expr {close_context();})
	| VARIATIC
	;
call: ^(CALL e=expr arg*)
	; // Traiter a part le cas de assign
arg
	: {open_call_position($arg.start);} expr {close_context();}
	| ^(KW i=id {open_call_kw($i.start);} expr {close_context();})
	| ^(KW n=NULL {open_call_kw($n);} expr {close_context();})
	;
if_expr
	: ^(IF cond=expr t=expr (f=expr)?)
	;
while_expr
	: ^(WHILE expr expr)
	;
for_expr
	: ^(FOR ID expr expr)
	;
repeat_expr
	: ^(REPEAT expr)
	;
sequence
	: ^(SEQUENCE expr*)
	;
basic_expression
	: ^(binary_operator expr expr)
    | ^(unary_operator expr)
    ;

binary_expression
	: ^(binary_operator expr expr)
	;
unary_expression
	: ^(unary_operator expr)
	;
unary_operator
	: UPLUS
	| UMINUS
	| UTILDE
	| NOT
	;
binary_operator
	: OR
	| BITWISEOR
	| AND
	| BITWISEAND
	| EQ
	| NE
	| GE
	| LE
	| GT
	| LT
	| COLUMN
	| CARRET
	| TILDE
	| PLUS
	| MULT
	| MOD
	| DIV
	| MINUS
	| OP
	;
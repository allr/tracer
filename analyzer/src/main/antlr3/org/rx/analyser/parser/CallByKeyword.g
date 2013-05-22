tree grammar CallByKeyword;

options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.CallByKeyword';
}
@header {
package org.rx.analyser.parser;
  
import org.rx.analyser.*;
}
@members {
	public void process_tree(CommonTree tree) throws Exception {
		script();
	}
}
/*
@rulecatch {
	catch(RecognitionException e){
		throw e; 
	}
}
*/
//////////////////////////////////////////////////////////////////////////////////////////////

script: expr+;

expr returns [ boolean is_id ]
	: id { $is_id = true;}
	| assign 
    | NULL
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
    ;
id	: ID
	| VARIATIC
	| STRING
	| ^(NS_GET id id)
	| ^(NS_GET_INT id id)
	;
assign
	: ^(ARROW expr value=expr) 
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
	: ^(FUNCTION  parms=param* body=expr)
	;
param
	: ^(name=ID value=expr)
	| VARIATIC
	;
call:	{ int nb_keyword = 0; int nb_param = 0; }
		^(CALL e=expr (a=arg { nb_keyword += $a.is_keyword ? 1 : 0; nb_param ++;} )*)
		{ add_call(nb_keyword, nb_param, $e.is_id); }
	;
arg returns [ boolean is_keyword ]
	: ^(KW (id|NULL) expr) { keyword_args ++; $is_keyword = true;}
	| expr { normal_args ++; $is_keyword = false; }
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
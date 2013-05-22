tree grammar NopTree;

options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.TreeStatParser';
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
@rulecatch {
	catch(RecognitionException re){
		throw re; // Stop at first error ??? Doesn't work at all ??? why ??
	}
}
//////////////////////////////////////////////////////////////////////////////////////////////

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
	://	{open_context();}
		^(FUNCTION  parms=param* body=expr)
	//	{close_context();}
	;
param
	: ^(name=ID value=expr)// { add_var($name.text);}
	| VARIATIC
	;
call: ^(CALL expr arg*)
	;
arg
	: expr
	| ^(KW id expr)
	| ^(KW NULL expr)
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
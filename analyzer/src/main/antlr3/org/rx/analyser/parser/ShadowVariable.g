tree grammar ShadowVariable;

options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.ShadowVariable';
}

@header {
package org.rx.analyser.parser;
  
import org.rx.analyser.*;
}
@members {
	public void process_tree(CommonTree tree) throws Exception {
		script();
    	stats.print(System.out);
	}
}
//@rulecatch {}
//////////////////////////////////////////////////////////////////////////////////////////////
script: expr+;

expr: id
	| assign 
    | unary_expression
    | binary_expression
    | if_expr
    | function
   	| NEXT {get_context().escape();}
   	| BREAK {get_context().escape();}
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
id	: a=ID { access_var($a.text); }
	| VARIATIC
	| STRING
	| ^(NS_GET id id)
	| ^(NS_GET_INT id id)
	;
assign
	: ^(ARROW assign_expr value=expr) 
	| ^(SUPER_ARROW assign_expr value=expr)
	;
assign_expr
	: assign_id
	| assign 
    | unary_expression
    | binary_expression
    | if_expr
    | function
   	| NEXT {get_context().escape();}
   	| BREAK {get_context().escape();}
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
assign_id
	: a=ID { add_var($a.text); }
	| VARIATIC
	| STRING
	| ^(NS_GET id id)
	| ^(NS_GET_INT id id)
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
	:	{ ShadowContext ctx = function_context(); }
		^(FUNCTION  parms=param* body=expr)
		{ ctx.close_context(); }
	;
param
	: ^(name=ID value=expr)  { add_var($name.text); }
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
	:	^(IF cond=expr
		{ CompositeContext ctx = if_context();}
			t=expr
		{ ctx.next(); }
			(f=expr)?
		{ ctx.close_context(); }
		)
	;
while_expr
	: ^(WHILE
	{ ShadowContext ctx = while_context(); }
		expr expr
	{ ctx.close_context(); }
	)
	;
for_expr
	: ^(FOR ID expr
		expr
	)
	;
repeat_expr
	: ^(REPEAT
		{ ShadowContext ctx = repeat_context(); }
		expr
		{ ctx.close_context(); }
	)
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
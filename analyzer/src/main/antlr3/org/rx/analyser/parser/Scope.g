tree grammar Scope;

options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.Scope';
}

@header {
package org.rx.analyser.parser;
  
import org.rx.analyser.*;
}
@members {
	public void process_tree(CommonTree tree) throws Exception {
		load_context("global.names");
		open_context();
		script();
		close_context();
	}
}
@rulecatch {
	catch(RecognitionException re){
		throw re; // Stop at first error ??? Doesn't work at all ??? why ??
	}
}
//////////////////////////////////////////////////////////////////////////////////////////////

script: expr+;

expr: expr_no_fun
    | function
    ;
id	: idname
	| VARIATIC
	| ^(NS_GET idname name=idname)
	| ^(NS_GET_INT idname name=idname)
	;
idname
	: ID
	| STRING
	;
assign
	:	( ^(ARROW id_or_string .*))=>(
			(^(ARROW name=id_or_string expr_no_fun) {add_var($name.text);})
			| (^(ARROW name=id_or_string function) {add_fun($name.text);})
		)
	| ^(ARROW lhs=expr rhs=expr) { System.out.println($lhs.text+"(:"+tokenNames[$lhs.start.getToken().getType()]+":"+tokenNames[$rhs.start.getToken().getType()]+":)\t"+$text); }
	| ^(SUPER_ARROW expr value=expr)
	;
id_or_string
	: ID
	| STRING
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
	:	{open_context();}
		^(FUNCTION  parms=param* body=expr)
		{close_context();}
	;
param
	: ^(name=ID fun=function) { add_fun($name.text);}
	| ^(name=ID value=expr_no_fun) { add_var($name.text);}
	| VARIATIC
	;
expr_no_fun
	: ID
	| VARIATIC
	| STRING
	| ^(NS_GET id name=id)
	| ^(NS_GET_INT id name=id)
	| assign 
    | unary_expression
    | binary_expression
    | if_expr
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
call: ^(CALL 
		((id)=>	(name=(ID|STRING) { if(is_function($name.text)) stats.increment("CALL"); else { stats.increment("VAR");  DEBUG($name.text);} }
				| ^((NS_GET | NS_GET_INT) idname cname=idname) { stats.increment("CALL"); }
				| VARIATIC { stats.increment("VARIATIC"); }
				)
		| e=expr { stats.increment("COMPLEX"); DEBUG("complex: "+ $e.text); }
		)
		arg*) 
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
	: ^(FOR vname=ID expr { open_context(); add_var($vname.text); } expr { close_context(); })
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
tree grammar CGBuilder;

options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.TreeStatParser';
}

@header {
package org.rx.analyser.parser.callgraph;
  
//import org.rx.analyser.*;
import org.rx.analyser.callgraph.*;
}
@members {
	CGNode call_stack[] = new CGNode[100];
	int sp;
	
	static int id = 1;
	static CallGraph cg = new CallGraph();
	Context<Object> ctx = new Context<Object>();
	public Context<Object> get_context(){ return ctx; }

	public void process_tree(CommonTree tree) throws Exception {
		sp = 0;
		call_stack[sp] = cg.get_node("MAIN"); 
		script();
		cg.to_dot(System.out);
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
    | function[-1]
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
	: ^((ARROW|SUPER_ARROW) 
				( (ID ^(FUNCTION .*))=> (name=ID {get_context().add_var($name.text, id++);} function[id])
				| (. ^(FUNCTION .*))=> (expr {get_context().add_var($name.text, id++);} function[0])
			 	| expr expr
				)
		)
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
function[int id]
	: ^(FUNCTION  parms=param* body=expr)
	;
param
	: ^(name=ID value=expr)
	| VARIATIC
	;
call: ^(CALL 	(((ID)=> cid=ID { cg.add_link(call_stack[sp], $cid.text);})
				| expr { cg.add_link(call_stack[sp], "LAMBDA");}
				)
		arg*)
	;
arg
	: expr
	| ^(KW (id|NULL) expr)
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

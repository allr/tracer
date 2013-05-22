tree grammar CountRecursive;

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
	protected String[] callstack = new String[100]; // FIXME
	protected int sp = 0;
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
	: ^((ARROW|SUPER_ARROW) 
			(	(ID ^(FUNCTION .*))=> (name=ID { callstack[sp++] = $name.text;} function { sp--;})
				| (. ^(FUNCTION .*))=> (expr { stats.increment("LAMBDA-DEF");} function) 
			 	| expr expr
			)
		)
	//| ^(SUPER_ARROW name=ID value=function) {System.out.println("Super declare: "+$name);}
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
call: ^(CALL ((ID)=>name=ID
				{
					if($name.text.equals("Recall"))
						if(sp > 0)
							stats.increment(callstack[sp - 1]);
						else
							stats.increment("RECALL-TL");
					else{
						int cmpt = sp;
						while(cmpt-- > 0)
							if(callstack[cmpt].equals($name.text)){
								stats.increment($name.text);
								break;
							}
					}
				}
				| expr {stats.increment("LAMBDA-CALL");}) arg*)
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
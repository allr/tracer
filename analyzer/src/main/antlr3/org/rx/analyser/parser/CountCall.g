tree grammar CountCall;


options {
  language = Java;
  tokenVocab = R;
  ASTLabelType = CommonTree;
  superClass = 'org.rx.analyser.CountCall';
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

expr returns [boolean has_call]
	: id
	| as=assign { $has_call = $as.has_call; } 
    | u=unary_expression { $has_call = $u.has_call; }
    | b=binary_expression { $has_call = $b.has_call; }
    | i=if_expr { $has_call = $i.has_call; }
    | fu=function { $has_call = $fu.has_call; }
   	| fi=field { $has_call = $fi.has_call; }
   	| a=at { $has_call = $a.has_call; }
   	| bra=braket { $has_call = $bra.has_call; }
   	| c=call { $has_call = $c.has_call; }
   	| w=while_expr { $has_call = $w.has_call; }
   	| fo=for_expr { $has_call = $fo.has_call; }
   	| r=repeat_expr { $has_call = $r.has_call; }
   	| s=sequence { $has_call = $s.has_call; }
   	| DD
    | nil=NULL { $has_call = add_call($nil.text);}
   	| n=NEXT { $has_call = add_call($n.text);}
   	| br=BREAK { $has_call = add_call($br.text);}
   	| NUMBER
   	| TRUE
   	| FALSE
    ;
id	: ID
	| STRING
	| VARIATIC
	| ^(NS_GET id id)
	| ^(NS_GET_INT id id)
	;
assign returns [boolean has_call]
	: ^(ARROW ((ID)=>(i=ID { /*get_context().add_var($i.text);*/} ) | e=expr) value=expr)  { $has_call = add_call("<-") | $e.has_call | $value.has_call; }
	| ^(SUPER_ARROW ((ID)=>(i=ID {/*get_context().super_add_var($i.text);*/} ) | e=expr) value=expr) { $has_call = add_call("<<-" /*, get_context().get_parent()*/) | $e.has_call | $value.has_call; }
	;
field returns [boolean has_call]
	: ^(FIELD e=expr e2=expr) { $has_call = add_call("$") | $e.has_call | $e2.has_call; }
	;
at returns [boolean has_call]
	: ^(AT e=expr e2=expr) { $has_call = add_call("@") | $e.has_call | $e2.has_call; }
	;
braket returns [boolean has_call]
	: ^((BRAKET { $has_call = add_call("[["); }|LBB { $has_call = add_call("["); }) e=expr { $has_call |= has_call;} (el=expr_list { $has_call |= $e.has_call;})*) 
	;
expr_list returns [boolean has_call]
	: e=expr  { $has_call = $e.has_call; }
	| ^(KW id e=expr)  { $has_call = $e.has_call; }
	;
function returns [boolean has_call] // TODO what are we doing with params ?
	:	// {open_context();}
		^(kw=FUNCTION  parms=param* body=expr) { $has_call = add_call($kw.text); /*add_contains_call($body.has_call);*/ }
		// {close_context();}
	;
param
	: ^(name=ID value=expr) // { get_context().add_var($name.text);}
	| VARIATIC // {get_context().add_var("...");}
	;
call returns [boolean has_call]
	: ^(CALL c=call_expr { $has_call = $c.has_call;} (a=arg { $has_call |= $a.has_call;})* )
	;
call_expr returns [boolean has_call]
	: cname=ID { $has_call = add_call($cname.text); }
	| cs=STRING { $has_call = add_call($cs.text); }
	| ^(NS_GET id call_expr)
	| ^(NS_GET_INT id call_expr)
	| as=assign { $has_call = $as.has_call;}
    | u=unary_expression { $has_call = $u.has_call;}
    | b=binary_expression { $has_call = $b.has_call;}
    | i=if_expr { $has_call = $i.has_call;}
    | f=function { $has_call = $f.has_call;}
   	| DD { $has_call = add_call("DD"); }
   	| fi=field { $has_call = $fi.has_call;}
   	| a=at { $has_call = $a.has_call;}
   	| br=braket { $has_call = $br.has_call;}
   	| c=call { $has_call = $c.has_call;}
   	| w=while_expr { $has_call = $w.has_call;}
   	| fo=for_expr { $has_call = $fo.has_call;}
   	| r=repeat_expr { $has_call = $r.has_call;}
   	| s=sequence { $has_call = $s.has_call;}
    ;
arg  returns [boolean has_call]
	: e1=expr { $has_call = $e1.has_call; }
	| ^(KW (id|NULL) e2=expr) { $has_call = $e2.has_call; }
	;
if_expr returns [boolean has_call]
	: ^(kw=IF cond=expr t=expr (f=expr)?) { $has_call = add_call($kw.text);}
	;
while_expr returns [boolean has_call]
	: ^(kw=WHILE expr expr)  { $has_call = add_call($kw.text);}
	;
for_expr returns [boolean has_call]
	: ^(kw=FOR i=ID  expr /*{open_context(); get_context().add_var($i.text);}*/ expr /*{close_context();}*/)  { $has_call = add_call($kw.text);}
	;
repeat_expr returns [boolean has_call]
	: ^(kw=REPEAT expr)  { $has_call = add_call($kw.text);}
	;
sequence returns [boolean has_call]
	: ^(SEQUENCE (e=expr {$has_call |= $e.has_call;})*)  { $has_call |= add_call("{");}
	;
binary_expression  returns [boolean has_call]
	: ^(op=binary_operator expr expr) { $has_call = $op.has_call; }
	;
unary_expression  returns [boolean has_call]
	: ^(op=unary_operator expr)  { $has_call = $op.has_call; }
	;
unary_operator returns [boolean has_call]
	: op=(UPLUS
	| UMINUS
	| UTILDE
	| NOT) {$has_call =  add_call($op.text);}
	;
binary_operator returns [boolean has_call]
	: op=(OR
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
	| OP ) { $has_call = add_call($op.text);}
	;
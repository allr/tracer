package org.rx.analyser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.TreeNodeStream;
import org.rx.analyser.parser.RLexer;

public abstract class ClassHierarchy extends BasicTreeVisitor {
	CallArgs current_call;
	ArrayList<CallArgs> call_stack;
	HashSet<String> s3className;
	HashSet<String> s3methodsName;
	HashSet<String> s3potentialsName;
	
	final static String[][] calls = {
		{"setClass", "Class", "representation", "prototype", "contains"},
		{"setClassUnion", "name", "members", "where"},
		{"setMethod", "f", "signature", "definition", "where", "valueClass", "sealed"},
		{"setReplaceMethod", "f", "signature", "definition", "where", "valueClass", "sealed"},
		{"UseMethod", "generic", "object"}
	};
	final MethodValidator validators[] = {
			new MethodValidator(){ public void validate(CallArgs call){ validate_setClass(call, false);}},
			new MethodValidator(){ public void validate(CallArgs call){ validate_setClass(call, true);}},
			new MethodValidator(){ public void validate(CallArgs call){ validate_setMethod(call, false);}},
			new MethodValidator(){ public void validate(CallArgs call){ validate_setMethod(call, true);}},
			new MethodValidator(){ public void validate(CallArgs call){ validate_useMethod(call);}}
	};  
	public ClassHierarchy(TreeNodeStream input) {
		this(input, new RecognizerSharedState());
	}
    public ClassHierarchy(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
        s3className = new HashSet<String>();
    	s3methodsName = new HashSet<String>();
    	s3potentialsName = new HashSet<String>();
    }
    public void finalize_processor(){
		finalize_methods();
    }
    public void init(){
    	call_stack = new ArrayList<CallArgs>();
    	call_stack.add(current_call);
    }
    public void finalize_methods(){
    	for(String name: s3className)
    		System.out.println("(:s3class "+name+")");

    	for(String name: s3methodsName){
    		String types = "";
    		String prefix = name+".";
    		for(String match: s3potentialsName)
    			if(match.startsWith(prefix))
    				if(types == "")
    					types = "\""+match.substring(name.length()+1)+"\"";
    				else
    					types += " \""+match.substring(name.length()+1)+"\"";
    		System.out.println("(:generic \""+name+"\" ("+types+"))");
    	}
    }
    public void start_call(String call){
    	call_stack.add(current_call);
    	current_call = null;
    	for(int i = 0; i < calls.length; i++)
    		if(call.equals(calls[i][0])){
    			current_call = new CallArgs(i);
    			break;
    		}
    }
    public void end_call(String call){
    	if(current_call != null){
    		assert calls[current_call.call][0].equals(call);
    		current_call.validate();
    		validators[current_call.call].validate(current_call);
    	}
    	current_call = call_stack.remove(call_stack.size() - 1);
    }
    public void add_arg(CommonTree arg){
    	if(current_call != null)
    		current_call.pos.add(arg);
    }
    public void add_kw(String kw, CommonTree arg){
    	if(current_call != null){
    		current_call.kw.put(kw, arg);
    	}
    }
    public void add_name(String name){
    	if(name.contains("."))
    		s3potentialsName.add(name);
    }
    public void add_class(String callname, CommonTree cname){
    	if(callname.equals("class")){
    		switch (cname.getToken().getType()) {
			case RLexer.STRING:
					s3className.add(cname.getToken().getText());
				break;
			case RLexer.CALL:
				CommonTree call = (CommonTree)cname.getChild(0);
				if(call.getType() == RLexer.ID && call.getText().equals("c")){
					CommonTree child;
					for(int i = 1; i < cname.getChildCount(); i ++)
						if((child = (CommonTree)cname.getChild(i)).getType() == RLexer.STRING)
							s3className.add(child.getText());
				}
				break;
			}
    	}
    }
    
    public void validate_setClass(CallArgs call, boolean is_union){
    	CommonTree name = call.kw.get(is_union ? "name" : "Class");
    	boolean err = false;
    	if(name.getType() != RLexer.STRING){
    		System.err.println("Dynamic class: "+name+" "+name.getLine()+":"+name.getCharPositionInLine());
    		return;
    	}
    	CommonTree contains = call.kw.get(is_union ? "members" : "contains");
    	String buff = "";
    	if(contains != null){
    		switch(contains.token.getType()){
    			case RLexer.ID:
    				err = true;
    			case RLexer.STRING:
					buff = "\""+contains.token.getText()+"\"";    				
    			break;
    			case RLexer.CALL:
    				CommonTree call_name = (CommonTree)contains.getChild(0);
    				if(call_name.getToken().getType() != RLexer.ID &&
    						!call_name.getToken().getText().equals("c"))
    					throw new RuntimeException("Only c() is supported");
    				for(int i = 1; i < contains.getChildCount(); i++){
    					CommonTree child = (CommonTree) contains.getChild(i); 
    					if(child.getType() != RLexer.STRING)
    						err = true;
    					buff += " \""+ child+"\"";
    				}
    			break;
    			default:
    				System.err.println(contains.token.getType()+": "+contains.token.getText());
    				throw new RuntimeException(contains.token.getType()+": "+contains.token.getText());
    		}
    	}
    	PrintStream out = System.out;
    	if(err)
        	out = System.err;
   		out.println("("+(is_union ? ":union":":class")+" \""+name+"\" ("+buff+"))");
    }
    public void validate_useMethod(CallArgs call){
    	CommonTree generic = call.kw.get("generic");
    	if(generic != null){
    		if(generic.getType() != RLexer.STRING)
    			System.err.println("Warning call to UseMethod without a string: " + generic.toStringTree());
    		else
    			s3methodsName.add(generic.getText());
    	}
    }
    public void validate_setMethod(CallArgs call, boolean is_replace){
    	CommonTree name = call.kw.get("f");
    	boolean err = false;
    	if(name.getType() == RLexer.NS_GET || name.getType() == RLexer.NS_GET_INT)
    		name = (CommonTree)name.getChild(1);
    	if(name.getType() == RLexer.ID){
    		System.err.println("Warning method: "+name+" is define by another one at line "+name.getLine()+":"+name.getCharPositionInLine()+". We assume the name is correct !");    		
    	} else if(name.getType() != RLexer.STRING){
    		System.err.println("Dynamic method: "+name+" "+name.getLine()+":"+name.getCharPositionInLine());
    		return;
    	}
    	CommonTree contains = call.kw.get("signature");
    	String buff = "";
    	if(contains != null){
    		switch(contains.token.getType()){
    			case RLexer.ID:
    				err = true;
    			case RLexer.STRING:
					buff = "\""+contains.token.getText()+"\"";    				
    			break;
    			case RLexer.CALL:
    				CommonTree call_name = (CommonTree)contains.getChild(0);
    				if(call_name.getToken().getType() == RLexer.ID && call_name.getToken().getText().equals("match"))
    					call_name = (CommonTree)call_name.getChild(1);
    				if(call_name.getToken().getType() != RLexer.ID &&
    						!(
    								call_name.getToken().getText().equals("c")||
    								call_name.getToken().getText().equals("signature")))
    					throw new RuntimeException("Only c(), signature() or matchSignature is supported");
    				for(int i = 1; i < contains.getChildCount(); i++){
    					CommonTree child = (CommonTree) contains.getChild(i); 
    					switch(child.getType()){
    						case RLexer.STRING:
    							buff += " \""+ child+"\"";
							break;
    						case RLexer.KW:
    							buff += " \""+ child.getChild(1)+"\"";
    						break;
    						default:
    							err = true;
    							buff += " \""+ child+"\"";
    					}
    				}
    			break;
    			default:
    				System.err.println(contains.token.getType()+": "+contains.token.getText());
    				throw new RuntimeException(contains.token.getType()+": "+contains.token.getText()+" at line "+contains.getLine());
    		}
    	}
    	PrintStream out = System.out;
    	if(err)
        	out = System.err;
   		out.println("(:method \""+name+(is_replace? "<-" : "")+"\" ("+buff+"))");
    }
    
    interface MethodValidator {
    	void validate(CallArgs call);
    }
    
    static class CallArgs{
    	int call;
    	CallArgs(int call_id){
    		call = call_id;
    	}
    	ArrayList<CommonTree> pos = new ArrayList<CommonTree>();
    	Map<String, CommonTree> kw = new LinkedHashMap<String, CommonTree>();
    	
    	public String toString(){
    		if(pos.isEmpty())
        		return calls[call][0] + kw.toString();
    		return calls[call][0] + kw.toString()+" "+pos.toString();
    	}
    	
    	public void validate(){
    		int searchFrom = 1; // The first index is the call
    		final String cc[] = calls[call];
    		while(!pos.isEmpty()){
    			assert searchFrom < cc.length;
    			CommonTree t = pos.remove(0);
    			for(int i = searchFrom; i < cc.length; i++){
    				if(!kw.containsKey(cc[i])){
    					kw.put(cc[i], t);
    					searchFrom = i + 1;
    					t = null;
    					break;
    				}
    			}
    			if(t != null){
    				System.err.println("really weird "+t+": "+kw);
    				throw new RuntimeException("really weird "+t+": "+kw);
    			}
    		}
    	}
    }
}

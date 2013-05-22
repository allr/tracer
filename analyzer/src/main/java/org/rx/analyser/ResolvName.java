package org.rx.analyser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeNodeStream;
import org.rx.analyser.parser.RParser;

abstract public class ResolvName extends BasicTreeVisitor {
	String source_name;
	Map<String, ArrayList<FunctionLocation>> seen = new LinkedHashMap<String, ArrayList<FunctionLocation>>();
	
	public ResolvName(TreeNodeStream input) {
		this(input, new RecognizerSharedState());
	}
    public ResolvName(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
    }
    public boolean isTopLevel(CommonTree fun){
    	Tree anc = fun.getParent();
    	while(anc != null && anc.getType() != RParser.FUNCTION){
    		anc = anc.getParent();
    	}
    	return anc == null;
    }
    public void setSourceName(){
    	String name = getSourceName();
    	if(name.endsWith("all.R")){
    		int len = name.length() - 7;
    		name = name.substring(name.lastIndexOf('/', len)+1, len+1);
    	}
    	source_name = name;
    }
    
    public void add_function(String fun_name, CommonTree fun_start){
    	FunctionLocation fun = new FunctionLocation(fun_name, source_name, fun_start.getLine(),fun_start.getCharPositionInLine()+1);
    	ArrayList<FunctionLocation> prec = seen.get(fun_name);
    	if(prec == null)
    		prec = new ArrayList<FunctionLocation>();
    	prec.add(fun);
    	seen.put(fun_name, prec);
    }
    
	public void print_stats(PrintStream out, StatContext stats) {
		for(ArrayList<FunctionLocation> cf: seen.values()){
			out.print(cf.get(0).name);
			for(FunctionLocation fun: cf)
				out.print(" "+fun);
			out.println("");
		}
	}

    class FunctionLocation {
    	String name;
    	String source;
    	int line;
    	int column;
    	public FunctionLocation(String name, String source, int line, int column) {
    		this.name = name;
    		this.source = source;
    		this.line = line;
    		this.column = column;
		}
    	
    	public String toString(){
    		return source + ":"+line+":"+column;
    	}
    }
}

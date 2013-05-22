package org.rx.analyser;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeNodeStream;
import org.rx.DataBase;
import org.rx.RLibrary;
import org.rx.analyser.parser.RLexer;
import org.rx.analyser.parser.RParser;

public abstract class DBMaker extends TreeStatParser {
	int source;
	protected int sp;
	protected String stack[] = new String[100]; // FIXME
	protected Connection database;
	
	public DBMaker(TreeNodeStream input) {
		this(input, new RecognizerSharedState());
	}
    public DBMaker(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
    }
    
    public void initialize_processor(String[] opts) throws Exception{
    	
    }
  
	public void print_stats(PrintStream out, StatContext stats) {}
    public void setSourceName() throws SQLException {
    	String name = getSourceName();
    	if(name == null)
    		name = "stdin";
    	name = file_name(name);
    	sp = 0;
    	stack[sp] = null;
    	int lib = DataBase.register_library(database, RLibrary.relative_to_lib(name));
    	source = DataBase.register_file(database, lib, name);
    }
        
	public String file_name(String fname){
    	return RLibrary.source_to_relative(fname); //.substring(strip.length()+1);
    }
    public void set_fn_name(String name){
		stack[sp] = name;
    }
    public void open_named(String name){
		stack[sp++] = name;
    }
    public void open_call(CommonTree formal, String tag){
    	Tree call = formal.getAncestor(RLexer.CALL);
    	String parent = call.getChild(0).toString();
    	if(parent.equals("function"))
    		parent = "";
    	String name = parent + '?' + tag;
    	set_fn_name(name);
    }
    public void open_call_kw(CommonTree formal){
    	open_call(formal, formal.toString());
    }
    public void open_call_position(CommonTree formal){
    	open_call(formal, ""+formal.childIndex);
    }
    public void open_function_kw(CommonTree formal){
    	String name = formal.toString() + '=' + formal.childIndex;
    	open_named(name);
    }
    public void close_context(){
		stack[sp--] = null;
    }
    public void emit_fun(CommonTree fun){
    	try {
			/*final */String name = join_to(stack, sp, "#"); // compute the pretty name
			int line = fun.getLine();
			int column = fun.getCharPositionInLine() + 1;
			System.out.println("found at "+line+":"+column+": "+name);
			int id = DataBase.register_location(database, source, line, column);
			if(name != null && name != "")
				DataBase.update_location(database, id, name);

			stack[sp] = null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
    	//System.out.println(source + ":"+fun.getLine()+":"+(fun.getCharPositionInLine()+1)+ " "+name);
    }   
    public String join_to(String[] array, int limit, String separator){
    	String v = array[0];
		StringBuffer buff = new StringBuffer(v == null ? "" : v);
		int i;
		if(array.length < limit)
			limit = array.length;
		for(i = 1; i < limit; i++){
			buff.append(separator);
			v = array[i];
			if(v != null)
				buff.append(v);			
		}
		return buff.toString();
    }
}

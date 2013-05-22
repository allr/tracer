package org.rx.analyser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.DOTTreeGenerator;
import org.antlr.stringtemplate.StringTemplate;
import org.rx.analyser.parser.RLexer;
import org.rx.analyser.parser.RParser;
import org.rx.analyser.viewer.ASTFrame;

public class ValidatoR {
	static boolean display_swing_tree = false;
	static boolean display_text_tree = false;
	static boolean display_dot_tree = false;
	static boolean quiet = false;

	public static void main(String[] args) throws IOException{
		ArrayList<String> todo = new ArrayList<String>(args.length);
		for(String arg: args){
			if(arg.equalsIgnoreCase("-s")){
				display_swing_tree = true;
			}else if(arg.equalsIgnoreCase("-t")){
				display_text_tree = true;
			}else if(arg.equalsIgnoreCase("-d")){
				display_dot_tree = true;
			}else if(arg.equalsIgnoreCase("-q")){
				quiet = true;
			} else
				todo.add(arg);
		}
		if(todo.size() == 0)
			todo.add("-");
		process_files(todo);
	}
	
	static void process_files(Collection<String> files){
		for (String arg : files) {
			ANTLRStringStream reader;
			try {
				if(arg.equals("-"))
					reader = new ANTLRReaderStream(new InputStreamReader(System.in));
				else
					reader = new ANTLRFileStream(arg);
				check_file(reader, arg);
			} catch (Exception e) {
				System.err.println("Parse error Exception:"+e.getMessage());
			}
		}
	}
		
	static void check_file(ANTLRStringStream reader, String fname){
		if(!quiet)
			System.out.print("Checking "+fname+ ": ");
		RLexer lexer = null;
		RParser parser = null;
		try {
			lexer = new RLexer(reader);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			parser = new RParser(tokens);
			ParserRuleReturnScope result = parser.script();
			CommonTree tree; 
			if(result != null && (tree = (CommonTree)result.getTree()) != null){
				if(!quiet)
					System.out.println("OK ("+tree.getChildCount()+ " statements)");
				if(display_text_tree)
					System.out.println(tree.toStringTree());
				if(display_dot_tree){
					DOTTreeGenerator gen = new DOTTreeGenerator();
					StringTemplate st = gen.toDOT(tree);
					System.out.println(st);
				}
				if(display_swing_tree){
					ASTFrame af = new ASTFrame("Tree", tree);
					af.setVisible(true);
				}
			}else
				if(!quiet)
					System.out.println("OK (Tree empty)");
		} catch (RecognitionException e) {
			if(!quiet)
				System.out.println("Failed (Parse error)");
			System.err.println("Parse error in "+fname+" at "+parser.getErrorHeader(e)+": "+parser.getErrorMessage(e, parser.getTokenNames()));
		}
	}

}

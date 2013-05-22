package org.rx.analyser;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.Token;
import org.rx.analyser.parser.RLexer;
import org.rx.analyser.parser.RParser;


public class InterpreteR {
	static boolean quiet = false;
	static String prompt = "> ";
	static String more_prompt = "| ";
	static RLexer lexer;
	static RParser parser;
	static CommonTree tree;
	static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	static String incomplete = "";
	
	public static void main(String[] args) {
		boolean error_stmt = false; 
		try {
			// lexer = new RLexer(new ANTLRReaderStream(new BufferedReader(new InputStreamReader(System.in))));
			lexer = new RLexer();
			parser = new RParser(null);
			do{
				print(incomplete.isEmpty() ? prompt : more_prompt);
				error_stmt = !parse_statement();
				if(!error_stmt){
					parser.reset();
					if(!tree.isNil()){
						if(!quiet)
							System.out.println("OK ("+(1+tree.getChildCount())+ " statements)");
					} else 
						if(!quiet)
							System.out.println("OK (Tree empty)");
					System.out.println(tree.toStringTree());
				}
			}while(true);
		} catch (IOException e) {}
	}
	
	static boolean parse_statement() throws IOException{
		String line = in.readLine();
		if(line == null)
			throw new EOFException();
		incomplete += line;
		lexer.setCharStream(new ANTLRStringStream(incomplete));
		parser.setTokenStream(new CommonTokenStream(lexer));
		ParserRuleReturnScope result;
		try {
			result = parser.interactive();
		} catch (RecognitionException e) {
			if(e.getUnexpectedType() != -1){
				Token tok = e.token;
				String[] tok_names = parser.getTokenNames();
				int[] nexts = parser.next_tokens();
				System.err.print("Parse error on '"+tok.getText()+"' at "+tok.getLine()+":"+tok.getCharPositionInLine()+" ("+tok_names[tok.getType()]+") expected:");
				for(int i: nexts)
					if(i>3)
						System.err.print(" "+tok_names[i]);
				System.err.println("");
				incomplete = "";
			}else
				incomplete += "\n";
			return false;
		}
		incomplete = "";
		tree = (CommonTree)result.getTree(); 
		return true;
	}
	
	static void print(String text){
		print(text, System.out);
	}
	static void print(String text, PrintStream out){
		out.print(text);
		out.flush();
	}

}

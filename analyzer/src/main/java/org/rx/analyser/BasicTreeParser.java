package org.rx.analyser;

import java.io.PrintStream;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.runtime.tree.TreeParser;
import org.rx.analyser.parser.RParser;

public abstract class BasicTreeParser extends TreeParser {	
	public static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("analyser.debug", "false"));
	public BasicTreeParser(TreeNodeStream input) {
		super(input);
	}
	public BasicTreeParser(TreeNodeStream input, RecognizerSharedState state) {
		super(input, state);
	}
	public void initialize_processor(String[] opts) throws Exception { }

	public void process_tree(String name, CommonTree tree, TreeNodeStream node_stream, RParser parser) throws Exception {
		setTreeNodeStream(node_stream);
		process_tree(tree);
	}
	public void finalize_processor(){}
	public abstract void process_tree(CommonTree tree) throws Exception;
    
    //// Debug & other tools
	protected void dump_token(String prefix, CommonTree node) {
		System.out.print(prefix + ": ");
		if(node == null)
			System.out.println("<null>");
		else
			System.out.println(getTokenNames()[node.getType()]+": "+node.toStringTree());
	}
	@SuppressWarnings("unused")
	protected void DEBUG(String text) {
		if(false){
			System.out.println(text);
		}
	}
	protected void dump_expr(PrintStream out, String prefix, String node) {
		if(prefix == null)
			prefix = "";
		out.println(input.getTokenStream().getSourceName()+":"+ prefix + node);
	}
	protected double round_percent(double numerator, double denominator) {
		return ((double)Math.round((((double)numerator)/((double) denominator))*10000))/100;
	}
}
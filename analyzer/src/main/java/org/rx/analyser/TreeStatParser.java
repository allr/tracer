package org.rx.analyser;

import java.io.PrintStream;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;

public abstract class TreeStatParser extends BasicTreeParser implements ASTProcessor {
	protected StatContext stats;
	
	public TreeStatParser(TreeNodeStream input) {
		this(input, new RecognizerSharedState());
	}
	public TreeStatParser(TreeNodeStream input, RecognizerSharedState state) {
		super(input, state);
		stats = init_stat_context();
	}
	protected StatContext init_stat_context() {
		return new StatContext();
	}

	public StatContext get_stats(){
		return stats;
	}
	@Override
	public void finalize_processor() {
		print_stats(System.out);
	}
	
	final public void print_stats(PrintStream out) {
		print_stats(out, get_stats());
	}
	public void print_stats(PrintStream out, StatContext stats) {
		stats.print(out);
	}
}

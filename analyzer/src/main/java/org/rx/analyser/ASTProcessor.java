package org.rx.analyser;

import java.io.PrintStream;

import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.TreeNodeStream;
import org.rx.analyser.parser.RParser;

public interface ASTProcessor {
	void initialize_processor(String[] opts) throws Exception;
	void process_tree(String name, CommonTree tree, TreeNodeStream node_stream, RParser parser) throws Exception;
	void finalize_processor();
//	public void print_stats(PrintStream out);
}

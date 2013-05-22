package org.rx.analyser;

import java.io.File;
import java.io.PrintStream;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.DOTTreeGenerator;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.stringtemplate.StringTemplate;
import org.rx.FileName;
import org.rx.analyser.parser.RParser;
import org.rx.analyser.viewer.ASTFrame;

public class BasicASTProcessor implements ASTProcessor {
	public BasicASTProcessor(TreeNodeStream input) {
		//super(input);
	}
	public BasicASTProcessor(TreeNodeStream input, RecognizerSharedState state) {
		//super(input, state);
	}
	
	@Override
	public String toString(){
		return getClass().toString();
	}
	@Override
	public void initialize_processor(String[] opts) throws Exception {
	}

	@Override
	public void process_tree(String name, CommonTree tree, TreeNodeStream node_stream, RParser parser) throws Exception {}

	public void finalize_processor(){};
	public void print_stats(PrintStream out) {
	}
	
	static class DotGenerator extends BasicASTProcessor {
		public DotGenerator(TreeNodeStream input) {
			super(input);
		}

		@Override
		public void process_tree(String name, CommonTree tree, TreeNodeStream node_stream, RParser parser) throws Exception {
			DOTTreeGenerator gen = new DOTTreeGenerator();
			StringTemplate st = gen.toDOT(tree);
			System.out.println(st.toString().replaceFirst("digraph ", "digraph "+(new File(name)).getName().split("\\.(?=[^\\.]+$)")[0]+" "));
		}
	}
	static class TextGenerator extends BasicASTProcessor {
		public TextGenerator(TreeNodeStream input) {
			super(input);
		}

		@Override
		public void process_tree(String name, CommonTree tree, TreeNodeStream node_stream, RParser parser) throws Exception {
			System.out.println(tree.toStringTree());
		}
	}
	static class TreeViewer extends BasicASTProcessor {
		public TreeViewer(TreeNodeStream input) {
			super(input);
		}

		@Override
		public void process_tree(String name, CommonTree tree, TreeNodeStream node_stream, RParser parser) throws Exception {
			ASTFrame af = new ASTFrame(
					"Tree: "+name,
					tree,
					parser.getTokenNames());
			af.setVisible(true);
		}
	}
}

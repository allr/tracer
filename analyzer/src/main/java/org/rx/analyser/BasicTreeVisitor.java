package org.rx.analyser;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeAdaptor;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.runtime.tree.TreeRuleReturnScope;
import org.antlr.runtime.tree.TreeVisitor;
import org.antlr.runtime.tree.TreeVisitorAction;
import org.antlr.runtime.tree.TreeRewriter.fptr;
import org.rx.analyser.parser.RParser;

public abstract class BasicTreeVisitor extends BasicContextProcessor  implements ASTProcessor {
	protected TokenStream originalTokenStream;
	protected TreeAdaptor originalAdaptor;
	TreeVisitor visitor = new TreeVisitor(new CommonTreeAdaptor());
	TreeVisitorAction normal_visit = new TreeVisitorAction() {
		public Object pre(Object t)  { applyOnce(t, topdown_fptr); return t;}
		public Object post(Object t) { applyOnce(t, bottomup_fptr); return t;}
	};
	TreeVisitorAction stop_visit = new TreeVisitorAction() {
		public Object pre(Object t)  { return t;}
		public Object post(Object t) { return t;}
	};
	TreeVisitorAction actions = normal_visit;
	
	public BasicTreeVisitor(TreeNodeStream input) {
		super(input);
	}
	public BasicTreeVisitor(TreeNodeStream input, RecognizerSharedState state) {
		super(input, state);
	}
	
	public void process_tree(String name, CommonTree tree, TreeNodeStream node_stream, RParser parser) throws Exception {
		setTreeNodeStream(node_stream);
        originalAdaptor = node_stream.getTreeAdaptor();
        originalTokenStream = node_stream.getTokenStream();        

		process_tree(tree);
	}
	
	public abstract void process_tree(CommonTree tree) throws Exception;

	public Object downup(Object t, boolean showTransformations) {
		t = visitor.visit(t, actions);
		return t;
	}

    fptr topdown_fptr = new fptr() {
		@Override
		public Object rule() throws RecognitionException {
			topdown();
			return null;
		}
	};
	fptr bottomup_fptr = new fptr() {
		@Override
		public Object rule() throws RecognitionException {
			bottomup();
			return null;
		}
	};
	public void topdown() throws RecognitionException {};
	public void bottomup() throws RecognitionException {};

    public Object applyOnce(Object t, fptr whichRule) {
        if ( t==null ) return null;
        try {
            // share TreeParser object but not parsing-related state
            state = new RecognizerSharedState();
            input = new CommonTreeNodeStream(originalAdaptor, t);
            ((CommonTreeNodeStream)input).setTokenStream(originalTokenStream);
            setBacktrackingLevel(1);
            TreeRuleReturnScope r = (TreeRuleReturnScope)whichRule.rule();
            setBacktrackingLevel(0);
            if ( failed() ) return t;
            if ( DEBUG &&
                 r!=null && !t.equals(r.getTree()) && r.getTree()!=null )
            {
                reportTransformation(t, r.getTree());
            }
            if ( r!=null && r.getTree()!=null ) return r.getTree();
            else return t;
        }
        catch (RecognitionException e) { ; }
        return t;
    }

    public Object applyRepeatedly(Object t, fptr whichRule) {
        boolean treeChanged = true;
        while ( treeChanged ) {
            Object u = applyOnce(t, whichRule);
            treeChanged = !t.equals(u);
            t = u;
        }
        return t;
    }
    
    public void stop_visit(){
    	actions = stop_visit;
    }
    
    public void restart_visit(){
    	actions = normal_visit;
    }

    /** Override this if you need transformation tracing to go somewhere
     *  other than stdout or if you're not using Tree-derived trees.
     */
    public void reportTransformation(Object oldTree, Object newTree) {
        System.out.println(((Tree)oldTree).toStringTree()+" -> "+
                           ((Tree)newTree).toStringTree());
    }
}

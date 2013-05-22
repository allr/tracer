package org.rx.rtrace.processors;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.rx.rtrace.Node;


public class ASTFrame extends BasicProcessor {
	@Override
	public void process_trace(Node[] roots) throws Exception {
		
		JTree tree = new JTree(new ASTtoTreeModelAdapter(roots)){
			private static final long serialVersionUID = -3420598249369773574L;

			public String convertValueToText(Object value, boolean selected,
                    boolean expanded, boolean leaf, int row,
                    boolean hasFocus) {
				String msg = value.toString(); 
		    	/*if(!leaf)
		    		msg += " ("+item.getChildCount()+")";*/
				if(value instanceof Node.AbsCall){
					Node.AbsCall node = (Node.AbsCall) value;
					msg += " [ P:" + tab_size(node.getPrologue())  + " A:"+tab_size(node.getArgs())+"/" + node.getNbArgs()+" B:"+ tab_size(node.getBody())+ " R:"+tab_size(node.getReturns()) + " ]";
				}
		    	return msg;
			}
			int tab_size(Object[] tab){
				return (tab == null) ? 0 : tab.length;
			}
		};
		

		JScrollPane scrollPane = new JScrollPane(tree);
		JDialog dialog = new JDialog();
		dialog.setSize(640, 480);
		dialog.setModal(true);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(scrollPane);

		Container content = dialog.getContentPane();
		content.add(panel, BorderLayout.CENTER);

		expandToLast(tree);
		dialog.setVisible(true);
	}

	public void expandToLast(JTree tree) {
		TreeModel data = tree.getModel();
		Object node = data.getRoot();

		if (node == null) return;

		TreePath p = new TreePath(node);
		while (true) {
			int count = data.getChildCount(node);
			if (count == 0) break;
			node = data.getChild(node, count - 1);
			p = p.pathByAddingChild(node);
		}
		tree.scrollPathToVisible(p);
	}

	class ASTtoTreeModelAdapter implements TreeModel {
		Node.RootNode m_tree = null;

		public ASTtoTreeModelAdapter(Node[] roots) {
			m_tree = new Node.RootNode(null, roots);
		}
		public void addTreeModelListener(TreeModelListener l) {
		}
		public Object getChild(Object parent, int index) {
			return ((Node) parent).getChild(index);
		}
		public int getChildCount(Object parent) {
			return ((Node) parent).getChildCount();	
		}
		public int getIndexOfChild(Object parent, Object child) {
			if (parent == null || child == null)
				return -1;
			Node cp = ((Node) parent);
			Node cc = ((Node) child);
			for (int i = 0; i < cp.getChildCount(); i++) {
				if (cp.getChild(i).equals(cc))
					return i;
			}
			return -1;
		}
		public Object getRoot() {
			return m_tree;
		}
		public boolean isLeaf(Object node) {
			Node cn = ((Node) node);
			boolean r = (cn.getChildCount() == 0) ? true : false;
			return r;
		}
		public void removeTreeModelListener(TreeModelListener l) {}

		public void valueForPathChanged(TreePath path, Object newValue) {}
	}
}
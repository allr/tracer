package org.rx.analyser.viewer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;

/**
 * @author Joerg Werner
 * @autor Floreal Morandat (for revision)
 */
public class ASTFrame extends JFrame {
	private static final long serialVersionUID = -92059391014955313L;
	String tokens_name[];
	public ASTFrame(String lab, CommonTree t) {
		this(lab, t, null);
	}
	public ASTFrame(String lab, CommonTree t, String tokensname[]) {
		super(lab);
		tokens_name = tokensname;
		
		JTree tree = new JTree(new ASTtoTreeModelAdapter(t)){
			private static final long serialVersionUID = -3420598249369773574L;

			public String convertValueToText(Object value, boolean selected,
                    boolean expanded, boolean leaf, int row,
                    boolean hasFocus) {
		    	CommonTree item = (CommonTree)value;
		    	String msg;
		    	Token token = item.getToken();
		    	if(token != null)
		    		if(tokens_name == null || token.getType() < 0 || token.getType() >= tokens_name.length )
		    			msg = "" + token.getType();
		    		else
		    			msg = tokens_name[token.getType()];
		    	else
		    		msg = "NONE";
		    	msg += ":" + item.toString()+" ["+item.getLine()+":"+item.getCharPositionInLine()+"]";
		    	if(!leaf)
		    		msg += " ("+item.getChildCount()+")";
		    	return msg;
			}
		};

		JScrollPane scrollPane = new JScrollPane(tree);
		setSize(640, 480);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(scrollPane);

		Container content = getContentPane();
		content.add(panel, BorderLayout.CENTER);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				Frame f = (Frame) e.getSource();
				f.setVisible(false);
				f.dispose();
				// System.exit(0);
			}
		});
	}

}
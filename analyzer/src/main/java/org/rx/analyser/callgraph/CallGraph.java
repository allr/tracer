package org.rx.analyser.callgraph;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class CallGraph {
	Map<String, CGNode> nodes = new LinkedHashMap<String, CGNode>();
	
	public CGNode get_node(String name){
		CGNode node = nodes.get(name);
		if(node == null){
			node = new CGNode(name);
			nodes.put(name, node);
		}
		return node;
	}
	
	public void add_link(String src, String dst){
		add_link(get_node(src), get_node(dst));
	}
	public void add_link(CGNode src, String dst){
		add_link(src, get_node(dst));
	}
	public void add_link(CGNode src, CGNode dst){
		src.add(dst);
	}
		
	public void to_dot(PrintStream out){
		out.println("digraph G {");
		for(CGNode node: nodes.values()){
			out.println("\t"+node.getDotID()+" [ label=\""+node+"\" ];");
		}
		for(CGNode node: nodes.values()){
			for(CGNode to: node.getCalls()){
				out.println("\t"+node.getDotID()+" -> "+to.getDotID()+";");
			}
		}
		out.println("}");
	}
}

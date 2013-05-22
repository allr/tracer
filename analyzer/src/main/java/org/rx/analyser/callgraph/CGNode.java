package org.rx.analyser.callgraph;

import java.util.Collection;
import java.util.LinkedHashSet;

public class CGNode {
	private static int NEXT_ID = 0;
	String name;
	int id;
	
	Collection<CGNode> calls;
	
	CGNode(String name){
		this.name = name;
		id = NEXT_ID++;
		calls = new LinkedHashSet<CGNode>();
	}

	public void add(CGNode dst) {
		calls.add(dst);
	}

	public Collection<CGNode> getCalls() {
		return calls;
	}
	public int getID(){
		return id;
	}
	public String getDotID(){
		return "n"+id;
	}
	public String toString(){
		return name;
	}
}

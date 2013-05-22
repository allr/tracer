package org.rx;

import java.util.ArrayList;

public class ShubertNumbering {
	static Abstract any = new Abstract("Any", null);
	
	static Abstract array = new Abstract("Array", any);
	static Concrete s_lgl = new Concrete("Logical", array);

	static Abstract number = new Abstract("Number", array);
	static Concrete s_int = new Concrete("Int", number);
	static Concrete s_double = new Concrete("Double", number);
	static Concrete s_complex = new Concrete("Complex", number);
	
	static Concrete s_string = new Concrete("String", array);
	static Concrete s_vector = new Concrete("Vector", array);

	static Abstract atom = new Abstract("Atom", any);
	static Concrete s_raw = new Concrete("Raw", atom);
	static Concrete s_extptr = new Concrete("EPtr", atom);
	static Concrete s_list = new Concrete("List", atom);
	static Node hierarchy = any;	

	public static void main(String[] args) {
		hierarchy.numbering(0);
		System.out.println(hierarchy);
		hierarchy.test(s_int);
	}
	
	static abstract class Node {
		int id;
		String name;
		Abstract parent;
			
		Node(String name, Abstract parent){
			this.name = name;
			this.parent = parent;
			if(parent != null)
				parent.add(this);
		}
		void test(Node i){
			System.out.println(i.name+" is a "+name+": "+sub_type(i.id));
		}
		boolean sub_type(int i){
			return id <= i && i <= getMax();
		}
		int numbering(int i){
			id = i;
			return i;
		}
		public String toString(){
			return name+':'+id+":"+getMax();
		}
		abstract int getMax();
	}
	static class Concrete extends Node{
		Concrete(String name, Abstract parent){
			super(name, parent);
		}
		int getMax(){ return id;}
	}
	static class Abstract extends Node{
		int max;
		ArrayList<Node> children;
		
		Abstract(String name, Abstract parent){
			super(name, parent);
			children = new ArrayList<Node>();
		}
		int getMax(){ return max;}
		void add(Node n){
			children.add(n);
		}
		void test(Node i){
			super.test(i);
			for(Node child: children)
				child.test(i);
		}
		int numbering(int i){
			int m = super.numbering(i);
			for(Node child: children)
				m = Math.max(i = child.numbering(i+1), m);
			max = m;
			return m;
		}
		public String toString(){
			String sons = "";
			for(Node child: children)
				sons += child.toString()+'\n';
			return sons+super.toString();
		}
	}
}

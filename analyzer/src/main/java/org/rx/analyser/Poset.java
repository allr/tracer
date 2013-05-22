package org.rx.analyser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class Poset<E> {
	ArrayList<E> roots = new ArrayList<E>();
	Map<E, PosetNode> nodes = new HashMap<E, PosetNode>();
	int current_mark = 0;
	
	void add(E item, Collection<E> supers){
		supers = selectSmallest(supers);
		PosetNode poe;
		if((poe = nodes.get(item)) == null){
			poe = new PosetNode(item, supers);
			nodes.put(item, poe);
			if(supers.isEmpty())
				roots.add(item);
		}else{
			poe.addAll(supers);
			if(!supers.isEmpty())
				roots.remove(item);
		}
	}
	void computeSummary(PosetSummary<E> sum){
		
	}
	PosetSummary<E> getSummary(){
		PosetSummary<E> sum = new BasicSummary<E>();
		sum.compute(this);
		return sum;
	}
	void summary(PrintStream out){
		summary(out, getSummary());
	}
	void summary(PrintStream out, PosetSummary<E> sum){	
		sum.print(out);
	}
	static double percent(int num, int denum){
		return round(num*100, denum);
	}
	static double round(int num, int denum){
		return Math.round((((double)num)/(double)denum)*100d)/100d;
	};
	static double round(double num, double denum){
		return Math.round((num/denum)*100d)/100d;
	};
	int nbSubClasses(){
		HashSet<E> visited = new HashSet<E>();
		int subs = 0;
		for(E item: roots)
			subs += nodes.get(item).nbSubClasses(visited);
		return subs;
	}
	int maxDepth(){
		int maxDepth = 0;
		for(E item: roots)
			maxDepth = Math.max(nodes.get(item).depth(), maxDepth);
		return maxDepth;
	}
	ArrayList<E> selectSmallest(Collection<E> smallers){
		ArrayList<E> smallest = new ArrayList<E>(smallers.size());
		//smallest.addAll(smallers);
		for(E smaller: smallers){
			outer: {
				for(E other: smallers){ // TODO something efficient, i.e. do not what is already done
					if(inf(other, smaller))
						break outer;
				}
				smallest.add(smaller);
			}
		}
		return smallest;
	}
	String toDot(){
		return toDot(new DotAdapter<E>());
	}
	String toDot(DotAdapter<E> converter, String name){
		converter.graphName = name;
		return toDot(converter);
	}
	String toDot(DotAdapter<E> converter){
		StringBuffer buffer = new StringBuffer(converter.toDotHeader());
		for(E item: nodes.keySet()){
			buffer.append(converter.toDotNode(item));
			for(E sup: nodes.get(item).direct_greaters)
				buffer.append(converter.toDotEdge(item, sup));
		}
		buffer.append("}\n");
		return buffer.toString();
	}
	PosetNode get(E item){
		return nodes.get(item);
	}
	boolean inf(E item, E other){
		PosetNode poe = nodes.get(item);
		if(poe == null)
			return false;
		return poe.inf(other);
	}
	Iterator<E> iterator(){
		return nodes.keySet().iterator();
	}
	class PosetNode{
		E item;
		ArrayList<E> direct_greaters;
		ArrayList<E> direct_smallers;
		HashSet<E> greaters;
		int mark;
		public PosetNode(E e, Collection<E> supers) {
			item = e;
			direct_greaters = new ArrayList<E>();
			direct_smallers = new ArrayList<E>();
			greaters = new HashSet<E>();
			addAll(supers);
			mark = current_mark;
		}
		private void addAll(Collection<E> items) {
			for(E item: items)
				addGreater(item);
		}
		private void addGreater(E item){
			if(greaters.contains(item)) return;
			direct_greaters.add(item);
			greaters.add(item);
			PosetNode poe = nodes.get(item);
			if(poe == null){
				add(item, new ArrayList<E>()); // FIXME null
				poe = nodes.get(item);
			}
			greaters.addAll(poe.greaters);
			poe.direct_smallers.add(this.item);
		}
		public boolean inf(E other){
			return greaters.contains(other);
		}
		public boolean isMarked(int newMark){
			boolean r = newMark == mark;
			mark = newMark;
			return r;
		}
		public int depth(){
			int depth = 0;
			for(E item: direct_smallers)
				depth = Math.max(nodes.get(item).depth(), depth);
			return depth + 1;
		}
		public int nbSubClasses(HashSet<E> visited){
			visited.add(item);
			int subs = 0;
			for(E item: direct_smallers){
				if(!visited.contains(item))
					subs += 1 + nodes.get(item).nbSubClasses(visited);
			}
			return subs;
		}
	}
	interface PosetSummary<T>{
		void compute(Poset<T> poset);
		void print(PrintStream out);
		
		public double getNbNodes();
		public double getNbRoots();
		public double getLeaves();
		public double getGreaters();
		public double getDirectGreaters();
		public double getSmallers();
		public double getMaxDepth();
	}
	static class BasicSummary<T> implements PosetSummary<T>{
		double directGreaters, greaters, leaves;
		double nbNodes, nbRoots, nbSmallers, maxDepth;
		public double getNbNodes() {
			return nbNodes;
		}
		public double getNbRoots() {
			return nbRoots;
		}
		public double getLeaves() {
			return leaves;
		}
		public double getGreaters() {
			return greaters;
		}
		public double getDirectGreaters() {
			return directGreaters;
		}
		public double getSmallers() {
			return nbSmallers;
		}
		public double getMaxDepth() {
			return maxDepth;
		}
		public void compute(Poset<T> poset){
			nbNodes = poset.nodes.size();
			nbRoots = poset.roots.size();
			nbSmallers = poset.nbSubClasses();
			maxDepth = poset.maxDepth();
			for(Poset<T>.PosetNode item: poset.nodes.values()){
				directGreaters += item.direct_greaters.size();
				if(item.direct_smallers.isEmpty()) leaves ++;
				greaters += item.greaters.size();
			}
		}
		public void print(PrintStream out){
			defaultPrint(out, this);
		}
		static <T> void defaultPrint(PrintStream out, PosetSummary<T> sum){
			double nbNodes = sum.getNbNodes();
			out.println("// Number of nodes: "+nbNodes);
			out.println("// Number of roots: "+sum.getNbRoots());		
			out.println("// Number of leaves: "+sum.getLeaves());
			out.println("// Avg of parents: "+round(sum.getGreaters(), nbNodes));
			out.println("// Avg of direct-parents: "+round(sum.getDirectGreaters(), nbNodes));
			out.println("// Avg of children: "+round(sum.getSmallers(), nbNodes));
			out.println("// Max depth: " + sum.getMaxDepth());
		}
	}
	static class DotAdapter<E>{
		String graphName = "G";
		String toDotHeader(){
			return "digraph \""+graphName+"\" {\ngraph [rankdir=BT];\n";
		}
		String toDotNode(E item){
			return "\""+toDotLabel(item)+"\";\n";
		}
		String toDotEdge(E item, E sup){
			return "\""+toDotLabel(item)+"\" -> \""+toDotLabel(sup)+"\";\n";
		}
		String toDotLabel(E item){
			return item.toString();
		}
	}
}

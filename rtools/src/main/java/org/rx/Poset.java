package org.rx;

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
	boolean validated;

	public ArrayList<E> getRoots(){
		return roots;
	}
	
	public void validate(){
		E node = null;
		validated = true;

		ArrayList<E> todo = new ArrayList<E>(roots);
		HashSet<E> done = new HashSet<E>(nodes.size());
		while(!todo.isEmpty()) top:{
			node = todo.remove(0);
			PosetNode poe = nodes.get(node);
			for(E sup: poe.direct_greaters)
				if(!done.contains(sup)){
					todo.add(node);
					break top;
				}
			done.add(node);
			poe.compute_greaters();
			todo.addAll(poe.direct_smallers);
		}

		if(nodes.size() != done.size())
			throw new RuntimeException(" "+nodes.size() +" "+done.size());
	}
	public void add(E item, Collection<E> supers){
		//supers = selectSmallest(supers);
		PosetNode poe;
		if((poe = nodes.get(item)) == null){
			poe = new PosetNode(item, supers);
			nodes.put(item, poe);
			if(supers == null || supers.isEmpty())
				roots.add(item);
		}else if(supers != null) {
			poe.addAll(supers);
			if(!supers.isEmpty())
				roots.remove(item);
		}
	}
	
	public void remove(E item){
		PosetNode poe = get(item);
		if(!poe.isLeave())
			throw new RuntimeException("Couldn't remove '"+item+"' it isn't a leave");
		if(poe.isRoot())
			roots.remove(item);
		else
			for(E parent: poe.direct_greaters)
				get(parent).removeChild(item);
		nodes.remove(item);
	}
	
	public int size() {
		return nodes.size();
	}
	public void computeSummary(PosetSummary<E> sum){}
	public PosetSummary<E> getSummary(){
		PosetSummary<E> sum = new BasicSummary<E>();
		sum.compute(this);
		return sum;
	}
	public void summary(PrintStream out){
		summary(out, getSummary());
	}
	public void summary(PrintStream out, PosetSummary<E> sum){	
		sum.print(out);
	}
	static double percent(int num, int denum){
		return round(num*100, denum);
	}
	public static double round(int num, int denum){
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
					if(inf(other, smaller)){
						break outer;
					}
				}
				smallest.add(smaller);
			}
		}
		return smallest;
	}
	
	String toDot(){
		return toDot(new DotAdapter<E>());
	}
	public String toDot(DotAdapter<E> converter, String name){
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
	
	public boolean isLeave(E item){
		return get(item).isLeave();
	}
	
	public boolean isRoot(E item){
		return get(item).isRoot();
	}
	
	public boolean inf(E item, E other){
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
			if(supers != null)
				addAll(supers);
			mark = current_mark;
		}
		
		public void removeChild(E child) {
			direct_smallers.remove(child);
		}
		
		private void compute_greaters(){
			greaters = new HashSet<E>();
			greaters.addAll(direct_greaters);
			for(E parent: direct_greaters)
				greaters.addAll(get(parent).greaters);
			direct_greaters = selectSmallest(direct_greaters);
		}
		
		private void addAll(Collection<E> items) {
			for(E item: items)
				addGreater(item);
		}
		
		private void addGreater(E item){
			if(direct_greaters.contains(item)) return;
			direct_greaters.add(item);
			add(item, null);
			PosetNode poe = nodes.get(item);
			poe.addSmaller(this.item);
		}
		
		private void addSmaller(E item){
			if(direct_smallers.contains(item)) return;
			direct_smallers.add(item);
		}
		
		public boolean inf(E other){
			if(!validated) throw new RuntimeException("");
			return greaters.contains(other);
		}
		
		public boolean isLeave(){
			return direct_smallers == null || direct_smallers.size() == 0;
		}
		public boolean isRoot(){
			return direct_smallers == null || direct_smallers.size() == 0;
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
	
	public interface PosetSummary<T>{
		void compute(Poset<T> poset);
		void print(PrintStream out);
		
		public double getNbNodes();
		public double getNbRoots();
		public double getLeaves();
		public double getGreaters();
		public double getDirectGreaters();
		public double getSmallers();
		public double getMaxDepth();
		public double getMaxDirectGreaters();
	}
	
	public static class BasicSummary<T> implements PosetSummary<T>{
		public double directGreaters, greaters, leaves;
		public double nbNodes, nbRoots, nbSmallers, maxDepth, maxDirectGreaters;
		
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
		public double getMaxDirectGreaters() {
			return maxDirectGreaters;
		}
		public void compute(Poset<T> poset){
			nbNodes = poset.nodes.size();
			nbRoots = poset.roots.size();
			nbSmallers = poset.nbSubClasses();
			maxDepth = poset.maxDepth();
			maxDirectGreaters = 0;
			for(Poset<T>.PosetNode item: poset.nodes.values()){
				directGreaters += item.direct_greaters.size();
				maxDirectGreaters = Math.max(maxDirectGreaters, item.direct_greaters.size());
				if(item.direct_smallers.isEmpty()) leaves ++;
				if(item.greaters == null)
					throw new RuntimeException(this.toString());
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
			out.println("// Max Direct parents: " + sum.getMaxDirectGreaters());
			out.println("// Max depth: " + sum.getMaxDepth());
		}
	}
	public static class DotAdapter<E>{
		String graphName = "G";
		public String toDotHeader(){
			return "digraph \""+graphName+"\" {\ngraph [rankdir=BT];\n";
		}
		public String toDotNode(E item){
			return "\""+toDotLabel(item)+"\";\n";
		}
		public String toDotEdge(E item, E sup){
			return "\""+toDotLabel(item)+"\" -> \""+toDotLabel(sup)+"\";\n";
		}
		public String toDotLabel(E item){
			return item.toString();
		}
	}
}

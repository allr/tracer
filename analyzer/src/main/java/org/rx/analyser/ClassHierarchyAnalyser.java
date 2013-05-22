package org.rx.analyser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.rx.Poset;
import org.rx.Poset.BasicSummary;
import org.rx.Poset.DotAdapter;
import org.rx.Poset.PosetSummary;

public class ClassHierarchyAnalyser {
	static Poset<RClass> classHierarchy = new Poset<RClass>();
	static Map<String, RClass> classByName = new HashMap<String, RClass>();
	static Map<String, RMethod> methodByName = new HashMap<String, RMethod>();
	static DotAdapter<RClass> rclassDotAdapter = new RClassDotAdapter();
	static DotAdapter<RClass[]> rsigDotAdapter = new RSignatureDotAdapter();
	
	static ArrayList<String> s3classes = new ArrayList<String>();
	static ArrayList<S3Method> s3methods = new ArrayList<S3Method>();

	static void readFile(Reader reader) throws IOException {
		StreamTokenizer tok = initTokenizer(reader);
		while(tok.nextToken() != StreamTokenizer.TT_EOF){
			if(tok.ttype == '('){
				tok.nextToken();
				String which = readString(tok, "defintion keyword [:class, :union, :method]");
				if(which.equals(":class"))
					readClass(tok, false);
				else if(which.equals(":union"))
					readClass(tok, true);
				else if(which.equals(":method"))
					readMethod(tok);
				else if(which.equals(":s3class"))
					readS3(tok);
				else if(which.equals(":generic"))
					readGeneric(tok);
				else
					throw new RuntimeException("Invalid keyword");
			} else
				throw new RuntimeException("Need a definition 'i.e. ('");
		}
		validateMethods();
	}
	static private void readClass(StreamTokenizer tok, boolean is_union) throws IOException{
		tok.nextToken();
		String cclass = readString(tok, "Class");
		ArrayList<String> supers = readClassList(tok);
		while(tok.nextToken() != ')');
		boolean is_virtual = supers.remove("VIRTUAL");
		if(is_union)
			insertClassUnion(cclass, supers);
		else
			insertClass(cclass, supers, is_virtual);
	}
	static private void readS3(StreamTokenizer tok) throws IOException{
		tok.nextToken();
		String cclass = readString(tok, "Class");
		while(tok.nextToken() != ')');
		if(!s3classes.contains(cclass))
			s3classes.add(cclass);
	}
	static private void readGeneric(StreamTokenizer tok) throws IOException{
		tok.nextToken();
		String generic = readString(tok, "GenericMethod");
		ArrayList<String> supers = readClassList(tok);
		while(tok.nextToken() != ')');
		s3methods.add(new S3Method(generic, supers));
	}
	static private void readMethod(StreamTokenizer tok) throws IOException{
		tok.nextToken();
		String method = readString(tok, "Method");
		ArrayList<String> signature = readClassList(tok);
		while(tok.nextToken() != ')');
		RMethod rmethod = getMethodByName(method);
		rmethod.signatureQueue.add(signature);
	}
	static void validateMethods(){
		classHierarchy.validate();
		for(RMethod method: methodByName.values())
			method.validate();
	}
	static private void insertClassUnion(String cname, ArrayList<String> supers){
		RClass rclass = newClassByName(cname, true);
		ArrayList<RClass> class_union = new ArrayList<RClass>(1);
		class_union.add(rclass);
		rclass.is_virtual = true;
		for(String sub: supers)
			classHierarchy.add(getClassByName(sub), class_union);
	}
	static private void insertClass(String cname, ArrayList<String> supers, boolean is_virtual){
		RClass rclass = newClassByName(cname, false);

		ArrayList<RClass> rsupers = new ArrayList<RClass>(supers.size());
		for(String n: supers){
			rsupers.add(getClassByName(n));
		}
		if(is_virtual)
			rclass.is_virtual = is_virtual;
		classHierarchy.add(rclass, rsupers);
	}
	static RMethod getMethodByName(String mname){
		RMethod rmethod = methodByName.get(mname);
		if(rmethod == null){
			rmethod = new RMethod(mname);
			methodByName.put(mname, rmethod);
		}
		return rmethod;
	}
	static RClass getClassByName(String cname){
		RClass rclass = classByName.get(cname);
		if(rclass == null){
			rclass = new RClass(cname, false);
			classByName.put(cname, rclass);
		}
		return rclass;
	}
	static RClass newClassByName(String cname, boolean is_union){
		RClass rclass = classByName.get(cname);
		if(rclass == null){
			rclass = new RClass(cname, is_union);
			classByName.put(cname, rclass);
		}else
			rclass.is_union = is_union;
		return rclass;
	}
	static private ArrayList<String> readClassList(StreamTokenizer tok) throws IOException {
		if(tok.nextToken() != '(')
			throw new RuntimeException("Need a class definition 'i.e. ('");
		ArrayList<String> supers = new ArrayList<String>();
		while(tok.nextToken() != ')')
			supers.add(readString(tok, "SuperClass"));
		return supers;
	}
	static private String readString(StreamTokenizer tok, String msg){
		if(tok.ttype != StreamTokenizer.TT_WORD && tok.ttype != '"')
			throw new RuntimeException("Need a String ("+msg+"), found " + ((tok.ttype> 0)? " '"+(char)tok.ttype+"'" : tok.ttype)+ " at line: "+tok.lineno());
		return tok.sval;
	}
	static private StreamTokenizer initTokenizer(Reader reader) {
		StreamTokenizer tok = new StreamTokenizer(reader);
		tok.resetSyntax();
		tok.eolIsSignificant(false);
		tok.lowerCaseMode(false);
		tok.quoteChar('"');
        tok.wordChars('a', 'z');
        tok.wordChars('A', 'Z');
        tok.wordChars('0', '9');
		tok.wordChars(128 + 32, 255);
		tok.wordChars(':', ':');
		tok.wordChars('_', '_');
		tok.wordChars('.', '.');
        tok.whitespaceChars(0, ' ');
		tok.slashSlashComments(false);
		tok.slashStarComments(false);
		return tok;
	}
	public static void main(String[] args) throws IOException {
		InputStream in = System.in;
		if(args.length > 0)
			in = new FileInputStream(args[0]);
		readFile(new BufferedReader(new InputStreamReader(in)));
		//System.out.println(classHierarchy.toDot(rclassDotAdapter));
		System.err.println("// S4 Object Model");
		classHierarchy.summary(System.err);
		System.err.println("// ------------------- without roots/leaves");
		
		RClass[] roots = classHierarchy.getRoots().toArray(new RClass[classHierarchy.getRoots().size()]);
		int lonely = 0;
		for(RClass c: roots){
			if(classHierarchy.isLeave(c)){
				if(!classHierarchy.isRoot(c))
					throw new RuntimeException("toto");
				classHierarchy.remove(c);
				lonely ++;
			}
		}
		classHierarchy.summary(System.err);
		System.err.println("// Lonely: "+lonely);
		System.err.println("// -------------------");

		MethodsSummary sum = new MethodsSummary();
		sum.compute(null);

		sum.print(System.err);
		System.err.println("// -------------------");
		
		System.err.println("// S3 Object Model");
		s3Summary(System.err);
	}
	
	static void s3Summary(PrintStream out){
		int classIsUsed = 0;
		int distro[] = new int[s3classes.size()];
		int s3defs = 0, max = 0;

		out.println("// Number of s3 classes: "+s3classes.size());
		out.println("// Number of s3 methods: "+s3methods.size());
		
		for(S3Method method: s3methods){
			int nb = method.classes.length;
			s3defs += nb;
			distro[nb] ++;
			if(nb > max) max = nb;
			if(nb > 50)
				System.err.println(method.name+" => "+nb);
			if(nb == 0)
				System.err.println("Empty "+method.name);
		}
		out.println("// Avg method redef: " + Poset.round(s3defs, s3methods.size()));
	
		out.print("// Method redef distribution:");
		for(int i = 0; i < s3classes.size(); i++)
			if(distro[i] > 0)
				out.print(" "+i+":"+distro[i]);
		out.println();
		
		clsUsed:
		for(String cls: s3classes){
			for(S3Method meth: s3methods){
				//if(meth.name.equals("print") || meth.name.equals("plot"))
					if(meth.has(cls)){
						classIsUsed ++;
						continue clsUsed;
					}
			}
		}
		out.println("// Unused classes: "+ (s3classes.size() - classIsUsed));
	}
	
	static class RClass{
		String name;
		boolean is_union, is_virtual;
		RClass(String n, boolean union){
			name = n;
			is_union = union;
		}
		public String toString(){
			return name;
		}
		
	}
	
	static class RMethod {
		String name;
		HashSet<ArrayList<String>> signatureQueue;
		Poset<RClass[]> definitions;
		RMethod(String method_name){
			name = method_name;
			signatureQueue = new HashSet<ArrayList<String>>();
		}
		
		int getNbArgs(){
			if(definitions.size() == 0)
				return 0;
			return definitions.getRoots().get(0).length;
		}
		void validate(){
			definitions = new Poset<RClass[]>();
			RClass sigs[][] = toRClassSignature(signatureQueue);
			signatureQueue = null;
			Arrays.sort(sigs, signatureComparator);
			ArrayList<RClass[]> supers = new ArrayList<RClass[]>();
			for(int i = 0; i < sigs.length; i++){
				for(int j = 0; j < sigs.length; j ++)
					if(signatureComparator.compare(sigs[i], sigs[j]) < 0){
						//System.out.println("I'm addding "+Arrays.toString(sigs[j])+ " as super class of "+ Arrays.toString(sigs[i]));
						supers.add(sigs[j]);
					}
				definitions.add(sigs[i], supers);
				supers.clear();
			}
			definitions.validate();
			//definitions.summary(System.out);
			//System.out.println(definitions.toDot(rsigDotAdapter, name));
		}
		
		private RClass[][] toRClassSignature(HashSet<ArrayList<String>> sigs) {
			RClass[][] newSigs = new RClass[sigs.size()][];
			int i = 0;
			for(ArrayList<String> sig: sigs){
				int j = 0;
				RClass[] current = newSigs[i++] = new RClass[sig.size()];
				for(String cls: sig)
					current[j++] = getClassByName(cls);
			}
			return newSigs;
		}
		
		static Comparator<RClass[]> signatureComparator = new Comparator<RClass[]>(){	
			public int compare(RClass[] one, RClass[] other){
				int size = Math.min(one.length, other.length);
				boolean allMax = size != 0;
				boolean allMin = allMax;
				for(int i = 0; i < size && ((allMin || allMax) == true); i++){
					if(allMin && !(classHierarchy.inf(one[i], other[i]) || one[i].equals(other[i])))
						allMin = false;
					if(allMax && !(classHierarchy.inf(other[i], one[i]) || one[i].equals(other[i])))
						allMax = false;
				}
				//System.out.println("one "+Arrays.toString(one) + " other "+ Arrays.toString(other) +" min "+allMin + " max "+ allMax );
				if(allMax && !allMin) return 1;
				if(allMin && !allMax) return -1;
				return 0;
			}
		};
	}
	
	static class S3Method{
		String name;
		String classes[];
		S3Method(String name, Collection<String> classes){
			this.name = name;
			this.classes = new String[classes.size()];
			int i = 0;
			for(String cla: classes)
				this.classes[i++] = cla;
		}
		
		boolean has(String cls){
			return Arrays.asList(classes).contains(cls);
		}
	}
	
	static class RSignatureDotAdapter extends DotAdapter<RClass[]>{
		public String toDotLabel(RClass[] item){
			return Arrays.toString(item);
		}
	}
	
	static class RClassDotAdapter extends DotAdapter<RClass>{
		public String toDotNode(RClass item){
			return "\""+item+"\""+(item.is_union ? "[color=red]":"")+";\n";
		}
	}
	
	static class MethodsSummary extends BasicSummary<RClass[]>{
		int nbMethods;
		double defs, nbArgs, maxArgs, avgDepth;
		public void compute(Poset<RClass[]> poset) {
			nbNodes = 0;
			nbRoots = 0;
			leaves = 0;
			greaters = 0;
			directGreaters = 0;
			maxDirectGreaters = 0;
			nbSmallers = 0;
			maxDepth = 0;
			defs = 0;
			nbMethods = methodByName.size();
			nbArgs = 0;
			for(RMethod method: methodByName.values()){
				PosetSummary<RClass[]> sum = method.definitions.getSummary();
				nbNodes += sum.getNbNodes();
				nbRoots += sum.getNbRoots();
				leaves += sum.getLeaves();
				greaters += sum.getGreaters();
				directGreaters += sum.getDirectGreaters();
				nbSmallers += sum.getSmallers();
				maxDepth = Math.max(maxDepth, sum.getMaxDepth());	
				avgDepth += sum.getMaxDepth();	
				maxDirectGreaters = Math.max(maxDirectGreaters, sum.getMaxDirectGreaters());	
				defs += method.definitions.size();
				int args = method.getNbArgs();
				nbArgs += args;
				maxArgs = args > maxArgs ? args : maxArgs;
			}
			nbNodes /= nbMethods;
			nbRoots /= nbMethods;
			leaves /= nbMethods;
			greaters /= nbMethods;
			directGreaters /= nbMethods;
			nbSmallers /=  nbMethods;
			defs /= nbMethods;
			nbArgs /= nbMethods;
			avgDepth /= nbMethods;
		}
		public void print(PrintStream out){
			out.println("// Number of S4 methods: " + nbMethods);
			out.println("// Avg of S4 defs by method: " + defs);
			out.println("// Avg of arguments by method: " + nbArgs);
			out.println("// Max of arguments by method: " + maxArgs);
			out.println("// Avg depth by method: " + avgDepth);
			super.print(out);
		}
	}
}

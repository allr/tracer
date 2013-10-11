package org.rx.rtrace;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rx.DataBase;
import org.rx.rtrace.processors.TraceProcessor;
import org.rx.rtrace.processors.TraceProcessor.NodeVisitor;

public abstract class Node {
	final static private boolean flyweight = Boolean.parseBoolean(System.getProperty("RTrace.Node.flyweight", "false"));
	final static private boolean strict = Boolean.parseBoolean(System.getProperty("RTrace.NodeBuilder.strict", "true"));
	final static private boolean littleEndian = Boolean.parseBoolean(System.getProperty("RTrace.NodeBuilder.littleendian", "false"));
	final static private boolean old_header = Boolean.parseBoolean(System.getProperty("RTrace.NodeBuilder.oldheader", "false"));
	final static private boolean skip_internal = Boolean.parseBoolean(System.getProperty("RTrace.NodeBuilder.skipinternal", "true"));
	final static private boolean _32bitmode = Boolean.parseBoolean(System.getProperty("RTrace.NodeBuilder.32bits", "false"));
//	final static private boolean DEBUG = Boolean.parseBoolean(System.getProperty("RTrace.NodeBuilder.debug", "false"));

//	static private final Node[] null_node_array = new Node[0];
	static private DataInputStream stream;
        // actually used as a stack:
        //   addLast in PrologueNode.accept_visitor
        //   removeLast in AbsCall.attach_args
	static private ArrayDeque<Node> prologues = new ArrayDeque<Node>();
	
	static protected int bytes_read;
	static protected boolean found_err = false;

	static private ArrayList<String> error_msg;
	protected final Node parent;
	protected Node(Node parent){ this.parent = parent; }
	public Node getParent(){ return parent; }

	//////////////////////// MOVEME to a proper class //////////////

        /* stack of integer IDs:
         *   pushed in
         *   - AbsCall.build_node  (id)
         *   - EvalUnBndPromise.accept_visitor (getPromiseOwner(getID()))
         *                                                      -> returns a node id?
         *
         *   popped in
         *   - AbsCall.build_node  (after accepting visitors)
         *   - EvalUnBndPromise.accept_visitor (after build_node)
         */
	static ArrayList<Integer> enclosing = new ArrayList<Integer>();
	static { enclosing.add(0); } // add an invalid(!) entry as base location

        // promise-node-id (sexpaddr) -> context ID
	static protected Map<Integer,Integer> prom_map = new LinkedHashMap<Integer, Integer>();

        // pid - promise id (sexpaddr), fid - context ID (getCurrentContext)
	static private void setPromiseOwner(int pid, int fid){
		prom_map.put(pid, fid);
	}

        // lookup context by promise ID
	static private int getPromiseOwner(int pid){
		Integer fid = prom_map.get(pid);
		if(fid == null)
			return 0;
		return fid;
	}

        // push an entry onto "enclosing"
        // called with a node ID (address for promises, loc_id for funs) or location_id
	static private void addContext(int fun){
		enclosing.add(fun);
	}

        // drop the last entry in "enclosing"
	static private void dropContext(){
		enclosing.remove(enclosing.size() - 1);
	}

	// returns a location_id
	static public int getCurrentContext(){
		int pos = enclosing.size() - 1;
		do {
			int ctx = enclosing.get(pos);
			if(skip_internal){
                                // returns a context that is not a primitive
				if(ctx > DataBase.last_primitive_location)
					return ctx;
			} else if(ctx != 33 && ctx != 18) // 33: .Internal, 18: { - happens to be the same in R-3
                                // returns any context except .Internal and {
				return ctx;
			pos --;
		} while(pos >= 0);
		return 0;
	}
	//////////////////////////////////////////////////////////////
	
	public static void process_stream(TraceProcessor[] processors, DataInputStream iStream) throws Exception {
		try {
			bytes_read = 0;
			stream = iStream;
			read_header(stream);
			// Everything's ok ... gogogo !!!
			Node node = new RootNode(null);
			node.accept_visitor(processors);
		} catch (StackOverflowError e) {
			System.err.println("Stack overflow, read until position: 0x"+Integer.toHexString(bytes_read));
			throw e;
		} catch (OutOfMemoryError e) {
			System.err.println("Out of memory, read until position: 0x"+Integer.toHexString(bytes_read));
			System.gc();
			throw e;
		}
		if(prologues.size() > 0)
			RTrace.emit_error_msg("Some prologues are still in the stack: "+ prologues.size());		
		if(found_err)
			RTrace.emit_error_msg(error_msg.toString());
	}
	
	public String toString(){
		return "Node"+getClass().toString();
	}
	
	public boolean is_s3() { return false ;}
	abstract protected void accept_visitor(TraceProcessor[] processors) throws Exception;

	public static class RootNode extends CompositeNode {
		public RootNode(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			NodeVisitor v;
			for(TraceProcessor processor: processors)
				if((v = processor.getVisitor()) != null)
					v.pre_visit_root(this);
			build_node(processors);
			for(TraceProcessor processor: processors)
				if((v = processor.getVisitor()) != null)
					v.visit_root(this);
		}
		private void build_node(TraceProcessor[] processors) throws Exception {
			setBody(build_tree_until(processors, null, -1));
		}
		@Override
		public String toString() {
			return "[]";
		}
	}

	public static class PrologueNode extends CompositeNode {
		public PrologueNode(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			for(TraceProcessor processor: processors)
				processor.getVisitor().pre_visit_prologue(this);
			prologues.addLast(this);
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_prologue(this);
		}
		private void build_node(TraceProcessor[] processors) throws Exception {
			setBody(build_tree_until(processors, this, END_PROLOGUE));
		}
		@Override
		public String toString() {
			return "Prologue:"+getBody();
		}
	}
	
	public static class NilSXP extends Node {
		protected NilSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_nil(this);
		}
		@Override
		public String toString() {
			return "Nil";
		}
		static NilSXP unique = flyweight ? new NilSXP(null) : null;
		static NilSXP make_node(Node parent){
			if(flyweight)
				return unique;
			return new NilSXP(parent);
		}
	}

	public static class UnitSXP extends Node {
		protected UnitSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_unit(this);
		}
		@Override
		public String toString() {
			return "Unit";
		}
		static UnitSXP unique = flyweight ? new UnitSXP(null) : null;
		static UnitSXP make_node(Node parent){
			if(flyweight)
				return unique;
			return new UnitSXP(parent);
		}
	}
	
	public static class SymbolSXP extends Node {
		protected SymbolSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_symbol(this);
		}
		@Override
		public String toString() {
			return "Symbol";
		}
		static SymbolSXP unique = flyweight ? new SymbolSXP(null) : null;
		static SymbolSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new SymbolSXP3(parent);
			return new SymbolSXP(parent);
		}
	}
	public static class SymbolSXP3 extends SymbolSXP {
		protected SymbolSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class ListSXP extends Node {
		protected ListSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_list(this);
		}
		@Override
		public String toString() {
			return "List";
		}
		static ListSXP unique = flyweight ? new ListSXP(null) : null;
		static ListSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new ListSXP3(parent);
			return new ListSXP(parent);
		}
	}
	public static class ListSXP3 extends ListSXP {
		protected ListSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class ClosSXP extends Node {
		protected ClosSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_closure(this);
		}
		@Override
		public String toString() {
			return "Closure";
		}
		static ClosSXP unique = flyweight ? new ClosSXP(null) : null;
		static ClosSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new ClosSXP3(parent);
			return new ClosSXP(parent);
		}
	}
	public static class ClosSXP3 extends ClosSXP {
		protected ClosSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class EnvSXP extends Node {
		protected EnvSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_env(this);
		}
		@Override
		public String toString() {
			return "Env";
		}
		static EnvSXP unique = flyweight ? new EnvSXP(null) : null;
		static EnvSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new EnvSXP3(parent);
			return new EnvSXP(parent);
		}
	}
	public static class EnvSXP3 extends EnvSXP {
		protected EnvSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	abstract public static class AbsPromise extends Node {
		private int id;
		private boolean is_new;
		protected AbsPromise(Node parent, boolean is_new) {
			super(parent);
			this.is_new = is_new;
		}
		
		public int getID() {
			return id;
		}
		public boolean is_new() {
			return is_new;
		}
		@Override
		public String toString() {
			return get_promise_type()+"PROM-0x"+Integer.toHexString(getID())+ (is_new ? "'" : "");
		}
		public abstract String get_promise_type();
		
		protected void build_node(TraceProcessor[] processors) throws Exception{
			id = readPtr(stream);
			if(is_new())
				setPromiseOwner(getID(), getCurrentContext());
		}
	}
	
	public static class UnbndPromise extends AbsPromise {
		protected UnbndPromise(Node parent, boolean is_new) {
			super(parent, is_new);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_unbnd(this);
		}
		public String get_promise_type() {
			return "U";
		}
	}
	
	public static class BndPromise extends AbsPromise {
		protected BndPromise(Node parent, boolean is_new) {
			super(parent, is_new);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_bnd(this);
		}
		public String get_promise_type() {
			return "B";
		}
	}
	
	public static class LangSXP extends Node {
		protected LangSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_lang(this);
		}
		@Override
		public String toString() {
			return "Lang";
		}
		static LangSXP unique = new LangSXP(null);
		static LangSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new LangSXP3(parent);
			return new LangSXP(parent);
		}
	}
	public static class LangSXP3 extends LangSXP {
		protected LangSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class SpecialSXP extends Node {
		protected SpecialSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_specialsxp(this);
		}
		@Override
		public String toString() {
			return "Special";
		}
		static SpecialSXP unique = flyweight ? new SpecialSXP(null) : null;
		static SpecialSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new SpecialSXP3(parent);
			return new SpecialSXP(parent);
		}
	}
	public static class SpecialSXP3 extends SpecialSXP {
		protected SpecialSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class BuiltinSXP extends Node {
		protected BuiltinSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_builtinsxp(this);
		}
		@Override
		public String toString() {
			return "BuiltIn";
		}
		static BuiltinSXP unique = flyweight ? new BuiltinSXP(null) : null;
		static BuiltinSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new BuiltinSXP3(parent);
			return new BuiltinSXP(parent);
		}
	}
	public static class BuiltinSXP3 extends BuiltinSXP {
		protected BuiltinSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	
	public static class CharSXP extends Node {
		protected CharSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_char(this);
		}
		@Override
		public String toString() {
			return "Char";
		}
		static CharSXP unique = flyweight ? new CharSXP(null) : null;
		static CharSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new CharSXP3(parent);
			return new CharSXP(parent);
		}
	}
	public static class CharSXP3 extends CharSXP {
		protected CharSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class LogicalSXP extends Node {
		protected LogicalSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_logical(this);
		}
		@Override
		public String toString() {
			return "Logical";
		}
		static LogicalSXP unique = flyweight ? new LogicalSXP(null) : null;
		static LogicalSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new LogicalSXP3(parent);
			return new LogicalSXP(parent);
		}
	}
	public static class LogicalSXP3 extends LogicalSXP {
		protected LogicalSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	abstract public static class ArraySXP extends Node {
		private int length;
		
		protected ArraySXP(Node parent) {
			super(parent);
		}
		
		protected void build_node(TraceProcessor[] processor) throws IOException{
			DataInputStream s = stream;
			length = readByte(s);
			readByte(s);
		}
		
		public int get_length(){
			return length;
		}
	}
	
	public static class VectorSXP extends ArraySXP {
		protected VectorSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_vector(this);
		}
		@Override
		public String toString() {
			return "Vector";
		}
		static VectorSXP unique = flyweight ? new VectorSXP(null) : null;
		static VectorSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new VectorSXP3(parent);
			return new VectorSXP(parent);
		}
	}
	public static class VectorSXP3 extends VectorSXP {
		protected VectorSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class IntSXP extends ArraySXP {
		protected IntSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_int(this);
		}
		@Override
		public String toString() {
			return "Int";
		}
		static IntSXP unique = flyweight ? new IntSXP(null) : null;
		static IntSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new IntSXP3(parent);
			return new IntSXP(parent);
		}
	}
	public static class IntSXP3 extends IntSXP {
		protected IntSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class RealSXP extends ArraySXP {
		protected RealSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_real(this);
		}
		@Override
		public String toString() {
			return "Real";
		}
		static RealSXP unique = flyweight ? new RealSXP(null) : null;
		static RealSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new RealSXP3(parent);
			return new RealSXP(parent);
		}
	}
	public static class RealSXP3 extends RealSXP {
		protected RealSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class CplxSXP extends ArraySXP {
		protected CplxSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_cplx(this);
		}
		@Override
		public String toString() {
			return "Complex";
		}
		static CplxSXP unique = flyweight ? new CplxSXP(null) : null;
		static CplxSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new CplxSXP3(parent);
			return new CplxSXP(parent);
		}
	}
	public static class CplxSXP3 extends CplxSXP {
		protected CplxSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class StringSXP extends ArraySXP {
		protected StringSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_string(this);
		}
		@Override
		public String toString() {
			return "String";
		}
		static StringSXP unique = flyweight ? new StringSXP(null) : null;
		static StringSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new StringSXP3(parent);
			return new StringSXP(parent);
		}
	}
	public static class StringSXP3 extends StringSXP {
		protected StringSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class ExprSXP extends ArraySXP {
		protected ExprSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_expr(this);
		}
		@Override
		public String toString() {
			return "Expr";
		}
		static ExprSXP unique = flyweight ? new ExprSXP(null) : null;
		static ExprSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new ExprSXP3(parent);
			return new ExprSXP(parent);
		}
	}
	public static class ExprSXP3 extends ExprSXP {
		protected ExprSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class DotSXP extends Node {
		protected DotSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_dot(this);
		}
		@Override
		public String toString() {
			return "Dot";
		}
		static DotSXP unique = flyweight ? new DotSXP(null) : null;
		static DotSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new DotSXP3(parent);
			return new DotSXP(parent);
		}
	}
	public static class DotSXP3 extends DotSXP {
		protected DotSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class AnySXP extends Node {
		protected AnySXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_any(this);
		}
		@Override
		public String toString() {
			return "Any";
		}
		static AnySXP unique = flyweight ? new AnySXP(null) : null;
		static AnySXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new AnySXP3(parent);
			return new AnySXP(parent);
		}
	}
	public static class AnySXP3 extends AnySXP {
		protected AnySXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class BCodeSXP extends Node {
		protected BCodeSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_bcode(this);
		}
		@Override
		public String toString() {
			return "Bcode";
		}
		static BCodeSXP unique = flyweight ? new BCodeSXP(null) : null;
		static BCodeSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new BCodeSXP3(parent);
			return new BCodeSXP(parent);
		}
	}
	public static class BCodeSXP3 extends BCodeSXP {
		protected BCodeSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class EPtrSxp extends Node {
		protected EPtrSxp(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_eptr(this);
		}
		@Override
		public String toString() {
			return "Eptr";
		}
		static EPtrSxp unique = flyweight ? new EPtrSxp(null) : null;
		static EPtrSxp make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new EPtrSxp3(parent);
			return new EPtrSxp(parent);
		}
	}
	public static class EPtrSxp3 extends EPtrSxp {
		protected EPtrSxp3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class WRefSXP extends Node {
		protected WRefSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_wref(this);
		}
		@Override
		public String toString() {
			return "WRef";
		}
		static WRefSXP unique = flyweight ? new WRefSXP(null) : null;
		static WRefSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new WRefSXP3(parent);
			return new WRefSXP(parent);
		}
	}
	public static class WRefSXP3 extends WRefSXP {
		protected WRefSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class RawSXP extends Node {
		protected RawSXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_raw(this);
		}
		@Override
		public String toString() {
			return "Raw";
		}
		static RawSXP unique = flyweight ? new RawSXP(null) : null;
		static RawSXP make_node(Node parent, boolean is_s3){
			if(flyweight)
				return unique;
			if(is_s3)
				return new RawSXP3(parent);
			return new RawSXP(parent);
		}
	}
	public static class RawSXP3 extends RawSXP {
		protected RawSXP3(Node parent) {
			super(parent);
		}
		public boolean is_s3() { return true ;}
	}
	
	public static class S4SXP extends Node {
		protected S4SXP(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_s4(this);
		}
		@Override
		public String toString() {
			return "S4";
		}
		static S4SXP unique = flyweight ? new S4SXP(null) : null;
		static S4SXP make_node(Node parent){
			if(flyweight)
				return unique;
			return new S4SXP(parent);
		}
	}
	public static class SpecialEvent extends Node {
		protected SpecialEvent(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_special_event(this);
		}
		@Override
		public String toString() {
			return "SEvent";
		}
		static SpecialEvent unique = flyweight ? new SpecialEvent(null) : null;
		static SpecialEvent make_node(Node parent){
			if(flyweight)
				return unique;
			return new SpecialEvent(parent);
		}
	}
	public static abstract class CompositeNode extends Node {
		private int body; // set to number of nodes created by current build_tree_until call
		protected CompositeNode(Node parent) {
			super(parent);
		}
		protected void setBody(int body) {
			this.body = body;
		}
		public int getBody(){
			return body;
		}
	}
	public static abstract class AbsCall extends CompositeNode {
//		private static final boolean check_args = Boolean.parseBoolean(System.getProperty("RTrace.NodeBuilder.checkargs", "false"));
//		private static final boolean check_returns = Boolean.parseBoolean(System.getProperty("RTrace.NodeBuilder.checkreturns", "false"));
		final private boolean has_prologue;
		private PrologueNode prologue;
		private int args;
		private Node _return;
		private int id; // PRIMOFFSET if PrimitiveCall, ??? if FunctionCall
		int by_position, more_args;

		protected AbsCall(Node parent, boolean hasPrologue) throws Exception {
			super(parent);
			has_prologue = hasPrologue;
			id = buildName();
		}
		public void setID(int id) {
			this.id = id;
		}
		public int getID() {
			return id;
		}
		public void setArgs(int args) {
			this.args = args;
		}
		public int getArgs(){
			return args;
		}
		public void setReturn(Node _return) {
			this._return = _return;
		}
		public Node getReturn(){
			return _return;
		}
		public int getPrologue() {
			return this.prologue.getBody();
		}
		public String getName() {
			int id = getID();
			return "0x"+Integer.toHexString(id);
		}
		public boolean hasPrologue(){
			return has_prologue;
		}
		protected void build_node(TraceProcessor[] processors) throws Exception {
			final DataInputStream s = stream;
			addContext(id);
			if(has_prologue) attach_args();
			build_body(processors);
			int bcode = readByte(s);

                        /* read return type */
			Node n = make_new_node(bcode, this);
			_return = n;
			n.accept_visitor(processors);
			dropContext();
		}
		
		protected void attach_args(){
//			prologues.removeLast(); // remove self
//			int size = prologues.size();
//			while(size -- > 0){
				Node current = prologues.removeLast();
				if(current instanceof PrologueNode){
					prologue = (PrologueNode)current;
//					break;
				} else
//					args ++; // TODO attach the arg
//			}
//			if(size < 0)
				throw_exception("No prologue matching", this);
		}

		abstract protected int buildName() throws Exception;
		
		private void build_body(TraceProcessor[] processors) throws Exception {
			setBody(build_tree_until(processors, this, END_EVAL_CALL));
		}

		/*
		private void build_args(TraceProcessor[] processors) throws Exception {
			args = build_tree_until(processors, this, 0x85);
		}
		
		public int build_prologue(TraceProcessor[] processors) throws Exception {
			final DataInputStream s = stream;
			int prolog = 0;
			int start = bytes_read;
			int current = start;
			int bcode = readByte(s);
			while((bcode & (~ 3 )) != 0x94){
				Node cnode = make_new_node(bcode, this);
				prolog ++;
				if(found_err){
					System.err.println(formatErrorString(bcode, start, current));
					break;
				}
				current = bytes_read;
				cnode.accept_visitor(processors);
				bcode = readByte(s);
			}
			prologue = prolog;
			return bcode;
		}
		*/
		public int get_by_keywords(){
			return 0;
		}
		public int get_by_position(){
			return by_position;
		}
		public int get_more_args(){
			return more_args;
		}
	}


	public static class FunctionCall extends AbsCall {
		int by_keywords;

		protected FunctionCall(Node parent, boolean has_prologue) throws Exception {
			super(parent, has_prologue);
		}

		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			for(TraceProcessor processor: processors)
				processor.getVisitor().pre_visit_apply_call(this);
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_apply_call(this);
		}
		
		@Override
		public String toString() {
			return "Function: "+getName();
		}

                // returns a location_id
		protected int buildName() throws Exception{
                        /* read parameters from trace */
			int addr = readPtr(stream);     // contents of passed SEXP ptr
			by_position = readByte(stream); // number of by-position params
			by_keywords = readByte(stream); // number of by-keyword params
			more_args = readByte(stream);   // number of other params
			return FunctionMap.load_and_get(addr, bytes_read);
		}
		
		public int get_by_keywords(){
			return by_keywords;
		}

	}
	
	public abstract static class PrimitiveCall extends AbsCall {

		protected PrimitiveCall(Node parent, boolean has_prologue) throws Exception {
			super(parent, has_prologue);
		}

		protected int buildName() throws IOException{
			int sid = readShort(stream);
			by_position = readByte(stream);
			more_args = readByte(stream);
			return DataBase.primitive_locations[sid];
		}
	}
	
	public static class SpecialCall extends PrimitiveCall { //TODO
		protected SpecialCall(Node parent, boolean has_prologue) throws Exception {
			super(parent, has_prologue);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			for(TraceProcessor processor: processors)
				processor.getVisitor().pre_visit_apply_special(this);
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_apply_special(this);
		}
		
		@Override
		public String toString() {
			return "Special: "+getName();
		}
	}

	public static class BuiltInCall extends PrimitiveCall { // TODO
		protected BuiltInCall(Node parent, boolean has_prologue) throws Exception {
			super(parent, has_prologue);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			for(TraceProcessor processor: processors)
				processor.getVisitor().pre_visit_apply_builtin(this);
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_apply_builtin(this);
		}
		
		@Override
		public String toString() {
			return "BuiltIn: "+getName();
		}
	}
	
	public static abstract class EvalAbsPromise extends CompositeNode {
		private int id;
		protected EvalAbsPromise(Node parent) {
			super(parent);
		}
		public void setID(int id) {
			this.id = id;
		}
		public int getID() {
			return id;
		}
		protected void build_node(TraceProcessor[] processors) throws Exception {
			id = readPtr(stream);
			setBody(build_tree_until(processors, this, END_EVAL_PROM));
		}
	}
	public static class EvalBndPromise extends EvalAbsPromise {
		protected EvalBndPromise(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			for(TraceProcessor processor: processors)
				processor.getVisitor().pre_visit_eval_bnd_promise(this);
			build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_eval_bnd_promise(this);
		}
		@Override
		public String toString() {
			return "Bnd Promise-0x"+Integer.toHexString(getID());
		}
	}
	public static class EvalUnBndPromise extends EvalAbsPromise {
		protected EvalUnBndPromise(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			for(TraceProcessor processor: processors)
				processor.getVisitor().pre_visit_eval_unbnd_promise(this);
			addContext(getPromiseOwner(getID()));
			build_node(processors);
			dropContext();
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_eval_unbnd_promise(this);
		}
		@Override
		public String toString() {
			return "UnBnd Promise-0x"+Integer.toHexString(getID());
		}
	}
	public static class UnkType extends Node {
		protected UnkType(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_unk_type(this);
		}
		@Override
		public String toString() {
			return "UnkType";
		}
		static UnkType unique = flyweight ? new UnkType(null) : null;
		static UnkType make_node(Node parent){
			if(flyweight)
				return unique;
			return new UnkType(parent);
		}
	}
	
	public static class UnkEvent extends Node {
		protected UnkEvent(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_unk_event(this);
		}
		@Override
		public String toString() {
			return "UnkEvent";
		}
		static UnkEvent unique = flyweight ? new UnkEvent(null) : null;
		static UnkEvent make_node(Node parent){
			if(flyweight)
				return unique;
			return new UnkEvent(parent);
		}
	}
	
	public static class RError extends Node {
		protected RError(Node parent) {
			super(parent);
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			// build_node(processors);
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_r_error(this);
		}
		public String toString() {
			return "Rerror";
		}
		private static RError unique = flyweight ? new RError(null) : null;
		public static RError make_node(Node parent){
			if(flyweight)
				return unique;
			return new RError(parent);
		}
	}
	
	public static class LogError extends Node {
		String text = "";
		protected LogError(Node parent) {
			super(parent);
		}
		public LogError(Node parent, String string) {
			this(parent);
			text = string;
		}
		@Override
		protected void accept_visitor(TraceProcessor[] processors) throws Exception {
			for(TraceProcessor processor: processors)
				processor.getVisitor().visit_log_error(this);
		}
		public String toString() {
			if(text != "")
				return "LogError: "+ text;
			return "LogError";
		}
		public String getText(){
			return text;
		}
	}

	// return value is number of nodes created on this level
        // (i.e. trace-opcodes read, not counting recursion)
	static private int build_tree_until(TraceProcessor[] processors, Node parent, int stop) throws Exception{
		final DataInputStream s = stream;

		int nodes = 0;
		/* TODO: Add flyweight object (i.e. reuse already instanciated object */
		if(found_err) return 0;
		int bcode = -1;
		int last_good_read = bytes_read;
		int begin_position = bytes_read;
		try {
			while(!found_err){
				bcode = readByte(s);
				if(stop == bcode) // not very elegant way to exit from this loop, but this avoid some tests
					break;
				Node node = make_new_node(bcode, parent);
				nodes ++;
				if(!found_err){
					last_good_read = bytes_read;
					//prologues.addLast(node);
					node.accept_visitor(processors);
				}
			}
		} catch (EOFException e) {
			// Reach EOF, it's ok ... or not, do something if stop != -1
			if(stop != -1){
				// TODO an error message
				System.err.println(formatErrorString(bcode, begin_position, last_good_read)+ "\texpecting 0x"+Integer.toHexString(stop)+", found EOF ("+e.getMessage()+")");
				found_err = true;
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			System.err.println(formatErrorString(bcode, begin_position, last_good_read)+ "\t: "+e.getMessage());
			throw e;
		}
		if(found_err)
			System.err.println(formatErrorString(bcode, begin_position, last_good_read));
		return nodes;
	}
	
	static private void read_header(DataInputStream stream) throws IOException, Exception{
		byte header[] = new byte[12];
		
		if(old_header){
			header = new byte[5];
			stream.read(header);
			return ;
		}
		
		if(stream.read(header) < 12)
			throw new Exception("Bad header format: "+new String(header));
		bytes_read += header.length;
		if(RTrace.supported_version == null || new String(header).equals(RTrace.supported_version))
			return;
		String msg = "Version tag '"+new String(header)+"' differs from '"+RTrace.supported_version+"'";
		if(strict) // TODO hardcode some supported versions
			throw new Exception(msg);
		else
			System.err.println(msg+" ... but we try ;)");
	}
	static private String formatErrorString(int bcode, int start_position, int last_good_read){
		return "Error while building node: 0x"+Integer.toHexString(bcode)+"\t"+
		"Start position: 0x"+Integer.toHexString(start_position)+"\t"+
		"Last Good read: 0x"+Integer.toHexString(last_good_read);
	}
	@SuppressWarnings("unused")
	static private int readNumber(DataInputStream stream, int size) throws IOException { // Size in bytes
		int value = 0;
		int current = 0; // FIXME Why using a byte is buggy ?
		for(int i = 0; i < size; i ++){
			current = stream.readByte() & 0xFF; // make a byte;
			if(littleEndian)
				value = (value << 8) | current;
			else
				value |= (current << (8*i));
		}
		bytes_read += size;
		return value;
	}
	static private int readInt(DataInputStream stream) throws IOException {
		// return readNumber(stream, 4);
		int v = stream.readInt();
		bytes_read += 4;
		if(littleEndian)
			return v;
		return (v >>> 24) | (v << 24) | 
	      ((v << 8) & 0x00FF0000) | ((v >> 8) & 0x0000FF00);
	}
	static private int readByte(DataInputStream stream) throws IOException {
		int b = stream.readByte() & 0xFF;
		bytes_read ++;
		return b;
	}
	static private int readShort(DataInputStream stream) throws IOException {
	//	return readNumber(stream, 2) & 0xFFFF;
		int v = stream.readShort();
		bytes_read += 2;
		return (v&0xFF) << 8 | (v & 0xFF00) >> 8; // & 0xFFFF;*/
	}
	static private int readPtr(DataInputStream stream) throws IOException {
		//return readNumber(stream, 8);
		int v = readInt(stream);
		if(!_32bitmode)
			readInt(stream); // discards 0 ... and reads one int too much for 32bit (go)
		return v;
	}
	static private LogError throw_exception(String msg, Node parent){
		found_err = true;
		msg += " at position: 0x"+Integer.toHexString(bytes_read);
		System.err.println(msg);
		if(error_msg == null)
			error_msg = new ArrayList<String>();
		error_msg.add(msg);
		return new Node.LogError(parent, msg);
	}
	public static final int NIL_SXP = 0x00;
	public static final int SYMBOL_SXP = 0x01;
	public static final int LIST_SXP = 0x02;
	public static final int CLOS_SXP = 0x03;
	public static final int ENV_SXP = 0x04;
	public static final int LANG_SXP = 0x06;
	public static final int SPECIAL_SXP = 0x07;
	public static final int BUILTIN_SXP = 0x08;
	public static final int CHAR_SXP = 0x09;
	public static final int LOGICAL_SXP = 0x0A;
	public static final int INT_SXP = 0x0D;
	public static final int REAL_SXP = 0x0E;
	public static final int CPLX_SXP = 0x0F;
	public static final int STRING_SXP = 0x10;
	public static final int DOT_SXP = 0x11;
	public static final int ANY_SXP = 0x12;
	public static final int VECTOR_SXP = 0x13;
	public static final int EXPR_SXP = 0x14;
	public static final int BCODE_SXP = 0x15;
	public static final int EPTR_SXP = 0x16;
	public static final int WREF_SXP = 0x17;
	public static final int RAW_SXP = 0x18;
	public static final int S4_SXP = 0x19;
	public static final int UNIT_SXP = 0x20;


	public static final int S3FLAG = 0x40;

	public static final int SPECIAL_EVENT = 0x80;
	public static final int UNK_TYPE = 0x8A;
	public static final int UNK_EVENT = 0x8B;
	public static final int R_ERROR = 0x8C;
	public static final int LOG_ERROR = 0x8D;
	
	public static final int PROM_NEW_MOD = 0x40; // NEW_PROMISE / "value 0x40 is still in r-trace"[orig comment]
	public static final int UNBND_PROM = 0x8E;   // UBND / emit_simple_type
	public static final int BND_PROM = 0x8F;     // BND  / emit_simple_type
	public static final int EVAL_BND_PROM = 0x87;   // BND_PROM_START  / emit_bnd_promise
	public static final int EVAL_UNBND_PROM = 0x88; // UBND_PROM_START / emit_unbnd_promise
	public static final int END_EVAL_PROM = 0x89;   // PROM_END
	
	public static final int PROLOGUE = 0x96;
	public static final int END_PROLOGUE = 0x97;

	public static final int EVAL_BUILTIN = 0x90;
	public static final int EVAL_CLOSURE = 0x94;    // CLOS_ID
	public static final int EVAL_SPECIAL = 0x92;
	public static final int NO_PROLOGUE_MOD = 0x01;
	public static final int END_EVAL_CALL = 0x86;   // FUNC_END / emit_function_return
	
	static private Node make_new_node(int node_id, Node parent) throws Exception{
		switch(node_id){
		case NIL_SXP:
			return NilSXP.make_node(parent);
		case SYMBOL_SXP:
		case SYMBOL_SXP | S3FLAG:
			return SymbolSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case LIST_SXP:
		case LIST_SXP | S3FLAG:
			return ListSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case CLOS_SXP:
		case CLOS_SXP | S3FLAG:
			return ClosSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case ENV_SXP:
		case ENV_SXP | S3FLAG:
			return EnvSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case LANG_SXP:
		case LANG_SXP | S3FLAG:
			return LangSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case SPECIAL_SXP:
		case SPECIAL_SXP | S3FLAG:
			return SpecialSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case BUILTIN_SXP:
		case BUILTIN_SXP | S3FLAG:
			return BuiltinSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case CHAR_SXP:
		case CHAR_SXP | S3FLAG:
			return CharSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case LOGICAL_SXP:
		case LOGICAL_SXP | S3FLAG:
			return LogicalSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case INT_SXP:
		case INT_SXP | S3FLAG:
			return IntSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case REAL_SXP:
		case REAL_SXP | S3FLAG:
			return RealSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case CPLX_SXP:
		case CPLX_SXP | S3FLAG:
			return CplxSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case STRING_SXP:
		case STRING_SXP | S3FLAG:
			return StringSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case DOT_SXP:
		case DOT_SXP | S3FLAG:
			return DotSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case ANY_SXP:
		case ANY_SXP | S3FLAG:
			return AnySXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case VECTOR_SXP:
		case VECTOR_SXP | S3FLAG:
			return VectorSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case EXPR_SXP:
		case EXPR_SXP | S3FLAG:
			return ExprSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case BCODE_SXP:
		case BCODE_SXP | S3FLAG:
			return BCodeSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case EPTR_SXP:
		case EPTR_SXP | S3FLAG:
			return EPtrSxp.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case WREF_SXP:
		case WREF_SXP | S3FLAG:
			return WRefSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case RAW_SXP:
		case RAW_SXP | S3FLAG:
			return RawSXP.make_node(parent, (node_id & S3FLAG) == S3FLAG);
		case S4_SXP:
		case S4_SXP | S3FLAG:
			return S4SXP.make_node(parent);
		case UNIT_SXP:
			return UnitSXP.make_node(parent);
		case SPECIAL_EVENT:
			return SpecialEvent.make_node(parent);
		case PROLOGUE:
			return new Node.PrologueNode(parent);
		case EVAL_BND_PROM:
			return new Node.EvalBndPromise(parent);
		case EVAL_UNBND_PROM:
			return new Node.EvalUnBndPromise(parent);
		case UNK_TYPE:
			return UnkType.make_node(parent);
		case UNK_EVENT:
			return UnkEvent.make_node(parent);
		case R_ERROR:
			return RError.make_node(parent);
		case LOG_ERROR:
			return new Node.LogError(parent);
		case UNBND_PROM:
		case UNBND_PROM | PROM_NEW_MOD:
			return new Node.UnbndPromise(parent, (node_id & PROM_NEW_MOD) != 0);
		case BND_PROM:
		case BND_PROM | PROM_NEW_MOD:
			return new Node.BndPromise(parent, (node_id & PROM_NEW_MOD) != 0);
		case EVAL_BUILTIN:
		case EVAL_BUILTIN | NO_PROLOGUE_MOD:
			return new Node.BuiltInCall(parent, (node_id & NO_PROLOGUE_MOD) == 0);
		case EVAL_SPECIAL:
		case EVAL_SPECIAL | NO_PROLOGUE_MOD:
			return new Node.SpecialCall(parent, (node_id & NO_PROLOGUE_MOD) == 0);
		case EVAL_CLOSURE:
		case EVAL_CLOSURE | NO_PROLOGUE_MOD:
			return new Node.FunctionCall(parent, (node_id & NO_PROLOGUE_MOD) == 0);
		}
		// TODO change to a more suitable exception
		return throw_exception("Unhandled node type: 0x"+Integer.toHexString(node_id), parent);
	}
}

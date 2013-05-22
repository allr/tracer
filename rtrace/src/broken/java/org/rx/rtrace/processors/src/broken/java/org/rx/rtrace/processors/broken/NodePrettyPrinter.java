package org.rx.rtrace.processors;

import java.io.PrintStream;

import org.rx.rtrace.BasicNodeVisitor;
import org.rx.rtrace.Node;
import org.rx.rtrace.NodeVisitor;
import org.rx.rtrace.Node.*;


public class NodePrettyPrinter extends BasicProcessor {
	int level = 0;
	PrintStream stream = System.out;

	public void process_root(Node node, NodeVisitor v) throws Exception {
		super.process_root(node, v);
		if(level != 0){
			System.err.println("Unbalanced trace, level is supposed to be 0, but it's: "+level);
			level = 0;
		}
	}
	
	/////// Usefull methods
	protected void out(String str){
		stream.print(str);
	}
	protected void outln(String str){
		stream.println(str);

		for(int i = 0; i < level; i ++)
			stream.print(' ');
	}
	@Override
	protected NodeVisitor make_visitor(){
		return new BasicNodeVisitor() {
			@Override
			public void visit_nil(NilSXP node) throws Exception {
				out("Nil");
			}

			@Override
			public void visit_symbol(SymbolSXP node) throws Exception {
				out("Symbol");
			}

			@Override
			public void visit_list(ListSXP node) throws Exception {
				out("List");
			}

			@Override
			public void visit_closure(ClosSXP node) throws Exception {
				out("Clos");	
			}

			@Override
			public void visit_env(EnvSXP node) throws Exception {
				out("Env");
			}

			@Override
			public void visit_promise(AbsPromise node) throws Exception {
				out("PROM-0x"+Integer.toHexString(node.getID()));
			}

			@Override
			public void visit_lang(LangSXP node) throws Exception {
				out("Lang");
			}

			@Override
			public void visit_special(SpecialSXP node) throws Exception {
				out("Special");
			}

			@Override
			public void visit_builtin(BuiltinSXP node) throws Exception {
				out("BuiltIn");
			}

			@Override
			public void visit_char(CharSXP node) throws Exception {
				out("Char");
			}

			@Override
			public void visit_logical(LogicalSXP node) throws Exception {
				out("Logical");
			}

			@Override
			public void visit_int(IntSXP node) throws Exception {
				out("Int");
			}

			@Override
			public void visit_real(RealSXP node) throws Exception {
				out("Real");
			}

			@Override
			public void visit_cplx(CplxSXP node) throws Exception {
				out("Cplx");
			}

			@Override
			public void visit_string(StringSXP node) throws Exception {
				out("String");
			}

			@Override
			public void visit_dot(DotSXP node) throws Exception {
				out("Dot");
			}

			@Override
			public void visit_any(AnySXP node) throws Exception {
				out("Any");
			}

			@Override
			public void visit_vector(VectorSXP node) throws Exception {
				out("Vector");
			}

			@Override
			public void visit_expr(ExprSXP node) throws Exception {
				out("Expr");
			}

			@Override
			public void visit_bcode(BCodeSXP node) throws Exception {
				out("BCode");
			}

			@Override
			public void visit_eptr(EPtrSxp node) throws Exception {
				out("ExternalPtr");
			}

			@Override
			public void visit_wref(WRefSXP node) throws Exception {
				out("WeakRef");
			}

			@Override
			public void visit_raw(RawSXP node) throws Exception {
				out("Raw");
			}

			@Override
			public void visit_s4(S4SXP node) throws Exception {
				out("S4");
			}

			@Override
			public void visit_special_event(SpecialEvent node) throws Exception {
				out("SPEC_EVNT");
			}

			public void visit_apply_abscall(AbsCall node, String fname) throws Exception {
				if(fname == null)
					fname = "0x"+Integer.toHexString(node.getID());
				out("| "+fname+" ");
				level ++;

				node.visit_prologue(this);

				out("("+node.getNbArgs());
				for(Node arg: node.getArgs()){
					out(" ");
					visit(arg);
				}

				outln(") ["); 
				for(Node body: node.getBody()){
					visit(body);
					out(" ");
				}
				out("]");
				node.visit_returns(this);
				level --;
				outln("|");
			}

			@Override
			public void visit_apply_call(FunctionCall node) throws Exception {
				String fname = node.getName();
				visit_apply_abscall(node, fname);
			}

			@Override
			public void visit_apply_closure(Closure node) throws Exception {
				//		throw new Exception("Unimplemented operation: "+ node.getClass().toString()+ " ("+node.toString()+")");
				visit_apply_abscall(node, "");
			}

			@Override
			public void visit_eval_bnd_promise(EvalBndPromise node) throws Exception {
				visit_abs_promise(node, "1");
			}

			@Override
			public void visit_eval_unbnd_promise(EvalUnBndPromise node) throws Exception {
				visit_abs_promise(node, "0");
			}
			public void visit_abs_promise(EvalAbsPromise node, String flag) throws Exception {
				out("{ 0x"+Integer.toHexString(node.getID())+" "+flag);
				for(Node body: node.getBody()){
					out(" ");
					visit(body);
				}
				out("}");
			}

			@Override
			public void visit_unk_type(UnkType node) throws Exception {
				out("UT");
			}

			@Override
			public void visit_unk_event(UnkEvent node) throws Exception {
				out("UE");
			}

			@Override
			public void visit_r_error(RError node) throws Exception {
				out("R_ERR");
			}

			@Override
			public void visit_log_error(LogError node) throws Exception {
				out("TRACE_ERR "+node.getText());	
			}

			@Override
			public void visit_unbnd(UnbndPromise node) throws Exception {
				out("0"); // FIXME what are this two tokens
			}

			@Override
			public void visit_bnd(BndPromise node) throws Exception {
				out("1"); // FIXME what are this two tokens
			}
		};
	}
}

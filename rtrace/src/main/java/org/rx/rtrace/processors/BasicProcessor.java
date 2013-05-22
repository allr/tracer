package org.rx.rtrace.processors;


import java.sql.Connection;
import java.sql.SQLException;

import org.rx.rtrace.Node;
import org.rx.rtrace.Node.AbsCall;
import org.rx.rtrace.Node.AbsPromise;
import org.rx.rtrace.Node.AnySXP;
import org.rx.rtrace.Node.BCodeSXP;
import org.rx.rtrace.Node.BndPromise;
import org.rx.rtrace.Node.BuiltinSXP;
import org.rx.rtrace.Node.BuiltInCall;
import org.rx.rtrace.Node.CharSXP;
import org.rx.rtrace.Node.ClosSXP;
import org.rx.rtrace.Node.CplxSXP;
import org.rx.rtrace.Node.DotSXP;
import org.rx.rtrace.Node.EPtrSxp;
import org.rx.rtrace.Node.EnvSXP;
import org.rx.rtrace.Node.EvalAbsPromise;
import org.rx.rtrace.Node.EvalBndPromise;
import org.rx.rtrace.Node.EvalUnBndPromise;
import org.rx.rtrace.Node.ExprSXP;
import org.rx.rtrace.Node.FunctionCall;
import org.rx.rtrace.Node.IntSXP;
import org.rx.rtrace.Node.LangSXP;
import org.rx.rtrace.Node.ListSXP;
import org.rx.rtrace.Node.LogError;
import org.rx.rtrace.Node.LogicalSXP;
import org.rx.rtrace.Node.NilSXP;
import org.rx.rtrace.Node.PrologueNode;
import org.rx.rtrace.Node.RError;
import org.rx.rtrace.Node.RawSXP;
import org.rx.rtrace.Node.RealSXP;
import org.rx.rtrace.Node.RootNode;
import org.rx.rtrace.Node.S4SXP;
import org.rx.rtrace.Node.SpecialCall;
import org.rx.rtrace.Node.SpecialEvent;
import org.rx.rtrace.Node.SpecialSXP;
import org.rx.rtrace.Node.StringSXP;
import org.rx.rtrace.Node.SymbolSXP;
import org.rx.rtrace.Node.UnbndPromise;
import org.rx.rtrace.Node.UnitSXP;
import org.rx.rtrace.Node.UnkEvent;
import org.rx.rtrace.Node.UnkType;
import org.rx.rtrace.Node.VectorSXP;
import org.rx.rtrace.Node.WRefSXP;

public abstract class BasicProcessor implements TraceProcessor {
	private NodeVisitor visitor;
	
	private final Connection conn;
	
	public BasicProcessor(Connection conn) {
		this.conn = conn;
	}
	
	@Override
	public void process_trace(Node[] roots) throws Exception {
	}
	public NodeVisitor getVisitor(){
		return visitor;
	}
	protected void setVisitor(NodeVisitor visitor){
		this.visitor = visitor;
	}
	
	/**
	 * Simply calls the {@link #create_schema()} method.
	 * @see {@link TraceProcessor#initialize_processor(String[])}
	 */
	@Override
	public void initialize_processor(String[] options) throws Exception {
		create_schema();
	}

	protected void create_schema() throws SQLException {}
	
	protected Connection getConnection() {
		return conn;
	}
	
	@Override
	public void finalize_trace(int id) throws SQLException {}
	
	@Override
	public boolean initialize_trace(int id) throws Exception {
		setVisitor(make_visitor());
		return true;
	}

	protected double round_percent(double numerator, double denominator){
		if(denominator == 0)
			return 100;
		return ((double)Math.round((((double)numerator)/((double) denominator))*10000))/100;
	}

	protected NodeVisitor make_visitor(){
		return new BasicNodeVisitor();
	}
	
	class NodeVisitorAdapter implements NodeVisitor {
		@Override
		public void visit_nil(NilSXP node) throws Exception {}
		
		@Override
		public void visit_unit(UnitSXP node) throws Exception {}

		@Override
		public void visit_symbol(SymbolSXP node) throws Exception {}

		@Override
		public void visit_list(ListSXP node) throws Exception {}

		@Override
		public void visit_closure(ClosSXP node) throws Exception {}

		@Override
		public void visit_env(EnvSXP node) throws Exception {}

		@Override
		public void visit_unbnd(UnbndPromise node) throws Exception {}

		@Override
		public void visit_bnd(BndPromise node) throws Exception {}

		@Override
		public void visit_lang(LangSXP node) throws Exception {}

		@Override
		public void visit_specialsxp(SpecialSXP node) throws Exception {}

		@Override
		public void visit_builtinsxp(BuiltinSXP node) throws Exception {}

		@Override
		public void visit_char(CharSXP node) throws Exception {}

		@Override
		public void visit_logical(LogicalSXP node) throws Exception {}

		@Override
		public void visit_int(IntSXP node) throws Exception {}

		@Override
		public void visit_real(RealSXP node) throws Exception {}

		@Override
		public void visit_cplx(CplxSXP node) throws Exception {}

		@Override
		public void visit_string(StringSXP node) throws Exception {}

		@Override
		public void visit_dot(DotSXP node) throws Exception {}

		@Override
		public void visit_any(AnySXP node) throws Exception {}

		@Override
		public void visit_vector(VectorSXP node) throws Exception {}

		@Override
		public void visit_expr(ExprSXP node) throws Exception {}

		@Override
		public void visit_bcode(BCodeSXP node) throws Exception {}

		@Override
		public void visit_eptr(EPtrSxp node) throws Exception {}

		@Override
		public void visit_wref(WRefSXP node) throws Exception {}

		@Override
		public void visit_raw(RawSXP node) throws Exception {}

		@Override
		public void visit_s4(S4SXP node) throws Exception {}
				
		@Override
		public void visit_special_event(SpecialEvent node) throws Exception {}

		@Override
		public void visit_apply_call(FunctionCall node) throws Exception {}

		@Override
		public void visit_apply_builtin(BuiltInCall node) throws Exception {}

		@Override
		public void visit_apply_special(SpecialCall node) throws Exception {}

		@Override
		public void visit_eval_bnd_promise(EvalBndPromise node)
				throws Exception {}

		@Override
		public void visit_eval_unbnd_promise(EvalUnBndPromise node)
				throws Exception {}

		@Override
		public void visit_root(RootNode rootNode) throws Exception {}

		@Override
		public void visit_unk_type(UnkType node) throws Exception {}

		@Override
		public void visit_unk_event(UnkEvent node) throws Exception {}

		@Override
		public void visit_r_error(RError node) throws Exception {}

		@Override
		public void visit_log_error(LogError node) throws Exception {}
		
		@Override
		public void pre_visit_root(RootNode rootNode) {}
		
		@Override
		public void pre_visit_eval_unbnd_promise(EvalUnBndPromise node)
		throws Exception {}
		
		@Override
		public void pre_visit_eval_bnd_promise(EvalBndPromise node)
		throws Exception {}
		
		@Override
		public void pre_visit_apply_special(SpecialCall node) throws Exception {}
		
		@Override
		public void pre_visit_apply_call(FunctionCall node) throws Exception {}
		
		@Override
		public void pre_visit_apply_builtin(BuiltInCall node) throws Exception {}

		@Override
		public void visit_prologue(PrologueNode prologueNode) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void pre_visit_prologue(PrologueNode prologueNode) {
			// TODO Auto-generated method stub
			
		}		
	}
	
	class BasicNodePostVisitor implements NodeVisitor {
		protected void visit_node(Node node) throws Exception {}
		@Override
		public void visit_root(RootNode node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_nil(NilSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_unit(UnitSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_symbol(SymbolSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_list(ListSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_closure(ClosSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_env(EnvSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_unbnd(UnbndPromise node) throws Exception {
			visit_node(node);
		}
		protected void visit_promise(AbsPromise node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_bnd(BndPromise node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_lang(LangSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_specialsxp(SpecialSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_builtinsxp(BuiltinSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_char(CharSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_logical(LogicalSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_int(IntSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_real(RealSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_cplx(CplxSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_string(StringSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_dot(DotSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_any(AnySXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_vector(VectorSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_expr(ExprSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_bcode(BCodeSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_eptr(EPtrSxp node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_wref(WRefSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_raw(RawSXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_s4(S4SXP node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_special_event(SpecialEvent node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_apply_call(FunctionCall node) throws Exception {
			visit_abs_call(node);
		}
		protected void visit_abs_call(AbsCall node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_eval_bnd_promise(EvalBndPromise node) throws Exception {
			visit_eval_abs_promise(node);
		}
		@Override
		public void visit_eval_unbnd_promise(EvalUnBndPromise node) throws Exception {
			visit_eval_abs_promise(node);
		}
		protected void visit_eval_abs_promise(EvalAbsPromise node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_unk_type(UnkType node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_unk_event(UnkEvent node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_r_error(RError node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_log_error(LogError node) throws Exception {
			visit_node(node);
		}
		@Override
		public void visit_apply_builtin(BuiltInCall node) throws Exception {
			visit_abs_call(node);
		}
		@Override
		public void visit_apply_special(SpecialCall node) throws Exception {
			visit_abs_call(node);
		}
		@Override
		public void pre_visit_apply_call(FunctionCall node) throws Exception {}
		@Override
		public void pre_visit_apply_builtin(BuiltInCall node) throws Exception {}
		@Override
		public void pre_visit_apply_special(SpecialCall node) throws Exception {}
		@Override
		public void pre_visit_eval_bnd_promise(EvalBndPromise node)
				throws Exception {}
		@Override
		public void pre_visit_eval_unbnd_promise(EvalUnBndPromise node)
				throws Exception {}
		@Override
		public void pre_visit_root(RootNode node) throws Exception  {}
		@Override
		public void visit_prologue(PrologueNode node) throws Exception {
			visit_node(node);
		}
		@Override
		public void pre_visit_prologue(PrologueNode node) {}
	}
	
	class BasicNodeVisitor extends BasicNodePostVisitor {
		public void pre_visit_abs_call(AbsCall node) throws Exception {}
		public void pre_visit_abs_promise(EvalAbsPromise node) throws Exception {}
		@Override
		public void pre_visit_apply_call(FunctionCall node) throws Exception {
			pre_visit_abs_call(node);
		}
		@Override
		public void pre_visit_apply_builtin(BuiltInCall node) throws Exception {
			pre_visit_abs_call(node);
		}
		@Override
		public void pre_visit_apply_special(SpecialCall node) throws Exception {
			pre_visit_abs_call(node);
		}
		@Override
		public void pre_visit_eval_bnd_promise(EvalBndPromise node)
				throws Exception {
			pre_visit_abs_promise(node);
		}
		@Override
		public void pre_visit_eval_unbnd_promise(EvalUnBndPromise node)
				throws Exception {
			pre_visit_abs_promise(node);
		}
	}
}
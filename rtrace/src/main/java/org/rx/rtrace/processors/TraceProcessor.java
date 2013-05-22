package org.rx.rtrace.processors;


import java.sql.SQLException;

import org.rx.rtrace.Node;
import org.rx.rtrace.Node.*;


public interface TraceProcessor {
	/**
	 * Call during the options parsing
	 * @param opts
	 * @throws Exception
	 */
	public void initialize_processor(String[] opts) throws Exception;
	/**
	 * Call before processing a trace
	 * @return need to proceed the trace
	 * @throws Exception
	 */
	public boolean initialize_trace(int id) throws Exception;
	
	/**
	 * Process the trace (main entry point)
	 * @param roots
	 * @throws Exception
	 */
	public void process_trace(Node[] roots) throws Exception;
	/**
	 * update stats after processing
	 * @throws SQLException 
	 */
	public void finalize_trace(int id) throws SQLException;
	
	public NodeVisitor getVisitor();
	
	public interface NodeVisitor {		
		void visit_nil(NilSXP node) throws Exception;
		void visit_unit(UnitSXP node) throws Exception;
		void visit_symbol(SymbolSXP node) throws Exception;
		void visit_list(ListSXP node) throws Exception;
		void visit_closure(ClosSXP node) throws Exception;
		void visit_env(EnvSXP node) throws Exception;
		
		void visit_unbnd(UnbndPromise node) throws Exception;
		void visit_bnd(BndPromise node) throws Exception;
		
		void visit_lang(LangSXP node) throws Exception;
		void visit_specialsxp(SpecialSXP node) throws Exception;
		void visit_builtinsxp(BuiltinSXP node) throws Exception;
		void visit_char(CharSXP node) throws Exception;
		void visit_logical(LogicalSXP node) throws Exception;
		void visit_int(IntSXP node) throws Exception;
		void visit_real(RealSXP node) throws Exception;
		void visit_cplx(CplxSXP node) throws Exception;
		void visit_string(StringSXP node) throws Exception;
		void visit_dot(DotSXP node) throws Exception;
		void visit_any(AnySXP node) throws Exception;
		void visit_vector(VectorSXP node) throws Exception;
		void visit_expr(ExprSXP node) throws Exception;
		void visit_bcode(BCodeSXP node) throws Exception;
		void visit_eptr(EPtrSxp node) throws Exception;
		void visit_wref(WRefSXP node) throws Exception;
		void visit_raw(RawSXP node) throws Exception;
		void visit_s4(S4SXP node) throws Exception;
		void visit_special_event(SpecialEvent node) throws Exception;
		
		void visit_apply_call(FunctionCall node) throws Exception;
		void pre_visit_apply_call(FunctionCall node) throws Exception;
		void visit_apply_builtin(BuiltInCall node) throws Exception;
		void pre_visit_apply_builtin(BuiltInCall node) throws Exception;
		void visit_apply_special(SpecialCall node) throws Exception;
		void pre_visit_apply_special(SpecialCall node) throws Exception;
		
		void visit_eval_bnd_promise(EvalBndPromise node) throws Exception;
		void pre_visit_eval_bnd_promise(EvalBndPromise node) throws Exception;
		void visit_eval_unbnd_promise(EvalUnBndPromise node) throws Exception;
		void pre_visit_eval_unbnd_promise(EvalUnBndPromise node) throws Exception;

		void visit_root(RootNode rootNode) throws Exception;
		void pre_visit_root(RootNode rootNode) throws Exception;
		
		void visit_unk_type(UnkType node) throws Exception;
		void visit_unk_event(UnkEvent node) throws Exception;
		void visit_r_error(RError node) throws Exception;
		void visit_log_error(LogError node) throws Exception;
		void visit_prologue(PrologueNode prologueNode) throws Exception;
		void pre_visit_prologue(PrologueNode prologueNode) throws Exception;
	}
}

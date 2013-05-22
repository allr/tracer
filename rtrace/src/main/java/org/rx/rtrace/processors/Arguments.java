package org.rx.rtrace.processors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rx.DataBase;
import org.rx.rtrace.Node;
import org.rx.rtrace.Node.AbsCall;
import org.rx.rtrace.Node.BuiltInCall;
import org.rx.rtrace.Node.BuiltinSXP;
import org.rx.rtrace.Node.ClosSXP;
import org.rx.rtrace.Node.EnvSXP;
import org.rx.rtrace.Node.FunctionCall;
import org.rx.rtrace.Node.LangSXP;
import org.rx.rtrace.Node.ListSXP;
import org.rx.rtrace.Node.PrologueNode;
import org.rx.rtrace.Node.S4SXP;
import org.rx.rtrace.Node.SpecialCall;
import org.rx.rtrace.Node.SpecialSXP;
import org.rx.rtrace.Node.SymbolSXP;
import org.rx.rtrace.Node.VectorSXP;

public class Arguments extends BasicProcessor {
	protected Map<Integer, ArgumentsCounter> counters = new LinkedHashMap<Integer, ArgumentsCounter>();
	ArrayDeque<ArgumentsCounter> queued_counters = new ArrayDeque<ArgumentsCounter>();
	ArgumentsCounter fake = new ArgumentsCounter();
	
	public Arguments(Connection conn) {
		super(conn);
	}
	
	@Override
	public void initialize_processor(String[] options) throws Exception {
		super.initialize_processor(options);
		queued_counters.clear();
		queued_counters.push(fake);
	}
	
	@Override
	public void create_schema() throws SQLException {
		Connection conn = getConnection();
		boolean ok = false;
		try {
			DataBase.create_table(conn, "arguments", "trace_id reference traces," +
					"location_id reference locations, s3 integer, legals3 integer, s4 integer, env integer, " +
					"lang integer, closure integer, special integer, builtin integer");
			ok = true;
		} finally {
			DataBase.commitOrRollbackIfNeeded(conn, ok);
		}
	}
	
	@Override
	public void finalize_trace(int id) throws SQLException {
		Connection conn = getConnection();
		boolean ok = false;
		try {
			PreparedStatement stmt = conn.prepareStatement("delete from arguments where trace_id="+id);
			try {
				stmt.executeUpdate();
			} finally {
				stmt.close();
			}

			stmt = conn.prepareStatement("insert into arguments "+
					"(trace_id, location_id, s3, legals3, s4, env, lang, closure, special, builtin)"+
					"values ("+id+", ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			try {
				for(int counter_id: counters.keySet()){
					stmt.setInt(1, counter_id);
					ArgumentsCounter cnt = counters.get(counter_id);
					stmt.setInt(2, cnt.s3);
					stmt.setInt(3, cnt.legals3);
					stmt.setInt(4, cnt.s4);
					stmt.setInt(5, cnt.env);
					stmt.setInt(6, cnt.lang);
					stmt.setInt(7, cnt.closure);
					stmt.setInt(8, cnt.special);
					stmt.setInt(9, cnt.builtin);
					stmt.addBatch();
				}
				stmt.executeBatch();
			} finally {
				stmt.close();
			}
			
			ok = true;
		} finally {
			DataBase.commitOrRollbackIfNeeded(conn, ok);
		}
	}
	
	class ArgumentsCounter {
		int s3;
		int legals3;
		int s4;
		int env;
		int lang;
		int closure;
		int special;
		int builtin;
		int other;
		void increment(ArgumentsCounter cmpt){
			s3 += cmpt.s3;
			legals3 += cmpt.legals3;
			s4 += cmpt.s4;
			env += cmpt.env;
			lang += cmpt.lang;
			closure += cmpt.closure;
			special += cmpt.special;
			builtin += cmpt.builtin;
			other += cmpt.other;
		}
	}

	@Override
	protected NodeVisitor make_visitor(){
		return new BasicNodePostVisitor() {
			@Override
			public void visit_s4(S4SXP node) {
				getCounter().s4 ++;
			}
			@Override
			public void visit_lang(LangSXP node) {
				getCounter().lang ++;
			}
			@Override
			public void visit_symbol(SymbolSXP node) {
				getCounter().lang ++;
			}
			@Override
			public void visit_closure(ClosSXP node) {
				getCounter().closure ++;				
			}
			@Override
			public void visit_env(EnvSXP node) {
				getCounter().env ++;
			}
			@Override
			public void visit_specialsxp(SpecialSXP node) {
				getCounter().special ++;
			}
			@Override
			public void visit_builtinsxp(BuiltinSXP node) {
				getCounter().builtin ++;
			}
			@Override
			public void visit_node(Node node) {
				if(node.is_s3())
					if(node instanceof VectorSXP || node instanceof ListSXP)
						getCounter().legals3 ++;
					else
						getCounter().s3 ++;
				else
					getCounter().other ++;
			}

			private ArgumentsCounter getCounter(){
				return queued_counters.peek();
			}
			
			private void pre_visit_apply_abs_call(AbsCall node) {
				if(node.hasPrologue())
					validateCounter(queued_counters.pop());
				queued_counters.push(fake);
			}
			@Override
			public void pre_visit_apply_special(SpecialCall node) {
				pre_visit_apply_abs_call(node);
			}
			
			@Override
			public void pre_visit_apply_call(FunctionCall node) {
				pre_visit_apply_abs_call(node);
			}
			
			@Override
			public void pre_visit_apply_builtin(BuiltInCall node) {
				pre_visit_apply_abs_call(node);
			}
			private void visit_apply_abs_call(AbsCall node) {
				queued_counters.pop();
			}
			@Override
			public void visit_apply_special(SpecialCall node) {
				visit_apply_abs_call(node);
			}
			
			@Override
			public void visit_apply_call(FunctionCall node) {
				visit_apply_abs_call(node);
			}
			
			@Override
			public void visit_apply_builtin(BuiltInCall node) {
				visit_apply_abs_call(node);
			}

			@Override
			public void pre_visit_prologue(PrologueNode node) {
				queued_counters.push(new ArgumentsCounter());
			}
			
			private void validateCounter(ArgumentsCounter new_args){
				int id = Node.getCurrentContext();
				
				ArgumentsCounter cnt = counters.get(id);
				if(cnt == null)
					counters.put(id, new_args);
				else
					cnt.increment(new_args);
			}
		};
	}
}

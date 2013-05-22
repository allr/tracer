package org.rx.rtrace.processors;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rx.rtrace.Node.BndPromise;
import org.rx.rtrace.Node.BuiltInCall;
import org.rx.rtrace.Node.EvalBndPromise;
import org.rx.rtrace.Node.EvalUnBndPromise;
import org.rx.rtrace.Node.FunctionCall;
import org.rx.rtrace.Node.UnbndPromise;
import org.rx.rtrace.processors.Promises.PromiseInfo;


public class PromisesUpward extends BasicProcessor {
	Map<Integer,Integer> promises_by_id = new LinkedHashMap<Integer, Integer>();
	Map<Integer,Integer> promises_creator = new LinkedHashMap<Integer, Integer>();
	Map<Integer,Integer> promises_upward = new LinkedHashMap<Integer, Integer>();

	int stackid = 0;

	long upward_eval = 0;
	long down = 0;

	public PromisesUpward(Connection conn) {
		super(conn);
	}

	@Override
	public void create_schema() throws SQLException {
//		Connection conn = getConnection();
//		boolean ok = false;
//		try {
//			DataBase.create_table(conn, "promises_upward", "trace_id reference traces, "+ 
//					"nb integer, eval integer, real_eval integer,"+
//					"upward integer, downward integer, same_level integer, before_seen integer,"+
//					"bootstrap integer, bound_before_eval integer, ghost integer, sure integer, "+
//					"rewrapped integer, max_stack, stack_cumul, "+
//					"constraint one_by_trace unique (trace_id)");
//			ok = true;
//		} finally {
//			DataBase.commitOrRollbackIfNeeded(conn, ok);
//		}
	}
	

	@Override
	public void initialize_processor(String[] options) throws Exception {
		super.initialize_processor(options);
		
		promises_by_id = new HashMap<Integer, Integer>();
		promises_creator = new HashMap<Integer, Integer>();
		promises_upward = new LinkedHashMap<Integer, Integer>();
		down = upward_eval = stackid = 0;
	}
	
	@Override
	public void finalize_trace(int id) throws SQLException{
//		Connection conn = getConnection();
//		boolean ok = false;
//		try {
//			Statement stmt = conn.createStatement();
//			stmt.executeUpdate("insert or replace into promises (trace_id, nb, eval, real_eval, upward, downward,"+
//					"same_level, before_seen, bootstrap, bound_before_eval, ghost, sure, rewrapped, max_stack, stack_cumul)"+
//					" values ("+id+", "+nb_promise+", "+evaluated+", "+total_eval+", "+upward_eval+", " +
//					downward_eval+", "+samelevel_eval+", "+eval_before_seen+", " +
//					from_bootstrap+", "+bound_before_eval+", "+ghost_promises+", "+nb_promise_sure+", "+
//					rewrapped+", "+max_stack+", "+stack_cumul+");");
//			ok = true;
//		} finally {
//			stmt.close();
//			DataBase.commitOrRollbackIfNeeded(conn, ok);
//		}
//		System.out.println("total: "+upward_eval);
//		for(int creator: promises_upward.keySet()){
//			long nb = promises_upward.get(creator);
//			System.out.println(""+creator+" "+nb);
//		}
	}
	
	private boolean has_promise(int id){
		return promises_by_id.containsKey(id);
	}
	
	@Override
	protected NodeVisitor make_visitor(){
		return new PromiseVisitor(null); 
	}
	
	class PromiseVisitor extends NodeVisitorAdapter {
		PromiseVisitor parent;
		int sid = 0;

		PromiseVisitor(PromiseVisitor p){
			sid = stackid ++;
			parent = p;
		}
		private void add_promise(int id){
			promises_by_id.put(id, sid);
		}
		@Override
		public void visit_apply_call(FunctionCall node) throws Exception {
			promises_creator.put(sid, node.getID());
			setVisitor(parent);
		}
		@Override
		public void pre_visit_apply_call(FunctionCall node) throws Exception {
			PromiseVisitor v = new PromiseVisitor(this);
			setVisitor(v);
		}
		@Override
		public void visit_apply_builtin(BuiltInCall node) throws Exception {
			promises_creator.put(sid, node.getID());
			setVisitor(parent);
		}
		@Override
		public void pre_visit_apply_builtin(BuiltInCall node) throws Exception {
			PromiseVisitor v = new PromiseVisitor(this);
			setVisitor(v);
		}
		@Override
		public void visit_bnd(BndPromise node) throws Exception {
			int id = node.getID(); 
			if(!has_promise(id)){
				add_promise(id);
			}
		}	
		@Override
		public void visit_unbnd(UnbndPromise node) throws Exception {
			int id = node.getID();
			if(node.is_new()){
				add_promise(id);
			}
		}
		@Override
		public void visit_eval_bnd_promise(EvalBndPromise node) throws Exception {
			int id = node.getID();
			if(!has_promise(id)){
				add_promise(id);
			}
		}
		@Override
		public void visit_eval_unbnd_promise(EvalUnBndPromise node) throws Exception {
			int id = node.getID();
			Integer p_sid = promises_by_id.get(id);
			if(p_sid == null){
				add_promise(id);
			} else {
				PromiseVisitor c_visitor = this;
				boolean downward = false;
				int sid = p_sid;

				do{
					if(sid == c_visitor.sid)
						downward = true;
				}while((!downward) && (c_visitor = c_visitor.parent) != null);
				if(!downward){
					upward_eval ++;
					int cid = promises_creator.get(sid);
					Integer nb = promises_upward.get(cid);
					if(nb != null)
						promises_upward.put(cid, nb + 1);
					else
						promises_upward.put(cid, 1);
				}
				else{
					down ++;
				}
			}
		}
	}

}

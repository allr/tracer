package org.rx.rtrace.processors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rx.DataBase;
import org.rx.rtrace.Node;
import org.rx.rtrace.Node.BndPromise;
import org.rx.rtrace.Node.BuiltInCall;
import org.rx.rtrace.Node.EvalBndPromise;
import org.rx.rtrace.Node.EvalUnBndPromise;
import org.rx.rtrace.Node.FunctionCall;
import org.rx.rtrace.Node.PrologueNode;
import org.rx.rtrace.Node.UnbndPromise;


public class Promises extends BasicProcessor {
	// SEXP-addr of promise -> "stack id" (unique counted number, not in db)
	Map<Integer,Integer> promises_by_id;
	// SEXP-addr of promise -> context (location_id that is not a builtin)
	Map<Integer,Integer> promises_unevaled;

	// context (location_id) -> PromiseInfo
	Map<Integer,PromiseInfo> stats;
//	Map<Integer,Integer> promise_distance = new LinkedHashMap<Integer, Integer>();
//	Map<Integer,Integer> distance = new LinkedHashMap<Integer, Integer>();
	
	// values are used as key for promises_unevaled
	ArrayList<Integer> queued_promises;
	int stackid;
	int max_stack;
	long stack_cumul;
	

	static class PromiseInfo {
		// incremented by add_promise
		long nb_promise = 0;

		// bound or unbound promises that were marked as "new" by emit_simple_type
		long nb_promise_sure = 0;

		long evaluated = 0;

		long upward_eval = 0;
		long downward_eval = 0;
		long samelevel_eval = 0;

		long eval_before_seen = 0;
		long bound_before_eval = 0;
		long from_bootstrap = 0; 

		long ghost_promises = 0;
		long rewrapped = 0;

		// incremented by count_unevaled
		long unevaled = 0;
	}

	public Promises(Connection conn) {
		super(conn);
	}

	@Override
	public void initialize_processor(String[] options) throws Exception {
		super.initialize_processor(options);
		
		promises_by_id = new HashMap<Integer, Integer>();
		promises_unevaled = new HashMap<Integer, Integer>();
		stats = new LinkedHashMap<Integer, PromiseInfo>();
		queued_promises = new ArrayList<Integer>();
		stack_cumul = max_stack = stackid = 0;
	}
	
	@Override
	public void create_schema() throws SQLException {
		Connection conn = getConnection();
		boolean ok = false;
		try {
			DataBase.create_table(conn, "promises", "trace_id reference traces, "+ 
					"location_id reference locations,"+
					"nb integer, eval integer, real_eval integer,"+
					"upward integer, downward integer, same_level integer, before_seen integer,"+
					"bootstrap integer, bound_before_eval integer, ghost integer, sure integer, "+
					"rewrapped integer, max_stack, stack_cumul, unevaled integer, "+
					"constraint one_by_trace unique (trace_id, location_id)");
			ok = true;
		} finally {
			DataBase.commitOrRollbackIfNeeded(conn, ok);
		}
	}
	
	@Override
	public void finalize_trace(int id) throws SQLException{
		Connection conn = getConnection();
		boolean ok = false;
		PreparedStatement stmt = conn.prepareStatement("insert or replace into promises (trace_id, location_id, nb, eval, real_eval, upward, downward,"+
				"same_level, before_seen, bootstrap, bound_before_eval, ghost, sure, rewrapped, max_stack, stack_cumul, unevaled)"+
				" values ("+id+", ?, ?, ?, ?, ?, " +
				"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
		try {
			int nb = 0;
			flush_unevaled();
			promises_by_id.clear();
			promises_unevaled.clear();
			//		promise_distance.clear();
			//		distance.clear();
			for(Integer enc: stats.keySet()){
				PromiseInfo stat = stats.get(enc);
				long total_eval = stat.eval_before_seen+stat.evaluated; 
				if(enc == null)
					stmt.setLong(1, 0);               // location_id
				else
				stmt.setLong(1, enc);                     // location_id
				stmt.setLong(2, stat.nb_promise);         // nb
				stmt.setLong(3, stat.evaluated);          // eval
				stmt.setLong(4, total_eval);              // real_eval
				stmt.setLong(5, stat.upward_eval);        // upward
				stmt.setLong(6, stat.downward_eval);      // downward
				stmt.setLong(7, stat.samelevel_eval);     // same_level
				stmt.setLong(8, stat.eval_before_seen);   // before_seen
				stmt.setLong(9, stat.from_bootstrap);     // bootstrap
				stmt.setLong(10, stat.bound_before_eval); // bound_before_eval
				stmt.setLong(11, stat.ghost_promises);    // ghost
				stmt.setLong(12, stat.nb_promise_sure);   // sure
				stmt.setLong(13, stat.rewrapped);         // rewrapped
				stmt.setInt(14, max_stack);               // max_stack
				stmt.setLong(15, stack_cumul);            // stack_cumul
				stmt.setLong(16, stat.unevaled);          // unevaled

				stmt.addBatch();
				if(nb++ == 100)
					stmt.executeBatch();
				//stats.remove(enc);
			}
			stmt.executeBatch();
			ok = true;
		} finally {
			stmt.close();
			DataBase.commitOrRollbackIfNeeded(conn, ok);
		}
		
		// TODO display_distance(out);
	}

	private void flush_unevaled(){
		for(int id: promises_unevaled.keySet())
			count_unevaled(id);
	}

	// runs through queued_promises and changes every promises_unevaled
	// value with a key listed in queued_promises to the value patch_for
	// queues_promises is cleared afterwards
	private void patch_promises(int patch_for){
		for(int id: queued_promises){
			Integer fun = promises_unevaled.get(id);
			if(fun != null)
				promises_unevaled.put(id, patch_for);
		}
		queued_promises.clear();
	}

	// returns the stats entry for the current context
	// creates a new one if none exists
	private PromiseInfo getFunctionStats(){
		int id = Node.getCurrentContext();

		PromiseInfo stat = stats.get(id);
		if(stat == null){
			stat = new PromiseInfo();
			stats.put(id, stat);
		}
		return stat;
	}

	
//	void display_distance(PrintStream out){
//		long total = 0;
//		out.print("Distances:");
//		for(int dst: distance.keySet()){
//			long nb = distance.get(dst);
//			total += dst*nb;
//			out.print(" "+dst+":"+nb);
//		}
//		out.println("");
//		out.println("Average distance: "+(total / (double)stat.evaluated)+" ("+total+"/"+stat.evaluated+")");
//	}
	
	private boolean has_promise(int id){
		return promises_by_id.containsKey(id);
	}
	
	@Override
	protected NodeVisitor make_visitor(){
		return new PromiseVisitor(null); 
	}

	private void maybe_unevaled(int id){
		if(promises_unevaled.containsKey(id)){
			count_unevaled(id);
			promises_unevaled.remove(id);
		}
	}

	// increment "unevaled" counter in the enclosing stats of <id> if it exists
	private void count_unevaled(int id){
		// map id (SEXP addr) to a location_id
		Integer enc = promises_unevaled.get(id);
		int pid = 0;
		if(enc != null)
			pid = enc;

		PromiseInfo stat = stats.get(pid);
		if(stat == null){
			stat = new PromiseInfo();
			stats.put(pid, stat);
		}
		stat.unevaled ++;
	}


	class PromiseVisitor extends NodeVisitorAdapter {
		PromiseVisitor parent;
		int sid = 0;
		int enclosed_promise = 0;

		PromiseVisitor(PromiseVisitor p){
			sid = stackid ++;
			parent = p;
			if(p != null)
				enclosed_promise = p.enclosed_promise;
		}

		private void add_promise(int id){
			PromiseInfo stat = getFunctionStats();
			if(has_promise(id))
				// promise already known, check
				maybe_unevaled(id);
			stat.nb_promise ++;			
			promises_unevaled.put(id, Node.getCurrentContext());
			promises_by_id.put(id, sid);
//			promise_distance.put(id, stackid);
		}

		public void visit_prologue(PrologueNode node) {
		}

		@Override
		public void visit_apply_call(FunctionCall node) {
			// end of call processing, switch visitor back to parent visitor
			setVisitor(parent);
		}

		@Override
		public void pre_visit_apply_call(FunctionCall node) {
			// start of a new call, reparent outstanding promises
			patch_promises(node.getID());
			// switch to a new visitor (note: promise queue is a global for it!)
			PromiseVisitor v = new PromiseVisitor(this);
			setVisitor(v);
		}

		@Override
		public void visit_apply_builtin(BuiltInCall node) {
			//if(queued_promises.size() > 0)
			//	System.err.println("Still some promises inside: "+queued_promises.size());
			// reset to previous visitor
			setVisitor(parent);
		}

		@Override
		public void pre_visit_apply_builtin(BuiltInCall node) {
			// start of a new call, reparent outstanding promises
			patch_promises(node.getID());

			// switch to a new visitor (note: promise queue is a global for it!)
			PromiseVisitor v = new PromiseVisitor(this);
			setVisitor(v);
		}

		@Override
		// came from an emit_simple_type call with a PROMSXP parameter
		public void visit_unbnd(UnbndPromise node) {
			int id = node.getID(); // SEXP address
			if(node.is_new()){
				// promise was marked as "new" by emitter
				add_promise(id);
				queued_promises.add(id);
				getFunctionStats().nb_promise_sure ++;
			}else{
				// promise should be known already
				// add it anyway if it wasn't there
				if(!has_promise(id))
					add_promise(id);
			}
		}

		@Override
		// came from an emit_simple_type call with a PROMSXP parameter
		public void visit_bnd(BndPromise node) {
			int id = node.getID();
			PromiseInfo stat = getFunctionStats();
			if(node.is_new())
				// if promise is marked as "new", count
				stat.nb_promise_sure ++;
			if(!has_promise(id)){
				if(node.is_new()){
					stat.bound_before_eval ++;
				}else{
					/* promise not in promises_by_id, but also not marked as new
					   by emitter - it does happen, but why? */
					stat.from_bootstrap ++;
				}
				add_promise(id);
			}
		}

		@Override
		public void visit_eval_bnd_promise(EvalBndPromise node) {
			int id = node.getID();
			if(!has_promise(id)){
				PromiseInfo stat = getFunctionStats();
				stat.eval_before_seen ++;
				stat.bound_before_eval ++;
				add_promise(id);
			}
		}

		@Override
		public void visit_eval_unbnd_promise(EvalUnBndPromise node) {
			int id = node.getID();
			Integer p_sid = promises_by_id.get(id);
			PromiseInfo stat = getFunctionStats();

			if(p_sid == null){
                                // no promise with this ID in list, may indicate bug? -ik
				add_promise(id);
				stat.eval_before_seen++;
			} else {
				PromiseVisitor c_visitor = this;
				boolean downward = false;

                                /* walk chain of visitors upward until
                                   visitor's "stack ID" matches the one of the promise
                                   (or end of chain)
                                */
				do{
					if(p_sid == c_visitor.sid)
						downward = true;
				}while((!downward) && (c_visitor = c_visitor.parent) != null);

				if(downward){ // found promise in chain
                                        /* promise was created in same or higher level */
					stat.downward_eval ++;
					if(c_visitor == this || c_visitor == parent)
                                                /* why count parent as same level? */
						stat.samelevel_eval ++;
				}else
                                        /* promise was created outside current chain,
                                           means creation in lower level? -ik
                                        */
					stat.upward_eval ++;
				
//				int dst = stackid - promise_distance.get(id);
//				Integer old_nb = distance.get(dst);
//				if(old_nb == null)
//					old_nb = 0;
//				distance.put(dst, old_nb + 1);

                                /* count in number of eval'd promises */
				stat.evaluated ++;
			}

			// promise was evaluated, remove from uneval list
			promises_unevaled.remove(id);
			if(node.getParent() instanceof Node.AbsPromise)
				stat.rewrapped ++;

                        // check if the promise node has no nodes below it
			if(node.getBody() == 0)
				stat.ghost_promises++;

			enclosed_promise --;
		}

		@Override
		public void pre_visit_eval_unbnd_promise(EvalUnBndPromise node) {
			if((++enclosed_promise) > max_stack)
				max_stack = enclosed_promise;
			stack_cumul += enclosed_promise;
		}

	}
}

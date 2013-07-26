package org.rx.rtrace.processors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rx.DataBase;
import org.rx.rtrace.Node.AbsCall;
import org.rx.rtrace.Node.BuiltInCall;
import org.rx.rtrace.Node.FunctionCall;
import org.rx.rtrace.Node.SpecialCall;

public class FunctionHidding  extends BasicProcessor{
	// location_id -> counter
	protected Map<Integer,Integer> function_hidden;
	// len (target_node.getPrologue()) -> counter
	protected Map<Integer,Integer> function_hide_chain;

	public FunctionHidding(Connection conn) {
		super(conn);
	}

	@Override
	public void initialize_processor(String[] options) throws Exception {
		super.initialize_processor(options);
		function_hidden = new LinkedHashMap<Integer, Integer>();
		function_hide_chain = new LinkedHashMap<Integer, Integer>();
	}
	
	@Override
	public void create_schema() throws SQLException {
		Connection conn = getConnection();
		boolean ok = false;
		try {
			DataBase.create_table(conn, "hidden_functions", "trace_id reference traces," +
					"location_id reference locations, nb integer");
			DataBase.create_table(conn, "hidden_lengths", "trace_id reference traces," +
					"size integer, nb integer, "+
					"constraint size_unique unique (trace_id, size)");
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
			PreparedStatement stmt = conn.prepareStatement("delete from "+
					"hidden_functions where trace_id="+id);
			try {
				stmt.executeUpdate();
			} finally {
			}
			stmt.close();

			stmt = conn.prepareStatement("delete from "+
					"hidden_lengths where trace_id="+id);
			stmt.executeUpdate();
			stmt.close();

			stmt = conn.prepareStatement("insert into hidden_functions "+
					"(trace_id, location_id, nb) values ("+id+", ?, ?)");
			try {
				for(int key: function_hidden.keySet()){
					stmt.setInt(1, key);
					stmt.setInt(2, function_hidden.get(key));
					stmt.addBatch();
				}
				stmt.executeBatch();
			} finally {
				stmt.close();
			}
			
			stmt = conn.prepareStatement("insert into hidden_lengths "+
					"(trace_id, size, nb) values ("+id+", ?, ?)");
			try {
				for(int size: function_hide_chain.keySet()){
					stmt.setInt(1, size);
					stmt.setInt(2, function_hide_chain.get(size));
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
//		out.println("CALL_HIDDEN_FUNCTION "+total);
//		out.println("FUNCTION_HIDDEN "+function_hidden.size());
//		out.println("HIDE_CHAIN_AVG "+(hide_chain_size/(double)total));
	}
	@Override
	protected NodeVisitor make_visitor(){
		return new NodeVisitorAdapter() {
			private void visit_apply_abs_call(AbsCall node){
				if(!node.hasPrologue())
					return;
				int id = node.getID();
				if(id == DataBase.UNKNOWN_LOCATION)
					return;
				int len = node.getPrologue();
				if(len > 1){
					Integer value = function_hidden.get(id);
					if(value == null)
						value = 0;
					value ++;
					function_hidden.put(id, value);
					Integer nb_of_this = function_hide_chain.get(len);
					if(nb_of_this == null)
						nb_of_this = 0;
					function_hide_chain.put(len, nb_of_this + 1);
				}
				
			}
			@Override
			public void visit_apply_call(FunctionCall node) throws Exception {
				visit_apply_abs_call(node);
			}
			@Override
			public void visit_apply_special(SpecialCall node) throws Exception {
				visit_apply_abs_call(node);
			}
			@Override
			public void visit_apply_builtin(BuiltInCall node) throws Exception {
				visit_apply_abs_call(node);
			}
		};
	}
}

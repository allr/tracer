package org.rx.rtrace.processors;


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.rx.DataBase;
import org.rx.rtrace.Node.AbsCall;
import org.rx.rtrace.Node.BuiltInCall;
import org.rx.rtrace.Node.FunctionCall;
import org.rx.rtrace.Node.PrimitiveCall;
import org.rx.rtrace.Node.SpecialCall;

public class CountSize  extends BasicProcessor {
	long max_size = -1;
	long min_size = Integer.MAX_VALUE;
	long total_length;
	long nb_calls;
	
	public CountSize(Connection conn) {
		super(conn);
	}
	
	@Override
	public void create_schema() throws SQLException {
		Connection conn = getConnection();
		boolean ok = false;
		try {
			DataBase.create_table(conn, "sizes", "trace_id reference traces, "+ 
					"max_size integer, min_size integer, total integer, nb_calls integer, constraint one_by_trace unique (trace_id)");
			ok = true;
		} finally {
			DataBase.commitOrRollbackIfNeeded(conn, ok);
		}
	}
	
	@Override
	public void initialize_processor(String[] options) throws Exception {
		max_size = -1;
		min_size = Integer.MAX_VALUE;
		total_length = nb_calls = 0;
	}
	
	@Override
	public void finalize_trace(int id) throws SQLException{
		Connection conn = getConnection();
		boolean ok = false;
		Statement stmt = conn.createStatement();
		try {
			stmt.executeUpdate("insert or replace into sizes (trace_id, max_size, min_size, total, nb_calls) VALUES "+
					"("+id+", "+max_size+", "+min_size+", "+total_length+", "+nb_calls+")");
			ok = true;
		} finally {
			stmt.close();
			DataBase.commitOrRollbackIfNeeded(conn, ok);
		}
		/*out.println("MAX_SIZE "+max_size);
		out.println("MIN_SIZE  "+min_size);
		out.println("TOTAL "+total_length);
		out.println("NB_CALLS "+nb_calls);
		out.println("AVG_SIZE "+ round_percent(total_length, nb_calls));*/
	}
	
	@Override
	protected NodeVisitor make_visitor(){
		return new NodeVisitorAdapter() {
			int in_special; 

			private void visit_apply_abs_call(AbsCall node){
				int len = node.getBody() + in_special;
				if(len > max_size)
					max_size = len;
				if(len < min_size)
					min_size = len;
				nb_calls ++;
				in_special = 0;
				total_length += len;
			}
			@Override
			public void visit_apply_call(FunctionCall node) throws Exception {
				visit_apply_abs_call(node);
			}
			private void visit_apply_prim_call(PrimitiveCall node){
				in_special += node.getBody();
			}
			@Override
			public void visit_apply_special(SpecialCall node) throws Exception {
				visit_apply_prim_call(node);
			}
			@Override
			public void visit_apply_builtin(BuiltInCall node) throws Exception {
				visit_apply_prim_call(node);
			}
		};
	}
}

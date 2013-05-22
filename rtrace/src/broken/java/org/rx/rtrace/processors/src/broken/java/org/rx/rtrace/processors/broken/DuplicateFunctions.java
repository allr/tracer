package org.rx.rtrace.processors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import org.rx.DataBase;
import org.rx.rtrace.FunctionMap;
import org.rx.rtrace.Node;
import org.rx.rtrace.RTrace;

public class DuplicateFunctions implements TraceProcessor {	
	@Override
	public void create_schema() throws SQLException {
		DataBase.create_table(RTrace.database, "dups", 
				"trace_id reference traces, function_id reference functions, nb integer, "+
				"constraint function_unique unique (trace_id, function_id)");
	}
	
	@Override
	public void finalize_trace() throws SQLException{
		Connection conn = RTrace.database;
		PreparedStatement stmt = conn.prepareStatement("delete from dups where trace_id="+RTrace.get_trace_id());
		stmt.executeUpdate();
		stmt.close();
		
		Map<String, ArrayList<Integer>> map = FunctionMap.reverse_names;
		stmt = RTrace.database.prepareStatement(
				"insert into dups (trace_id, function_id, nb) values "+
				"("+RTrace.get_trace_id()+", ?, ?)");
		for(String dups: map.keySet()){
			ArrayList<Integer> vals = map.get(dups);
			if(vals.size() > 1){
				stmt.setInt(1, FunctionMap.get_by_id(vals.get(0)).get_sql_index());
				stmt.setInt(2, vals.size());
				//out.println(""+dups+" "+vals.size());
				stmt.addBatch();
			}
		}
		stmt.executeBatch();
		stmt.close();
	}

	@Override
	public void initialize_processor(String[] opts) throws Exception {}

	@Override
	public boolean initialize_trace() throws Exception {
		return false; /* Don't need to process the trace 
		(but there must be at least one which does it to ensure FunctionMap loading */
	}

	@Override
	public void process_trace(Node[] roots) throws Exception { }


	@Override
	public NodeVisitor getVisitor() {
		return null;
	}

}

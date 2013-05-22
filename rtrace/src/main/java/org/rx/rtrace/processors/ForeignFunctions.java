package org.rx.rtrace.processors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rx.DataBase;
import org.rx.rtrace.FunctionMap;
import org.rx.rtrace.Node.BuiltInCall;


public class ForeignFunctions extends BasicProcessor {
	protected Map<Integer,Integer> func_map;
//	private String map_file;

	public ForeignFunctions(Connection conn) {
		super(conn);
	}
	
	@Override
	public void initialize_processor(String[] options) throws Exception {
		super.initialize_processor(options);
//		map_file = options[0];
		func_map = new LinkedHashMap<Integer, Integer>();
		func_map.put(FunctionMap.get_primitive_location(".C"), 0);
		func_map.put(FunctionMap.get_primitive_location(".Fortran"), 0);
		func_map.put(FunctionMap.get_primitive_location(".External"), 0);
		func_map.put(FunctionMap.get_primitive_location(".Call"), 0);
		func_map.put(FunctionMap.get_primitive_location(".Call.ignored"), 0);
	}
	
	@Override
	public void create_schema() throws SQLException {
		Connection conn = getConnection();
		boolean ok = false;
		try {
			DataBase.create_table(conn, "foreigns", "trace_id reference trace,"+
					"location_id reference locations, nb integer");
			ok = true;
		} finally {
			DataBase.commitOrRollbackIfNeeded(conn, ok);
		}
	}

	@Override
	public void finalize_trace(int id) throws SQLException{
		Connection conn = getConnection();
		boolean ok = false;
		try {
			PreparedStatement stmt = conn.prepareStatement("delete from foreigns where trace_id="+id);
			try {
				stmt.executeUpdate();
			} finally {
				stmt.close();
			}

			stmt = conn.prepareStatement("insert into foreigns (trace_id, location_id, nb) VALUES "+
					"("+id+", ?, ?)");
			try {
				for(int key: func_map.keySet())
					if(func_map.get(key) > 0){
						stmt.setInt(1, key);
						stmt.setInt(2, func_map.get(key));
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
		
//		out.println(FunctionMap.get_by_id(key)+" "+func_map.get(key));
	}
	
	@Override
	protected NodeVisitor make_visitor(){
		return new NodeVisitorAdapter() {
			@Override
			public void visit_apply_builtin(BuiltInCall node) throws Exception {
				int id = node.getID();
				Integer count = func_map.get(id);
				if(count != null){
					if(node.getBody() > 0){
						func_map.put(id, count + 1);
					}
				}
			}
		};
	}
}

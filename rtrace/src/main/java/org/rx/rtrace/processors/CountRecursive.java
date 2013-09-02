package org.rx.rtrace.processors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rx.DataBase;
import org.rx.rtrace.FunctionMap;
import org.rx.rtrace.Node;
import org.rx.rtrace.Node.AbsCall;
import org.rx.rtrace.Node.FunctionCall;
import org.rx.rtrace.Node.SpecialCall;

public class CountRecursive extends BasicProcessor {
	int recursive_calls;
	int recall_count;
	Map<Integer, Integer> map = new LinkedHashMap<Integer, Integer>();
	private int recall_id = FunctionMap.get_primitive_location("Recall");
	
	public CountRecursive(Connection conn) {
		super(conn);
	}
	
	@Override
	public void create_schema() throws SQLException {
		Connection conn = getConnection();
		boolean ok = false;
		try {
			DataBase.create_table(conn, "recursives", "trace_id reference traces," +
					"location_id reference locations, nb integer");
			ok = true;
		} finally {
			DataBase.commitOrRollbackIfNeeded(conn, ok);
		}
	}

	@Override
	public void initialize_processor(String[] options) throws Exception {
                super.initialize_processor(options);

		map.clear();
		recursive_calls = recall_count = 0;
	}
	
	@Override
	public void finalize_trace(int id) throws SQLException {
		Connection conn = getConnection();
		boolean ok = false;
		try {
			PreparedStatement stmt = conn.prepareStatement("delete from recursives where trace_id="+id);
			try {
				stmt.executeUpdate();
			} finally {
				stmt.close();
			}

			stmt = conn.prepareStatement("insert into recursives (trace_id, location_id, nb) "+
					"values ("+id+", ? , ?)");
			try {
				for(int key: map.keySet()){
					stmt.setInt(1, key);
					stmt.setInt(2, map.get(key));
					stmt.addBatch();
				}
				stmt.setInt(1, recall_id);
				stmt.setInt(2, recall_count);
				stmt.addBatch();
	
				stmt.executeBatch();
			} finally {
				stmt.close();
			}
			
			ok = true;
		} finally {
			DataBase.commitOrRollbackIfNeeded(conn, ok);
		}
	}
	
	@Override
	protected NodeVisitor make_visitor(){
		return new NodeVisitorAdapter() {
			@Override
			public void visit_apply_call(FunctionCall node) throws Exception {
				int id = node.getID();
				if(id == DataBase.UNKNOWN_LOCATION)
					return;
				Node current = node;
				while((current = current.getParent()) != null) {
					if(current instanceof AbsCall){
						AbsCall cnode = (AbsCall) current; 
						if(cnode.getID() == id){
							recursive_calls ++;
							Integer count = map.get(id);
							if(count == null)
								map.put(id, 1);
							else
								map.put(id, count + 1);
							break;
						}
					}
				}
			}
			@Override
			public void visit_apply_special(SpecialCall node) throws Exception {
				int id = node.getID();
				if(id == recall_id)
					recall_count ++;
			}
		};
	}
}

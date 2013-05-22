package org.rx.rtrace.processors;

import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rx.DataBase;
import org.rx.rtrace.Node;

public class SimpleCounters extends BasicProcessor {
	Map<Class<?>, Integer> counters = new LinkedHashMap<Class<?>, Integer>();
	
	public SimpleCounters(Connection conn) {
		super(conn);
	}

	@Override
	public void initialize_processor(String[] options) throws Exception {
		counters.clear();
	}
	
	@Override
	public void create_schema() throws SQLException {
		Connection conn = getConnection();
		boolean ok = false;
		try {
			DataBase.create_table(conn, "counter_type", "id integer primary key autoincrement," +
					"name text, "+
					"constraint counter_unique unique (name)");
			
			PreparedStatement stmt = conn.prepareStatement("insert or ignore into counter_type "+
					"(name) values (?)");
			for(Class<?> c: Node.class.getDeclaredClasses())
				if(!Modifier.isAbstract(c.getModifiers())){
					stmt.setString(1, c.getSimpleName());
					stmt.addBatch();
				}
			stmt.executeBatch();
			stmt.close();
			DataBase.create_table(conn, "counters", "trace_id reference traces," +
					"counter_id reference counter_type, nb integer, "+
					"constraint function_unique unique (trace_id, counter_id)");
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
			PreparedStatement stmt = conn.prepareStatement("delete from counters where trace_id="+id);
			try {
				stmt.executeUpdate();
			} finally {
				stmt.close();
			}

			stmt = conn.prepareStatement("insert into counters (trace_id, counter_id, nb) "+
					"select "+id+", id, ? from counter_type where name=?");
			try {
				for(Class<?> name: counters.keySet()){
					stmt.setInt(1, counters.get(name));
					stmt.setString(2, name.getSimpleName());
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
	
	protected NodeVisitor make_visitor(){
		return new BasicNodePostVisitor() {
			protected void visit_node(Node node) throws Exception{
				Class<?> name = node.getClass();
				Integer cmpt = counters.get(name);
				if(cmpt == null)
					cmpt = 0;
				counters.put(name, cmpt + 1);
			}
		};
	}
}
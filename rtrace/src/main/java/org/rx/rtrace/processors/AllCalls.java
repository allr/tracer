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
import org.rx.rtrace.Node.BuiltInCall;
import org.rx.rtrace.Node.FunctionCall;
import org.rx.rtrace.Node.SpecialCall;
import org.rx.rtrace.Node.UnitSXP;

public class AllCalls extends BasicProcessor {
	protected Map<Long, CallCounter> func_map;
	protected Map<Integer, Long> nb_param;
	protected Map<Integer, Long> nb_position;
	protected Map<Integer, Long> nb_keywords;
	protected Map<Integer, Long> nb_rest;

	public AllCalls(Connection conn) {
		super(conn);
	}

	@Override
	public void initialize_processor(String[] options) throws Exception {
		super.initialize_processor(options);
		func_map = new LinkedHashMap<Long, CallCounter>();
		nb_param = new LinkedHashMap<Integer, Long>();
		nb_position = new LinkedHashMap<Integer, Long>();
		nb_keywords = new LinkedHashMap<Integer, Long>();
		nb_rest = new LinkedHashMap<Integer, Long>();
		for(int i = 0; i < 256; i++){
			nb_param.put(i, 0L);
			nb_position.put(i, 0L);
			nb_keywords.put(i, 0L);
			nb_rest.put(i, 0L);
		}
	}
	public static long hashKey(int fun_id, int enclosed_fun){
		return (((long)fun_id) << Integer.SIZE) | (long)enclosed_fun;
	}
	public static int funId(long key){
		return  (int) (key >> Integer.SIZE);
	}
	public static int enclosingId(long key){
		return (int)(key & Integer.MAX_VALUE);
	}
	long putIntoFunMap(int fun_id, int enclosed_fun, CallCounter c){
		long key;
		func_map.put(key = hashKey(fun_id, enclosed_fun), c);
		return key;
	}
	CallCounter getFromMap(int fun_id, int enclosed_fun){
		return func_map.get(hashKey(fun_id, enclosed_fun));
	}
	
	@Override
	public void create_schema() throws SQLException {
		Connection conn = getConnection();
		boolean ok = false;
		try {
			DataBase.create_table(conn, "calls", "trace_id reference traces," +
					"location_id reference locations, owner_id reference locations, nb integer, dups integer, " +
					"position integer, keywords integer, rest integer, unit");
			DataBase.create_table(conn, "calls_frequency", "trace_id reference traces," +
					" value, param, position, keywords, rest");
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
			PreparedStatement stmt = conn.prepareStatement("delete from calls where trace_id="+id);
			try {
				stmt.executeUpdate();
			} finally {
				stmt.close();
			}
	
			stmt = conn.prepareStatement("insert into calls "+
					"(trace_id, location_id, owner_id, nb, dups, position, keywords, rest, unit)"+
					"values ("+id+", ?, ?, ?, ?, ?, ?, ?, ?)");
			try {
				for(long key: func_map.keySet()){
					int fun_id = funId(key);
					int enc_fun = enclosingId(key);

					stmt.setInt(1, fun_id);
					stmt.setInt(2, enc_fun);
					CallCounter c = func_map.get(key);
					stmt.setInt(3, c.nb);
					stmt.setInt(4, FunctionMap.get_duplicates(fun_id));
					stmt.setLong(5, c.args_by_position);
					stmt.setLong(6, c.args_by_keyword);
					stmt.setLong(7, c.args_rest);
					stmt.setLong(8, c.unit);
					stmt.addBatch();
				}
				stmt.executeBatch();
			} finally {
				stmt.close();
			}
			
			stmt = conn.prepareStatement("insert into calls_frequency "+
					"(trace_id, value, param, position, keywords, rest)"+
					"values ("+id+", ?, ?, ?, ?, ?)");
			try {
				for(int i = 0; i < 256 ; i++)
					if(nb_param.get(i) > 0
							|| nb_position.get(i) > 0 
							|| nb_keywords.get(i) > 0 
							|| nb_rest.get(i) > 0){
						stmt.setInt(1, i);
						stmt.setLong(2, nb_param.get(i));
						stmt.setLong(3, nb_position.get(i));
						stmt.setLong(4, nb_keywords.get(i));
						stmt.setLong(5, nb_rest.get(i));
						stmt.addBatch();
					}
				stmt.executeBatch();
			} finally {
				stmt.close();
			}
			//out.println(format_name(key)+" "+func_map.get(key));
			ok = true;
		} finally {
			DataBase.commitOrRollbackIfNeeded(conn, ok);
		}
	}
	
	class CallCounter {
		int nb = 1;
		long args_by_position;
		long args_by_keyword;
		long args_rest;
		long unit = 0;
		CallCounter(int pos, int keys, int rest){
			args_by_keyword = keys;
			args_by_position = pos;
			args_rest = rest;
		}
		void increment(int pos, int keys, int rest){
			args_by_keyword += keys;
			args_by_position += pos;
			args_rest += rest;
			nb ++;
		}
		void increment_unit(){
			unit ++;
		}
	}

	@Override
	protected NodeVisitor make_visitor(){
		return new NodeVisitorAdapter() {
			private void visit_apply_abs_call(AbsCall node){
				int fid = 0;
				Integer enc_call = Node.getCurrentContext();
				if(enc_call != null)
					fid = enc_call;
				long id = hashKey(node.getID(), fid);
				CallCounter value = func_map.get(id);
				if(value == null){
					value = new CallCounter(node.get_by_position(), node.get_by_keywords(), node.get_more_args());
					func_map.put(id, value);
				}else{
					value.increment(node.get_by_position(), node.get_by_keywords(), node.get_more_args());
				}
				if(node.getReturn() instanceof UnitSXP)
					value.increment_unit();
			}
			private void count_in_frequency(AbsCall node){
				int total = 0, nb;
				total += nb = node.get_by_position();
				if(nb > 0)
					nb_position.put(nb, nb_position.get(nb)+1);
				total += nb = node.get_by_keywords();
				if(nb > 0)
					nb_keywords.put(nb, nb_keywords.get(nb)+1);
				total += nb = node.get_more_args();
				if(nb > 0)
					nb_rest.put(nb, nb_rest.get(nb)+1);
				if(total > 255)
					total = 255;
				nb_param.put(total, nb_param.get(total)+1);
			}
			@Override
			public void visit_apply_call(FunctionCall node) throws Exception {
				visit_apply_abs_call(node);
				count_in_frequency(node);
			}
			@Override
			public void visit_apply_special(SpecialCall node) throws Exception {
				visit_apply_abs_call(node);
			}
			@Override
			public void visit_apply_builtin(BuiltInCall node) throws Exception {
				visit_apply_abs_call(node);
				//count_in_frequency(node);
			}
		};
	}
}

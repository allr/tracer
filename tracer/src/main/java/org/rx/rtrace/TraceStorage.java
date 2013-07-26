package org.rx.rtrace;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rx.rtrace.util.CommandLineUtils;

public class TraceStorage {
	
	private static final String GET_TRACE_BY_NAME_QUERY = 
			"SELECT FROM traces WHERE name = ?";
	
	private static final String GET_LAST_TRACE_BY_NAME_QUERY = 
			"SELECT FROM traces WHERE name = ? AND ts = " +
			"(SELECT MIN(ts) AS min_ts FROM traces WHERE name = ?)";
//	private static final String SELECT_TRACE_BY_ID_STR = 
//			"SELECT FROM traces WHERE id = ?";
	
	private static final String LAST_TRACE =
			"FROM traces WHERE src_file = ? AND run_cmd_args = ? AND ts = " +
			"(SELECT MIN(ts) AS min_ts FROM traces" +
			" WHERE src_file = ? AND run_cmd_args = ?)";
	
	private static final String GET_LAST_TRACE_QUERY =
		"SELECT * " + LAST_TRACE;
	
	private static final String DELETE_LAST_TRACE_QUERY =
		"DELETE " + LAST_TRACE;
	
	private static final String UPDATE_TRACE_QUERY =
			"UPDATE traces SET" +
			" name = ?, " +
			" ts = ?, " +
			" status = ?, " +
			" error = ?, " +
			" output_dir = ?, " +
			" file_type = ?, " +
			" map_file_type = ?" +
			" WHERE id = ?";
	
	private static final String INSERT_TRACE_QUERY =
			"INSERT OR IGNORE INTO traces(" +
					"name, " +
					"ts, " +	
					"status, " +
					"error, " +
					"src_file, " +
					"run_cmd_args, " +
					"output_dir, " +
					"file_type, " +
					"map_file_type" +
					") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final Trace.Status[] TRACE_STATUS_BY_ORDINAL =
			Trace.Status.values();
	
	public static Trace getLastTrace(Connection conn, String name) throws
	SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				GET_LAST_TRACE_BY_NAME_QUERY);
		try {
			pstmt.setString(1, name);
			pstmt.setString(2, name);
			return getTrace(pstmt.executeQuery());
		} finally {
			pstmt.close();
		}
	}
	
	public static Trace getLastTrace(Connection conn, File srcFile,
			String[] runCmdArgs) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				GET_LAST_TRACE_QUERY);
		System.out.println(GET_LAST_TRACE_QUERY);
		try {
			String uniqueSrcFilePath = srcFile.getAbsolutePath();
			String runCmdArgsStr = CommandLineUtils.joinArguments(runCmdArgs);
			pstmt.setString(1, uniqueSrcFilePath);
			pstmt.setString(2, runCmdArgsStr);
			pstmt.setString(3, uniqueSrcFilePath);
			pstmt.setString(4, runCmdArgsStr);
			ResultSet rs = pstmt.executeQuery();
			return rs.next() ? getTrace(rs) : null;
		} finally {
			pstmt.close();
		}
	}
	
	public static Collection<Trace> getTraces(Connection conn) throws
	SQLException {
		Statement stmt = conn.createStatement();
		try {
			ResultSet rs = stmt.executeQuery("SELECT * FROM traces");
			List<Trace> traces = new ArrayList<Trace>();
			while (rs.next())
				traces.add(getTrace(rs));
			return traces;
		} finally {
			stmt.close();
		}
	}
	
//	public static Trace getTraces(Connection conn, int id) throws SQLException {
//		PreparedStatement pstmt = conn.prepareStatement(
//				SELECT_TRACE_BY_ID_STR);
//		try {
//			pstmt.setInt(1, id);
//			return getTrace(pstmt.executeQuery());
//		} finally {
//			pstmt.close();
//		}
//	}
	
//	public static Collection<Trace> getTraces(Connection conn,
//			Collection<String> names) throws SQLException {
//		PreparedStatement pstmt = conn.prepareStatement(
//				GET_TRACE_BY_NAME_QUERY);
//		List<Trace> traces = new ArrayList<Trace>();
//		try {
//			for (String name : names) {
//				pstmt.clearParameters();
//				pstmt.setString(1, name);
//				traces.add(getTrace(pstmt.executeQuery()));
//			}
//		} finally {
//			pstmt.close();
//		}
//		return traces;
//	}
	
	public static Collection<Trace> getTraces(Connection conn, String name)
			throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				GET_TRACE_BY_NAME_QUERY);
		try {
			pstmt.clearParameters();
			pstmt.setString(1, name);
			return getTraces(pstmt.executeQuery());
		} finally {
			pstmt.close();
		}
	}
	
	public static boolean createTrace(Connection conn, Trace trace) throws
	SQLException {
		checkTrace(trace);
		PreparedStatement pstmt = conn.prepareStatement(INSERT_TRACE_QUERY);
		try {
			prepareTraceForInsert(pstmt, trace);
			if (pstmt.executeUpdate() <= 0)
				return false;
		} finally {
			pstmt.close();
		}
		
		Integer id = Database.getGeneratedId(conn);
		assert(id != null);
		trace.setId(id);
		return true;
	}
	
//	public static boolean registerTrace(Connection conn, Trace trace) throws
//	SQLException {
//		if (trace.getStatus() != Trace.Status.UNREGISTERED)
//			throw new IllegalArgumentException(
//					"Trace status must be be " + Trace.Status.UNREGISTERED);
//		
//		return createTrace(conn, trace);
//	}
	
//	public static void registerTraces(Connection conn,
//			Collection<Trace> traces) throws SQLException {
//		PreparedStatement pstmt = conn.prepareStatement(INSERT_TRACE_STR);
//		try {
//			for (Trace trace : traces) {
//				pstmt.clearParameters();
//				prepareTraceForRegister(pstmt, trace);
//				pstmt.addBatch();
//			}
//			pstmt.executeBatch();  // TODO check return value
//		} finally {
//			pstmt.close();
//		}
//	}
	
	public static boolean updateTrace(Connection conn, Trace trace)
			throws SQLException {
		checkTraceSavable(trace);
		PreparedStatement pstmt = conn.prepareStatement(UPDATE_TRACE_QUERY);
		try {
			prepareTraceForUpdate(pstmt, trace);
			return pstmt.executeUpdate() > 0;
		} finally {
			pstmt.close();
		}
	}
	
	public static boolean updateTraces(Connection conn,
			Collection<Trace> traces)
			throws SQLException {
		boolean updatedAll = true;
		PreparedStatement pstmt = conn.prepareStatement(UPDATE_TRACE_QUERY);
		try {
			for (Trace trace : traces) {
				pstmt.clearParameters();
				try {
					prepareTraceForUpdate(pstmt, trace);
					pstmt.addBatch();
				} catch (Exception e) {
					updatedAll = false;
				}
			}
			for (int ok : pstmt.executeBatch())
				updatedAll &= ok > 0;
			return updatedAll;
		} finally {
			pstmt.close();
		}
	}
	
	public static boolean deleteTrace(Connection conn, String name) throws
	SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				"DELETE FROM traces WHERE name = ?");
		try {
			pstmt.setString(1, name);
			return pstmt.executeUpdate() > 0;
		} finally {
			pstmt.close();
		}
	}
	
	public static boolean deleteTrace(Connection conn, int id) throws
	SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				"DELETE FROM traces WHERE id = ?");
		try {
			pstmt.setInt(1, id);
			return pstmt.executeUpdate() > 0;
		} finally {
			pstmt.close();
		}
	}
	
	public static boolean deleteLastTrace(Connection conn, File srcFile,
			String[] runCmdArgs) throws
	SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				DELETE_LAST_TRACE_QUERY);
		try {
			pstmt.setString(1, srcFile.getAbsolutePath());
			pstmt.setString(2, CommandLineUtils.joinArguments(runCmdArgs));
			return pstmt.executeUpdate() > 0;
		} finally {
			pstmt.close();
		}
	}
	
	public static boolean deleteTraces(Connection conn, Collection<String> names,
			Collection<Integer> ids) throws Exception {
		boolean deletedAll = true;
		Set<String> nameSet = new HashSet<String>();
		for (String name : names)
			if (!nameSet.contains(name)) {
				nameSet.add(name);
				try {
					deletedAll &= deleteTrace(conn, name);
				} catch (SQLException e) {
					deletedAll = false;
				}
			}
		nameSet.clear();
		Set<Integer> idSet = new HashSet<Integer>();
		for (Integer id : ids)
			if (!idSet.contains(id)) {
				idSet.add(id);
				try {
					boolean ok = deleteTrace(conn, id);
					System.out.println("ok(" + id + ") = " + ok);
					deletedAll &= ok;
				} catch (SQLException e) {
					deletedAll = false;
				}
			}
		return deletedAll;
	}
	
	public static Trace getTrace(ResultSet rs) throws SQLException {
		Trace trace = new Trace(
				rs.getString("name"),
				new File(rs.getString("src_file")),
				CommandLineUtils.splitArguments(rs.getString("run_cmd_args")),
				new File(rs.getString("output_dir")),
				rs.getString("file_type"),
				rs.getString("map_file_type"));
		trace.setError(rs.getBoolean("error"));
		trace.setStatus(TRACE_STATUS_BY_ORDINAL[rs.getInt("status")]);
		trace.setDate(longToDate(rs.getLong("ts")));
		trace.setId(rs.getInt("id"));
		return trace;
	}
	
	public static Collection<Trace> getTraces(ResultSet rs) throws
	SQLException {
		Collection<Trace> traces = new ArrayList<Trace>();
		while (rs.next())
			getTrace(rs);
		return traces;
	}
	
	private static PreparedStatement prepareTraceForInsert(
			PreparedStatement pstmt, Trace trace) throws SQLException {
		pstmt.setString(1, trace.getName());                         // name
		pstmt.setLong(2, dateToLong(trace.getDate()));               // ts
		pstmt.setInt(3, trace.getStatus().ordinal());                // status
		pstmt.setBoolean(4, trace.isError());                        // error
		pstmt.setString(5, trace.getSourceFile().getAbsolutePath()); // src_file
		pstmt.setString(6, CommandLineUtils.joinArguments(
		                trace.getRunCmdArgs()));                     // run_cmd_args
		pstmt.setString(7, trace.getOutputDir().getAbsolutePath());  // output_dir
		pstmt.setString(8, trace.getFileType());                     // file_type
		pstmt.setString(9, trace.getMapFileType());                  // map_file_type
		return pstmt;
	}
	
	private static PreparedStatement prepareTraceForUpdate(
			PreparedStatement pstmt, Trace trace) throws SQLException {
		pstmt.setString(1, trace.getName());
		pstmt.setLong(2, dateToLong(trace.getDate()));
		pstmt.setInt(3, trace.getStatus().ordinal());
		pstmt.setBoolean(4, trace.isError());
		pstmt.setString(5, trace.getOutputDir().getAbsolutePath());
		pstmt.setString(6, trace.getFileType());
		pstmt.setString(7, trace.getMapFileType());
		pstmt.setInt(8, trace.getId());
		return pstmt;
	}
	
	private static long dateToLong(Date date) {
		return date != null ? date.getTime() : 0L;
	}
	
	private static Date longToDate(long dateValue) {
		return dateValue != 0L ? new Date(dateValue) : null;
	}
	
	private static final void checkTrace(Trace trace) {
		if (trace == null)
			throw new IllegalArgumentException("Trace cannot be null");
//		if (trace.getName() == null)
//			throw new IllegalArgumentException("Trace name be null");
//		if (trace.getName().isEmpty())
//			throw new IllegalArgumentException("Trace name must not be empty");
	}
	
	private static final void checkTraceSavable(Trace trace) {
		Database.checkEntitySavable(trace);
	}
}

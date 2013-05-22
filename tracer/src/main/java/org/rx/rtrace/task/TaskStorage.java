package org.rx.rtrace.task;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rx.rtrace.Database;
import org.rx.rtrace.Trace;
import org.rx.rtrace.TraceInfo;
import org.rx.rtrace.TraceStorage;
import org.rx.rtrace.util.CommandLineUtils;

public class TaskStorage {
//	private static final String GET_TASK_BY_NAME_QUERY =
//	"SELECT * FROM tasks WHERE name = ?";

	private static final String GET_TASK_ID_QUERY =
		"SELECT id FROM tasks WHERE name = ?";
	
	private static final String GET_TASKS_QUERY =
		"SELECT" +
				" t.id as task_id, " +
				" t.name AS task_name, " +
				" r.id AS id, " +
				" r.name AS name, " +
				" r.status AS status, " +
				" r.src_file AS src_file, " +
				" i.run_cmd_args AS run_cmd_args, " + 
				" r.ts AS ts, " +
				" r.output_dir AS output_dir, " +
				" r.file_type AS file_type, " +
				" r.map_file_type AS map_file_type, " +
				" r.error AS error" +
		" FROM tasks t" +
		" LEFT JOIN traceinfos i ON t.src_file = i.task_id" +
		" LEFT JOIN traces r ON r.src_file = i.src_file" +
		" AND r.run_cmd_args = i.run_cmd_args" +
		" ORDER BY t.id";
	
	private static final String ADD_TRACEINFO_QUERY =
		"INSERT OR IGNORE INTO traceinfos" +
		"(task_id, src_file, run_cmd_args, name," +
		" output_dir, file_type, map_file_type) VALUES(" +
		"(" + GET_TASK_ID_QUERY + "), ?, ?, ?, ?, ?, ?)";

	private static final String ADD_TRACEINFO_QUERY_FORCE =
		"INSERT OR REPLACE INTO traceinfos" +
		"(task_id, src_file, run_cmd_args, name," +
		" output_dir, file_type, map_file_type) VALUES(" +
		"(" + GET_TASK_ID_QUERY + "), ?, ?, ?, ?, ?, ?)";
	
	private static final String REMOVE_TRACEINFO_QUERY =
		"DELETE FROM traceinfos WHERE task_id = " +
		"(" + GET_TASK_ID_QUERY + ")" +
		" AND src_file = ? AND run_cmd_args = ?";
	
	private static final String GET_TRACEINFO_IDS_BY_TASK_ID_QUERY =
		"SELECT id FROM traceinfos WHERE task_id = ?";
	
	//private static final String GET_TRACEINFO_IDS_BY_TASK_NAME_QUERY =
	//	"SELECT id FROM traceinfos i" +
	//	" JOIN tasks t ON i.task_id = t.id" +
	//	"WHERE t.name = ?";
	
	private static final String GET_TRACEINFOS_QUERY =
		"SELECT" +
				" r.name AS task_name, " +
				" r.id AS id, " +
				" r.name AS name, " +
				" r.status AS status, " +
				" r.src_file AS src_file, " +
				" r.run_cmd_args AS run_cmd_args, " + 
				" r.ts AS ts, " +
				" r.output_dir AS output_dir, " +
				" r.file_type AS file_type, " +
				" r.map_file_type AS map_file_type, " +
				" r.error AS error" +
		" FROM tasks t" +
		" JOIN traceinfos i ON t.id = i.task_id" +
		" LEFT JOIN traces r ON r.src_file = i.src_file" +
		" AND r.run_cmd_args = i.run_cmd_args";
	
	private static final String GET_TRACEINFOS_BY_TASK_NAME_QUERY =
		GET_TRACEINFOS_QUERY + " WHERE t.name = ?";
	
	private static final String STATUS_NOT_DONE =
		"r.status <> " + Trace.Status.DONE.ordinal();
	
	private static final String GET_INCOMPLETE_TRACEINFOS_BY_TASK_NAME_QUERY =
		GET_TRACEINFOS_BY_TASK_NAME_QUERY + " AND " + STATUS_NOT_DONE;
	
	private static final String GET_INCOMPLETE_TRACEINFOS_QUERY =
		GET_TRACEINFOS_QUERY + " WHERE " + STATUS_NOT_DONE;
	
	
	public static final void checkTaskName(String name) {
		if (name == null)
			throw new IllegalArgumentException("Task name cannot be null");
		if (name.isEmpty())
			throw new IllegalArgumentException("Task name cannot be empty");
		if (name.startsWith("@"))
			throw new IllegalArgumentException(
					"Task name must not begin with @");
	}
	
	public static final void checkTaskExists(Connection conn, String name)
	throws SQLException {
		if (!existsTask(conn, name))
			throw new NoSuchTaskException(name);
	}
	
//	private static final void checkTaskExists(Connection conn, int id)
//	throws SQLException {
//		if (!existsTask(conn, id))
//			throw new NoSuchTaskException("Task #" + id + " already exists");
//	}
	
	private static Task getTask(ResultSet rs) throws SQLException {
		Collection<Trace> traces = new ArrayList<Trace>();
		while (rs.next())
			traces.add(TraceStorage.getTrace(rs));
		return new Task(rs.getString("task_name"), traces);
	}
	
//	private static Collection<Trace> getTaskTraces(ResultSet rs) throws
//	SQLException {
//		Collection<Trace> traces = new ArrayList<Trace>();
//		while (rs.next()) {
//			Trace trace = new Trace(
//					rs.getString("name"),
//					new File(rs.getString("src_file")),
//					CommandLineUtils.splitArguments(
//							rs.getString("run_cmd_args")),
//					new File(rs.getString("output_dir")),
//					rs.getString("file_type"),
//					rs.getString("map_file_type"));
//			Long ts = rs.getLong("ts");
//			trace.setDate(ts != null ? new Date(ts) : null);
//			traces.add(trace);
//		}
//		return traces;
//	}
	
	public static Task createTask(Connection conn, String name,	boolean overwrite) throws SQLException, IllegalArgumentException, NoSuchTaskException {
		checkTaskName(name);
		
		if (overwrite)
			deleteTask(conn, name, true);
		Integer id = createTask(conn, name);
		if (id == null)
			return null;
		
		Task newTask = new Task(name);
		newTask.setId(id);
		return newTask;
	}
	
	private static Integer createTask(Connection conn, String name) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement("INSERT INTO tasks(name) VALUES (?)");
		try {
			pstmt.setString(1, name);
			return pstmt.executeUpdate() > 0 ? (Integer) Database.getGeneratedId(conn) : null;
		} finally {
			pstmt.close();
		}
	}
	
	public static Collection<TaskInfo> getTasksInfo(Connection conn)
			throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				"SELECT t.name AS task_name, i.* FROM tasks t" +
				" LEFT JOIN traceinfos i ON t.id = i.task_id");
		try {
			Map<String, TaskInfo> taskInfoMap =
					new LinkedHashMap<String, TaskInfo>();
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String taskName = rs.getString("task_name");
				TaskInfoImpl taskInfo = (TaskInfoImpl) taskInfoMap.get(taskName);
				if (taskInfo == null) {
					taskInfoMap.put(taskName, new TaskInfoImpl(taskName));
					taskInfo = (TaskInfoImpl) taskInfoMap.get(taskName);
				}
				TraceInfo traceInfo = getTraceInfo(rs);
				if (traceInfo != null)
					taskInfo.addTraceInfo(traceInfo);
			}
			return taskInfoMap.values();
		} finally {
			pstmt.close();
		}
	}
	
	public static Task getTask(Connection conn, String name) throws
	SQLException {
		checkTaskName(name);
		PreparedStatement pstmtTraces = conn.prepareStatement(
				GET_TRACEINFOS_BY_TASK_NAME_QUERY);
		try {
			pstmtTraces.setString(1, name);
			return getTask(pstmtTraces.executeQuery());
		} finally {
			pstmtTraces.close();
		}
	}
	
	public static List<Task> getTasks(Connection conn) throws SQLException {
		PreparedStatement pstmtTraces = conn.prepareStatement(GET_TASKS_QUERY);
		try {
			ResultSet rs = pstmtTraces.executeQuery();
			List<Task> tasks = new ArrayList<Task>();
			Collection<Trace> traces = new ArrayList<Trace>();
			int lastTaskId = 0;
			String lastTaskName = null;
			while (rs.next()) {
				String taskName = rs.getString("task_name");
				int curTaskId = rs.getInt("task_id");
				if (!rs.isFirst() && curTaskId != lastTaskId) {
					tasks.add(new Task(lastTaskName, traces));
					traces.clear();
				}
				if (rs.getObject("status") != null)
				lastTaskId = curTaskId;
				lastTaskName = taskName;
			}
			if (lastTaskName != null)
				tasks.add(new Task(lastTaskName, traces));
			return tasks;
		} finally {
			pstmtTraces.close();
		}
	}
	
	public static List<String> getTaskNames(Connection conn) throws
	SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				"SELECT name FROM tasks");
		try {
			if (pstmt.execute()) {
				ResultSet rs = pstmt.getResultSet();
				List<String> taskNames = new ArrayList<String>();
				while (rs.next())
					taskNames.add(rs.getString(1));
				return taskNames;
			}
		} finally {
			pstmt.close();
		}
		return Collections.emptyList();
	}
	
	public static Collection<Trace> getIncompleteTraces(Connection conn,
			String taskName) throws SQLException {
		return getTracesByTaskName(conn,
				GET_INCOMPLETE_TRACEINFOS_BY_TASK_NAME_QUERY, taskName);
	}
	
	public static Map<String, Collection<Trace>> getIncompleteTraces(
			Connection conn) throws SQLException {
		return getTracesByTaskName(conn, GET_INCOMPLETE_TRACEINFOS_QUERY);
	}
	
	public static Collection<Trace> getTraces(Connection conn, String taskName)
			throws SQLException {
		return getTracesByTaskName(conn, GET_TRACEINFOS_BY_TASK_NAME_QUERY,
				taskName);
	}
	
	public static Map<String, Collection<Trace>> getTraces(Connection conn)
			throws SQLException {
		return getTracesByTaskName(conn, GET_TRACEINFOS_QUERY);
	}
	
	private static Collection<Trace> getTracesByTaskName(Connection conn, 
			String sql, String taskName) throws SQLException {
		checkTaskName(taskName);
		checkTaskExists(conn, taskName);
		PreparedStatement pstmt = conn.prepareStatement(sql);
		try {
			pstmt.setString(1, taskName);
			ResultSet rs = pstmt.executeQuery();
			Collection<Trace> traces = new ArrayList<Trace>();
			while (rs.next())
				if (isTrace(rs))
					traces.add(TraceStorage.getTrace(rs));
			return traces;
		} finally {
			pstmt.close();
		}
	}
	
	private static Map<String, Collection<Trace>> getTracesByTaskName(
			Connection conn, String sql) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement(sql);
		try {
			Map<String, Collection<Trace>> tracesMap =
					new LinkedHashMap<String, Collection<Trace>>();
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String taskName = rs.getString("task_name");
				Collection<Trace> traces = tracesMap.get(taskName);
				if (traces == null) {
					tracesMap.put(taskName, new ArrayList<Trace>());
					traces = tracesMap.get(taskName);
				}
				if (isTrace(rs))
					traces.add(TraceStorage.getTrace(rs));
			}
			return tracesMap;
		} finally {
			pstmt.close();
		}
	}
	
	public static boolean existsTask(Connection conn, String name) throws
	SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				"SELECT COUNT(*) FROM tasks WHERE name = ?");
		try {
			pstmt.setString(1, name);
			ResultSet rs = pstmt.executeQuery();
			return rs.next() && rs.getInt(1) > 0;
		} finally {
			pstmt.close();
		}
	}
	
//	private static boolean existsTask(Connection conn, int id) throws
//	SQLException {
//		PreparedStatement pstmt = conn.prepareStatement(
//				"SELECT COUNT(*) FROM tasks WHERE id = ?");
//		try {
//			pstmt.setInt(1, id);
//			ResultSet rs = pstmt.executeQuery();
//			return rs.next() && rs.getInt(1) > 0;
//		} finally {
//			pstmt.close();
//		}
//	}
	
	public static boolean deleteTask(Connection conn, String name,
			boolean removeTraces) throws
	SQLException {
		if (removeTraces)
			removeTracesInfo(conn, name);
		
		PreparedStatement pstmt = conn.prepareStatement(
				"DELETE FROM tasks WHERE name = ?");
		try {
			pstmt.setString(1, name);
			return pstmt.executeUpdate() > 0;
		} finally {
			pstmt.close();
		}
	}
	
	public static int copyTracesInfo(Connection conn, String srcTaskName,
			String dstTaskName) throws SQLException {
		final String columns = "src_file, run_cmd_args, name, output_dir," +
			"file_type, map_file_type";
		PreparedStatement pstmt = conn.prepareStatement(
				"INSERT OR IGNORE INTO traceinfos(task_id, " + columns + ")" +
				" SELECT (" + GET_TASK_ID_QUERY + "), " + columns +
				" FROM traceinfos i" +
				" WHERE task_id = (" + GET_TASK_ID_QUERY + ")");
		try {
			pstmt.setString(1, dstTaskName);
			pstmt.setString(2, srcTaskName);
			return pstmt.executeUpdate();
		} finally {
			pstmt.close();
		}
	}
	
	public static int removeTracesInfo(Connection conn, String taskName)
			throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				"DELETE FROM traceinfos WHERE task_id = " +
						"(" + GET_TASK_ID_QUERY + ")");
		try {
			pstmt.setString(1, taskName);
			return pstmt.executeUpdate();
		} finally {
			pstmt.close();
		}
	}
	
	public static boolean renameTask(Connection conn, String oldName,
			String newName) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				"UPDATE tasks SET name = ? WHERE name = ?");
		try {
			pstmt.setString(1, newName);
			pstmt.setString(2, oldName);
			return pstmt.executeUpdate() > 0;
		} finally {
			pstmt.close();
		}
	}
	
	public static TaskInfo getTaskInfo(Connection conn, String taskName)
			throws IllegalArgumentException, SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				"SELECT t.name AS task_name, i.* FROM traceinfos i" +
				" JOIN tasks t ON t.id = i.task_id" +
				" WHERE t.name = ?");
		try {
			pstmt.setString(1, taskName);
			ResultSet rs = pstmt.executeQuery();
			TaskInfoImpl taskInfo = new TaskInfoImpl(taskName);
			while (rs.next()) {
				taskInfo.addTraceInfo(getTraceInfo(rs));
			}
			return taskInfo;
		} finally {
			pstmt.close();
		}
	}
	
	public static boolean addTraceInfo(Connection conn, String taskName,
			TraceInfo traceInfo, boolean force) throws SQLException {
		checkTaskName(taskName);
		checkTaskExists(conn, taskName);
		checkTraceInfo(traceInfo);
		PreparedStatement pstmt = conn.prepareStatement(force ? ADD_TRACEINFO_QUERY_FORCE : ADD_TRACEINFO_QUERY);
		try {
			prepareTraceInfo(pstmt, taskName, traceInfo);
			return pstmt.executeUpdate() > 0;
		} finally {
			pstmt.close();
		}
	}
	
	public static boolean addTraceInfos(Connection conn, String taskName,
			Collection<TraceInfo> traceInfos, boolean force) throws SQLException {
		checkTaskName(taskName);
		checkTaskExists(conn, taskName);
		PreparedStatement pstmt = conn.prepareStatement(force ? ADD_TRACEINFO_QUERY_FORCE : ADD_TRACEINFO_QUERY);
		try {
			for (TraceInfo traceInfo : traceInfos) {
				checkTraceInfo(traceInfo);
				prepareTraceInfo(pstmt, taskName, traceInfo);
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			return true;  // TODO
		} finally {
			pstmt.close();
		}
	}
	
	public static boolean removeTraceInfo(Connection conn, String taskName,
			File traceSrcFile, String[] runCmdArgs) throws SQLException {
		checkTaskName(taskName);
		checkTaskExists(conn, taskName);
		CommandLineUtils.checkCommandLineArgs(runCmdArgs);
		
		PreparedStatement pstmt = conn.prepareStatement(REMOVE_TRACEINFO_QUERY);
		try {
			prepareTraceInfoKey(pstmt, taskName, traceSrcFile, runCmdArgs);
			return pstmt.executeUpdate() > 0;
		} finally {
			pstmt.close();
		}
	}
	
	private static TraceInfo getTraceInfo(ResultSet rs) throws SQLException {
		if (!isTraceInfo(rs))
			return null;
		System.out.println("trace name = " + rs.getString("name"));
		System.out.println("src file = " + rs.getString("src_file"));
		System.out.println("run args = " + rs.getString("run_cmd_args"));
		return new TraceInfo(
				rs.getString("name"),
				new File(rs.getString("src_file")),
				CommandLineUtils.splitArguments(rs.getString("run_cmd_args")),
				new File(rs.getString("output_dir")),
				rs.getString("file_type"),
				rs.getString("map_file_type"));
	}
	
	private static void prepareTraceInfo(PreparedStatement pstmt,
			String taskName, TraceInfo traceInfo) throws
	SQLException {
		prepareTraceInfoKey(pstmt, taskName, traceInfo.getSourceFile(),
				traceInfo.getRunCmdArgs());
		pstmt.setString(4, traceInfo.getName());
		pstmt.setString(5, traceInfo.getOutputDir().getAbsolutePath());
		pstmt.setString(6, traceInfo.getFileType());
		pstmt.setString(7, traceInfo.getMapFileType());
	}
	
	private static void prepareTraceInfoKey(PreparedStatement pstmt,
			String taskName, File srcFile, String[] runCmdArgs) throws
			SQLException {
		pstmt.setString(1, taskName);
		pstmt.setString(2, srcFile.getAbsolutePath());
		pstmt.setString(3, CommandLineUtils.joinArguments(runCmdArgs));
	}
	
	
	private static boolean isTraceInfo(ResultSet rs) throws SQLException {
		return rs.getString("src_file") != null;
	}
	
	private static boolean isTrace(ResultSet rs) throws SQLException {
		return rs.getString("name") != null;
	}
	
	private static final void checkTaskSavable(Task task) {
		Database.checkEntitySavable(task);
		checkTaskName(task.getName());
	}
	
	private static void checkTraceInfo(TraceInfo traceInfo) {
		// TODO
	}
	
	
	private static class TaskInfoImpl extends TaskInfo {
		Map<TraceKey, TraceInfo> traceKeys =
				new LinkedHashMap<TraceKey, TraceInfo>();

		public TaskInfoImpl(String name) {
			super(name);
		}
		
		@Override
		public Set<TraceKey> getTraceKeys() {
			return Collections.unmodifiableSet(traceKeys.keySet());
		}

		@Override
		public TraceInfo getTraceInfo(TraceKey traceKey) {
			return traceKeys.get(traceKey);
		}
		
		public boolean addTraceInfo(TraceInfo traceInfo) {
			return traceKeys.put(new TraceKey(traceInfo), traceInfo) != null;
		}

//		public boolean removeTraceInfo(TraceKey traceKey) {
//			return traceKeys.remove(traceKey) != null;
//		}
	}
}

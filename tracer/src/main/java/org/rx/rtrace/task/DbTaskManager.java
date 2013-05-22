package org.rx.rtrace.task;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rx.rtrace.Database;
import org.rx.rtrace.DatabaseFactory;
import org.rx.rtrace.Trace;
import org.rx.rtrace.TraceInfo;
import org.rx.rtrace.TraceStorage;
import org.rx.rtrace.Trace.Status;
import org.rx.rtrace.task.TaskInfo.TraceKey;
import org.rx.rtrace.util.StringUtils;
import org.sglj.util.Pair;

/**
 * A simple, monolithic implementation of the {@link TaskManager} interface.
 * The implementation consolidates two tiers:
 * <ul>
 *   <li>business logic associated with tasks</li>
 *   <li>persistent storage of tasks through <tt>SQLite3</tt> database</li>
 * </ul>
 * 
 * @author "Leo Osvald"
 *
 */
class DbTaskManager implements TaskManager {

	private Connection db;
	
	public DbTaskManager() throws SQLException, IOException {
		db = DatabaseFactory.getInstance();
		Database.createTracesTable(db);
		Database.create_table(db, "tasks",
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"name TEXT NOT NULL, " +
				"CONSTRAINT task_unique UNIQUE (name)");
		Database.create_table(db, "traceinfos",
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"task_id INTEGER NOT NULL, " +
				"src_file TEXT NOT NULL, " +
				"run_cmd_args TEXT NOT NULL, " +
				"name TEXT NOT NULL, " +
				"output_dir TEXT, " +
				"file_type TEXT, " +
				"map_file_type TEXT, " +
				"FOREIGN KEY (task_id) REFERENCES tasks(id), " +
				"FOREIGN KEY (src_file, run_cmd_args) REFERENCES traces(src_file, run_cmd_args), " +
				"CONSTRAINT traceinfos_unique UNIQUE (task_id, src_file, run_cmd_args)");
	}
	
	@Override
	public Task createTask(String name, boolean overwrite) throws SQLException {
		boolean ok = false;
		try {
			Task newTask = TaskStorage.createTask(db, name, overwrite);
			ok = true;
			return newTask;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public boolean deleteTask(String name) throws SQLException {
		boolean ok = false;
		try {
			ok = TaskStorage.deleteTask(db, name, true);
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
		return ok;
	}
	
	@Override
	public boolean deleteTasks(Collection<String> names) throws SQLException {
		boolean ok = false;
		try {
			Set<String> nameSet = new HashSet<String>(names.size());
			boolean deletedAll = true;
			for (String name : names) {
				if (nameSet.contains(name))
					continue;
				nameSet.add(name);
				deletedAll &= TaskStorage.deleteTask(db, name, true);
			}
			ok = true;
			return deletedAll;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public boolean existsTask(String name) throws SQLException {
		boolean ok = false;
		try {
			ok = TaskStorage.existsTask(db, name);
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
		return ok;
	}
	
	@Override
	public Task getTask(String name) throws SQLException {
		boolean ok = false;
		try {
			Task task = TaskStorage.getTask(db, name);
			ok = true;
			return task;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public Collection<Task> getTasks() throws
	SQLException {
		Collection<Task> tasks = null;
		try {
			return tasks = TaskStorage.getTasks(db);
		} finally {
			Database.commitOrRollbackIfNeeded(db, tasks != null);
		}
	}
	
	@Override
	public Collection<String> getTaskNames() throws SQLException {
		Collection<String> taskNames = null;
		try {
			return taskNames = TaskStorage.getTaskNames(db);
		} finally {
			Database.commitOrRollbackIfNeeded(db, taskNames != null);
		}
	}
	
	@Override
	public Collection<Trace> getIncompleteTraces(String taskName)
	throws SQLException {
		Collection<Trace> incompleteTraces = null;
		try {
			return incompleteTraces = TaskStorage.getIncompleteTraces(db,
					taskName);
		} finally {
			Database.commitOrRollbackIfNeeded(db, incompleteTraces != null);
		}
	}
	
	@Override
	public Map<String, Collection<Trace>> getIncompleteTraces()
			throws NoSuchTaskException, Exception {
		Map<String, Collection<Trace>> incompleteTraces = null;
		try {
			return incompleteTraces = TaskStorage.getIncompleteTraces(db);
		} finally {
			Database.commitOrRollbackIfNeeded(db, incompleteTraces != null);
		}
	}
	
	@Override
	public Collection<Trace> getTraces(String taskName)
			throws NoSuchTaskException, Exception {
		Collection<Trace> traces = null;
		try {
			return traces = TaskStorage.getTraces(db, taskName);
		} finally {
			Database.commitOrRollbackIfNeeded(db, traces != null);
		}
	}
	
	@Override
	public Map<String, Collection<Trace>> getTraces() throws Exception {
		Map<String, Collection<Trace>> traces = null;
		try {
			return traces = TaskStorage.getTraces(db);
		} finally {
			Database.commitOrRollbackIfNeeded(db, traces != null);
		}
	}
	
	@Override
	public Collection<TaskInfo> getTasksInfo() throws Exception {
		Collection<TaskInfo> tasksInfo = null;
		try {
			return tasksInfo = TaskStorage.getTasksInfo(db);
		} finally {
			Database.commitOrRollbackIfNeeded(db, tasksInfo != null);
		}
	}
	
	public void saveTaskInfo(TaskInfo taskInfo) throws Exception {
		String taskName = taskInfo.getName();
		TaskStorage.checkTaskName(taskName);
		boolean ok = false;
		try {
			TaskStorage.checkTaskExists(db, taskName);
			TaskStorage.removeTracesInfo(db, taskName);
			TaskStorage.addTraceInfos(db, taskName, taskInfo.getTraceInfos(), false);
			ok = true;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public Task copyTask(String srcName, String dstName, boolean overwrite)
			throws Exception {
		
		boolean ok = false;
		try {
			TaskStorage.checkTaskName(dstName);
			if (dstName.equals(srcName))
				throw new IllegalArgumentException(
						"Cannot copy: srcName must not be equal to dstName");
			if (TaskStorage.existsTask(db, dstName) &&
					(!overwrite || !TaskStorage.deleteTask(db, dstName, true)))
				return null;
			
			Task newTask = TaskStorage.createTask(db, dstName, overwrite);
			if (newTask == null)
				return null;
			// TODO use id from newTask instead of name
			TaskStorage.copyTracesInfo(db, srcName, dstName);
			
			ok = true;
			return newTask;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public boolean renameTask(String oldName, String newName, boolean overwrite)
			throws Exception {
		boolean ok = false; 
		try {
			TaskStorage.checkTaskName(newName);
			TaskStorage.checkTaskExists(db, oldName);
			if (oldName.equals(newName))
				return ok = true;
			
			if (TaskStorage.existsTask(db, newName) &&
					(!overwrite || !TaskStorage.deleteTask(db, newName, true)))
				return ok = false;

			return ok = TaskStorage.renameTask(db, oldName, newName);
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public Task mergeTasks(String name, Collection<String> tasksToMergeNames,
			boolean overwrite) throws Exception {
		Task mergedTask = null;
		try {
			mergedTask = TaskStorage.createTask(db, name, overwrite);
			if (mergedTask == null)
				return null;
			for (String taskToMergeName : tasksToMergeNames) {
				TaskStorage.checkTaskExists(db, taskToMergeName);
				TaskStorage.copyTracesInfo(db, taskToMergeName, name);
			}
		} finally {
			Database.commitOrRollbackIfNeeded(db, mergedTask != null);
		}
		return mergedTask;
	}
	
	@Override
	public TaskInfo getTaskInfo(String taskName)
			throws IllegalArgumentException, Exception {
		TaskInfo taskInfo = null;
		try {
			return taskInfo = (TaskStorage.existsTask(db, taskName)
					? TaskStorage.getTaskInfo(db, taskName) : null);
		} finally {
			Database.commitOrRollbackIfNeeded(db, taskInfo != null);
		}
	}
	
	@Override
	public boolean addTraceInfo(String taskName, TraceInfo traceInfo, boolean force) throws
	Exception {
		boolean ok = false;
		try {
			boolean added = TaskStorage.addTraceInfo(db, taskName, traceInfo, force);
			ok = true;
			return added;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public boolean removeTraceInfo(String taskName, File traceSrcFile,
			String[] runCmdArgs) throws Exception {
		boolean ok = false;
		try {
			boolean removed = TaskStorage.removeTraceInfo(db, taskName, 
					traceSrcFile, runCmdArgs);
			ok = true;
			return removed;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public boolean clearTask(String taskName) throws Exception {
		boolean ok = false;
		try {
			boolean cleared = TaskStorage.existsTask(db, taskName);
			if (cleared)
				TaskStorage.removeTracesInfo(db, taskName);
			ok = true;
			return cleared;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}

	@Override
	public Task startTask(String taskName, boolean continueLast,
			boolean overwrite)
			throws NoSuchTaskException, Exception {
		boolean ok = false;
		try {
			TaskInfo taskInfo = TaskStorage.getTaskInfo(db, taskName);
			Collection<Trace> incompleteTraces = TaskStorage
					.getIncompleteTraces(db, taskName);
			
			// erase incomplete traces if necessary
			if (!continueLast) {
				// ensure that nothing is deleted unless overwrite is set
				if (!overwrite && !incompleteTraces.isEmpty())
					return null;
				for (Trace incompleteTrace : incompleteTraces) {
					System.err.println("Deleting incompl trace: " +
							incompleteTrace.getId() + " " + incompleteTrace);
					TraceStorage.deleteTrace(db, incompleteTrace.getId());
				}
			}
			
			Task task = (continueLast
					? new Task(taskName, incompleteTraces)
					: new Task(taskName));
			Set<TraceKey> continueTraceKeys = task.getTraceKeys();
			System.err.println("to continue: " + continueTraceKeys);
			for (TraceKey traceKey : taskInfo.getTraceKeys())
				if (!continueTraceKeys.contains(traceKey)) {
					System.err.println("Missing: " + traceKey + " -> " +
							taskInfo.getTraceInfo(traceKey));
					Trace trace = new Trace(taskInfo.getTraceInfo(traceKey));
					trace.setStatus(Status.REGISTERED);
					if (!TraceStorage.createTrace(db, trace)) {
						TraceInfo traceInfo = taskInfo.getTraceInfo(traceKey);
						throw new IllegalStateException(
								"Inconsistent traces for task " + taskName +
								": " + traceInfo.getSourceFile() + " " +
								StringUtils.join(" ",
										traceInfo.getRunCmdArgs()));
//						// in the extremely unlikely case of failure, load last
//						trace = Database.getLastTrace(db, trace.getSourceFile(),
//								trace.getRunCmdArgs());
//						assert(trace != null);
					}
					task.addTrace(trace);
				}
			task.setId(taskInfo.getId());
			ok = true;
			return task;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public Set<String> importTraces(Collection<Pair<Trace, String>> importMap,
			TraceImportMode mode) throws NoSuchTaskException, Exception {
		if (mode == null)
			throw new IllegalArgumentException("mode cannot be null");
		
		Set<String> existingTaskNames = new HashSet<String>(getTaskNames());
		Set<String> newlyCreatedTasks;
		
		if (mode == TraceImportMode.CLEAN) {
			for (String taskName : existingTaskNames)
				TaskStorage.removeTracesInfo(db, taskName);
			newlyCreatedTasks = new HashSet<String>(importMap.size());
		} else {
			newlyCreatedTasks = new HashSet<String>();
		}
		
		boolean ok = false;
		try {
			for (Pair<Trace, String> pair : importMap) {
				Trace trace = pair.first();
				String taskName = pair.second();
				if (existingTaskNames.contains(taskName)) {
					switch (mode) {
					case IGNORE:
						continue;
					case CLEAN:
						addTraceInfo(taskName, trace, true);
					case APPEND:
						TraceStorage.createTrace(db, trace);
						newlyCreatedTasks.add(taskName);
					}
				}
			}
			ok = true;
			return newlyCreatedTasks;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	
}

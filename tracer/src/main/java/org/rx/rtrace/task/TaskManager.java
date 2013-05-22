package org.rx.rtrace.task;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.rx.rtrace.Trace;
import org.rx.rtrace.TraceInfo;
import org.sglj.util.Pair;

/**
 * Interface which describes management of tasks, such as creation,
 * deletion, updates etc.
 * 
 * @author "Leo Osvald"
 * @see {@link Task}
 * @see {@link TaskInfo}
 */
public interface TaskManager {
	/**
	 * Creates an empty task with the specified name, unless there already
	 * exists one with the same name.
	 * 
	 * @param name the name of the task to be created
	 * @param overwrite if <tt>true</tt> and a task with the name equal to
	 * <tt>name</tt> already exists, it is replaced with the newly created one
	 * @return the created task or <tt>null</tt> if the one with that name
	 * already exist and overwrite is equal to <tt>false</tt>
	 * @throws {@link IllegalArgumentException} if the task name is invalid
	 */
	Task createTask(String name, boolean overwrite) throws 
	IllegalArgumentException, Exception;
	
	/**
	 * Deletes a task with the specified name. If no such task exists,
	 * returns <tt>false</tt>.
	 * 
	 * @param name the name of the task to be deleted
	 * @return <tt>true</tt> if task was deleted, <tt>false</tt> otherwise
	 */
	boolean deleteTask(String name) throws Exception;
	
	/**
	 * Deletes task with specified names.
	 * 
	 * @param name the name of the task to be deleted
	 * @return <tt>true</tt> if all tasks were deleted, <tt>false</tt> otherwise
	 */
	boolean deleteTasks(Collection<String> name) throws Exception;
	
	/**
	 * Returns the task with the specified name. The task will contain
	 * the last traces. 
	 * 
	 * @param name the name of the task to be retrieved
	 * @return the task whose name equals the provided name or <tt>null</tt>
	 * if none such exists
	 * @throws IllegalArgumentException if the task name is invalid
	 */
	Task getTask(String name) throws Exception;
	
	/**
	 * Returns incomplete traces associated with the given task.
	 * 
	 * @param name the name of the task for which incomplete traces
	 * should be retrieved
	 * @return the collection of incomplete traces associated with the specified
	 * task
	 * @throws NoSuchTaskException if the task does not exist
	 * @throws IllegalArgumentException if the task name is invalid
	 */
	Collection<Trace> getIncompleteTraces(String taskName) throws 
	NoSuchTaskException, Exception;
	
	/**
	 * Returns incomplete traces for each task.
	 * 
	 * @return the map in which keys are task names and values are the
	 * corresponding collection of traces
	 */
	Map<String, Collection<Trace>> getIncompleteTraces() throws Exception;
	 
	/**
	 * Returns all traces associated with the given task.
	 * 
	 * @param name the name of the task for which incomplete traces
	 * should be retrieved
	 * @return the collection of traces associated with the specified task
	 * @throws NoSuchTaskException if the task does not exist
	 * @throws IllegalArgumentException if the task name is invalid
	 */
	Collection<Trace> getTraces(String taskName) throws NoSuchTaskException,
	Exception;
	
	/**
	 * Returns a collection of traces for each task.
	 * 
	 * @return the map in which keys are task names and values are the
	 * corresponding collection of traces
	 */
	Map<String, Collection<Trace>> getTraces() throws Exception;
	
	/**
	 * Checks whether there exists a task with the specified name
	 * 
	 * @param srcName the name of the task to be checked for existence
	 * @return <tt>true</tt> if the task exists, <tt>false</tt> otherwise
	 */
	boolean existsTask(String name) throws Exception;
	
	/**
	 * Copies an exiting task to a new task and associates the corresponding
	 * traces to it.
	 * 
	 * @param srcName the name of the task to be copied
	 * @param dstName the name of the newly created copy
	 * @param overwrite if <tt>true</tt> and a task with the name equal to
	 * <tt>dstName</tt> already exists, it is replaced with the copy (otherwise
	 * no copy is done)
	 * @return the created task or <tt>null</tt> if the one with that name
	 * already exist and overwrite is equal to <tt>false</tt>
	 * @throws IllegalArgumentException if the name of the copy to be created
	 * is invalid
	 * @throws NoSuchTaskException if the task to be copied does not exist
	 */
	Task copyTask(String srcName, String dstName, boolean overwrite) throws
	NoSuchTaskException, Exception;
	
	Collection<TaskInfo> getTasksInfo() throws Exception;
	
	/**
	 * <p>Saves an existing task. This updates the name and the associated
	 * traces.</p>
	 * <p>If the name is changed to the name of an existing task, the save
	 * is skipped and <tt>false</tt> is returned. This is a precaution measure
	 * to avoid accidentally overwriting tasks by saving them. Otherwise,
	 * <tt>true</tt> is returned, which indicates that the save was successful.
	 * 
	 * @param name the name of the task to be saved
	 * @return <tt>true</tt> if task has been successfully saved,
	 * <tt>false</tt> otherwise.
	 * @throws IllegalArgumentException if the task is non-savable (i.e. the id
	 * is missing or the name is invalid)
	 * @throws NoSuchTaskException if the task does not exist
	 */
	void saveTaskInfo(TaskInfo taskInfo) throws NoSuchTaskException, Exception;
	
	Collection<Task> getTasks() throws Exception;
	Collection<String> getTaskNames() throws Exception;
	
	/**
	 * Changes the name of an existing task.
	 * 
	 * @param srcName the name of the task to be renamed
	 * @param dstName a new name to be used for the original task
	 * @param overwrite if <tt>true</tt> and a task with the name equal to
	 * <tt>newName</tt> already exists, it is replaced with the task whose
	 * name is equal to <tt>oldName</tt> (otherwise, no changes occur)
	 * @return <tt>true</tt> if a rename was done, <tt>false</tt> otherwise
	 * @throws IllegalArgumentException if the new name of the task is invalid
	 * @throws NoSuchTaskException if the task to be renamed does not exist
	 */
	boolean renameTask(String oldName, String newName, boolean overwrite) throws
	IllegalArgumentException, NoSuchTaskException, Exception;
	
	/**
	 * Merges the tasks specified by names to a new task which contains
	 * the union of all traces from the merged tasks.
	 * 
	 * @param name the name of a the merged task to be created
	 * @param tasksToMergeNames the names of the tasks to be merged
	 * @param overwrite if <tt>true</tt> and a task with the name equal to
	 * <tt>name</tt> already exists, it is replaced with the merged task
	 * (otherwise, no merging occurs)
	 * @return the newly created, merged task
	 * @throws IllegalArgumentException if the name of the merged task or any
	 * task to merge is invalid
	 */
	Task mergeTasks(String name, Collection<String> tasksToMergeNames,
			boolean overwrite) throws IllegalArgumentException, Exception;
	
	/**
	 * Retrieves information the task.
	 * 
	 * @param name the name of a task whose trace keys are to be retrieved
	 * @return info about the task
	 * @throws IllegalArgumentException if the task name is invalid
	 */
	TaskInfo getTaskInfo(String taskName) throws IllegalArgumentException,
	Exception;
	
	/**
	 * Adds a trace to the specified task.
	 * 
	 * @param name the name of a the merged task to be created
	 * @param traceInfo info about the trace to be traced
	 * @param force replace the task if it already exists
	 * @return <tt>true</tt> if the trace was added, <tt>false</tt> if it
	 * had already existed
	 * @throws IllegalArgumentException if parameters are illegal (i.e.
	 * source file to trace, run command arguments etc.)
	 */
	boolean addTraceInfo(String taskName, TraceInfo traceInfo, boolean force) throws Exception;

	/**
	 * Removes a trace from the specified task.
	 * 
	 * @param name the name of a task the trace is associated with
	 * @param traceSrcFile the source file to be traced
	 * @param runCmdArgs the command line arguments used to run the program
	 * @return <tt>true</tt> if the trace was added, <tt>false</tt> if it
	 * does not exist
	 * @throws IllegalArgumentException if parameters are illegal (i.e.
	 * source file to trace, run command arguments etc.)
	 */
	boolean removeTraceInfo(String taskName, File traceSrcFile,
			String[] runCmdArgs) throws Exception;
	
	/**
	 * Clears the task. This removes all the traces from the specified task.
	 * 
	 * @param name the name of a task to be cleared
	 * @return <tt>true</tt> if the trace was cleared, <tt>false</tt> if it
	 * did not exist
	 */
	boolean clearTask(String task) throws Exception;
	
	/**
	 * Starts an existing task. This creates the corresponding traces, unless
	 * <tt>continueLast</tt> is set to <tt>true</tt>; in that case, uncompleted
	 * traces are not re-created.
	 * 
	 * @param taskName the name of the task to start
	 * @param continueLast whether to continue uncompleted traces
	 * @param overwrite whether to overwrite uncompleted traces
	 * @return the task the corresponding traces (each with the appropriate
	 * status and id) or <tt>null</tt> if there exist uncompleted tasks and
	 * <tt>overwrite</tt> is <tt>false</tt>
	 * @throws NoSuchTaskException if the task does not exist
	 * @throws IllegalArgumentException if the task name is invalid
	 */
	Task startTask(String taskName, boolean continueLast, boolean overwrite)
			throws NoSuchTaskException, Exception;
	
	/**
	 * Imports the specified traces and associates them to the specified
	 * task. The second element of each pair in the <tt>importMap</tt>
	 * determines the task to which the imported trace should be associated to.
	 * If the task already exists, the behavior is determined by the
	 * specified import mode:
	 * <ul>
	 *   <li>{@link TraceImportMode#IGNORE} - does nothing (skips import of 
	 *   the trace)</li>
	 *   <li>{@link TraceImportMode#APPEND} - keeps the info about traces
	 *   associated with the task</li>
	 *   <li>{@link TraceImportMode#CLEAN} - modifies the task so
	 *   that it contains info only about the imported traces</li>
	 * </ul>
	 * 
	 * @param taskName the name of the task to import the traces to
	 * @param importMap the collection of traces to be imported
	 * @param mode the import mode
	 * @return the set of names for the newly created tasks
	 * @throws NoSuchTaskException if a task does not exist
	 */
	Set<String> importTraces(Collection<Pair<Trace, String>> importMap,
			TraceImportMode mode)
	throws NoSuchTaskException, Exception;
	
	public enum TraceImportMode {
		IGNORE,
		APPEND,
		CLEAN
	};
}

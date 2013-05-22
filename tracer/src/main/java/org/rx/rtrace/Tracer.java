package org.rx.rtrace;

import java.util.Collection;
import java.util.List;

import org.rx.rtrace.task.Task;

public interface Tracer  {
	
	/**
	 * Starts the tracing process. All exceptions will be dispatched
	 * to the registered listeners via call to 
	 * {@link TracerListener#exceptionThrown(Tracer, TracerException)}. 
	 */
	void start();
	
	/**
	 * Add the specified task for tracing.
	 * 
	 * @param task the task to be traced
	 */
	void addTask(Task task);
	
	/**
	 * Returns a collection of traces, in the order of dependencies.
	 * 
	 * @return unmodifiable collection
	 */
	List<Trace> getTraces();
	
	/**
	 * Returns the collection of completed traces, in the order of completion.
	 * 
	 * @return unmodifiable list
	 */
	Collection<Trace> getCompletedTraces();
	
	int getFlags();
	
	void addTraceListener(TracerListener l);
	void removeTraceListener(TracerListener l);
}

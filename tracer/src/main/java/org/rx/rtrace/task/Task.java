package org.rx.rtrace.task;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.rx.rtrace.Trace;
import org.rx.rtrace.TraceInfo;

public class Task extends TaskInfo {
	
	private Map<TraceKey, Trace> traces;

	public Task(String name) {
		super(name);
		this.traces = new HashMap<TraceKey, Trace>();
	}
	
	public Task(String name, Task clone) {
		super(name);
		this.traces = new HashMap<TraceKey, Trace>(clone.traces);
	}
	
	public Task(String name, Task[] tasksToMerge) {
		super(name);
		int capacity = 0;
		for (Task t : tasksToMerge)
			capacity += t.traces.size();
		this.traces = new HashMap<TraceKey, Trace>(capacity);
		for (Task taskToMerge : tasksToMerge)
			traces.putAll(taskToMerge.traces);
	}
	
	public Task(String name, Collection<? extends Trace> traces) {
		super(name);
		this.traces = new HashMap<TraceKey, Trace>(traces.size());
		for (Trace trace : traces)
			addTrace(trace);
	}
	
	@Override
	public Set<TraceKey> getTraceKeys() {
		return Collections.unmodifiableSet(traces.keySet());
	}
	
	@Override
	public int getTraceKeyCount() {
		return traces.size();
	}

	@Override
	public TraceInfo getTraceInfo(TraceKey traceKey) {
		return traces.get(traceKey);
	}
	
	public Collection<Trace> getTraces() {
		return Collections.unmodifiableCollection(traces.values());
	}
	
	public int getTraceCount() {
		return getTraceKeyCount();
	}
	
	public void addTrace(Trace trace) {
		checkTraceNotNull(trace);
		traces.put(new TraceKey(trace), trace);
	}
	
	public boolean removeTrace(Trace trace) {
		return traces.remove(new TraceKey(trace)) != null;
	}
	
	public boolean removeTrace(File srcFile, String[] runCmdArgs) {
		return removeTrace(new TraceKey(srcFile, runCmdArgs));
	}
	
	public boolean removeTraces(Collection<TraceKey> traceKeys) {
		boolean removedAll = true;
		for (TraceKey tk : traceKeys)
			removedAll &= removeTrace(tk);
		return removedAll;
	}
	
	private boolean removeTrace(TraceKey traceKey) {
		return traces.remove(traceKey) != null;
	}
	
	@Override
	public int hashCode() {
		return getTraceKeys().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Task other = (Task) obj;
		return getTraceKeys().equals(other.getTraceKeys());
	}
	
	@Override
	public String toString() {
		return getName();
	}

	private static void checkTraceNotNull(Trace trace) {
		if (trace == null)
			throw new IllegalArgumentException("Trace cannot be null");
	}
}

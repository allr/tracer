package org.rx.rtrace;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.rx.rtrace.Profile.PropertyKey;
import org.rx.rtrace.task.Task;
import org.rx.rtrace.util.StringUtils;

abstract class AbstractTracer implements Tracer {

	private final Set<TracerListener> listeners = new HashSet<TracerListener>();
	
	private final Map<Trace.Status, Set<Trace>> tracesByStatus =
			new EnumMap<Trace.Status, Set<Trace>>(Trace.Status.class);
	
	public AbstractTracer() {
		for (Trace.Status status : Trace.Status.values())
			tracesByStatus.put(status, new LinkedHashSet<Trace>());
	}
	
	@Override
	public void addTask(Task task) {
		for (Trace trace : task.getTraces())
			if (!trace.isDone())
				addTrace(trace);
	}
	
	@Override
	public Set<Trace> getCompletedTraces() {
		return Collections.unmodifiableSet(
				tracesByStatus.get(Trace.Status.DONE));
	}

	@Override
	public void addTraceListener(TracerListener l) {
		listeners.add(l);
	}
	
	@Override
	public void removeTraceListener(TracerListener l) {
		listeners.remove(l);
	}
	
	protected void fireTracingStarted() {
		for (TracerListener l : listeners)
			l.tracingStarted(this);
	}
	
	protected void fireTracingDone() {
		for (TracerListener l : listeners)
			l.tracingDone(this);
	}
	
	protected void fireTraceRegistered(Trace trace) {
		for (TracerListener l : listeners)
			l.traceRegistered(this, trace);
	}
	
	protected void fireRTraceStarted(Trace trace) {
		for (TracerListener l : listeners)
			l.rTraceStarted(this, trace);
	}
	
	protected void fireRTraceDone(Trace trace) {
		for (TracerListener l : listeners)
			l.rTraceDone(this, trace);
	}
	
	protected void fireProcessingStarted(Trace trace) {
		for (TracerListener l : listeners)
			l.processingStarted(this, trace);
	}
	
	protected void fireProcessingDone(Trace trace) {
		for (TracerListener l : listeners)
			l.processingDone(this, trace);
	}
	
	protected void fireTracerException(TracerException e) {
		for (TracerListener l : listeners)
			l.exceptionThrown(this, e);
	}
	
	protected void fireThrowableThrown(Throwable t) {
		fireTracerException(new TracerException(t));
	}
	
	protected void fireTraceFailed(Trace trace, Throwable cause) {
		for (TracerListener l : listeners)
			l.traceFailed(this, trace, cause);
	}
	
	protected void addTrace(Trace trace) {
		Set<Trace> s = tracesByStatus.get(trace.getStatus());
		if (s.contains(trace))
			throw new IllegalArgumentException("Trace already added");
		s.add(trace);
		
		changeTraceStatus(trace, Trace.Status.REGISTERED);
//		fireTraceRegistered(trace);
	}
	
	protected void failTrace(Trace trace, Throwable t) {
//		System.err.println(">>> ERROR on: " + trace + ":\n" + t);
		trace.setError(true);
		
		try {
			try {
				TraceManagerFactory.getInstance().saveTrace(trace);
			} catch (Exception e) {
				fireThrowableThrown(e);
			}
		} finally {
			if (t instanceof TracerException)
				fireTracerException((TracerException) t);
			else
				fireThrowableThrown(t);
			
			fireTraceFailed(trace, t);
		}
	}
	
	protected void changeTraceStatus(Trace trace, Trace.Status newStatus) {
		if (trace.getStatus() == newStatus)
			return;
		
		Trace.Status oldStatus = trace.getStatus();
		trace.setStatus(newStatus);
		tracesByStatus.get(oldStatus).remove(trace);
		tracesByStatus.get(newStatus).add(trace);
		
		try {
			TraceManagerFactory.getInstance().saveTrace(trace);
		} catch (Exception e) {
			fireThrowableThrown(e);
		}
		
		switch (newStatus) {
		case REGISTERED:
			fireTraceRegistered(trace);
		case R_STARTED:
			fireRTraceStarted(trace);
		case R_DONE:
			fireRTraceDone(trace);
		case PROCESSING:
			fireProcessingStarted(trace);
		case DONE:
			fireProcessingDone(trace);
		}
	}
	
	
	abstract static class TracerTask implements Callable<Integer> {
		Trace trace;
		
		public TracerTask(Trace trace) {
			this.trace = trace;
		}
		
	}
	
	
	abstract static class RProcessTask extends TracerTask {
		protected static final int EXIT_SUCCESS = 0;
		
		public RProcessTask(Trace trace) {
			super(trace);
		}
		
		@Override
		public Integer call() throws Exception {
			String[] cmdArray = getCommandArray();
			System.err.println("cmdArray = " + Arrays.toString(cmdArray));
			Process proc = Runtime.getRuntime().exec(cmdArray);
			return proc.waitFor();
		}
		
		abstract String[] getCommandArray();
		
		static boolean isSuccess(int exitCode) {
			return exitCode == EXIT_SUCCESS;
		}
	}
	
	
	static class RTraceTask extends RProcessTask {
		public RTraceTask(Trace trace) {
			super(trace);
		}

		@Override
		public Integer call() throws RTraceFailedException {
			Integer exitCode;
			try {
				exitCode = super.call();
				if (!isSuccess(exitCode))
					 throw new RTraceFailedException(trace, exitCode);
			} catch (Exception e) {
				throw new RTraceFailedException(trace, e);
			}
			return exitCode;
		}
		
		@Override
		String[] getCommandArray() {
			String rInstrumentedCmd = (String) Profile.getActiveProfile()
					.getProperty(PropertyKey.R_INSTRUMENTED_PATH).getValue();
			return StringUtils.flatten(
					rInstrumentedCmd,
					"--no-restore",
					"--no-save",
					"--slave",
					"--trace", "all",  // TODO
					"--tracedir", trace.getOutputDir().getPath(),
					"-f", trace.getSourceFile().getPath(),
					"--args", trace.getRunCmdArgs());
		}
	}
	
	
	static class RTimedTask extends RProcessTask {
		public RTimedTask(Trace trace) {
			super(trace);
		}

		@Override
		public Integer call() throws RTimedFailedException {
			Integer exitCode;
			try {
				System.err.println("Calling... ");
				exitCode = super.call();
				System.err.println("DONE (exitcode = " + exitCode);
				if (!isSuccess(exitCode))
					throw new RTimedFailedException(trace, exitCode);
			} catch (Exception e) {
				throw new RTimedFailedException(trace, e);
			}
			return exitCode;
		}
		
		@Override
		String[] getCommandArray() {
			String rInstrumentedCmd = (String) Profile.getActiveProfile()
					.getProperty(PropertyKey.R_TIMED_PATH).getValue();

			File outputFile = trace.getRTimedOutputFile();
			outputFile.getParentFile().mkdirs();  // XXX hack
			return StringUtils.flatten(
					rInstrumentedCmd,
					"--no-restore",
					"--no-save",
					"--slave",
					"--time=" + outputFile.getPath(),
					"-f", trace.getSourceFile().getPath(),
					"--args", trace.getRunCmdArgs());
		}
	}
	
}

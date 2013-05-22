package org.rx.rtrace;

/**
 * A listener for tracer events.
 * 
 * @author "Leo Osvald"
 *
 */
public interface TracerListener {
	public void tracingStarted(Tracer t);
	public void tracingDone(Tracer t);
	
	/**
	 * Called when a trace changes its status from
	 * {@link Trace.Status#UNREGISTERED} to {@link Trace.Status#REGISTERED}.
	 * 
	 * @param tracer the tracer performing the tracing
	 * @param trace the trace that has been registered
	 */
	public void traceRegistered(Tracer tracer, Trace trace);
	
	/**
	 * Called when a trace changes its status from
	 * {@link Trace.Status#REGISTERED} to {@link Trace.Status#R_STARTED}.
	 * 
	 * @param tracer the tracer performing the tracing
	 * @param trace the trace for which R tracing has started
	 */
	public void rTraceStarted(Tracer tracer, Trace trace);
	
	/**
	 * Called when a trace changes its status from
	 * {@link Trace.Status#R_STARTED} to {@link Trace.Status#R_DONE}.
	 * 
	 * @param tracer the tracer performing the tracing
	 * @param trace the trace for which R tracing has completed
	 */
	public void rTraceDone(Tracer tracer, Trace trace);
	
	/**
	 * Called when a trace changes its status from
	 * {@link Trace.Status#R_DONE} to {@link Trace.Status#PROCESSING}.
	 * 
	 * @param tracer the tracer performing the tracing
	 * @param trace the trace whose processing has started
	 */
	public void processingStarted(Tracer tracer, Trace trace);
	
	/**
	 * Called when a trace changes its status from
	 * {@link Trace.Status#PROCESSING} to {@link Trace.Status#DONE}.
	 * 
	 * @param tracer the tracer performing the tracing
	 * @param trace the trace which has been processed
	 */
	public void processingDone(Tracer tracer, Trace trace);
	
	/**
	 * Called when an exception or error occurs during tracing
	 * @param t the tracer where the exception occurred
	 * @param e the tracer exception
	 */
	public void exceptionThrown(Tracer t, TracerException e);
	
	/**
	 * Called when a trace operation fails. If a trace fails because of
	 * an exception, the {@link #exceptionThrown(Tracer, TracerException)}
	 * is called first.
	 * 
	 * @param t the tracer where the exception occurred
	 * @param trace the trace that has failed
	 * @param cause cause of the failure (if known, otherwise <tt>null</tt>)
	 */
	public void traceFailed(Tracer t, Trace trace, Throwable cause);
}

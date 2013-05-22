package org.rx.rtrace;

public class TraceException extends TracerException {
	private static final long serialVersionUID = 1L;

	private final Trace trace;
	
	public TraceException(Trace trace, Throwable t) {
		this(trace, t, null);
	}
	
	public TraceException(Trace trace, Throwable t, String msg) {
		super(t, msg);
		this.trace = trace;
	}
	
	public Trace getTrace() {
		return trace;
	}
}

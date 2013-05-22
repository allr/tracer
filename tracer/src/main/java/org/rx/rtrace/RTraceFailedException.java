package org.rx.rtrace;

public class RTraceFailedException extends ProcessFailedException {
	private static final long serialVersionUID = 1L;
	
	public RTraceFailedException(Trace trace, int exitCode) {
		super(trace,
				generateMessage(trace) + " (exit code = " + exitCode + ")",
				exitCode);
	}
	
	public RTraceFailedException(Trace trace, Throwable t) {
		super(trace, t);
	}

	private static String generateMessage(Trace trace) {
		return "R-timed of source " + trace.getSourceFile() + " failed";
	}
}

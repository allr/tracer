package org.rx.rtrace;

public class RTimedFailedException extends ProcessFailedException {
	private static final long serialVersionUID = 1L;

	public RTimedFailedException(Trace trace, int exitCode) {
		super(trace,
				generateMessage(trace) + " (exit code = " + exitCode + ")",
				exitCode);
	}
	
	public RTimedFailedException(Trace trace, Throwable t) {
		super(trace, t);
	}

	private static String generateMessage(Trace trace) {
		return "R-timed of source " + trace.getSourceFile() + " failed";
	}
}

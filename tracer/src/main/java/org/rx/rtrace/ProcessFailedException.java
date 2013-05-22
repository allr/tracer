package org.rx.rtrace;

class ProcessFailedException extends TraceException {
	private static final long serialVersionUID = 1L;
	
	private final Integer exitCode;
	
	public ProcessFailedException(Trace trace, String msg, int exitCode) {
		this(trace, msg, null, exitCode);
	}
	
	public ProcessFailedException(Trace trace, String msg, Throwable t) {
		this(trace, msg, t, null);
	}
	
	public ProcessFailedException(Trace trace, Throwable t) {
		this(trace, t.getMessage(), t, null);
	}
	
	private ProcessFailedException(Trace trace, String msg, Throwable t,
			Integer exitCode) {
		super(trace, t, msg);
		this.exitCode = exitCode;
	}
	
	public Integer getExitCode() {
		return exitCode;
	}
}

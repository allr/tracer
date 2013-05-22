package org.rx.rtrace;

public class TracerException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private final Throwable t;
	private final Code code;
	
	public TracerException(Throwable t, String msg) {
		super(msg, t);
		this.t = t;
		if (t instanceof Error)
			this.code = Code.ERROR;
		else if (t instanceof RuntimeException)
			this.code = Code.RUNTIME_EXCEPTION;
		else
			this.code = Code.EXCEPTION;
	}
	
	public TracerException(Throwable t) {
		this(t, null);
	}
	
	public Throwable getThrowable() {
		return t;
	}
	
	public int getCode() {
		return code.val;
	}
	
	@Override
	public String toString() {
		return code.toString() + ": " + (t != null ? t.toString() : "");
	}
	
	private enum Code {
		ERROR(1),
		RUNTIME_EXCEPTION(2),
		EXCEPTION(3);
		
		final int val;

		private Code(int val) {
			this.val = val;
		}
	}
}

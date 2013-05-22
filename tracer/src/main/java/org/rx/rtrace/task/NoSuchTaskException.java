package org.rx.rtrace.task;

public class NoSuchTaskException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private String taskName;

	public NoSuchTaskException(String taskName) {
		this(taskName, "No such task" +
				(taskName != null ? ": " + taskName : "!")); 
	}
	
	public NoSuchTaskException(String taskName, String message) {
		super(message);
		this.taskName = taskName;
	}
	
	public String getTaskName() {
		return taskName;
	}
}

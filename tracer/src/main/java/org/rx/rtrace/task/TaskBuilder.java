package org.rx.rtrace.task;

import java.io.File;

import org.rx.rtrace.Trace;

public class TaskBuilder {

	private final Task task;
	
	public TaskBuilder(String taskName) {
		task = new Task(taskName);
		// TODO Auto-generated constructor stub
	}
	
	public void clear() {
		task.getTraces().clear();
	}
	
	public void addSourceFile(File sourceFile, String... runArgs) {
		task.getTraces().add(new Trace(sourceFile, runArgs));
	}
	
	public void addRTraceFiles(File traceDir) {
		// TODO read summary and initialize trace
	}
	
	public Task toTask() {
		return new Task(task.getName(), task);
	}
	
}

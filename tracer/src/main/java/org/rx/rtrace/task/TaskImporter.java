package org.rx.rtrace.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

public interface TaskImporter {
	Collection<TaskInfo> importTasks(File inputFile) throws
	FileNotFoundException, IOException, ParsingException;
	
	public static class ParsingException extends Exception {
		private static final long serialVersionUID = 1L;

		public ParsingException(String message) {
			super(message);
		}
		
		public ParsingException(Throwable t) {
			super(t);
		}
		
		public ParsingException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}

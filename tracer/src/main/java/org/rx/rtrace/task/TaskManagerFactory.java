package org.rx.rtrace.task;

import java.io.IOException;
import java.sql.SQLException;

public class TaskManagerFactory {

	private static TaskManager instance;
	
	public static TaskManager getInstance() throws SQLException, IOException {
		if (instance == null)
			instance = new DbTaskManager();
		return instance;
	}
	
	
	private TaskManagerFactory() {  // disable instantiation
	}
}

package org.rx.rtrace.task;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public interface TaskExporter {
	void exportTasks(Collection<TaskInfo> taskInfos, File outputFile)
			throws IOException;
}

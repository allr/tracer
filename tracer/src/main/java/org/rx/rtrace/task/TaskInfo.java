package org.rx.rtrace.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.rx.rtrace.Entity;
import org.rx.rtrace.TraceInfo;

public abstract class TaskInfo extends Entity<Integer> {
	private String name;
	
	public TaskInfo() {
	}
	
	public TaskInfo(String name) {
		setName(name);
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		checkName(name);
		this.name = name;
	}
	
	public abstract Set<TraceKey> getTraceKeys();
	
	public abstract TraceInfo getTraceInfo(TraceKey traceKey);
	
	public int getTraceKeyCount() {
		return getTraceKeys().size();
	}
	
	public Collection<TraceInfo> getTraceInfos() {
		Collection<TraceInfo> traceInfos = new ArrayList<TraceInfo>(
				getTraceKeyCount());
		for (TraceKey traceKey : getTraceKeys()) {
			if (traceKey != null)
				traceInfos.add(getTraceInfo(traceKey));
		}
		return traceInfos;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaskInfo other = (TaskInfo) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	private static void checkName(String name) {
		if (name == null)
			throw new IllegalArgumentException("Task name cannot be null");
		if (name.isEmpty())
			throw new IllegalArgumentException("Task name cannot be empty");
	}
	
	public static class TraceKey {
		private File srcFile;
		private String[] runCmdArgs;
		
		public TraceKey(File srcFile, String[] runCmdArgs) {
			this.srcFile = srcFile;
			this.runCmdArgs = runCmdArgs;
		}
		
		public TraceKey(TraceInfo traceInfo) {
			this(traceInfo.getSourceFile(), traceInfo.getRunCmdArgs());
		}
		
		public File getSrcFile() {
			return srcFile;
		}

		public void setSrcFile(File srcFile) {
			this.srcFile = srcFile;
		}

		public String[] getRunCmdArgs() {
			return runCmdArgs;
		}

		public void setRunCmdArgs(String[] runCmdArgs) {
			this.runCmdArgs = runCmdArgs;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(runCmdArgs);
			result = prime * result
					+ ((srcFile == null) ? 0 : srcFile.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TraceKey other = (TraceKey) obj;
			if (srcFile == null) {
				if (other.srcFile != null)
					return false;
			} else if (!srcFile.equals(other.srcFile))
				return false;
			return Arrays.equals(runCmdArgs, other.runCmdArgs);
		}

		@Override
		public String toString() {
			return "TraceKey [srcFile=" + srcFile + ", runCmdArgs="
					+ Arrays.toString(runCmdArgs) + "]";
		}
		
		
	}
	
}

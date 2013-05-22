package org.rx.rtrace;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;

import org.rx.rtrace.util.CommandLineUtils;
import org.rx.rtrace.util.StringUtils;

public class TraceInfo extends Entity<Integer> implements Serializable {
	private static final long serialVersionUID = 1L;

	private String name;

	private final File sourceFile;
	private final String[] runCmdArgs;
	
	private File outputDir;
	private String fileType;
	private String mapFileType;
	
	public TraceInfo(String name, File sourceFile, String[] runCmdArgs,
			File outputDir, String fileType, String mapFileType) {
		CommandLineUtils.checkCommandLineArgs(runCmdArgs);
		this.sourceFile = sourceFile;
		this.runCmdArgs = runCmdArgs;
		setName(name);
		setOutputDir(outputDir);
		setFileType(fileType);
		setMapFileType(mapFileType);
	}
	
	public TraceInfo(String name, File sourceFile, String... runCmdArgs) {
		this(name, sourceFile, runCmdArgs, sourceFile.getParentFile(), null,
				null);
	}
	
	public TraceInfo(File sourceFile, String... runCmdArgs) {
		this(generateName(sourceFile, runCmdArgs), sourceFile, runCmdArgs);
	}
	
	public TraceInfo(TraceInfo clone) {
		this(clone.name, clone.sourceFile, clone.runCmdArgs, clone.outputDir,
				clone.fileType, clone.mapFileType);
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException("Name cannot be null");
		this.name = name;
	}
	
	public File getSourceFile() {
		return sourceFile;
	}

	public String[] getRunCmdArgs() {
		return runCmdArgs;
	}

	public File getOutputDir() {
		return outputDir;
	}
	
	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}
	
	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getMapFileType() {
		return mapFileType;
	}

	public void setMapFileType(String mapFileType) {
		this.mapFileType = mapFileType;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(runCmdArgs);
		result = prime * result
				+ ((sourceFile == null) ? 0 : sourceFile.hashCode());
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
		TraceInfo other = (TraceInfo) obj;
		if (!Arrays.equals(runCmdArgs, other.runCmdArgs))
			return false;
		if (sourceFile == null) {
			if (other.sourceFile != null)
				return false;
		} else if (!sourceFile.equals(other.sourceFile))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TraceInfo [name=" + name + ", sourceFile=" + sourceFile
				+ ", runCmdArgs=" + Arrays.toString(runCmdArgs) + "]";
	}
	
	private static String generateName(File sourceFile, String[] args) {
		return sourceFile.getAbsolutePath() + (args.length == 0 ? "" :
			"-" + StringUtils.join("-", args).replaceAll("\\s+", "-"));
	}
}

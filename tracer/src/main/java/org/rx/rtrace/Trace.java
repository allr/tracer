package org.rx.rtrace;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

/**
 * Tracer task. It is uniquely identified by the following three properties:
 * <ul>
 * <li>{@link #getSourceFile()} - the source file to be traced</li>
 * <li>{@link #getRunCmdArgs()} - command line arguments to be passed to R</li>
 * <li>{@link #getDate()} - the time when the tracing started</li>
 * </ul>
 * 
 * @author "Leo Osvald"
 *
 */
public class Trace extends TraceInfo {
	private static final long serialVersionUID = 1L;
	
	private Date date;

	private transient Status status;
	private transient boolean error;

	public Trace(String name, File sourceFile, String[] runCmdArgs,
			File outputDir, String fileType, String mapFileType) {
		super(name, sourceFile, runCmdArgs, outputDir, fileType, mapFileType);
		initialize();
	}

	public Trace(String name, File sourceFile, String... runCmdArgs) {
		super(name, sourceFile, runCmdArgs);
		initialize();
	}

	public Trace(File sourceFile, String... runCmdArgs) {
		super(sourceFile, runCmdArgs);
		initialize();
	}
	
	public Trace(TraceInfo info) {
		super(info);
		initialize();
	}
	
	public void restart() {
		setId(null);
		setDate(null);
		initialize();
	}


	/**
	 * If the tracing has started, i.e. method {@link #isStarted()} returns
	 * <tt>true</tt>, returns the corresponding time.
	 *
	 * @return the date corresponding to the  time when the trace is started,
	 *  or <tt>null</tt> otherwise.
	 */
	public Date getDate() {
		return date;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		if (this.status == null)
			throw new IllegalArgumentException("Trace status cannot be null!");
		
		this.status = status;
		
		// set the date to current time to avoid differentiating
		// between two traces on possible different files
		// (note that the source file could have been updated up to this point
		if (status == Status.R_STARTED)
			date = new Date();
	}
	
	public File getRTimedOutputFile() {
		return new File(getOutputDir(), getName() + ".rawtime");
	}
	
	public boolean isStarted() {
		return getStatus() == Status.R_STARTED;
	}
	
	public boolean isDone() {
		return getStatus() == Status.DONE;
	}
	
	public boolean isError() {
		return error;
	}
	
	public void setError(boolean error) {
		this.error = error;
	}
	
	@Override
	public int hashCode() {
		return 31 * super.hashCode() + ((date == null) ? 0 : date.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		Trace other = (Trace) obj;
		
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Trace [name=" + getName() + ", sourceFile=" + getSourceFile()
				+ ", runCmdArgs=" + Arrays.toString(getRunCmdArgs()) + ", date="
				+ date + ", status = " + status + ", output_dir = "
				+ getOutputDir() + ", file_type = " + getFileType()
				+ ", map_file_type = " + getMapFileType() + ", error = "
				+ error + "]";
	}

	void setDate(Date date) {
		this.date = date;
	}
	
	private void initialize() {
		this.status = Status.UNREGISTERED;
		this.error = false;
	}


	public enum Status {
		UNREGISTERED,
		REGISTERED,
		R_STARTED,
		R_DONE,
		PROCESSING,
		STATIC,
		DONE;
	}
	
}

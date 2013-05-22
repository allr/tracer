package org.rx.rtrace;

import java.io.File;
import java.util.Collection;

public interface TraceManager {
	Trace registerTrace(Trace trace, boolean overwrite) throws Exception;
	void saveTrace(Trace trace) throws Exception;
	boolean deleteTrace(String name) throws Exception;
	boolean deleteTrace(int id) throws Exception;
	Trace getLastTrace(String name) throws Exception;
	boolean existsTrace(String name) throws Exception;
	
	Collection<Trace> getTraces() throws Exception;
	Collection<Trace> getTraces(String name) throws Exception;
	boolean deleteTraces(Collection<String> names, Collection<Integer> ids)
	throws Exception;
	
	boolean importTrace(TraceInfo traceInfo, boolean overwrite) throws
	Exception;
	
	boolean deleteTracesForSource(File sourceFile) throws Exception;
	Collection<Trace> getTracesForSource(File sourceFile) throws Exception;
}

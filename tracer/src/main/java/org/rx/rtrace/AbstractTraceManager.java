package org.rx.rtrace;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstract implementation of the {@link TraceManager} interface.
 * Overriding these methods in base class is highly recommended for
 * efficiency.
 * 
 * @author "Leo Osvald"
 *
 */
abstract class AbstractTraceManager implements TraceManager {

	@Override
	public Trace getLastTrace(String name) throws Exception {
		for (Trace trace : getTraces())
			if (trace.getName().equals(name))
				return trace;
		return null;
	}
	
	@Override
	public boolean existsTrace(String name) throws Exception {
		return getLastTrace(name) != null;
	}
	
	@Override
	public Collection<Trace> getTracesForSource(File sourceFile) throws
	Exception {
		Collection<Trace> ret = new ArrayList<Trace>();
		for (Trace trace : getTraces())
			if (trace.getSourceFile().equals(sourceFile))
				ret.add(trace);
		return ret;
	}

	@Override
	public boolean deleteTracesForSource(File sourceFile) throws Exception {
		boolean deletedAll = true;
		for (Trace trace : getTraces()) {
			if (trace.getSourceFile().equals(sourceFile))
				deletedAll &= deleteTrace(trace.getName());
		}
		return deletedAll;
	}

}

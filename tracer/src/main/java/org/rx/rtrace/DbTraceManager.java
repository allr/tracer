package org.rx.rtrace;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;

import org.rx.rtrace.Trace.Status;

public class DbTraceManager extends AbstractTraceManager {

	private Connection db;
	
	public DbTraceManager() throws IOException, SQLException {
		db = DatabaseFactory.getInstance();
		Database.createTracesTable(db);
	}
	
	@Override
	public Trace registerTrace(Trace trace, boolean overwrite) throws
	SQLException {
		boolean ok = false;
		Status oldStatus = trace.getStatus();
		try {
			trace.setStatus(Status.REGISTERED);
			ok = TraceStorage.createTrace(db, trace);
			if (!ok) {
				if (!overwrite)
					return null;
				
				Trace uncompletedTrace = TraceStorage.getLastTrace(db,
						trace.getSourceFile(),
						trace.getRunCmdArgs());
				if (uncompletedTrace == null || uncompletedTrace.isDone())
					return null;
				
				trace.setId(uncompletedTrace.getId());
				TraceStorage.updateTrace(db, trace);
				ok = true;
			}
			return trace;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
			if (!ok)
				trace.setStatus(oldStatus);
		}
	}

	@Override
	public boolean deleteTrace(String name) throws SQLException {
		boolean ok = false;
		try {
			ok = TraceStorage.deleteTrace(db, name);
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
		return ok;
	}
	
	@Override
	public boolean deleteTrace(int id) throws Exception {
		boolean ok = false;
		try {
			ok = TraceStorage.deleteTrace(db, id);
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
		return ok;
	}

	@Override
	public void saveTrace(Trace trace) throws SQLException {
		boolean ok = false;
		try {
			TraceStorage.updateTrace(db, trace);
			ok = true;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}

	@Override
	public Collection<Trace> getTraces() throws SQLException {
		Collection<Trace> traces = null;
		try {
			return traces = TraceStorage.getTraces(db);
		} finally {
			Database.commitOrRollbackIfNeeded(db, traces != null);
		}
	}
	
	@Override
	public Collection<Trace> getTraces(String name) throws SQLException {
		Collection<Trace> traces = null;
		try {
			return traces = TraceStorage.getTraces(db, name);
		} finally {
			Database.commitOrRollbackIfNeeded(db, traces != null);
		}
	}
	
	@Override
	public Trace getLastTrace(String name) throws Exception {
		boolean ok = false;
		try {
			// TODO ultra-low pri: replace with an efficient query
			Trace trace = super.getLastTrace(name);
			ok = true;
			return trace;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public boolean importTrace(TraceInfo traceInfo, boolean overwrite)
			throws Exception {
		boolean ok = false;
		Trace trace = new Trace(traceInfo);
		trace.setDate(Calendar.getInstance().getTime());
		trace.setStatus(Trace.Status.R_DONE);
		try {
			Trace lastTrace = TraceStorage.getLastTrace(db, 
					traceInfo.getSourceFile(),
					traceInfo.getRunCmdArgs());
			if (lastTrace != null && !lastTrace.isDone()) {
				if (!overwrite)
					return false;
				if (!TraceStorage.deleteTrace(db, lastTrace.getId()))
					return false;
			}
			System.err.println(trace);
			if (!TraceStorage.createTrace(db, trace))
				return false;
			ok = true;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
		return true;
	}

	@Override
	public boolean deleteTraces(Collection<String> names,
			Collection<Integer> ids) throws Exception {
		boolean ok = false;
		try {
			boolean deletedAll = TraceStorage.deleteTraces(db, names, ids);
			ok = true;
			return deletedAll;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public boolean deleteTracesForSource(File sourceFile) throws Exception {
		boolean ok = false;
		try {
			// TODO ultra-low pri: replace with an efficient query
			boolean deletedAll = super.deleteTracesForSource(sourceFile);
			ok = true;
			return deletedAll;
		} finally {
			Database.commitOrRollbackIfNeeded(db, ok);
		}
	}
	
	@Override
	public Collection<Trace> getTracesForSource(File sourceFile) throws
	Exception {
		Collection<Trace> traces = null;
		try {
			// TODO ultra-low pri: replace with an efficient query
			traces = super.getTracesForSource(sourceFile);
			return traces;
		} finally {
			Database.commitOrRollbackIfNeeded(db, traces != null);
		}
	}

}

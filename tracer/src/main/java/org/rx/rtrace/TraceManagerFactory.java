package org.rx.rtrace;

import java.io.IOException;
import java.sql.SQLException;

public class TraceManagerFactory {

	private static TraceManager instance;
	
	public static TraceManager getInstance() throws IOException,
	SQLException {
		if (instance == null)
			instance = new DbTraceManager();
		return instance;
	}
	
	private TraceManagerFactory() {  // disable instantiation
	}
}

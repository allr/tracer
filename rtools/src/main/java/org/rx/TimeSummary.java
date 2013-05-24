package org.rx;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;


public class TimeSummary extends Summary {
	public String get_reference_field(){ return "vignettes"; };
	public String get_table_name() { return "time_summary"; }

	static final TraceInfo fields[] = {
		new TraceInfo("TraceDate", "text"),
		new TraceInfo("Workdir", "text"),
		new TraceInfo("RusageMaxResidentMemorySet"),
		new TraceInfo("RusageSharedMemSize"),
		new TraceInfo("RusageUnsharedDataSize"),
		new TraceInfo("RusagePageReclaims"),
		new TraceInfo("RusagePageFaults"),
		new TraceInfo("RusageSwaps"),
		new TraceInfo("RusageBlockInputOps"),
		new TraceInfo("RusageBlockOutputOps"),
		new TraceInfo("RusageIPCSends"),
		new TraceInfo("RusageIPCRecv"),
		new TraceInfo("RusageSignalsRcvd"),
		new TraceInfo("RusageVolnContextSwitches"),
		new TraceInfo("RusageInvolnContextSwitches"),
		
		new TraceMultiInfo("MainLoop", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("Repl", new String[]{"time", "atime", "hrtime", "forced"}),

		new TraceMultiInfo("SymLookup", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("FunLookup", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("FunLookupEval", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("Match", new String[]{"time", "atime", "hrtime", "forced"}),

		new TraceMultiInfo("dotCall", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("dotCode", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("dotExternal", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("dotBuiltIn", new String[]{"time", "atime", "hrtime", "forced"}),
		//new TraceMultiInfo("dotBuiltIn2", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("dotSpecial2", new String[]{"time", "atime", "hrtime", "forced"}),
		//new TraceMultiInfo("dotCallFull", new String[]{"time", "atime", "hrtime", "forced"}),
		//new TraceMultiInfo("dotCodeFull", new String[]{"time", "atime", "hrtime", "forced"}),

		new TraceMultiInfo("doLogic", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("doArith", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("doSubset", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("doSubset2", new String[]{"time", "atime", "hrtime", "forced"}),
		
		
		new TraceMultiInfo("Duplicate", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("allocVector", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("allocS4", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("allocList", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("cons", new String[]{"time", "atime", "hrtime", "forced"}),
		//new TraceMultiInfo("R_alloc", new String[]{"time", "atime", "hrtime", "forced"}),

		new TraceMultiInfo("CheckStack", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("Install", new String[]{"time", "atime", "hrtime", "forced"}),

		new TraceMultiInfo("UnprotectPtr", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("Protect", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("GCInternal", new String[]{"time", "atime", "hrtime", "forced"}),
		
		new TraceMultiInfo("System", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("Download", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("Sleep", new String[]{"time", "atime", "hrtime", "forced"}),
		//new TraceMultiInfo("CurlPerform", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("inSockConnect", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("inSockOpen", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("inSockRead", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("inSockWrite", new String[]{"time", "atime", "hrtime", "forced"}),

		new TraceMultiInfo("xzFile", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("bzFile", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("gzFile", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("zipRead", new String[]{"time", "atime", "hrtime", "forced"}),
		new TraceMultiInfo("doUnzip", new String[]{"time", "atime", "hrtime", "forced"}),

		new TraceMultiInfo("onExits", new String[]{"time", "atime", "hrtime", "forced"}),
		//new TraceMultiInfo("FindContext", new String[]{"time", "atime", "hrtime", "forced"})
		};
	
	public TraceInfo[] get_summary_fields(){
		return fields;
	}
	
	public static void register_time_summary(Connection database, File sum_file,
			int trace_id) throws IOException, SQLException{
		(new TimeSummary()).register_summary(database, sum_file, trace_id);
	}
	
	void register_time_summary(Connection database, File file) throws IOException, SQLException{
		String vignette_name = file.getName().replace(".time", "");
		int vignette_id = DataBase.register_vignette(database, vignette_name);
		register_summary(database, file, vignette_id);
		if(Option.Verbose.verbose)
			System.out.println("Imported: "+file+" ("+vignette_id+")");
	}
	
	public static void main(String[] args) throws IOException, SQLException {
		Option[] avaible_options = {
				new DataBase.DBNameOption(),
				new DataBase.DBOverwriteOption(),
				new Option.Quiet()
			};
		String[] todo = Option.process_command_line(args, avaible_options);

		Connection database = DataBase.create_database();

		TimeSummary ts = new TimeSummary();
		for(String fname: todo)
			try {
				File file = new File(fname);
				if(file.isDirectory())
					for(File file_indir: file.listFiles(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.endsWith(".time");
						}
					}))
						ts.register_time_summary(database, file_indir);
				else
					ts.register_time_summary(database, file);
			} catch (Exception e) {
				System.err.println("Couldn't import: " + fname);
			}
	}
}

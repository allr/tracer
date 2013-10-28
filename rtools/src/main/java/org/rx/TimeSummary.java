// NOTICE: main function seems to be unused

package org.rx;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;


public class TimeSummary extends Summary {
	public String get_reference_field(){ return "vignettes"; };
	public String get_table_name() { return "time_summary"; }

        // wrapper class to generate a default parameter
        public static class TimeTMI extends TraceMultiInfo {
                public TimeTMI(String name) {
                        super(name, new String[]{"self", "total", "starts", "aborts"});
                }
        }

        // FIXME: Rotate table by 90 degrees, throw this array away and
        //        create the names dynamically from the .time file
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
                new TraceMultiInfo("OverheadEstimates",
                                   new String[]{"Small", "Large"}),

                // timeR internal
                new TimeTMI("Startup"),
                new TimeTMI("UserFuncFallback"),

                // memory.c
		new TimeTMI("cons"),
		new TimeTMI("allocVector"),
		new TimeTMI("allocList"),
		new TimeTMI("allocS4"),
		new TimeTMI("GCInternal"),

                // arith.c
		new TimeTMI("doArith"),

                // array.c
                new TimeTMI("doMatprod"),

                // connections.c
		new TimeTMI("gzFile"),
		new TimeTMI("bzFile"),
		new TimeTMI("xzFile"),

                // context.c
		new TimeTMI("onExits"),

                // dotcode.c
		new TimeTMI("dotExternalFull"),
		new TimeTMI("dotExternal"),
		new TimeTMI("dotCallFull"),
		new TimeTMI("dotCall"),
		new TimeTMI("dotCFull"),
		new TimeTMI("dotC"),
		new TimeTMI("dotFortranFull"),
		new TimeTMI("dotFortran"),

                // dounzip.c
		new TimeTMI("doUnzip"),
		new TimeTMI("zipRead"),

		// duplicate.c
		new TimeTMI("Duplicate"),

                // envir.c
                new TimeTMI("findVarInFrame3other"),
		new TimeTMI("SymLookup"),
		new TimeTMI("FunLookup"),
		new TimeTMI("FunLookupEval"),

		// eval.c
                new TimeTMI("Match"),
                new TimeTMI("evalList"),

                // internet.c
		new TimeTMI("Download"),

                // logic.c
		new TimeTMI("doLogic"),
                new TimeTMI("doLogic2"),

                // main.c
		new TimeTMI("Repl"),
		new TimeTMI("SetupMainLoop"),
                new TimeTMI("endMainloop"),

                // names.c
		new TimeTMI("Install"),
		new TimeTMI("dotSpecial2"),

                // relop.c
                new TimeTMI("doRelop"),

                // subset.c
		new TimeTMI("doSubset"),
		new TimeTMI("doSubset2"),
		new TimeTMI("doSubset3"),

                // rsock.c
		new TimeTMI("inSockRead"),
		new TimeTMI("inSockWrite"),
		new TimeTMI("inSockOpen"),
		new TimeTMI("inSockConnect"),

                // sys-std.c
		new TimeTMI("Sleep"),

		// sys-unix.c
		new TimeTMI("System"),

                // additional sums synthesized by timeR
                new TimeTMI("BuiltinSum"),
                new TimeTMI("SpecialSum"),
                new TimeTMI("UserFunctionSum"),
                new TraceInfo("TotalRuntime"),

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

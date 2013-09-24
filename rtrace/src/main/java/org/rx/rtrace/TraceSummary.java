// main probably not used in traceR?

package org.rx.rtrace;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.rx.DataBase;
import org.rx.Option;
import org.rx.Summary;

public class TraceSummary extends Summary {
	public static String DEFAULT_FILE_NAME = "trace_summary";
	public String get_reference_field(){ return "traces"; };
	public String get_table_name() { return "summary"; }

	static final TraceInfo fields[] = {
		new TraceInfo("File", "text"),
		new TraceInfo("Args", "text"),
		new TraceInfo("TraceDate", "text"),
		new TraceInfo("FinalContextStackHeight"),
		new TraceInfo("FinalContextStackFlushed"),
		new TraceInfo("FatalErrors"),
		new TraceInfo("NonFatalErrors"),
		new TraceInfo("MaxStackHeight"),
		new TraceInfo("TraceStackErrors"),
		new TraceInfo("BytesWritten"),
		new TraceInfo("EventsTraced"),
		new TraceInfo("FuncsDecld"),
		new TraceInfo("NullSrcrefs"),
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
		new TraceInfo("EvalsCount"),
		new TraceInfo("AllocatedCons"),
                new TraceInfo("AllocatedConsPeak"),
                new TraceInfo("AllocatedNonCons"),
		new TraceInfo("AllocatedEnv"),
		new TraceInfo("AllocatedPromises"),
		new TraceInfo("AllocatedSXP"),
                new TraceInfo("AllocatedExternal"), // R_alloc
		new TraceMultiInfo("Dispatch", new String[]{"total", "failed"}),
		new TraceMultiInfo("AllocatedList", new String[]{"tl", "elts"}),
		new TraceMultiInfo("AllocatedVectors", new String[]{"tl", "elts", "size", "asize"}),
		new TraceMultiInfo("AllocatedSmallVectors", new String[]{"tl", "elts", "size", "asize"}),
		new TraceMultiInfo("AllocatedLargeVectors", new String[]{"tl", "elts", "size", "asize"}),
		new TraceMultiInfo("AllocatedOneVectors", new String[]{"tl", "elts", "size", "asize"}),
		new TraceMultiInfo("AllocatedNullVectors", new String[]{"tl", "elts", "size", "asize"}),
		new TraceMultiInfo("AllocatedStringBuffer", new String[]{"tl", "elts", "size"}),
		//new TraceMultiInfo("AllocatedObjects", new String[]{"tl", "elts", "size"}),
		new TraceMultiInfo("Duplicate", new String[]{"tl", "elts", "elts1"}),
		new TraceMultiInfo("Named", new String[]{"elts", "promoted", "dowgraded", "keeped"}),
		new TraceMultiInfo("ApplyDefine", new String[]{"local", "other"}),
		new TraceMultiInfo("DefineVar", new String[]{"local", "other"}),
		new TraceMultiInfo("SetVar", new String[]{"local", "other"}),
		new TraceInfo("DefineUserDb"),
		new TraceInfo("ErrCountAssign"),
		new TraceMultiInfo("AvoidedDup", new String[]{"named", "duplicate"}),
		new TraceMultiInfo("ErrorEvalSet", new String[]{"simple", "super"}),
		new TraceInfo("UnusedTag"),
		new TraceInfo("UnusedAttr"),
		new TraceInfo("GCObj"),
		new TraceInfo("GCCons"),
                new TraceMultiInfo("AvoidedDup", new String[]{"avoided","needed"}),
//		new TraceMultiInfo("ScalarVector", new String[]{"int", "lgl", "cplx", "real", "str", "expr", "vec", "raw", "total"}),
//		new TraceMultiInfo("NullVector", new String[]{"int", "lgl", "cplx", "real", "str", "expr", "vec", "raw", "total"}),
//		new TraceMultiInfo("TrueVector", new String[]{"int", "lgl", "cplx", "real", "str", "expr", "vec", "raw", "total"})
	};
	
	public TraceInfo[] get_summary_fields(){
		return fields;
	}
	
	static void register_trace_summary(Connection database, String trace_file,
			int trace_id) throws IOException, SQLException{
		File sum_file = new File(new File(trace_file).getParent(), DEFAULT_FILE_NAME);
		(new TraceSummary()).register_summary(database, sum_file, trace_id);
	}
	
	public static void main(String[] args) throws IOException, SQLException {
		Option[] avaible_options = {
				new DataBase.DBNameOption(),
				new DataBase.DBOverwriteOption(),
				new Option.Quiet()
			};
		String[] todo = Option.process_command_line(args, avaible_options);

		Connection database = DataBase.create_database();
		DataBase.create_table(database, "traces", "id integer primary key autoincrement, vignette_id reference vignettes, status integer, ts text");

		for(String fname: todo)
			try {
				File file = new File(fname);
				if(file.isDirectory() && ! new File(file, DEFAULT_FILE_NAME).exists())
					for(File file_indir: file.listFiles(new FileFilter() {
						@Override
						public boolean accept(File name) {
							return name.isDirectory() && new File(name, DEFAULT_FILE_NAME).exists();
						}
					}))
						register_trace_summary(database, file_indir.toString()+'/'+DEFAULT_FILE_NAME, file.hashCode());
				else
					register_trace_summary(database, file.toString(), file.hashCode());
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Couldn't import: " + fname);
			}
	}

}

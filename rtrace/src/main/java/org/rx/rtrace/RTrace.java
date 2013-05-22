package org.rx.rtrace;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import org.rx.DataBase;
import org.rx.FileTools;
import org.rx.Option;
import org.rx.Option.Help;
import org.rx.rtrace.processors.TraceProcessor;

public class RTrace {
	public static Connection database;

	private static final String DEFAULT_FILE_NAME = "-";
	public static boolean auto_patch = true;
	public static String file_type = null;
	public static String map_file_type = null;

	public static String supported_version = null;

	public static int trace_id = -1;  // FIXME remove this
	public static String vignette_name;

	/* TODO list: Add System properties to change data length */
	protected static ArrayList<TraceProcessor> option_processors = new ArrayList<TraceProcessor>();

	static protected Option[] avaible_options = {
		new Option.Help(){
			protected void process_option(String name, String opts[]) {
				usage(System.out, 0);
			}
		},
		new Option.Verbose(),
		new Option("--name", "Set vignette name", 1){
			protected void process_option(String name, String opts[]) {
				assert opts.length > 0;
				vignette_name = opts[0];
			}
		},
		new Option("-T", "File type [none, auto, zip, gz, jar]", 1){
			protected void process_option(String name, String opts[]) {
				assert opts.length > 0;
				file_type = FileTools.choose_file_type(opts[0]);
			}
		},
		new Option("--map-file-type", "File type [none, auto, zip, gz, jar] (default use same as trace file)", 1){
			protected void process_option(String name, String opts[]) {
				assert opts.length > 0;
				map_file_type = FileTools.choose_file_type(opts[0]);
			}
		},
		new Option("--map-file-name", "Map file name, default: "+FunctionMap.DEFAULT_MAP_FILE_NAME, 1){
			protected void process_option(String name, String opts[]) {
				assert opts.length > 0;
				FunctionMap.DEFAULT_MAP_FILE_NAME = opts[0];
			}
		},
		new Option("--no-autopatch", "Disable auto-patching feature", 0){
			protected void process_option(String name, String opts[]) {
				auto_patch = false;
			}
		},
		new Option("--summary-file", "Summary file name, default"+ TraceSummary.DEFAULT_FILE_NAME, 1){
			protected void process_option(String name, String opts[]) {
				TraceSummary.DEFAULT_FILE_NAME = opts[0];
			}
		},
		new Option("--trace-version", "Assert for a specific trace version", 1){
			protected void process_option(String name, String opts[]) {
				assert opts.length > 0;
				supported_version = opts[0];
			}
		},
		new DataBase.DBNameOption(),
		new DataBase.DBOverwriteOption(),
		
		new Option.Text("Processors"),
		new OptionTraceProcessor("--allcalls", org.rx.rtrace.processors.AllCalls.class, "Count all calls", 0),
		new OptionTraceProcessor("--foreign", org.rx.rtrace.processors.ForeignFunctions.class, "Count function call after some functions", 0),
		new OptionTraceProcessor("--promise", org.rx.rtrace.processors.Promises.class, "Count all promises stuffs", 0),
		new OptionTraceProcessor("--hidden", org.rx.rtrace.processors.FunctionHidding.class, "Function hidded by vars", 0),
		new OptionTraceProcessor("--counters", org.rx.rtrace.processors.SimpleCounters.class, "Counts simple things/stats"),
		new OptionTraceProcessor("--size", org.rx.rtrace.processors.CountSize.class, "Counts size of functions"),
		new OptionTraceProcessor("--recursive", org.rx.rtrace.processors.CountRecursive.class, "Counts recursive calls"),
		new OptionTraceProcessor("--args", org.rx.rtrace.processors.Arguments.class, "Count args type", 0),
		new Option.Text("Dev/test"),
		new OptionTraceProcessor("--promise-upward", org.rx.rtrace.processors.PromisesUpward.class, "Count all promises stuffs", 0),
		new Option.Text(""),
		new OptionTraceProcessor("-0", org.rx.rtrace.processors.Nothing.class, "Do nothing ..." )
		/*,
		new Option.Text("Broken processors"),
		new OptionTraceProcessor("--types", org.rx.rtrace.processors.TypesOfCalls.class, "Count types of calls", 0),
		new OptionTraceProcessor("-c", org.rx.rtrace.processors.FunctionCounter.FunctionCounterByName.class, "Count function call by name (require Function map file '-m' option)", 1),
		new OptionTraceProcessor("-C", org.rx.rtrace.processors.FunctionCounter.class, "Count function call by id", 1),
		new OptionTraceProcessor("--text", org.rx.rtrace.processors.NodePrettyPrinter.class, "BGH Pretty printer" ),
		new OptionTraceProcessor("--swing", org.rx.rtrace.processors.ASTFrame.class, "Show tree (swing)", 0)
		*/
	};
	
	public static void main(String[] args) {
		String fname = null;
		String[] todo = Option.process_command_line(args, avaible_options);
		if(todo.length == 0)
			fname = DEFAULT_FILE_NAME;
		else{
			fname = todo[0];
			if(todo.length > 1)
				System.err.println("More than one filenames given, using first:'"+fname+"'");
		}
		
		if(option_processors.isEmpty()){
			System.err.println("Error: At least one processor must be specified");
			Option.Help.display_help(System.err, avaible_options, -2);
		}

		if(process_files(fname, option_processors))
			System.exit(-1);
	}
	
	private static DataInputStream open_trace(String file) throws IOException, SQLException {
		create_schema();
		vignette_name = vignette_name(file);
		trace_id = register_trace(database, vignette_name);
		TraceSummary.register_trace_summary(database, file, trace_id);
		String ftype = FileTools.guess_file_type(file, file_type);
		FunctionMap.bind_map_file(file, (map_file_type == null) ? (FileTools.guess_file_type(FunctionMap.DEFAULT_MAP_FILE_NAME, ftype)) : map_file_type);
		return new DataInputStream(FileTools.open_file(file, ftype));
	}
	
	static public void create_schema() throws IOException, SQLException {
		database = DataBase.create_database();
		DataBase.create_table(database, "traces", "id integer primary key autoincrement, vignette_id reference vignettes, status integer, ts text");
		DataBase.create_table(database, "errors", "trace_id reference traces, message text, ts text");
	}

	public static boolean process_files(String file, ArrayList<TraceProcessor> processors){
		long time_start = System.currentTimeMillis();
		try {
			//// Initialize all processors
			DataInputStream stream = open_trace(file);
			
			ArrayList<TraceProcessor> procs = new ArrayList<TraceProcessor>();
			for(TraceProcessor processor: processors)
				try{
					if(processor.initialize_trace(trace_id))
						procs.add(processor);
				} catch (Exception e) {
					System.err.println("Unable to initialize processor '"+processor+"': "+e.getMessage());
					e.printStackTrace(System.err);
				}
			TraceProcessor[] good_processors = new TraceProcessor[procs.size()];
			procs.toArray(good_processors);
			long time_before = System.currentTimeMillis();
			if(good_processors.length > 0) {
				Node.process_stream(good_processors, stream);
			}
			long time_after = System.currentTimeMillis();
			update_trace_status(0);
			database.commit();
			
			//// Display results
			for(TraceProcessor processor: processors){
				if(Option.Verbose.verbose)
					System.out.println("Finalizing "+ processor);
				processor.finalize_trace(trace_id);
				database.commit();
			}
			long time_stop = System.currentTimeMillis();
			if(Option.Verbose.verbose){
				System.out.println("init: "+(time_before - time_start));
				System.out.println("process: "+(time_after - time_before));
				System.out.println("finalize: "+(time_stop - time_after));
			}
		} catch (Error e) {
			process_exception(e, 3);
		} catch (RuntimeException e) {
			process_exception(e, 2);
		} catch (Exception e) {
			process_exception(e, 1);
		}
		return Node.found_err;
	}
	
	static String vignette_name(String file){
		String name = vignette_name;
		if(name == null){
			String parts[] = file.split(File.separator);
			if(parts.length < 2){
				name = "NONAME"; // TODO generate random name or try to look into summary
				System.err.println("Not able to generate a vignette name for '"+file+"', please use --name option");
			}else
				name = parts[parts.length - 2];
		}
		return name;
	}
	
	static public void process_exception(Throwable e, int err_value){
		Node.found_err = true;
		if(trace_id != -1){
			try {
				emit_error_msg(e.getMessage()+" "+Arrays.toString(e.getStackTrace()));
				update_trace_status(err_value);
			} catch (SQLException e1) {
			} finally {
				try {
					if(database != null){
						database.commit();
						database.close();
					}
				} catch (SQLException e1) {}
			}
		}else{
			System.err.print("(No trace id) ");
			e.printStackTrace();
		}
	}
	
	static int register_trace(Connection database, String vignette_name) throws SQLException{		
		int vignette_id = DataBase.register_vignette(database, vignette_name);
		Statement stmt = database.createStatement();
		ResultSet set;

		stmt.executeUpdate("insert or replace into traces (id, vignette_id, status, ts) VALUES ('"+vignette_id+"', '"+vignette_id+"', -1, datetime('now'))");
		set = stmt.getGeneratedKeys();

		int t_id = set.getInt(1);
		set.close();
		return t_id;
	}
	
	static void update_trace_status(int new_status) throws SQLException {
		Statement stmt = database.createStatement();
		stmt.executeUpdate("update traces set status='"+new_status+"' where id="+trace_id+";");
		database.commit();
		stmt.close();
	}

	static public void emit_error_msg(String msg){
		if(trace_id < 0){
			System.err.println("(No trace id) "+msg);
			return;
		}
		try {
			System.err.println(msg);
			PreparedStatement stmt = database.prepareStatement("insert into errors (trace_id, message, ts) values "+
					"("+trace_id+", ?, datetime('now')) ");
			stmt.setString(1, msg);
			stmt.executeUpdate();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	protected static void usage(PrintStream out, int exit_code) {
		out.println("Usage: [flags | file]+");
		out.println("file: file_name | - (stdin)");
		out.println("");
		out.println("Avaible processors (order & duplicate matter):");
		Help.display_help(out, avaible_options, exit_code);
		if(supported_version != null){
			out.println("");
			out.println("Reader compatible with versions: "+supported_version);
		}
	}
	
	static class OptionTraceProcessor extends Option{
		private Class<? extends TraceProcessor> clazz;
		OptionTraceProcessor(String option_name, Class<? extends TraceProcessor> clazz, String helptext){
			this(option_name, clazz, helptext, 0);
		}
		OptionTraceProcessor(String option_name, Class<? extends TraceProcessor> clazz, String helptext, int nb_params){
			super(option_name, helptext, nb_params);
			this.clazz = clazz;
		}
		protected void process_option(String name, String opts[]) {
			try {
				if(clazz != null){
					Constructor<? extends TraceProcessor> cons =
								clazz.getConstructor(Connection.class);
					TraceProcessor processor = cons.newInstance(database);
					processor.initialize_processor(opts);
					option_processors.add(processor);
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace(); // Initialization error
			}
		}
	}
}

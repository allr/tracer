package org.rx.rtrace;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.antlr.runtime.tree.TreeNodeStream;
import org.rx.DataBase;
import org.rx.Option;
import org.rx.Option.Help;
import org.rx.analyser.ASTProcessor;
import org.rx.analyser.BasicASTProcessor;
import org.rx.analyser.DBMaker;
import org.rx.rtrace.processors.TraceProcessor;
import org.rx.rtrace.task.NoSuchTaskException;
import org.rx.rtrace.task.PlainTextTaskIO;
import org.rx.rtrace.task.Task;
import org.rx.rtrace.task.TaskExporter;
import org.rx.rtrace.task.TaskImporter;
import org.rx.rtrace.task.TaskInfo;
import org.rx.rtrace.task.TaskInfo.TraceKey;
import org.rx.rtrace.task.TaskManager;
import org.rx.rtrace.task.TaskManager.TraceImportMode;
import org.rx.rtrace.task.TaskManagerFactory;
import org.rx.rtrace.util.ArrayUtils;
import org.rx.rtrace.util.CharacterEscaper;
import org.rx.rtrace.util.CommandLineUtils;
import org.rx.rtrace.util.StringUtils;
import org.rx.rtrace.util.WhitespaceEscaper;
import org.sglj.util.Pair;


public class Main {
	//	private static final String DEFAULT_FILE_NAME = "-";
	//	private static final int COMMAND_LINE_WIDTH = 80;

	static String supported_version = null;
	static String profileName;

	// Some options used more than once ... maybe do class for them
	static File outputDir;
	static String traceName;
	static boolean force;
	static boolean escape;

	/* TODO list: Add System properties to change data length */
	private static ArrayList<TraceProcessor> traceProcessors = new ArrayList<TraceProcessor>();
	private static ArrayList<ASTProcessor> astProcessors = new ArrayList<ASTProcessor>();

	private static Command activeCommand; // TODO Remove this active command 

	protected static void usage(PrintStream out, int exit_code) {
		out.println("Usage: [flags | file]+");
		out.println("file: file_name | - (stdin)");
		out.println("");
		out.println("Avaible processors (order & duplicate matter):");
		Help.display_help(out, AVAILABLE_OPTIONS, exit_code);
		if(supported_version != null) {
			out.println("");
			out.println("Reader compatible with versions: " + supported_version);
		}
	}

	static Profile.Listener profileListener = new Profile.Listener() {
		@Override
		public void propertyMissing(String profileName, String key) {
			printErrorAndExit("Missing required property: " + key, 	ExitCodes.INVALID_PROFILE);
		}
		@Override
		public void propertyInvalid(String profileName, String key, String value) {
			printErrorAndExit("Invalid property: "+key+"("+value+")", ExitCodes.INVALID_PROFILE);
		}
		@Override
		public void ioException(String profileName, IOException e) {
			printErrorAndExit("Error loading profile: "+profileName+"\n"+e.getMessage(), ExitCodes.INVALID_PROFILE);
		}
		@Override
		public void fileMissing(String profileName) {
			printErrorAndExit("Missing profile: "+profileName, ExitCodes.INVALID_PROFILE);
		}
	};
	
	public static void main(String[] args) {
		String[] cmdArgs = Option.process_command_line(args, AVAILABLE_OPTIONS);

		// load profile
		if (profileName != null)
			Profile.loadProfile(profileName, profileListener);
		else
			Profile.loadDefaultProfile(profileListener);

		// process command
		if (activeCommand == null)
			usage(System.err, ExitCodes.SUCCESS);
		try {
			activeCommand.process(cmdArgs);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(e.getClass().getCanonicalName().hashCode() << 1);
		}
	}

	private static void setActiveCommand(Command cmd) {
		if (cmd == null)
			usage(System.err, ExitCodes.MULTIPLE_COMMANDS);
		activeCommand = cmd;
	}
	
	private static class OptionTraceProcessor extends Option {
		private Class<? extends TraceProcessor> clazz;

		OptionTraceProcessor(String option_name,
				Class<? extends TraceProcessor> clazz, String helptext) {
			this(option_name, clazz, helptext, 0);
		}

		OptionTraceProcessor(String option_name,
				Class<? extends TraceProcessor> clazz, String helptext,
				int nb_params) {
			super(option_name, helptext, nb_params);
			this.clazz = clazz;
		}
		protected void process_option(String name, String opts[]) {
			try {
				if(clazz != null) {
					Constructor<? extends TraceProcessor> cons = clazz.getConstructor(Connection.class);
					TraceProcessor processor = cons.newInstance(DatabaseFactory.getInstance());
					processor.initialize_processor(opts);
					traceProcessors.add(processor);
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

	static class OptionAnalyser extends Option {
		Class<? extends ASTProcessor> clazz;
		OptionAnalyser(String option_name, Class<? extends ASTProcessor> clazz){
			this(option_name, clazz, clazz.toString(), 0);
		}
		OptionAnalyser(String option_name, Class<? extends ASTProcessor> clazz, String helptext){
			this(option_name, clazz, helptext, 0);
		}
		OptionAnalyser(String option_name, Class<? extends ASTProcessor> clazz, String helptext, int nb_params){
			super(option_name, helptext, nb_params);
			this.clazz = clazz;
		}
		protected void process_option(String name, String opts[]) {
			try {
				if (clazz == null) return;
				Constructor<? extends ASTProcessor> ctor = clazz.getDeclaredConstructor(TreeNodeStream.class);
				ASTProcessor processor = ctor.newInstance((Object)null);
				processor.initialize_processor(opts);
				astProcessors.add(processor);

				if (processor instanceof DBMaker) {
					// dirty hack, inject database into DB Maker. :-(
					Field db = clazz.getSuperclass().getDeclaredField("database");
					boolean accessible = db.isAccessible();
					db.setAccessible(true);
					db.set(processor, DatabaseFactory.getInstance());
					db.setAccessible(accessible);
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

	static private Option optionOutputDir = new Option("--output-dir", "Sets output directory", 1){
		protected void process_option(String name, String[] opts) {
			outputDir = new File(opts[0]);
		}
	};
	static private Option optionEscape = new Option("--escape", "Escapes whitespace when needed"){	
		protected void process_option(String name, String[] opts) {
			escape = true;
		}
	};
	static private Option optionForce = new Option("--force", "Allow overwriting"){
		protected void process_option(String name, String[] opts) {
			force = true;
		}
	};

	static private Option optionTraceName = new Option("--name", "Sets the name of the trace", 1){
		protected void process_option(String name, String[] opts) {
			traceName = opts[0];
		}
	};

	private static abstract class Command extends Option {
		int commandOptions;
		
		Option[] getOptions(){ return null; }
		
		public Command(String name, String helptext, int nb_params) {
			super(name, helptext, 0);
			commandOptions = nb_params;
		}

		public Command(String name, String helptext) {
			super(name, helptext);
		}
		
		String getDescription(){ return ""; }
		abstract void execute(String[] args) throws Exception;

		public void process(String args[]) throws Exception {
			String todo[] = args;
			Option[] subOptions = getOptions();
			if(subOptions != null)
				todo = Option.process_command_line(args, subOptions);
			if(todo.length < commandOptions)
				reportBadArgumentCount();
			execute(todo);
		}

		@Override
		protected void process_option(String name, String[] opts) {
			setActiveCommand(this);
		}

		void displayShortHelp(PrintStream out, int exitCode) {
			out.print("Command: "+getOption());
			String dscr = getDescription();
			if(dscr != null){
				out.print(" "+dscr);
			}
			out.print("\n");
			out.println(getHelp());
		}
		
		void usage(PrintStream out, int exitCode){
			displayShortHelp(out, getParams());
			Option[] subOptions = getOptions();
			if(subOptions != null)
				Help.display_help(out, subOptions, exitCode);
			System.exit(exitCode);
		}

		private void reportBadArgumentCount() {
			usage(System.err, ExitCodes.BAD_ARGUMENTS);
		}
	};

	static Command commandRunTask = new Command("run-task", "runs existing task(s)", 1){
		boolean continueTracing = false;
		String getDescription(){ return "<task_name>+"; }		
		Option[] getOptions(){
			return new Option[]{
					new Option.Text("Trace Processors"),
					new OptionTraceProcessor("--trace-allcalls", org.rx.rtrace.processors.AllCalls.class, "Count all calls", 0),
					new OptionTraceProcessor("--trace-foreign",	org.rx.rtrace.processors.ForeignFunctions.class, "Count function call after some functions", 0),
					new OptionTraceProcessor("--trace-promise", org.rx.rtrace.processors.Promises.class, "Count all promises stuffs", 0),
					new OptionTraceProcessor("--trace-hidden", org.rx.rtrace.processors.FunctionHidding.class,	"Function hidded by vars", 0),
					new OptionTraceProcessor("--trace-counters", org.rx.rtrace.processors.SimpleCounters.class, "Counts simple things/stats"),
					new OptionTraceProcessor("--trace-size", org.rx.rtrace.processors.CountSize.class, "Counts size of functions"),
					new OptionTraceProcessor("--trace-recursive", org.rx.rtrace.processors.CountRecursive.class, "Counts recursive calls"),
					new Option.Text("Dev/test"),
					new OptionTraceProcessor("--trace-promise-upward", org.rx.rtrace.processors.PromisesUpward.class, "Count all promises stuffs", 0),
					new OptionTraceProcessor("--trace-0", org.rx.rtrace.processors.Nothing.class,	"Do nothing ..." ),
					new Option.Text(""),
					new Option.Text("AST Processors"),
					new OptionAnalyser("--analyze-keywords", org.rx.analyser.parser.CallByKeyword.class, "Count calls by keyword"),
					new OptionAnalyser("--analyze-recursive", org.rx.analyser.parser.CountRecursive.class, "Count recursive calls"),
					new OptionAnalyser("--analyze-tokens", org.rx.analyser.parser.TokenCounter.class, "Count token usage"),
					new OptionAnalyser("--analyze-resolv", org.rx.analyser.parser.ResolvName.class, "Tool to resolv names"),
					new OptionAnalyser("--analyze-calls", org.rx.analyser.parser.CountCall.class, "Count specific calls <map_file>", 1),
					new OptionAnalyser("--analyze-assign", org.rx.analyser.parser.CountAssign.class, "Count Assignments <map files>", 1),
					new OptionAnalyser("--analyze-names", org.rx.analyser.parser.DBMaker.class, "Resolve names to db", 0),
					new OptionAnalyser("--analyze-class", org.rx.analyser.parser.ClassHierarchy.class, "Dump class hierarchy info", 0),
					new OptionAnalyser("--analyze-promside", org.rx.analyser.parser.SideEffectInProm.class, "Counts Side effects in prom", 0),
					new OptionAnalyser("--analyze-0", org.rx.analyser.parser.NopTree.class, "Do nothing ..."),
					new Option.Text(""),
					optionForce,
					new Option("--continue", "Continue uncompleted traces"){
						protected void process_option(String name, String[] opts) {	continueTracing = true; }
					}
			};
		}
		
		void execute(String[] args) throws Exception {
			// initialize tracer and listeners
			Tracer tracer = new DbTracer(new SimplePathMangler(), traceProcessors, astProcessors);
			tracer.addTraceListener(new TracerAdapter() {
				@Override
				public void exceptionThrown(Tracer t, TracerException e) {
					// TODO handle exceptions in a more clever way...
					if (Verbose.verbose)
						System.err.println("Tracer exception: " + e);

					if (e.getThrowable() instanceof IllegalStateException) {
						e.printStackTrace();
						System.err.println(e.getMessage());
						Option.Help.display_help(System.err, AVAILABLE_OPTIONS,	-2);
					} else {
						System.err.println("FATAL ERROR: " + e.getMessage());
						e.printStackTrace();
					}
				}

				@Override
				public void traceFailed(Tracer tracer, Trace trace, Throwable t) {
					String errMsg = null;
					switch (trace.getStatus()) {
					case R_STARTED:
						errMsg = "R profiling of " + trace.getName() + " failed";
						break;
					}
					if (errMsg != null)  // TODO
						printErrorOrWarning(!force, errMsg);
					if (Verbose.verbose && t != null)
						System.err.println("Failure cause: " + t);

					if (!force)
						System.exit(ExitCodes.TASK_OPERATION_FAILED | ExitCodes.TRACE_OPERATION_FAILED);
				}
			});

			// add tasks to tracer
			for (String taskName : args) {
				try {
					Task task = TaskManagerFactory.getInstance().startTask(taskName, continueTracing, force);
					if (task == null)
						handleTraceOverwriteFailure(taskName, true);

					tracer.addTask(task);
					System.out.println("Task to run: " + task);
					for (Trace trace : task.getTraces())
						System.out.println(trace);

				} catch (NoSuchTaskException e) {
					handleNoSuchTaskException(e, false);
				}
			}

			// start tracing
			tracer.start();
		}
	};

	static Command commandShowTask = new Command("show-task", "displays info about existing task(s)"){
		boolean sort;
		String getDescription(){ return "[<task_name>...]"; }
		Option[] getOptions(){
			return new Option[]{
					optionEscape,
					new Option("--sort", "Sorts task info"){
						protected void process_option(String name, String[] opts) {
							sort = true;
						}
					}
			};
		}
		
		void execute(String[] args) throws Exception {
			Collection<TaskInfo> tasksInfo = null;
			if (args.length == 0)
				tasksInfo = TaskManagerFactory.getInstance().getTasksInfo();
			else {
				tasksInfo = new ArrayList<TaskInfo>(args.length);
				for (String taskName : args)
					tasksInfo.add(TaskManagerFactory.getInstance().getTaskInfo(taskName));
			}
			if (sort)
				tasksInfo = sortTaskInfos(tasksInfo);

			PrintFormat<TraceKey> traceKeyFormat = PrintFactory.getDefaultTraceKeyFormater();

			for (TaskInfo taskInfo : tasksInfo) {
				Collection<TraceKey> traceKeys = taskInfo.getTraceKeys();
				if (sort)
					traceKeys = sortTraceKeys(traceKeys);
				System.out.println("Task: " + taskInfo.getName());
				for (TraceKey traceKey : traceKeys)
					System.out.println("\t" + traceKeyFormat.format(traceKey));
			}
		}

		private final List<TaskInfo> sortTaskInfos(Collection<? extends TaskInfo> taskInfos) {
			List<TaskInfo> taskInfosList = new ArrayList<TaskInfo>(taskInfos);
			Collections.sort(taskInfosList, new Comparator<TaskInfo>() {
				@Override
				public int compare(TaskInfo o1, TaskInfo o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			return taskInfosList;
		}

		private final List<TraceKey> sortTraceKeys(Collection<? extends TraceKey> traceKeys) {
			List<TraceKey> traceKeysList = new ArrayList<TraceKey>(traceKeys);
			Collections.sort(traceKeysList, new Comparator<TraceKey>() {
				@Override
				public int compare(TraceKey o1, TraceKey o2) {
					int cmpSrcFile = o1.getSrcFile()
					.compareTo(o2.getSrcFile());
					if (cmpSrcFile != 0)
						return cmpSrcFile;
					String runCmdArgsStr1 = CommandLineUtils.joinArguments(o1.getRunCmdArgs());
					String runCmdArgsStr2 = CommandLineUtils.joinArguments(o2.getRunCmdArgs());
					return runCmdArgsStr1.compareTo(runCmdArgsStr2);
				}
			});
			return traceKeysList;
		}
	};

	static Command commandStatusTask = new Command("status-task", "displays status about task(s)"){
		boolean traceAll;
		boolean namesOnly;	
		String getDescription(){ return "[task_name]*"; }
		Option[] getOptions(){
			return new Option[]{
					new Option("--all", "Shows all traces"){
						protected void process_option(String name, String[] opts){ traceAll = true; }},
					new Option("--names-only", "Only print trace names"){
						protected void process_option(String name, String[] opts){ namesOnly = true; }},
					optionEscape
			};
		}
		
		void execute(String[] args) throws Exception {
			PrintFormat<Trace> traceFormatter = namesOnly 
					? PrintFactory.getNameOnlyTraceFormater()
					: PrintFactory.getOneLineTraceFormater();

			TaskManager taskManager = TaskManagerFactory.getInstance();
			if (args.length == 0) {
				Map<String, Collection<Trace>> traceMap = (traceAll	? taskManager.getTraces() : taskManager.getIncompleteTraces());
				for (Entry<String, Collection<Trace>> e : traceMap.entrySet())
					showStatus(e.getKey(), e.getValue(), traceFormatter);
			} else {
				for (String taskName : args) {
					Collection<Trace> traces = (traceAll ? taskManager.getTraces(taskName) : taskManager.getIncompleteTraces(taskName));
					showStatus(taskName, traces, traceFormatter);
				}
			}
		}

		private void showStatus(String taskName,	Collection<Trace> traces, PrintFormat<Trace> traceFormatter) {
			System.out.println("Task: " + taskName);
			for (Trace trace : traces)
				System.out.println(traceFormatter.format(trace));
		}
	};

	static Command commandCreateTask = new Command("create-task", "creates a new tracer task", 1){
		String getDescription(){ return "<task_name>"; }
		
		void execute(String[] args) throws Exception {
			String name = args[0];

			try {
				if (TaskManagerFactory.getInstance().createTask(name, force) == null)
					handleTaskOverwriteFailure(name, !force);
			} catch (SQLException e) {
				System.out.println("Unable to create task '"+name+"': task already exists or name is invalid");
			}
		}
	};

	static Command commandDeleteTask = new Command("delete-task", "deletes an existing task(s)", 1){
		String getDescription(){ return "<task_name>+"; }
		
		void execute(String[] args) throws Exception {
			if (!TaskManagerFactory.getInstance().deleteTasks(Arrays.asList(args)))
				printErrorAndExit("Some tasks were not deleted", ExitCodes.TASK_OPERATION_FAILED | ExitCodes.BAD_ARGUMENTS);
		}
	};

	static Command commandCopyTask = new Command("copy-task", "copies an existing task(s)", 2){
		String getDescription(){ return "<src_task_name> <dst_task_name>"; }
		Option[] getOptions(){
			return new Option[]{ optionForce };
		}
		
		void execute(String[] args) throws Exception {
			String srcName = args[0], dstName = args[1];
			try {
				if (TaskManagerFactory.getInstance().copyTask(srcName, dstName, force) == null)
					handleTaskOverwriteFailure(dstName, !force);
			} catch (NoSuchTaskException e) {
				handleNoSuchTaskException(e, true);
			}
		}
	};

	static Command commandMergeTask = new Command("merge-task", "merge an existing task", 2){
		String getDescription(){ return "<dst_task_name> <src_task_name>"; }
		Option[] getOptions(){
			return new Option[]{ optionForce };
		}
		
		void execute(String[] args) throws Exception {
			String dstName = args[0];
			checkTaskOverwrite(dstName, !force);

			Collection<String> tasksToMergeNames = Arrays.asList(args).subList(1, args.length);
			try {
				if (TaskManagerFactory.getInstance().mergeTasks(dstName, tasksToMergeNames, force) == null)
					printErrorAndExit("Some tasks could not be merged, merge aborted.",	ExitCodes.TASK_OPERATION_FAILED | ExitCodes.BAD_ARGUMENTS);
			} catch (NoSuchTaskException e) {
				handleNoSuchTaskException(e, true);
			}
		}
	};

	static Command commandRenameTask = new Command("rename-task", "renames an existing task", 2){
		String getDescription(){ return "<src_task_name> <dst_task_name>"; }
		Option[] getOptions(){
			return new Option[]{ optionForce };
		}
		
		void execute(String[] args) throws Exception {
			String srcName = args[0], dstName = args[1];

			try {
				if (!TaskManagerFactory.getInstance().renameTask(srcName, dstName, force))
					handleTaskOverwriteFailure(dstName, true);
			} catch (NoSuchTaskException e) {
				handleNoSuchTaskException(e, true);
			}
		}
	};

	static Command commandExportTask = new Command("export-task", "exports existing task(s)", 1){
		String getDescription(){ return "<output_file> <task_name>+"; }
		Option[] getOptions(){
			return new Option[]{ optionForce };
		}
		
		void execute(String[] args) throws Exception {
			File outputFile = new File(args[0]);

			TaskExporter taskExporter = PlainTextTaskIO.getInstance();
			Collection<TaskInfo> taskInfosToExport = new ArrayList<TaskInfo>();
			if (args.length == 1) {
				for (TaskInfo taskInfo : TaskManagerFactory.getInstance().getTasksInfo())
					taskInfosToExport.add(taskInfo);
			} else {
				for (String taskName : args) {
					TaskInfo taskInfo = TaskManagerFactory.getInstance().getTaskInfo(taskName);
					if (taskInfo == null)
						handleTaskDoesNotExist(taskName, !force);
					taskInfosToExport.add(taskInfo);
				}
			}

			try {
				taskExporter.exportTasks(taskInfosToExport, outputFile);
			} catch (IOException e) {
				if (!force) {
					System.err.println("Fatal error: export failed");
					System.exit(ExitCodes.TASK_OPERATION_FAILED | ExitCodes.FATAL_ERROR);
				} else {
					printWarning("export failed");
				}
			}
		}
	};

	static Command commandImportTask = new Command("import-task", "imports existing task(s)", 1){
		String getDescription(){ return "<input_file> [task_name]+"; }
		Option[] getOptions(){
			return new Option[]{ optionForce };
		}
		
		void execute(String[] args) throws Exception {
			File inputFile = new File(args[0]);

			TaskImporter taskImporter = PlainTextTaskIO.getInstance();
			Set<String> whiteList = new HashSet<String>(ArrayUtils.asCollection(args, 1));

			boolean importAll = (args.length == 1);
			Collection<TaskInfo> taskInfosToImport = null;
			try {
				taskInfosToImport = taskImporter.importTasks(inputFile);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Fatal error: import failed");
				System.exit(ExitCodes.TASK_OPERATION_FAILED | ExitCodes.FATAL_ERROR);
			}

			for (TaskInfo taskInfo : taskInfosToImport) {
				if (!importAll && !whiteList.contains(taskInfo.getName()))
					continue;
				if (TaskManagerFactory.getInstance().existsTask(taskInfo.getName())) {
					if (!force) {
						handleTaskOverwriteFailure(taskInfo.getName(), false);
						continue;
					}
				} else
					TaskManagerFactory.getInstance().createTask(taskInfo.getName(), false);
				TaskManagerFactory.getInstance().saveTaskInfo(taskInfo);
			}
		}
	};

	static Command commandAddTrace = new Command("add-trace", "adds a trace to the specified task", 2){
		String getDescription(){ return "<task_name> <src_file_path> [cmd_arg]*"; }
		Option[] getOptions(){
			return new Option[]{ optionTraceName, optionOutputDir, optionForce };
		}
		
		void execute(String[] args) throws Exception {
			String taskName = args[0];
			File srcFile = new File(args[1]);
			String[] runCmdArgs = Arrays.copyOfRange(args, 2, args.length);

			TraceInfo traceInfo = new TraceInfo(srcFile, runCmdArgs);
			if (traceName != null)
				traceInfo.setName(traceName);
			if (outputDir != null)
				traceInfo.setOutputDir(outputDir);

			try {
				if (!TaskManagerFactory.getInstance().addTraceInfo(taskName, traceInfo, force))
					printWarning("Trace info already exists");
			} catch (NoSuchTaskException e) {
				handleNoSuchTaskException(e, true);
			}
		}
	};

	static Command commandRemoveTrace = new Command("remove-trace", "removes a trace from the specified task", 2){
		String getDescription(){ return "<task_name> <src_file_path> [cmd_arg]*"; } 
		
		void execute(String[] args) throws Exception {
			String taskName = args[0];
			File srcFile = new File(args[1]);
			String[] runCmdArgs = Arrays.copyOfRange(args, 2, args.length);

			try {
				if (!TaskManagerFactory.getInstance().removeTraceInfo(taskName,	srcFile, runCmdArgs)) {
					System.err.println("Error: Trace does not exist");
					System.exit(ExitCodes.TASK_OPERATION_FAILED);
				}
			} catch (NoSuchTaskException e) {
				handleNoSuchTaskException(e, true);
			}
		}
	};

	static Command commandDeleteTrace = new Command("delete-trace", "deletes a trace with the specified name or id", 1){
		String getDescription(){ return "(<trace_name> | @<trace_id>)+"; }
		Option[] getOptions(){
			return new Option[]{ optionForce };
		}
		
		void execute(String[] args) throws Exception {
			Collection<String> toDelNames = new ArrayList<String>(args.length);
			Collection<Integer> toDelIds = new ArrayList<Integer>(args.length);
			try {
				for (String arg: args) {
					if (arg.startsWith("@"))
						toDelIds.add(Integer.parseInt(arg.substring(1)));
					else
						toDelNames.add(arg);
				}
				if (!TraceManagerFactory.getInstance().deleteTraces(toDelNames,	toDelIds)) {
					printError("Some traces were not deleted");
					System.exit(ExitCodes.TASK_OPERATION_FAILED | ExitCodes.BAD_ARGUMENTS);
				}
			} catch (NumberFormatException e) {
				// TODO
				usage(System.err, ExitCodes.TASK_OPERATION_FAILED |	ExitCodes.BAD_ARGUMENTS);
			} catch (NoSuchTaskException e) {
				handleNoSuchTaskException(e, true);
			}
		}
	};

	static Command commandTraceImport = new Command("import-traces", "imports a trace from a directory", 1){
		String importName;
		boolean ignore = false;
		boolean append = false;
		boolean clean = false;
		String getDescription(){ return "<trace_dir>+"; }
		Option[] getOptions(){
			return new Option[]{
				new Option("--task", "Sets the task to import traces to", 1){
					protected void process_option(String name, String[] opts) {
						importName = name;
					}
				},	
				new Option("--ignore", "Skips imports of non-associated traces"){
					protected void process_option(String name, String[] opts) {
						ignore = true;
					}
				},
				new Option("--append", "Appends trace info to task"){
					protected void process_option(String name, String[] opts) {
						append = true;
					}
				},
				new Option("--clean", "Appends trace info to task"){
					protected void process_option(String name, String[] opts) {
						clean = true;
					}
				}
			};
		}
		
		void execute(String[] args) throws Exception {
			// Only one option at the same time is legal
			if((ignore ? 1:0)+(append ? 1:0)+(clean ? 1:0) != 1) {
				printError("Missing --ignore, --append or --clean option");
				System.exit(ExitCodes.BAD_ARGUMENTS);
			}

			TraceImportMode mode = TraceImportMode.IGNORE;
			if (append)
				mode = TraceImportMode.APPEND;
			else if (clean)
				mode = TraceImportMode.CLEAN;

			Collection<Pair<Trace, String>> importMap = new ArrayList<Pair<Trace, String>>();
			for (String traceDirStr : args) {
				File traceDir = new File(traceDirStr);
				Trace trace = TraceIOUtils.importTrace(traceDir);
				String taskName = importName;
				if (taskName == null)
					taskName = trace.getName();
				importMap.add(new Pair<Trace, String>(trace, taskName));
			}
			TaskManagerFactory.getInstance().importTraces(importMap, mode);
		}
	};

	static Command commandCreateTrace = new Command("create-trace", "creates a new trace (debug only)", 1){
		String getDescription(){ return "<src_file_path> [cmd_arg]*"; }
		Option[] getOptions(){
			return new Option[]{ optionTraceName, optionOutputDir };
		}
		
		void execute(String[] args) throws Exception {
			File srcFile = new File(args[0]);
			String[] runCmdArgs = Arrays.copyOfRange(args, 1, args.length);
			Trace newTrace = (traceName != null	? new Trace(traceName, srcFile, runCmdArgs)	: new Trace(srcFile, runCmdArgs));
			if (outputDir != null)
				newTrace.setOutputDir(outputDir);

			if (TraceManagerFactory.getInstance().registerTrace(newTrace, force) == null)
				handleTraceOverwriteFailure(newTrace.getName(), true);
		}
	};


	protected static final Option[] AVAILABLE_OPTIONS = {
		new Option.Help() {
			@Override
			protected void process_option(String name, String opts[]) {
				usage(System.out, ExitCodes.SUCCESS);
			}
		},
		new Option("--summary-file", "Summary file name, default" + TraceSummary.DEFAULT_FILE_NAME, 1) {
			protected void process_option(String name, String opts[]) {
				TraceSummary.DEFAULT_FILE_NAME = opts[0];
			}
		},
		new Option("--trace-version", "Assert for a specific trace version", 1) {	
			protected void process_option(String name, String opts[]) {
				assert opts.length > 0;
				supported_version = opts[0];
			}
		},
		new DataBase.DBNameOption(),
		new DataBase.DBOverwriteOption(),
		new Option("--profile", "Loads the specified profile", 1){
			protected void process_option(String name, String[] opts) {
				profileName = name;
			}
		},
		new Option.Text(""),
		new Option.Text("\t(Basic Task Management)"),
		commandCreateTask,
		commandRunTask,
		commandShowTask,
		commandStatusTask,
		commandDeleteTask,
		new Option.Text("\t(Trace Management)"),
		commandAddTrace,
		commandRemoveTrace,
		commandDeleteTrace,
		commandTraceImport,
		commandCreateTrace,  // TODO (for testing purposes only)
		new Option.Text("\t(Advanced Task Management)"),
		commandCopyTask,
		commandMergeTask,
		commandRenameTask,
		commandExportTask,
		commandImportTask
	};
	
	private static boolean checkTaskOverwrite(String taskName, boolean strict) throws Exception {
		if (!force && TaskManagerFactory.getInstance()
				.existsTask(taskName)) {
			printErrorOrWarning(strict,	"Cannot overwrite existing task: "+taskName+" (use "+optionForce+" to force overwrite)");
			if (strict)
				System.exit(ExitCodes.TASK_OPERATION_FAILED | ExitCodes.CANNOT_OVERWRITE);
			return false;
		}
		return true;
	}

	private static void handleTaskOverwriteFailure(String taskName,	boolean strict) {
		printErrorOrWarning(strict,	"Cannot overwrite existing task: "+taskName+" (use "+optionForce+" to force overwrite)");
		if(strict)
			System.exit(ExitCodes.TASK_OPERATION_FAILED | ExitCodes.CANNOT_OVERWRITE);
	}

	private static void handleTaskDoesNotExist(String taskName,	boolean strict) {
		System.err.println((strict ? "Error" : "Warning") + ": Task does not exist: " + taskName);
		if(strict)
			System.exit(ExitCodes.TASK_OPERATION_FAILED | ExitCodes.DOES_NOT_EXIST);
	}

	private static void handleNoSuchTaskException(NoSuchTaskException e, boolean strict) {
		handleTaskDoesNotExist(e.getTaskName(), strict);
	}

	private static void handleTraceOverwriteFailure(String trace, boolean strict) {
		printErrorOrWarning(strict, "Cannot overwrite existing trace: "+trace+" (use "+optionForce+" to force overwrite)");
		if(strict)
			System.exit(ExitCodes.TRACE_OPERATION_FAILED | ExitCodes.CANNOT_OVERWRITE);
	}

	private static void printErrorOrWarning(boolean strict, String msg) {
		if(strict)
			printError(msg);
		else
			printWarning(msg);
	}

	private static void printWarning(String msg) {
		System.err.println("Warning: " + msg);
	}

	private static void printError(String msg) {
		System.err.println("Error: " + msg);
	}

	private static void printErrorAndExit(String msg, int exitCode) {
		printError(msg);
		System.exit(exitCode);
	}

	private static boolean checkTraceOverwrite(String traceName, boolean strict) throws Exception {
		if (!force && TraceManagerFactory.getInstance().existsTrace(traceName)) {
			printErrorOrWarning(strict,	"Cannot overwrite existing trace: "+traceName+" (use "+optionForce+" to force overwrite)");
			if (strict)
				System.exit(ExitCodes.TRACE_OPERATION_FAILED | ExitCodes.CANNOT_OVERWRITE);
			return false;
		}
		return true;
	}
	
//
//  Print Formater
//
	interface PrintFormat<T> {
		public String format(T o);
	};
	
	private static abstract class AbstractTaskPrintFormat implements PrintFormat<Task> {
		PrintFormat<Trace> tracePrintFormat;
		String traceInfoSeparator;
		String taskTracesSeparator;

		public AbstractTaskPrintFormat(PrintFormat<Trace> tracePrintFormat,	String traceInfoSeparator, String taskTracesSeperator) {
			this.tracePrintFormat = tracePrintFormat;
			this.traceInfoSeparator = traceInfoSeparator;
			this.taskTracesSeparator = taskTracesSeperator;
		}

		@Override
		public String format(Task task) {
			StringBuilder sb = new StringBuilder(formatTaskInfo(task));
			sb.append(taskTracesSeparator);
			boolean first = true;
			for (Trace trace : task.getTraces()) {
				if (!first)
					sb.append(traceInfoSeparator);
				sb.append(tracePrintFormat.format(trace));
			}
			return sb.toString();
		}

		protected abstract String formatTaskInfo(Task task);
	}

	static class PrintFactory {
		static PrintFormat<TraceKey> getDefaultTraceKeyFormater(){ 
			return escape ? getEscapeTraceKeyFormater() : getSimpleTraceKeyFormater();
		}
		static PrintFormat<Task> getSimpleTaskFormater(PrintFormat<Trace> tracePrintFormat){
			return new AbstractTaskPrintFormat(tracePrintFormat, "\n", "\n"){
				protected String formatTaskInfo(Task task) {
					return "Task: " + task.getName();
				}
			};
		}
		static PrintFormat<TraceKey> getSimpleTraceKeyFormater(){
			return new PrintFormat<TraceKey>(){
				public String format(TraceKey tk) {
					return tk.getSrcFile() + " " + StringUtils.join(" ", tk.getRunCmdArgs());
				} 
			};
		}
		static PrintFormat<TraceKey> getEscapeTraceKeyFormater(){
			return new PrintFormat<TraceKey>(){
				public String format(TraceKey tk) {
					CharacterEscaper e = WhitespaceEscaper.getInstance();
					StringBuilder sb = new StringBuilder(e.escape(
							tk.getSrcFile().toString()));
					for (String runCmdArg : tk.getRunCmdArgs())
						sb.append(' ').append(e.escape(runCmdArg));
					return sb.toString();
				}
			};
		}
		static PrintFormat<Trace> getNameOnlyTraceFormater(){
			return new PrintFormat<Trace>() {
				public String format(Trace trace) {
					return trace.getName();
				}
			};
		}
		static PrintFormat<Trace> getOneLineTraceFormater(){
			return new PrintFormat<Trace>() {
				PrintFormat<TraceKey> traceKeyFormatter = getDefaultTraceKeyFormater();
				public String format(Trace trace) {
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String dateString = (trace.getDate() != null ? df.format(trace.getDate()) : "?");
					return trace.getId() + " " + trace.getName() + " @ " + dateString + " (" + trace.getStatus().toString() + "): " + 
					traceKeyFormatter.format(new TraceKey(trace));
				}
			};
		}
	}
//
// Error codes
//
	private static class ExitCodes {
		private static final int SUCCESS = 0;
		private static final int MULTIPLE_COMMANDS = 1 << 0;
		private static final int BAD_ARGUMENTS = 1 << 2;
		private static final int CANNOT_OVERWRITE = 1 << 3;
		private static final int DOES_NOT_EXIST = 1 << 4;
		private static final int TASK_OPERATION_FAILED = 1 << 5;
		private static final int TRACE_OPERATION_FAILED = 1 << 6;
		private static final int INVALID_PROFILE = 1 << 7;
		private static final int FATAL_ERROR = 1 << 20;
		//		private static final int NOT_IMPLEMENTED = 1 << 29; 
	};
}
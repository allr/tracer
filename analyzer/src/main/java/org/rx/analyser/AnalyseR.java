package org.rx.analyser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.TreeNodeStream;
import org.rx.DataBase;
import org.rx.Option;
import org.rx.Option.Help;
import org.rx.analyser.parser.RLexer;
import org.rx.analyser.parser.RParser;

public class AnalyseR {
	public static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("analyser.debug", "false"));
	
	static protected ArrayList<ASTProcessor> processors = new ArrayList<ASTProcessor>();
	// XXX Bad singleton
	private static Connection database;
	
	public static RLexer lexer;
	public static RParser parser;
	private static CommonTokenStream test_tokens;
	
	final static protected Option[] avaible_options = {
		new Help(){
			@Override
			protected void process_option(String name, String[] opts) {
				usage(System.out, 0);
			}
		},
		new DataBase.DBNameOption(),
		new DataBase.DBOverwriteOption(),
		new Option.Verbose(),
		new Option.Text("Visualizers"),
		new OptionAnalyser("--text", BasicASTProcessor.TextGenerator.class, "Display text tree"),
		new OptionAnalyser("--gui", BasicASTProcessor.TreeViewer.class, "Display graphic Tree"),
		new OptionAnalyser("--dot", BasicASTProcessor.DotGenerator.class, "Generate dot tree"),
		new Option.Text("Processors"),
		new OptionAnalyser("--0", org.rx.analyser.parser.NopTree.class, "Do nothing ..."),
		new OptionAnalyser("--keywords", org.rx.analyser.parser.CallByKeyword.class, "Count calls by keyword"),
		new OptionAnalyser("--recursive", org.rx.analyser.parser.CountRecursive.class, "Count recursive calls"),
		new OptionAnalyser("--tokens", org.rx.analyser.parser.TokenCounter.class, "Count token usage"),
		new OptionAnalyser("--resolv", org.rx.analyser.parser.ResolvName.class, "Tool to resolv names"),
		new OptionAnalyser("--calls", org.rx.analyser.parser.CountCall.class, "Count specific calls <map_file>", 1),
		new OptionAnalyser("--assign", org.rx.analyser.parser.CountAssign.class, "Count Assignments <map files>", 1),
		new OptionAnalyser("--names", org.rx.analyser.parser.DBMaker.class, "Resolve names to db", 0),
		new OptionAnalyser("--class", org.rx.analyser.parser.ClassHierarchy.class, "Dump class hierarchy info", 0),
		new OptionAnalyser("--promside", org.rx.analyser.parser.SideEffectInProm.class, "Counts Side effects in prom", 0),
		new OptionAnalyserByName("--processor", "Load processor from its java class name. It must be reachable from the current CLASSPATH."),
		new Option.Text("Broken"),
		new OptionAnalyser("--aliases", org.rx.analyser.parser.Aliases.class, "Count aliases"),
		// TODO remove (since CGBuilder no longer exists)		
//		new OptionAnalyser("--callgraph", org.rx.analyser.parser.CGBuilder.class, "Generate call graph"),
		new OptionAnalyser("--shadow", org.rx.analyser.parser.ShadowVariable.class, "Count shadowed access"),
		new OptionAnalyser("--scope", org.rx.analyser.parser.Scope.class, "Scope conflilct"),
		new OptionAnalyser("--unused", org.rx.analyser.parser.Unused.class, "Show unused variable")
	};
	
	public static void main(String[] args) throws SQLException, IOException{
		database = DataBase.create_database();
		
		processors.clear();
		String[] todo = Option.process_command_line(args, avaible_options);
		if(todo.length == 0)
			todo = new String[]{ "-" };
		if(processors.size() == 0){
			System.err.println("Error: At least one processor must be specified");
			usage(System.err, -1);
		}

		process_files(todo);
		database.commit();
		database.close();
	}
	
	static void process_files(String[] files) throws SQLException, IOException{
		for (String arg : files) {
			ANTLRStringStream reader;
			try {
				if(arg.equals("-"))
					reader = new ANTLRReaderStream(new InputStreamReader(System.in));
				else
					reader = new ANTLRFileStream(arg);
				process_file(reader, arg);
			} catch (Exception e) {
				System.err.println("Parse error Exception:"+e.getMessage());
			}
		}
		for(ASTProcessor processor: processors){
			// processor.print_stats(FileTools.output_file(processor.getClass().getSimpleName()+".log", outdir, prefix));
			processor.finalize_processor();
			database.commit();
		}
	}
	
	static void process_file(ANTLRStringStream reader, String fname) throws Exception {
		CommonTree tree  = parse_file(reader, fname);
		if(tree == null){
			if(Option.Verbose.verbose)
				System.out.println("Processing '"+fname+"': Empty ... aborting");
			return;
		}
		CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree); // BufferedTreeNodeStream
		nodes.setTokenStream(test_tokens);
		for(ASTProcessor processor: processors)
			try {
				nodes.reset();
				if(Option.Verbose.verbose)
					System.out.println("Processing '"+fname+"' with: "+ processor.getClass().toString());
				processor.process_tree(fname, tree, nodes, parser);
			} catch (RecognitionException e) {
				reportError(e, fname);
			} catch (Exception e) {
				System.err.println("Error in '"+fname+"' processing '"+processor.getClass().toString()+"': "+e.getMessage());
				e.printStackTrace();
			}
	}
	

	static CommonTree parse_file(ANTLRStringStream reader, String fname) throws IOException {
		CommonTree tree = null;
		if(Option.Verbose.verbose)
			System.out.print("Checking "+fname+ ": ");
		try {
			lexer = new RLexer(reader);
			// CommonTokenStream tokens = new CommonTokenStream(lexer);
			CommonTokenStream tokens = new TokenRewriteStream(lexer);
			test_tokens = tokens;
			parser = new RParser(tokens);
			RParser.script_return result = parser.script();
			if(result == null || (tree = (CommonTree)result.getTree()) == null){
				return null;
				//throw new Exception("Resulting tree is null");
			}
			if(Option.Verbose.verbose)
				System.out.println("OK ("+tree.getChildCount()+ " statements)");
			return tree;
		} catch (RecognitionException e) {
			if(Option.Verbose.verbose)
				System.out.println("Failed (Parse error)");
			reportError(e, fname);
		} catch (RuntimeException e) {
			Throwable cause = e.getCause();
			if(cause == null)
				reportError(e, fname);
			else if(cause instanceof RecognitionException)
				reportError((RecognitionException)cause, fname);
			else
				reportError((Exception)cause, fname);
		} catch (Exception e) {
			if(Option.Verbose.verbose)
				System.out.println("Failed (Parse error)");
			reportError(e, fname);
		}
		return null;
	}
	protected static void reportError(Exception e, String fname){
		System.err.println("Parse error in '"+fname+"': "+e);
		e.printStackTrace();		
	}
	protected static void reportError(RecognitionException e, String fname){
		System.err.println("Parse error in '"+fname+":"+e.line+"': "+e);
	}
	protected static void usage(PrintStream out, int exit_value) {
		out.println("Usage: [flags | file]+");
		out.println("file: file_name | - (stdin)");
		out.println("");
		Option.Help.display_help(out, avaible_options, exit_value);
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
				if(clazz != null){
					Constructor<? extends ASTProcessor> ctor = clazz.getDeclaredConstructor(TreeNodeStream.class);
					ASTProcessor processor = ctor.newInstance((Object)null);
					processor.initialize_processor(opts);
					processors.add(processor);
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
	static class OptionAnalyserByName extends Option {
		public OptionAnalyserByName(String option_name, String helptext) {
			super(option_name, helptext, 1);
		}

		protected void process_option(String name, String opts[]) {
			try {
				String n = opts[0];
				Class<? extends ASTProcessor> clazz = Class.forName(n).asSubclass(ASTProcessor.class);
				Constructor<? extends ASTProcessor> ctor = clazz.getDeclaredConstructor(TreeNodeStream.class);
				ASTProcessor processor = ctor.newInstance((Object)null);
				processor.initialize_processor(opts);
				processors.add(processor);
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
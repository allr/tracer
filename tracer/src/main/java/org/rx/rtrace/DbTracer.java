package org.rx.rtrace;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.rx.FileTools;
import org.rx.Option;
import org.rx.TimeSummary;
import org.rx.analyser.ASTProcessor;
import org.rx.analyser.parser.RLexer;
import org.rx.analyser.parser.RParser;
import org.rx.rtrace.Trace.Status;
import org.rx.rtrace.processors.TraceProcessor;

public class DbTracer extends AbstractTracer implements TraceMangler {

	private final PathMangler pathMangler;
	
	private final List<TraceProcessor> traceProcessors;
	private final List<ASTProcessor> astProcessors;
	private static CommonTokenStream test_tokens;

	private final List<Trace> traces = new ArrayList<Trace>();
	
	private static final String DEFAULT_TRACE_FILE_NAME = "trace.gz";
	
	public DbTracer(PathMangler pathMangler,
			Collection<TraceProcessor> traceProcessors, Collection<ASTProcessor> astProcessors) {
		this.pathMangler = pathMangler;
		this.traceProcessors = new ArrayList<TraceProcessor>(traceProcessors);
		this.astProcessors = new ArrayList<ASTProcessor>(astProcessors);
	}
	
	@Override
	public List<Trace> getTraces() {
		return Collections.unmodifiableList(traces);
	}

	@Override
	public void start() {
		Stack<Trace> todoStack = new Stack<Trace>();
		boolean firstProcessing = true;
		for (Trace trace : traces)
			todoStack.add(trace);
		while (!todoStack.empty()) {
			Trace trace = todoStack.pop();

			boolean ok = false;
			try {
				switch (trace.getStatus()) {
				case UNREGISTERED:
					throw new IllegalStateException("Trace " + trace +
							" not registered");
				case REGISTERED:
					ok = generateTrace(trace);
				case R_STARTED:
					// TODO
				case R_DONE:
					System.err.println("PROCESSING... " + trace);
					// TODO
				case PROCESSING:
					if (firstProcessing) {
					}
					// TODO
					processTrace(trace); // FIXME uncommented (go)
				case STATIC:
					runStaticAnalysis(trace);
				case DONE:
					continue;
				}
			} catch (Exception e) {
				failTrace(trace, new TraceException(trace, e));
				// if exception is not related to individual trace
				// but rather the tracer itself, stop
				if (e instanceof TracerException &&
						!(e instanceof TraceException))
					return ;
			}
			if (ok)
				todoStack.push(trace);
		}
	}

	@Override
	public int getFlags() {
		return TracerFlagConstants.TIME_AND_TRACE;  // XXX // FIXME changed from TIME (go)
	}
	
	@Override
	protected void addTrace(Trace trace) {
		super.addTrace(trace);
		traces.add(trace);
	}
	
	private void runStaticAnalysis(Trace trace) throws IOException {

		String fname = trace.getSourceFile().getAbsolutePath();
		ANTLRStringStream reader =  new ANTLRFileStream(fname);
		
		CommonTree tree  = parse_file(reader, fname, trace);
		if(tree == null){
			if(Option.Verbose.verbose)
				System.out.println("Processing '"+fname+"': Empty ... aborting");
			return;
		}
		CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree); // BufferedTreeNodeStream
		nodes.setTokenStream(test_tokens);
		for(ASTProcessor processor: astProcessors) {
			try {
				nodes.reset();
				if(Option.Verbose.verbose)
					System.out.println("Processing '"+fname+"' with: "+ processor.getClass().toString());
				processor.process_tree(fname, tree, nodes, null);
			} catch (RecognitionException e) {
				failTrace(trace, e);
			} catch (Exception e) {
				System.err.println("Error in '"+fname+"' processing '"+processor.getClass().toString()+"': "+e.getMessage());
				e.printStackTrace();
			}
		}
		for(ASTProcessor processor: astProcessors){
			System.out.println(processor.getClass().getSimpleName());
			processor.finalize_processor();
		}
		changeTraceStatus(trace, Status.DONE);
	}
	

	private CommonTree parse_file(ANTLRStringStream reader, String fname, Trace trace) {
		CommonTree tree = null;
		if(Option.Verbose.verbose)
			System.out.print("Checking "+fname+ ": ");
		try {
			RLexer lexer = new RLexer(reader);
			// CommonTokenStream tokens = new CommonTokenStream(lexer);
			CommonTokenStream tokens = new TokenRewriteStream(lexer);
			test_tokens = tokens;
			RParser parser = new RParser(tokens);
			RParser.script_return result = parser.script();
			if(result == null || (tree = (CommonTree)result.getTree()) == null){
				return null;
				//throw new Exception("Resulting tree is null");
			}
			if(Option.Verbose.verbose)
				System.out.println("OK ("+tree.getChildCount()+ " statements)");
			return tree;
		} catch (Exception e) {
			failTrace(trace, e);
		}
		return null;
	}	
	
	
	private boolean generateTrace(Trace trace) {
		if ((getFlags() & TracerFlagConstants.TIME_AND_TRACE) == 0)
			fireThrowableThrown(new IllegalStateException());  // TODO
		
		changeTraceStatus(trace, Trace.Status.R_STARTED);
		try {
			if ((getFlags() & TracerFlagConstants.TRACE) != 0)
				new RTraceTask(trace).call();
			if ((getFlags() & TracerFlagConstants.TIME) != 0)
				new RTimedTask(trace).call();
		} catch (Exception e) {
			failTrace(trace, e);
			return false;
		}
		changeTraceStatus(trace, Trace.Status.R_DONE);
		return true;
	}
		
	private DataInputStream openTrace(Trace trace) throws Exception {
		String file = getRTraceOutputFile(trace).getCanonicalPath();
		TraceSummary.register_trace_summary(DatabaseFactory.getInstance(),
				file, trace.getId());
		TimeSummary.register_time_summary(DatabaseFactory.getInstance(), trace.getRTimedOutputFile(), trace.getId());
		FunctionMap2 functionMap = new FunctionMap2();
		String ftype = FileTools.guess_file_type(file, trace.getFileType());
		functionMap.bindMapFile(DatabaseFactory.getInstance(),
				file, (trace.getMapFileType() == null) 
				? (FileTools.guess_file_type(FunctionMap2.DEFAULT_MAP_FILE_NAME, 
						ftype)) 
				: trace.getMapFileType());
		return new DataInputStream(FileTools.open_file(file, ftype));
	}
	
	public void processTrace(Trace trace){
		if (traceProcessors.isEmpty()) {
			fireThrowableThrown(new IllegalStateException(
					"Error: At least one trace processor must be specified"));
			return ;
		}
		
		//// Initialize all processors
		DataInputStream stream;
		try {
			stream = openTrace(trace);
		} catch (Throwable t) {
			failTrace(trace, t);
			return ;
		}
		
		// TODO ???
		ArrayList<TraceProcessor> procs = new ArrayList<TraceProcessor>();
		for(TraceProcessor processor: traceProcessors)
			try{
				if(processor.initialize_trace(trace.getId()))
					procs.add(processor);
			} catch (Exception e) {
				fireThrowableThrown(new IllegalStateException(
						"Unable to initialize trace processor '"+
								processor+"': "+e.getMessage()));
			}
		TraceProcessor[] good_processors = new TraceProcessor[procs.size()];
		procs.toArray(good_processors);
		fireProcessingStarted(trace);
		try {
			if(good_processors.length > 0) {
				Node.process_stream(good_processors, stream);
			}
		} catch (Throwable t) {
			failTrace(trace, t);
			return ;
		}
		
		try {
			for(TraceProcessor processor : good_processors)
				processor.finalize_trace(trace.getId());
		} catch (Throwable t) {
			fireThrowableThrown(t);
		}
		
		if (good_processors.length > 0)
			changeTraceStatus(trace, Trace.Status.STATIC);
	}
	
	@Override
	public String mangle(Trace trace) {
		return pathMangler.mangle(trace.getSourceFile()) + "-" +
				Arrays.hashCode(trace.getRunCmdArgs()) + "@" +
				trace.getDate().getTime();
	}
	
	private File getRTraceOutputFile(Trace trace) {
		return new File(trace.getOutputDir(), DEFAULT_TRACE_FILE_NAME);
	}
}

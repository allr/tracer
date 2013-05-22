package org.rx.rtrace;

import java.io.File;
import java.io.IOException;

import org.rx.FileTools.ListFileReader;
import org.rx.rtrace.util.CommandLineUtils;
import org.rx.rtrace.util.StringUtils;

public class TraceIOUtils {

	public static Trace importTrace(File outputDir) throws Exception {
		File summaryFile = new File(outputDir, TraceSummary.DEFAULT_FILE_NAME);
		TraceSummaryReader tsr = new TraceSummaryReader(summaryFile);
		return new Trace(tsr.srcFile, tsr.runCmdArgs);
	}
	
	private static class TraceSummaryReader extends ListFileReader {
		public TraceSummaryReader(File summaryFile) throws IOException,
		Exception {
			super(summaryFile.toString());
		}

		// XXX refactor TraceSummary to allow reference
		private static final String SOURCE_FILE_KEY = "File";
		private static final String RUN_CMD_ARGS_KEY = "Args";
		
		private File srcFile;
		private String[] runCmdArgs;
		
		@Override
		protected void parse_line(String line) throws Exception {
			int from = line.indexOf(':');
			if (from == -1)
				throw new IOException("Bad trace summary format");
			System.err.println("Parsing line: " + line);
			String key = StringUtils.strip(line.substring(0, from));
			String value = StringUtils.stripStart(line.substring(from + 1));
			if (key.equals(SOURCE_FILE_KEY)) {
				srcFile = new File(value).getCanonicalFile();
			} else if (key.equals(RUN_CMD_ARGS_KEY)) {
				runCmdArgs = CommandLineUtils.splitArguments(value);
			}
		}
	};
}

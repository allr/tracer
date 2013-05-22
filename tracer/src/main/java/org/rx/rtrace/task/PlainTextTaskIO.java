package org.rx.rtrace.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.rx.FileTools;
import org.rx.rtrace.TraceInfo;
import org.rx.rtrace.task.TaskInfo.TraceKey;
import org.rx.rtrace.util.CharacterEscaper;
import org.rx.rtrace.util.CommandLineUtils;
import org.rx.rtrace.util.StringUtils;

public class PlainTextTaskIO implements TaskImporter, TaskExporter {

	private static final PlainTextTaskIO INSTANCE = new PlainTextTaskIO();
	
	private static final CharacterEscaper TAB_NEWLINE_ESCAPER =
			new CharacterEscaper(
					new char[]{'\\', '\t', '\n'},
					new char[]{'\\', 't', 'n'});
	
	public static PlainTextTaskIO getInstance() {
		return INSTANCE;
	}

	@Override
	public Collection<TaskInfo> importTasks(File inputFile) throws
	FileNotFoundException, IOException, ParsingException {
		final List<TaskInfo> taskInfos = new ArrayList<TaskInfo>();
		try {
			new FileTools.MapFileReader(inputFile.toString()) {
				Map<TraceKey, TraceInfo> traceInfoMap;
				
				String taskName; 
						
				@Override
				protected void put_values(String[] words) throws IOException,
				ParsingException {
					switch (words.length){
					case 0: break;  // skip empty lines
					case 1:
						if (taskName != null)
							addTaskInfo();
						taskName = TAB_NEWLINE_ESCAPER.unescape(
								StringUtils.strip(words[0]));
						break;
					case 2:
						throw new TaskImporter.ParsingException(
								"Invalid format");
					default:
						String traceName = TAB_NEWLINE_ESCAPER.unescape(
								words[0]);
						File srcFile = new File(
								TAB_NEWLINE_ESCAPER.unescape(words[1]));
						String[] cmdRunArgs = CommandLineUtils.splitArguments(
								TAB_NEWLINE_ESCAPER.unescape(words[2]));
						File outputDir = (words.length > 3 ? new File(
								TAB_NEWLINE_ESCAPER.unescape(words[3])) : null);
						String fileTypeStr = (words.length > 4 ?
								StringUtils.strip(words[4]) : "");
						String mapFileTypeStr = (words.length > 5 ?
								StringUtils.strip(words[5]) : "");
						TraceInfo traceInfo = new TraceInfo(traceName,
								srcFile, cmdRunArgs);
						if (outputDir != null)
							traceInfo.setOutputDir(outputDir);
						if (!fileTypeStr.isEmpty())
							traceInfo.setFileType(fileTypeStr);
						if (!mapFileTypeStr.isEmpty())
							traceInfo.setMapFileType(mapFileTypeStr);
						
						if (traceInfoMap == null)
							traceInfoMap = new LinkedHashMap<TraceKey, TraceInfo>();
						traceInfoMap.put(new TraceKey(traceInfo), traceInfo);
					}
				}
				
				@Override
				protected Pattern get_column_separator() {
					return Pattern.compile("\t");
				}
				
				@Override
				public void end_of_list() throws Exception {
					addTaskInfo();
				}
				
				private void addTaskInfo() {
					taskInfos.add(new TaskInfoImpl(taskName, traceInfoMap));
				}
				
				class TaskInfoImpl extends TaskInfo {
					final Map<TraceKey, TraceInfo> traceInfoMap;
					
					public TaskInfoImpl(String taskName,
							Map<TraceKey, TraceInfo>  traceInfoMap) {
						super(taskName);
						this.traceInfoMap =
								new LinkedHashMap<TraceKey, TraceInfo>(
										traceInfoMap);
					}
					
					@Override
					public Set<TraceKey> getTraceKeys() {
						return Collections.unmodifiableSet(traceInfoMap.keySet());
					}

					@Override
					public TraceInfo getTraceInfo(TraceKey traceKey) {
						return traceInfoMap.get(traceKey);
					}
				}
			};
		} catch (Exception e) {
			throw new IOException(e);
		}

		return taskInfos;
	}
	
	@Override
	public void exportTasks(Collection<TaskInfo> taskInfos, File outputFile)
			throws IOException {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(
					outputFile));
			for (TaskInfo taskInfo : taskInfos) {
				writer.write(TAB_NEWLINE_ESCAPER.escape(taskInfo.getName()) +
						"\n");
				for (TraceKey traceKey : taskInfo.getTraceKeys()) {
					TraceInfo traceInfo = taskInfo.getTraceInfo(traceKey);
					String srcFileStr = traceInfo.getSourceFile()
							.getAbsolutePath();
					String cmdRunArgsStr = CommandLineUtils.joinArguments(
							traceInfo.getRunCmdArgs());
					String outputDirStr = traceInfo.getOutputDir()
							.getAbsolutePath();
					String fileTypeStr = traceInfo.getMapFileType();
					if (fileTypeStr == null)
						fileTypeStr = "";
					String mapFileTypeStr = traceInfo.getMapFileType();
					if (mapFileTypeStr == null)
						mapFileTypeStr = "";
					writer.write(StringUtils.join("\t",
							TAB_NEWLINE_ESCAPER.escape(traceInfo.getName()),
							TAB_NEWLINE_ESCAPER.escape(srcFileStr),
							TAB_NEWLINE_ESCAPER.escape(cmdRunArgsStr),
							TAB_NEWLINE_ESCAPER.escape(outputDirStr),
							fileTypeStr,
							mapFileTypeStr) +
							"\n");
				}
			}
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	// disable instantiation
	private PlainTextTaskIO() {
	}

}

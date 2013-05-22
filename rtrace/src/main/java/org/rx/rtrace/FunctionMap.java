package org.rx.rtrace;

import java.sql.SQLException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.rx.DataBase;
import org.rx.FileTools;
import org.rx.Option;
import org.rx.PrimitiveNames;
import org.rx.SourcePatcher;

public class FunctionMap {
	public static String DEFAULT_MAP_FILE_NAME = "source.map";
	
	private final static Map<Long, Integer> memory_map = new HashMap<Long, Integer>();
	private final static Map<Integer, Integer> fun_count = new HashMap<Integer, Integer>();
	private static BufferedReader map_file_reader;
	private static BufferEntry map_file_buffer;
	private static int line_read = 0;
	
	private static Pattern colum_separator = Pattern.compile(" |\t");
	
	public static boolean is_initialized(){
		return map_file_reader != null;
	}
	
	public static int load_and_get(long addr, long ts) throws IOException, SQLException{
		read_throught(ts);
		return get(addr);
	}
	
	public static int get(long addr){
		Integer id = memory_map.get(addr);
		if(id == null)
			return DataBase.UNKNOWN_LOCATION;
		return id;
	}
	
	public static int get_duplicates(int id){
		Integer dups = fun_count.get(id);
		if(dups == null)
			return 0;
		return dups;
	}
	
	public static int get_primitive_location(String string) {
		int pos = -1;
		for(int i = 0; i < PrimitiveNames.primitive_names.length; i ++)
			if(PrimitiveNames.primitive_names[i].equals(string)){
				pos = i;
				break;
			}
		if(pos == -1)
			new RuntimeException("Unable to find primtive: "+string);
		return DataBase.primitive_locations[pos];
	}
		
	public static void clear(){
		fun_count.clear();
		memory_map.clear();
	}
	
	static protected void bind_map_file(String trace_file, String file_type) throws IOException, SQLException{
		String map_file = new File(trace_file).getParent();
		try {
			if(map_file == null)
				map_file = DEFAULT_MAP_FILE_NAME;
			else
				map_file += "/" + DEFAULT_MAP_FILE_NAME;
			if(!new File(map_file).exists())
				map_file += ".gz";
			String ftype = RTrace.map_file_type == null ? file_type : RTrace.map_file_type;
			if(RTrace.auto_patch){
				map_file = check_auto_patch(map_file, ftype);
				ftype = FileTools.guess_file_type(map_file, null);
			}
			map_file_reader = new BufferedReader(new InputStreamReader(FileTools.open_file(map_file, ftype)));
			bufferize_next();
		} catch (IOException e) {
			System.err.println("Unable to load function names from '"+map_file+": "+e.getMessage());
			throw(e);
		}
	}
	
	static private String check_auto_patch(String map_file, String file_type) throws IOException, SQLException{
		BufferedReader reader = new BufferedReader(new InputStreamReader(FileTools.open_file(map_file, RTrace.map_file_type == null ? file_type : RTrace.map_file_type)));
		String fline[] = colum_separator.split(reader.readLine());
		reader.close();
		if(fline.length != 3){
			String dst_file = SourcePatcher.get_patched_file(map_file);
			if(Option.Verbose.verbose)
				System.out.print("Auto-patching: "+map_file+" => "+dst_file);
			if(!SourcePatcher.patch_source_map(RTrace.database, map_file, dst_file))
				throw new IOException("Can't patch file: "+map_file);
			if(Option.Verbose.verbose)
				System.out.println(" OK");
			return dst_file;
		}
		return map_file;
	}
	
	static protected void read_throught(long ts) throws IOException, SQLException {
		while(map_file_buffer != null && map_file_buffer.ts < ts){
			map_file_buffer.put();
			int id = map_file_buffer.id;
			Integer dups = fun_count.get(id);
			if(dups == null)
				fun_count.put(id, 1);
			else
				fun_count.put(id, dups + 1);
			bufferize_next();
		}
	}
	static private void bufferize_next() throws IOException {
		String line = map_file_reader.readLine();
		line_read ++;
		if(line == null){
			map_file_buffer = null;
			return;
		}
		String[] parts = colum_separator.split(line);
		if(parts.length < 3)
			throw new RuntimeException("source.map syntax error on line "+line_read+" only "+parts.length+" entries !");
		map_file_buffer = new BufferEntry(FileTools.parseLong(parts[1]), FileTools.parseInt(parts[2]), FileTools.parseLong(parts[0]));
	}

	private static class BufferEntry{
		final Long addr;
		final int id;
		final Long ts;
		BufferEntry(Long addr, int id, Long ts){
			this.addr = addr;
			this.id = id;
			this.ts = ts;
		}
		
		void put(){
			memory_map.put(addr, id);
		}
	}
}
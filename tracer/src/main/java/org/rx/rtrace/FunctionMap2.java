package org.rx.rtrace;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.rx.DataBase;
import org.rx.FileTools;
import org.rx.Option;
import org.rx.PrimitiveNames;
import org.rx.SourcePatcher;

public class FunctionMap2 {
	public static String DEFAULT_MAP_FILE_NAME = "source.map";
	
	private final Map<Long, Integer> memory_map = new HashMap<Long, Integer>();
	private final Map<Integer, Integer> fun_count = new HashMap<Integer, Integer>();
	private BufferedReader map_file_reader;
	private BufferEntry map_file_buffer;
	private int line_read = 0;
	
	private static final Pattern colum_separator = Pattern.compile(" |\t");
	
	public boolean is_initialized(){
		return map_file_reader != null;
	}
	
	public int loadAndGet(long addr, long ts) throws IOException, SQLException{
		readThrought(ts);
		return get(addr);
	}
	
	public int get(long addr){
		Integer id = memory_map.get(addr);
		if(id == null)
			return DataBase.UNKNOWN_LOCATION;
		return id;
	}
	
	public int getDuplicates(int id){
		Integer dups = fun_count.get(id);
		if(dups == null)
			return 0;
		return dups;
	}
	
	public int getPrimitiveLocation(String string) {
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
		
	public void clear(){
		fun_count.clear();
		memory_map.clear();
	}
	
	protected void bindMapFile(Connection conn, String trace_file, String file_type) throws IOException, SQLException{
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
				map_file = checkAutoPatch(conn, map_file, ftype);
				ftype = FileTools.guess_file_type(map_file, null);
			}
			map_file_reader = new BufferedReader(new InputStreamReader(FileTools.open_file(map_file, ftype)));
			bufferizeNext();
		} catch (IOException e) {
			System.err.println("Unable to load function names from '"+map_file+": "+e.getMessage());
			throw(e);
		}
	}
	
	static private String checkAutoPatch(Connection conn, String map_file, String file_type) throws IOException, SQLException{
		BufferedReader reader = new BufferedReader(new InputStreamReader(FileTools.open_file(map_file, RTrace.map_file_type == null ? file_type : RTrace.map_file_type)));
		String fline[] = colum_separator.split(reader.readLine());
		reader.close();
		if(fline.length != 3){
			String dst_file = SourcePatcher.get_patched_file(map_file);
			if(Option.Verbose.verbose)
				System.out.print("Auto-patching: "+map_file+" => "+dst_file);
			if(!SourcePatcher.patch_source_map(conn, map_file, dst_file))
				throw new IOException("Can't patch file: "+map_file);
			if(Option.Verbose.verbose)
				System.out.println(" OK");
			return dst_file;
		}
		return map_file;
	}
	
	protected void readThrought(long ts) throws IOException, SQLException {
		while(map_file_buffer != null && map_file_buffer.ts < ts){
			map_file_buffer.put();
			int id = map_file_buffer.id;
			Integer dups = fun_count.get(id);
			if(dups == null)
				fun_count.put(id, 1);
			else
				fun_count.put(id, dups + 1);
			bufferizeNext();
		}
	}
	private void bufferizeNext() throws IOException {
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

	private class BufferEntry {
		final Long addr;
		final int id;
		final Long ts;
		BufferEntry(Long addr, int id, Long ts){
			this.addr = addr;
			this.id = id;
			this.ts = ts;
		}
		
		void put() {
			memory_map.put(addr, id);
		}
	}
}
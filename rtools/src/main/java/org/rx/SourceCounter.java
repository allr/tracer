package org.rx;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class SourceCounter {
	static Map<String, Integer> db;
	
	static protected Option[] avaible_options = {
		new Option.Help(){
			protected void process_option(String name, String opts[]) {
				Option.Help.display_help(System.out, avaible_options, 0);
			}
		},
		new Option("-d", "database to load (default stdin)", 1){
			protected void process_option(String name, String opts[]) {
				assert opts.length > 0;
				dbname = opts[0];
			}
		}
	};
	
	static String dbname = "-";
	
	public static void main(String[] args) throws Exception {
		String[] files = Option.process_command_line(args, avaible_options);
		InputStream in = FileTools.open_file(dbname, null);
		
		db = read_database(in);

		for(String file: files)
			System.out.println(source_size(FileTools.open_file(file, null)));
	}
	
	public static Map<String, Integer> read_database(InputStream in) throws Exception{
		final Map<String, Integer> map = new LinkedHashMap<String, Integer>();
		new FileTools.MapFileReader(in){
			@Override
			protected void put_values(String[] parts) {
				map.put(parts[0], Integer.parseInt(parts[1]));
			}
		};
		return map;
	}
	
	public static int source_size(InputStream in) throws Exception{
		final Set<String> seen = new LinkedHashSet<String>();
		int size = new FileTools.MapFileReader(in){
			int size;
			@Override
			protected void put_values(String[] parts) {
				String fname = RLibrary.source_to_relative(parts[2]);
				
				String name = fname+":"+FileTools.parseInt(parts[3])+":"+FileTools.parseInt(parts[4]);
				if(!seen.contains(name)){
					size += find_in_db(name);
				}
			}
			public int get_size(){
				return size;
			}
		}.get_size();
		return size;
	}
	
	public static int find_in_db(String name){
		Integer s = db.get(name);
		if(s != null)
			return s;
		System.err.println("not found: "+name);
		return 0;
	}
}
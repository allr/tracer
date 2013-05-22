package org.rx.rtrace;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.rx.FileTools;

public class OldFunctionMap {
	private final static Map<Integer, String> fun_names = new LinkedHashMap<Integer, String>();
	private final static Map<String, Integer> reverse_names = new LinkedHashMap<String, Integer>();
	private static final String DEFAULT_MAP_FILE_NAME = "funcName.map";

	public static boolean is_initialized(){
		return fun_names.size() > 0;
	}
	
	public static String get(int id){
		return fun_names.get(id);
	}
	
	public static int resolve(String fun_name) {
		Integer id = reverse_names.get(fun_name);
		if(id == null)
			return -1;
		return id;
	}
	
	static protected void read_map_file(String map_file, String file_type) {
		if(map_file == null){
			map_file = new File(map_file).getParent();
			if(map_file == null)
				map_file = DEFAULT_MAP_FILE_NAME;
			else
				map_file += "/" + DEFAULT_MAP_FILE_NAME;
		}

		try {
			new FileTools.MapFileReader(map_file, file_type){
				@Override
				protected void put_values(String[] parts) {
					store(Integer.parseInt(parts[1]), parts[0]);
				}	
			};
		} catch (Exception e) {
				System.err.println("Unable to load function names from '"+map_file+": "+e.getMessage());
		}
	}

	static private void store(int id, String name) {
		fun_names.put(id, name);
		reverse_names.put(name, id);
	}
}

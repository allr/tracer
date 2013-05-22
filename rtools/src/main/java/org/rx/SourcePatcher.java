package org.rx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

public class SourcePatcher {
	static Pattern space = Pattern.compile(" |\t");

	public static Map<String, Integer> cache = new HashMap<String, Integer>();
	
	public static String patch_extension = ".patch";
	
	public static void main(String[] args) throws SQLException, IOException {
		Option[] avaible_options = {
			new DataBase.DBNameOption(),
			new DataBase.DBOverwriteOption(),
			new Option.Quiet()
		};
		String[] todo = Option.process_command_line(args, avaible_options);
		Connection database = DataBase.create_database();
		if(Option.Verbose.verbose)
			System.out.println(Arrays.toString(RLibrary.library_path));
		
		long start_total = System.currentTimeMillis();
		int ok = 0, failed = 0;
		
		for(String fname: todo){
			if(Option.Verbose.verbose)
				System.out.print("Patching "+fname+" ("+(ok+failed+1)+"/"+todo.length+"): ");
			long start = System.currentTimeMillis();
			boolean is_ok = patch_source_map(database, fname);
			if(is_ok)
				ok ++;
			else
				failed ++;
			if(Option.Verbose.verbose)
				System.out.print(is_ok ? "OK" : "FAILED");
			long stop = System.currentTimeMillis();
			if(Option.Verbose.verbose)
				System.out.println(" ("+(((double)(stop-start))/1000)+"s)");
		}
		long stop_total = System.currentTimeMillis();
		if(Option.Verbose.verbose)
			System.out.println("Patched, Ok "+ok+" failed "+failed+" in "+(((double)(stop_total-start_total))/1000)+"s");
	}
	public static String get_patched_file(String src){
		return src+".patch";
	}
	public static boolean patch_source_map(Connection database, String src) throws SQLException {
		return patch_source_map(database, src, get_patched_file(src));
	}
	public static boolean patch_source_map(Connection database, String src, String dst) throws SQLException {
		PrintStream dst_stream = null;
		final Connection db = database;
		boolean ok = false;
		try {
			dst_stream = new PrintStream(new GZIPOutputStream(new FileOutputStream(dst, false)));
			final PrintStream out_stream = dst_stream; 
			new FileTools.ListFileReader(src){
				int line_cmpt = 0;
				@Override
				protected void parse_line(String line_buffer) throws SQLException {
					line_cmpt ++;
					String[] parts = space.split(line_buffer);
					if(parts.length < 5)
						throw new ArrayIndexOutOfBoundsException("File corrupt, found only "+parts.length+" entries on line "+line_cmpt);
					String name = RLibrary.source_to_relative(parts[2]);
					/*for(int i = 3; i < parts.length; i++)
						out_stream.print(" "+parts[i]);*/
					int line = (int) (FileTools.parseLong(parts[3])) ;
					int col = (int) (FileTools.parseLong(parts[4]));
					String compound_name = name+":"+line+":"+col;
					Integer id = cache.get(compound_name);
					if(id == null){
						id = DataBase.register_location(db, name, line, col);
						cache.put(compound_name, id);
					}
					out_stream.println(""+parts[0]+" "+parts[1]+" "+ id);
				}

			};
			ok = true;
		} catch (SQLException e){
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Can't patch "+src+" in "+dst+": "+e.getMessage());
			remove_file(dst_stream, dst);
			return false;
		} finally {
			dst_stream.close();
			DataBase.commitOrRollbackIfNeeded(db, ok);
		}
		return true;
	}
	
	static private void remove_file(PrintStream stream, String fname) {
		if(stream != null)
			stream.close();
		if(fname != null && fname != "")
			new File(fname).delete();
	}
}

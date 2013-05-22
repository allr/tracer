package org.rx;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;
import org.rx.FileTools;
import org.rx.RLibrary;

public class FileName {
	public final static String[] extensions = {".R",".q", ".S"};
	public static boolean list_libraries = false;
	public static boolean list_files = true;
	public static boolean list_others = false;
	
	static protected Option[] avaible_options = {
		new Option.Help() {
			protected void process_option(String name, String[] opts) {
				Option.Help.display_help(System.out, avaible_options, 0);
			}
		},
		new Option.Verbose(),
		new Option("-l", "list libraries", 0){
			protected void process_option(String name, String opts[]) {
				list_libraries = true;
			}
		},
		new Option("-o", "list others", 0){
			protected void process_option(String name, String opts[]) {
				list_others = true;
			}
		},
		new Option("-F", "No list files", 0){
			protected void process_option(String name, String opts[]) {
				list_files = false;
			}
		}
	};

	public static void main(String[] args) throws Exception {
		PrintStream out = System.out;
		for(String arg: Option.process_command_line(args, avaible_options)){
			Set<String> file_names;
			Set<String> library_names = new LinkedHashSet<String>();
			Set<String> other_files = new LinkedHashSet<String>();
			file_names = load_file_names(new FileInputStream(arg), library_names, other_files);
			if(Option.Verbose.verbose){
				out.println("--------------[ "+arg+" ]---------------");
				if(other_files.size() == 1)
					out.print("OK => ");
				out.println("Orig: "+ file_names.size() +
						"\tLibs: "+library_names.size()+
						"\tOther: "+other_files.size()+
						"\t(chk "+(file_names.size()-library_names.size()-other_files.size())+")");
			}
			if(list_libraries)
				out_set(out, library_names);
			if(list_files)
				out_set(out, file_names);
			if(list_others)
				out_set(out, other_files);;
		}
	}
	static void out_set(PrintStream out, Set<? extends Object> set){
		for(Object obj: set){
			out.println(obj);
		}
	}
	static Set<String> load_file_names(InputStream stream, Set<String> library_names, Set<String> other_names)  throws Exception {
		Set<String> fnames = get_source_files(stream);
		used_library(fnames, library_names, other_names);
		fnames.clear();

		for(String lib: library_names)
			for(File fname: RLibrary.library_files(lib))
				fnames.add(fname.toString());
		return fnames;
	}
	
	static Set<String> get_source_files(InputStream stream) throws Exception {
		final Set<String> fnames = new LinkedHashSet<String>();
		new FileTools.MapFileReader(stream){
			@Override
			protected void put_values(String[] parts) {
				fnames.add(parts[2]);
			}
		};
		return fnames;
	}
	
	static void used_library(Set<String> fnames, Set<String> library, Set<String> other_files){
		for(String fname: fnames){
			String relative = RLibrary.source_to_relative(fname);
			if(relative == null){
				other_files.add(relative);
				continue;
			}
			String lname = relative.split(File.separator)[0];
			if((lname = RLibrary.find_library_path_by_name(lname)) != null)
				library.add(lname);
			else
				other_files.add(relative);
		}
	}
}

package org.rx;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class RLibrary {
	public static String[] library_path;
	
	static {
		String library_path_str = System.getenv("R_LIBRARY_PATH");
		if (library_path_str != null)
			library_path = library_path_str.split(File.pathSeparator);
		else
			library_path = new String[]{"tmp"};  // XXX
	}

	public static Map<String, String> file_cache = new LinkedHashMap<String, String>(); 

	static public File[] library_files(String name){
		return library_files(name, true);
	}
	static public File[] library_files(String name, boolean useall){
		File file = find_library(name);
		if(file == null)
			return null; 
		return list_library_files(file, useall);
	}
	
	static public File[] list_library_files(File fname){
		return list_library_files(fname, true);
	}
	static public File[] list_library_files(File fname, boolean useall){
		if(!fname.isDirectory())
			return new File[] { fname };
		if(useall){
			File all = new File(fname, "all.R");
			if(all.exists())
				return  new File[] { all };
		}
		fname = new File(fname, "R");
		if(fname.exists() && fname.isDirectory())
			return fname.listFiles(new FilenameFilter() {	
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".R") || name.endsWith(".q") || name.endsWith(".S");
				}
			});
		return null;
	}
	
	public static File find_library(String name){
/*		File file = null;
		if(system_library_path != null)
			file = find_library(name, system_library_path.split(File.pathSeparator));
		if(file != null)
			return file;*/ 
		if(library_path != null)
			return find_library(name, library_path);
		return null;
	}
	public static File find_library(String name, String[] paths){
		for(String path: paths){
			File base = new File(path, name);
			if(base.exists()){
				if(!base.isDirectory()) // FIXME there might be others
					return base;
				File fname = new File(base, "all.R");
				if(fname.exists())
					return base;
				fname = new File(base, "R");
				if(fname.exists() && fname.isDirectory())
					return base;
			}
		}
		return null;
	}
	
	public static String find_library_path_by_name(String name){
/*		String file = null;
		if(system_library_path != null)
			file = find_library_name(name, system_library_path.split(File.pathSeparator));
		if(file != null)
			return file;*/ 
		if(library_path != null)
			return find_library_path_by_name(name, library_path);
		return null;
	}

	public static String find_library_path_by_name(String name, String[] paths){
		for(String path: paths){
			File fname = new File(new File(path, name), "R");
			if(fname.exists() && fname.isDirectory())
				return name;
			else{
				File dir = new File(path);
				File[] files = dir.listFiles(new FileFilter() {
					@Override
					public boolean accept(File f) {
						return f.isDirectory();
					}
				});
				if(files == null)
					continue;
				for(File dname: files){
					fname = new File(new File(dname, "R"), name);
					if(fname.exists())
						return dname.getName();
				}
			}
		}
		return null;
	}
	
	public static String library_for_file(String fname){
		return library_for_file(fname, library_path);
	}
	
	public static String library_for_file(String fname, String[] paths){
		for(String path: paths){
			File dir = new File(path);
			if(!dir.isDirectory())
				continue;
			File[] files = dir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory();
				}
			});
			for(File file: files){
				File guess = new File(file, fname);
				if(guess.exists())
					return file.getName();
			}
		}
		return "??";
	}
	
	public static String source_to_lib(String fname){
		fname = source_to_relative(fname);
		return relative_to_lib(fname);
		
	}
	
	public static String relative_to_lib(String fname){
		if(fname.startsWith(File.separator)) // TODO Check the windows case
			return null;
		return fname.split(File.separator)[0];
		
	}
	
	public static String source_to_relative(String fname){
		String found = file_cache.get(fname);
		if(found != null)
			return found;
		
		if(fname.startsWith("."))
			found = library_for_file(fname)+fname.substring(1);
		else if(fname.startsWith("/tmp/"))
			found = replace_tmp_dir(fname);
		else if(fname.startsWith("/opt/r/r-instrumented-hg/library"))
			found = fname.substring("/opt/r/r-instrumented-hg/library".length()+1);
		else{
			found = fname; // In case of we don't change the file name :S
			for(String dir: library_path)
				if(fname.startsWith(dir)) {
					found = fname.substring(dir.length()+(dir.endsWith(File.separator) ? 0 : 1));
					break;
				}
		}
		file_cache.put(fname, found);
		return found;
	}

	public static String replace_tmp_dir(String name){
		String[] matches = name.split(File.separator);
		String res = "";
		for(int i = 4; i < matches.length; i++)
			res += ((i != 4) ? File.separator : "")+matches[i];
		return res;
	}
	
	public static void main(String[] args) {
/*		if(system_library_path.length() == 0)
			system_library_path = "/usr/local/lib64/R/library";*/
		if(library_path.length == 0)
			library_path = new String[]{"work/src/Bioconductor/Rpacks-devel"};
		for(String arg: args){
			File[] files = library_files(arg);
			if(files != null)
				for(File file: files)
					System.out.println(file);
			else
				System.err.println("Library "+arg+" not found.");
		}
	}
	
	public static class LibraryOption extends Option {
		LibraryOption() {
			super("-L", "add library path", 1);
		}

		@Override
		protected void process_option(String name, String[] opts) {
			int len = library_path.length;
			library_path = Arrays.copyOf(library_path, len + 1);
			library_path[len] = opts[0];
		}
	}
	/*public static class SystemLibraryOption extends Option {
		SystemLibraryOption() {
			super("--baselib", "Change system library path", 1);
		}

		@Override
		protected void process_option(String name, String[] opts) {
			system_library_path = opts[0];
		}
	}*/
}

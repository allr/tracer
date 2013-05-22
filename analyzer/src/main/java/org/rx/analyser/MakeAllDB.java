package org.rx.analyser;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;

import org.rx.RLibrary;


public class MakeAllDB {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws IOException, SQLException {
		if(args.length == 0)
			args = RLibrary.library_path;
		for(String arg: args){
			System.err.println(arg != null);
			System.err.println(arg);
			File lib_root = new File(arg);
			File[] lib_files = lib_root.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
			if(lib_files == null){
				System.err.println("files null:"+lib_root);
				continue;
			}
			for(File lib: lib_files){
				File[] files = RLibrary.list_library_files(lib, false);
				if(files == null || files.length == 0)
					 continue;
				String passed[] = new String[files.length + 3];
				for(int i = 0; i < files.length; i ++)
					passed[i] = files[i].toString();
				
				passed[passed.length - 3] = "-q";
				passed[passed.length - 2] = "--db";
				passed[passed.length - 1] = arg;
				AnalyseR.main(passed);
			}
		}
	}
}

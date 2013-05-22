package org.rx;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class ImportMem {
	static final String memmap_name = "memory.map"; 
	static final int batch_buffer = 1024; 
	static Option[] avaible_options = {
			new DataBase.DBNameOption(),
			new DataBase.DBOverwriteOption(),
			new Option.Quiet()
		};

	static int id = 0; // TODO register trace, and use a true id 

	public static void from_file(Connection db, File file) throws Exception {
		if(Option.Verbose.verbose)
			System.out.print("Importing '"+file+"': ");
		final String fname = file.toString();
		final PreparedStatement stmt = db.prepareStatement("insert into memory (id, life, plane) values ("+(++id)+", ?, ?);");
		try {
			new FileTools.MapFileReader(file.toString()) {
				int cmpt = 0;
				@Override
				protected void put_values(String[] parts) throws Exception {
					if(parts.length != 2)
						throw new Exception("MemoryMapFile '"+fname+"' is corrupt !");
					
					stmt.setInt(1, Integer.parseInt(parts[0]));
					stmt.setInt(2, Integer.parseInt(parts[1]));
					stmt.addBatch();
					if((cmpt ++) % batch_buffer == 0)
						stmt.executeBatch();
				}
			};
			if(Option.Verbose.verbose)
				System.out.println("Ok");
		} catch (Exception e) {
			if(Option.Verbose.verbose)
				System.out.println("Failed ("+e.getMessage()+")");
		} finally {
			stmt.executeBatch();
			db.commit();
			System.err.println("Commit ?!");
		}
	}
	public static void main(String[] args) throws Exception {
		DataBase.db_name = new File("mem.db");
		String[] todo = Option.process_command_line(args, avaible_options);

		Connection database = DataBase.create_database();
		DataBase.create_table(database, "memory", "id integer, life integer, plane, integer");
		for(String fname: todo)
			try {
				File file = new File(fname);
				if(file.isDirectory()){
					for(String indir_fname: file.list(new FilenameFilter() {	
						@Override
						public boolean accept(File arg0, String ismemfile) {
							return (ismemfile.startsWith(memmap_name));
						}
					})){
						from_file(database, new File(file, indir_fname));
					}
				} else
					from_file(database, file);
			} catch(Exception e){
				e.printStackTrace(System.err);
			};
	}

}

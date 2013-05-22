package org.rx;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExternalCalls {
	public static int vignette_id = -1;
	public static Connection database;
	public static boolean skip = false; 
	static protected Option[] avaible_options = {
		new DataBase.DBNameOption(),
		new DataBase.DBOverwriteOption(),
		new Option.Quiet(),
		new Option("--skip", "", 0){
			protected void process_option(String name, String opts[]) {
				skip = true;
			}
		}
	};
	public static void main(String[] args) throws Exception {
		String[] todo = Option.process_command_line(args, avaible_options);
		database = DataBase.create_database();
		
		DataBase.create_table(database, "extern_names", "id integer primary key autoincrement, name text, type integer, lib text,constraint call_unique unique (name, type, lib)");
		DataBase.create_table(database, "extern_calls", "vignette_id reference vignettes, name_id reference extern_names, nb integer, constraint one_call_by_vignette unique (vignette_id, name_id)");
		int cmpt = 0;
		for(String file: todo){
			try {
				vignette_id = -1;
				cmpt ++;
				String vignette_name = vignette_name(file);
				if(Option.Verbose.verbose)
					System.out.print("Computing "+vignette_name+" ("+cmpt+"/"+todo.length+"): ");
				final Map<String, Integer> map = new LinkedHashMap<String, Integer>();
				Statement stmt = database.createStatement();
				
				if (skip)
					if(stmt.execute("select vignettes.id from vignettes left join external_calls where id=vignette_id and name='"+vignette_name+"' group by id")) {
						if(Option.Verbose.verbose)
							System.out.println("skipped ...");
						stmt.close();
						continue;
					}
				
				vignette_id = DataBase.register_vignette(database, vignette_name);
				stmt.execute("delete from extern_calls where vignette_id="+vignette_id);
				database.commit();
				
				new FileTools.MapFileReader(file) {
					@Override
					protected void put_values(String[] parts) throws SQLException{
						String name = ((parts.length>1)? parts[1]:"ANNONYMOUS")+":"+parts[0]+":"+((parts.length > 2)?parts[2]:"");
						Integer val = map.get(name);
						if(val == null)
							val = 1;
						else
							val = val + 1;
						map.put(name, val);
					}
				};
				for(String n: map.keySet())
					add_function(n, map.get(n));
				database.commit();
				if(Option.Verbose.verbose)
					System.out.print(map.size()+" keys");
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			if(Option.Verbose.verbose)
				System.out.println();
		}
	}
	
	static void add_function(String key_name, int val) throws SQLException{
		Statement stmt = database.createStatement();
		ResultSet set;
		String[] parts = key_name.split(":");
		if(stmt.executeUpdate("insert or ignore into extern_names (name, type, lib) values ('"+parts[0]+"', "+parts[1]+", '"+((parts.length > 2)?parts[2]:"")+"')") > 0){
			set = stmt.getGeneratedKeys();
		} else {
			if(!stmt.execute("select id from extern_names where name='"+parts[0]+"' and type='"+parts[1]+"' and lib='"+((parts.length > 2)?parts[2]:"")+"'"))
				throw new RuntimeException("Call exists ... and not exists in the same time: "+parts[0]);
			set = stmt.getResultSet();
		}
		int sql_index = set.getInt(1);	
		set.close();
		stmt.close();
		if(stmt.executeUpdate("insert into extern_calls (vignette_id, name_id, nb) values ("+vignette_id+", "+sql_index+", "+val+")") > 0){
			stmt.close();
		}else
			throw new RuntimeException("Cannot insert: "+key_name);
	}
	static final String GZ_EXT = ".gz";
	static final String EXTERNAL_EXT = ".external";
	
	static String vignette_name(String file){
		String name = new File(file).getName();
		if(name.endsWith(GZ_EXT))
			name = file.substring(0, file.length() - GZ_EXT.length());
		if(name.endsWith(EXTERNAL_EXT))
			name = file.substring(0, file.length() - EXTERNAL_EXT.length());
		return name;
	}
}

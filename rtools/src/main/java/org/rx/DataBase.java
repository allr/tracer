package org.rx;

import java.io.File;
import java.io.IOException;
import java.sql.*;

public class DataBase {
	public static File db_name = new File("rx.db");
	private static boolean new_database = false;
	public static int UNKNOWN_LOCATION;
	public static int CONTEXT_DROP_INCOMPLETE;
	public static int NO_METHOD_FOUND;
	public static int USE_METHOD_NOCLOSURE;
	public static int DO_BIND;
	public static int APPLY_METHOD;
	public static int last_primitive_location;
	public final static int primitive_locations[] = new int[PrimitiveNames.primitive_names.length]; 
	
	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}

	public static Connection create_database() throws IOException, SQLException {
		return create_database(db_name, new_database);
	}
	public static Connection create_database(boolean overwrite) throws IOException, SQLException {
		return create_database(db_name, overwrite);
	}
	public static Connection create_database(File fname) throws IOException, SQLException {
		return create_database(fname, new_database);
	}
	
	public static Connection create_database(File fname, boolean overwrite) throws IOException, SQLException {
		if(fname.exists() && overwrite)
			fname.renameTo(new File(fname.toString()+".bak")); // TODO check for a 'free' ext.
		Connection database = connect(fname.toString());
		DataBase.create_schema(database);
		return database;
	}
	
	public static Connection connect(String name) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:sqlite:"+name);
		conn.setAutoCommit(false);
		Statement s = conn.createStatement();
		s.setQueryTimeout(10);
		s.close();
		return conn;
	}

	public static void create_table(Connection conn, String name, String scheme) throws SQLException{
		create_table(conn, name, scheme, false);
	}
	
	public static void create_table(Connection conn, String name, String scheme, boolean drop) throws SQLException{
		Statement stat = conn.createStatement();
		if(drop)
			stat.executeUpdate("drop table if exists "+name);
		stat.executeUpdate("create table if not exists "+name+" ("+ scheme +");");
		stat.close();
	}

	public static void create_table_as(Connection conn, String name, String select_stmt, boolean drop) throws SQLException{
		Statement stat = conn.createStatement();
		if(drop)
			stat.executeUpdate("drop table if exists "+name);
		stat.executeUpdate("create table if not exists "+name+" as "+ select_stmt +";");
		stat.close();
	}
	
	public static void commitOrRollbackIfNeeded(Connection conn, boolean ok)
			throws SQLException {
		if (conn.getAutoCommit())
			return ;

		if (ok)
			conn.commit();
		else
			conn.rollback();
	}
	
	public static int register_vignette(Connection db, String name) throws SQLException{
		ResultSet set;
		Statement stmt = db.createStatement();
		if(stmt.executeUpdate("insert or ignore into vignettes (name) VALUES ('"+name+"')") > 0){
			set = stmt.getGeneratedKeys();
		} else {
			if(!stmt.execute("select id from vignettes where name='"+name+"'"))
				throw new RuntimeException("Vignette exists ... and not exists in the same time: "+name);
			set = stmt.getResultSet();
		}
		int id = set.getInt(1);		
		set.close();
		stmt.close();
		return id;
	}
	
    public static int register_file(Connection db, String name)  throws SQLException  {
    	int lib_id = register_library(db, RLibrary.relative_to_lib(name));
    	return register_file(db, lib_id, name);
    }
	
    public static int register_file(Connection db, int lib, String name)  throws SQLException  {
    	Statement stmt = db.createStatement();
    	ResultSet set;
		if(stmt.executeUpdate("insert or ignore into files (library, name) VALUES ("+lib+", '"+name+"')") > 0){
			set = stmt.getGeneratedKeys();
		} else {
			if(!stmt.execute("select id from files where name='"+name+"' and library='"+lib+"'"))
				throw new RuntimeException("File exists ... and not exists in the same time: "+lib);
			set = stmt.getResultSet();
		}
		int file_id = set.getInt(1);
		set.close();
		stmt.close();
		return file_id;
	}
    
    public static int register_library(Connection db, String lib) throws SQLException {
    	Statement stmt = db.createStatement();
    	ResultSet set;
		if(stmt.executeUpdate("insert or ignore into libraries (name) VALUES ('"+lib+"')") > 0){
			set = stmt.getGeneratedKeys();
		} else {
			if(!stmt.execute("select id from libraries where name='"+lib+"'"))
				throw new RuntimeException("Library exists ... and not exists in the same time: "+lib);
			set = stmt.getResultSet();
		}
		int lib_id = set.getInt(1);
		set.close();
		stmt.close();
		return lib_id;
	}

    public static int register_location(Connection db, String fname, int line, int column) throws SQLException {
    	int file_id = register_file(db, fname);
    	return register_location(db, file_id, line, column);
    }
    
    public static int register_location(Connection db, int file, int line, int column) throws SQLException {
    	ResultSet set;
		Statement stmt = db.createStatement();
		if(stmt.executeUpdate("insert or ignore into locations (file, line, col, status) VALUES "+
				"("+file+", "+line+", "+column+", 0)") > 0){
			set = stmt.getGeneratedKeys();
		} else {
			if(!stmt.execute("select id from locations where file='"+file+"' and line="+line+" and col="+column+""))
				throw new RuntimeException("Location exists ... and not exists in the same time: "+file+":"+line+":"+column);
			set = stmt.getResultSet();
		}
		int id = set.getInt(1);
		set.close();
		stmt.close();
		return id;
    }
    
    public static int register_location_with_name(Connection db, String fname, int line, int column, String name, int status) throws SQLException {
    	int file_id = register_file(db, fname);
    	return register_location_with_name(db, file_id, line, column, name, status);
    }
    
    public static int register_location_with_name(Connection db, int file, int line, int column, String name, int status) throws SQLException {
    	ResultSet set;
		Statement stmt = db.createStatement();
		if(stmt.executeUpdate("insert or ignore into locations (file, line, col, name, status) VALUES "+
				"("+file+", "+line+", "+column+", '"+name+"', "+status+")") > 0){
			set = stmt.getGeneratedKeys();
		} else {
			if(!stmt.execute("select id from locations where file='"+file+"' and line="+line+" and col="+column))
				throw new RuntimeException("Location exists ... and not exists in the same time: "+name+" in "+file+":"+line+":"+column);
			set = stmt.getResultSet();
		}
		int id = set.getInt(1);
		set.close();
		stmt.close();
		return id;
    }

	public static void update_location(Connection db, int id, String name) throws SQLException{
		PreparedStatement stmt = db.prepareStatement("update locations set name=? where id=?");
		stmt.setString(1, name);
		stmt.setInt(2, id);
		stmt.execute();
		stmt.close();
	}
	
	public static void create_schema(Connection database) throws SQLException {
		DataBase.create_table(database, "vignettes", "id integer primary key autoincrement, name text, constraint vignette_unique unique (name)");
    	DataBase.create_table(database, "libraries", "id integer primary key autoincrement, name text, constraint lib_unique unique (name)");
		DataBase.create_table(database, "files", "id integer primary key autoincrement, name text, library reference libraries, constraint file_unique unique (library, name)");
		DataBase.create_table(database, "locations", "id integer primary key autoincrement, file reference files, line integer, col integer, name text, status integer, constraint location_unique unique (file, line, col)");

		DataBase.create_table(database, "functions", "id integer primary key autoincrement, "+
				"name text, file text, type integer (1), line integer, col integer, " +
				"constraint closure_unique unique (file, line, col), "+
				"constraint primitive_unique unique (name)");

		int internal_id = register_file(database, "RINTERNAL");
		
		UNKNOWN_LOCATION = register_location_with_name(database, internal_id, -1, -1, "UNKNOWN_LOCATION", 3);
		CONTEXT_DROP_INCOMPLETE = register_location_with_name(database, internal_id, 0, 1, "CONTEXT_DROP_INCOMPLETE", 3);
		NO_METHOD_FOUND = register_location_with_name(database, internal_id, 0, 2, "NO_METHOD_FOUND", 3);
		USE_METHOD_NOCLOSURE = register_location_with_name(database, internal_id, 0, 3, "USE_METHOD_NOCLOSURE", 3);
		DO_BIND = register_location_with_name(database, internal_id, 0, 4, "DO_BIND", 4);
		APPLY_METHOD = register_location_with_name(database, internal_id, 0, 5, "APPLY_METHOD", 5);
		database.commit();
		for(int i = 0; i < PrimitiveNames.primitive_names.length; i++)
			last_primitive_location = primitive_locations[i] = register_location_with_name(database, internal_id, i, 0, PrimitiveNames.primitive_names[i], PrimitiveNames.is_special_primitive[i] ? 1 : 2);
		
		database.commit();
	}
	
	public static class DBNameOption extends Option{
		public DBNameOption(){
			this("--db", "Output db (if param is a dir, default name will be added)");
		}
		public DBNameOption(String name, String text){
			super(name, text, 1);
		}
		protected void process_option(String opt_name, String opts[]) {
			assert opts.length > 0;
			String name = opts[0];
			File outfile = new File(name);
			if(name.endsWith(File.separator) || outfile.isDirectory()){
				outfile.mkdirs();
				outfile = new File(outfile, DataBase.db_name.getName());
			}
			DataBase.db_name = outfile;
		}
	}
	public static class DBOverwriteOption extends Option{
		public DBOverwriteOption(){
			this("--newdb", "New db (even if it was already existing)");
		}
		public DBOverwriteOption(String name, String text){
			super(name, text, 0);
		}
		protected void process_option(String name, String opts[]) {
			new_database = true;
		}
	}
}

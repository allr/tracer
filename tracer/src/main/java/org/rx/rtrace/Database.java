package org.rx.rtrace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.rx.DataBase;

/**
 * <p>Monolithic class which serves as a Data Object Layer for common
 * persistable objects, such as {@link Trace}.</p>
 * 
 * <p>Explicit transaction management is required, so neither commits
 * nor rollbacks are done in any of the methods.</p>
 * 
 * @author "Leo Osvald"
 *
 */
public class Database extends DataBase {	

	public static <T> void checkEntitySavable(Entity<T> e) {
		if (e == null)
			throw new IllegalArgumentException("Cannot save null-entity");
		if (e.isNew())
			throw new IllegalArgumentException("Cannot save entity without id");
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getGeneratedId(Connection conn) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
				"SELECT LAST_INSERT_ROWID()");
		try {
			ResultSet rs = pstmt.executeQuery();
			if (!rs.next())
				return null;
			return (T) rs.getObject(1);
		} finally {
			pstmt.close();
		}
	}
	
	public static void createTracesTable(Connection conn) throws SQLException {
		create_table(conn, "traces",
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"name TEXT NOT NULL, " +
				"status INTEGER NOT NULL, " +
				"src_file TEXT NOT NULL, " +
				"run_cmd_args TEXT NOT NULL, " +
				"ts INTEGER, " +
				"output_dir TEXT, " +
				"file_type TEXT, " +
				"map_file_type TEXT, " +
				"error INTEGER, " +
				"CONSTRAINT trace_unique UNIQUE (src_file, run_cmd_args, ts)");
	}
	
	private Database() {  // disable instantiation
	}
	
}

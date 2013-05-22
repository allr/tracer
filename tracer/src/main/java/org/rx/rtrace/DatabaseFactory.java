package org.rx.rtrace;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.rx.DataBase;

public class DatabaseFactory {

	private static Connection instance;
	
	public static Connection getInstance() throws IOException, SQLException {
		if (instance == null)
			instance = DataBase.create_database(new File("db.db"), false);
		return instance;
	}
	
	private DatabaseFactory() {
	}
}

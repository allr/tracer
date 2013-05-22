package org.rx;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

import org.rx.FileTools.ListFileReader;

abstract public class Summary {

	abstract public TraceInfo[] get_summary_fields();
	abstract public String get_reference_field();
	abstract public String get_table_name();
	
	public void create_schema(Connection database) throws IOException, SQLException {
		StringBuffer buff = new StringBuffer("id reference "+get_reference_field());
		for(TraceInfo field: get_summary_fields()){
			buff.append(", ");
			buff.append(field.get_key_type());
		}

		DataBase.create_table(database, get_table_name(), buff.toString());
	}

	public void register_summary(Connection database, File sum_file, int trace_id) throws IOException, SQLException{
		if(!sum_file.exists()){
			System.err.println("Summary file doesn't exists: "+sum_file);
			return;
		}
		
		final Connection db = database;
		final int t_id = trace_id;
		create_schema(db);
		try {
			new ListFileReader(new FileInputStream(sum_file)){
				TraceInfo[] fields;
				@Override
				protected void parse_line(String line) throws IOException {
					for(TraceInfo field: fields){
						if(line.startsWith(field.tag+":")){
							field.value = line.substring(field.tag.length()+2);
							break;
						}
						if(line.startsWith(field.tag+' ')){
							field.value = line.substring(field.tag.length()+1);
							break;
						}
						if(line.startsWith(field.tag+'\t')){
							field.value = line.substring(field.tag.length()+1);
							break;
						}
					}
				}
				public void start_of_list(){
					fields = get_summary_fields();
					for(TraceInfo field: fields)
						field.value = null;
				}
				public void end_of_list() throws SQLException{
					StringBuffer keys = new StringBuffer("id");
					StringBuffer vals = new StringBuffer("" + t_id);

					for(TraceInfo field: fields)
						if(field.value != null){
							keys.append(", ").append(field.get_key_name());
							vals.append(", ").append(field.format_value());
						}
					
					String query = "insert or replace into "+get_table_name()+" ("+keys+") values ("+vals+")";
					Statement stmt = db.createStatement();
					try {
						stmt.executeUpdate(query);
					} finally {
						stmt.close();
					}
				}
			};
		} catch (Exception e) {
			System.err.println("Couldn't generate "+get_table_name()+": "+e.getMessage());
		} finally {
			db.commit();
		}
	}
	
	protected static class TraceInfo {
		protected String tag;
		String type = "integer";
		String dflt = "0";

		protected String value;
		public TraceInfo(String id){
			tag = id;
		}
		public TraceInfo(String id, String sqltype){
			this(id, sqltype, null);
		}
		public TraceInfo(String id, String sqltype, String defaultvalue){
			tag = id;
			type = sqltype;
			dflt = defaultvalue;
		}
		String get_key_name(){
			return tag.toLowerCase();
		}
		String get_key_type(){
			if(dflt != null)
				return tag.toLowerCase()+" "+type+" default "+dflt;
			return tag.toLowerCase()+" "+type;
		}
		String format_value(){
			return "'"+value+"'";
		}
	}
	
	protected static class TraceMultiInfo extends TraceInfo{
		String suffixes[];
		Pattern separator = Pattern.compile("\\s+");
		public TraceMultiInfo(String id, String suffixes[]){
			super(id);
			this.suffixes = suffixes;
		}
		
		String get_key_name(){
			String ltag = tag.toLowerCase();
			StringBuffer buff = new StringBuffer(ltag).append('_').append(suffixes[0]);
			for(int i = 1; i < suffixes.length; i++){
				buff.append(", ");
				buff.append(ltag).append('_').append(suffixes[i]);
			}
			return buff.toString();
		}
		String get_key_type(){
			String ltag = tag.toLowerCase();
			StringBuffer buff = new StringBuffer(ltag).append('_').append(suffixes[0]).append(" ").append(type);
			if(dflt != null)
				buff.append(" default "+dflt);
			for(int i = 1; i < suffixes.length; i++){
				buff.append(", ").append(ltag).append('_').append(suffixes[i]).append(" ").append(type);
				if(dflt != null)
					buff.append(" default "+dflt);
			}
			return buff.toString();
		}
		String format_value(){
			StringBuffer buff = new StringBuffer();
			String parts[] = separator.split(value);
			for(int i = 0; i < suffixes.length; i++){
				if(i>0)
					buff.append(", ");
				buff.append('\'');
				buff.append(parts[i]);
				buff.append('\'');
			}
			return buff.toString();
		}
	}
}

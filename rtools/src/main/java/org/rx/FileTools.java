package org.rx;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;


public class FileTools {
	final static private int bufferSize = Integer.parseInt(System.getProperty("rtools.buffersize", "32"));

	public static InputStream open_file(String fname, String file_type) throws IOException{
		InputStream iStream = System.in;
		if(fname == null)
			fname = "-";
		if(!fname.equals("-"))
			iStream = new FileInputStream(fname);
		if((file_type == null && fname.endsWith(".gz")) || file_type == "gz")
			iStream = new GZIPInputStream(iStream);
		else if((file_type == null && fname.endsWith(".zip")) || file_type == "zip")
			iStream = new ZipInputStream(iStream);
		else if((file_type == null && fname.endsWith(".jar")) || file_type == "jar")
			iStream = new JarInputStream(iStream);
		if(bufferSize > 0)
			return new BufferedInputStream(iStream, bufferSize*1024);
		else
			return iStream;
	}
	public static String guess_file_type(String fname, String file_type) throws IOException {
		if(file_type != null)
			return file_type;
		if(fname.endsWith(".gz"))
			return "gz";
		else if(fname.endsWith(".patch"))
			return "gz";
		else if(fname.endsWith(".zip"))
			return "zip";
		else if(fname.endsWith(".jar"))
			return "jar";
		else
			return "none";
	}
	public static String choose_file_type(String type){
		String file_type = null;
		if(type.equals("none"))
			file_type = "none";
		else if(type.equals("gz"))
			file_type = "gz";
		else if(type.equals("zip"))
			file_type = "zip";
		else if(type.equals("jar"))
			file_type = "jar";
		else if(type.equals("auto"))
			file_type = null;
		else
			System.err.println("Unknown file type '"+type+"', using 'auto' by default");
		return file_type;
	}
	public static PrintStream output_file(String name, File directory, String prefix) throws IOException{
		PrintStream  oStream = System.out;
		if(directory != null){
			if(!directory.exists())
				directory.mkdirs();
			if(prefix != null)
				name = prefix + "-" + name;
			oStream = new PrintStream(new FileOutputStream(new File(directory, name)));
		}
		return oStream;
	}
	public static int parseInt(String number) {
		if(number.startsWith("0x"))
			return Integer.parseInt(number.substring(2), 16);
		else
			return Integer.parseInt(number);
	}
	public static long parseLong(String number) {
		if(number.startsWith("0x"))
			return Long.parseLong(number.substring(2), 16);
		else
			return Long.parseLong(number);
	}
	public static String join(String array[], String separator){
		StringBuffer buff = new StringBuffer().append(array[0]);
		for(int i = 1; i < array.length; i++)
			buff = buff.append(separator).append(array[i]);			
		return buff.toString();
	}
	public static String map_join(String array[][], String key_separator, String separator){
		StringBuffer buff = new StringBuffer().
		append(array[0][0]).
		append(key_separator).
		append(array[0][1]);
		int i;
		for(i = 1; i < array.length; i++){
			buff = buff.append(separator).
					append(array[i][0]).
					append(key_separator).
					append(array[i][1]);
		}
		return buff.toString();
	}
	public static String join_column(String array[][],int col, String separator){
		StringBuffer buff = new StringBuffer(array[0][col]);
		int i;
		for(i = 1; i < array.length; i++){
			buff.append(separator);
			buff.append(array[i][col]);			
		}
		return buff.toString();
	}
	public static abstract class ListFileReader{
		protected final BufferedReader reader;
		public ListFileReader(String fname, String file_type) throws IOException, Exception {
			this(open_file(fname, file_type));
		}
		public ListFileReader(String fname) throws IOException, Exception {
			this(fname, null);
		}
		public ListFileReader(InputStream iStream) throws IOException, Exception {
			reader = new BufferedReader(new InputStreamReader(iStream), bufferSize*1024);
			String line;
			start_of_list();
			while((line = reader.readLine()) != null){
				parse_line(line);
			}
			end_of_list();
		}
		public void start_of_list() throws Exception {}
		public void end_of_list() throws Exception {}
		
		abstract protected void parse_line(String line) throws Exception;
	}
	public static abstract class MapFileReader extends ListFileReader {
		public MapFileReader(String fname, String file_type) throws IOException, Exception {
			this(open_file(fname, file_type));
		}
		public MapFileReader(String fname) throws IOException, Exception {
			this(fname, null);
		}
		public MapFileReader(InputStream iStream) throws IOException, Exception{
			super(iStream);
		}
		protected void parse_line(String line) throws Exception{
			String parts[] = get_column_separator().split(line);
			put_values(parts);
		}
		protected Pattern get_column_separator(){
			return Pattern.compile(" |\t");
		}
		abstract protected void put_values(String parts[]) throws Exception;
	}
}

package org.rx;

import java.io.PrintStream;
import java.util.ArrayList;

abstract public class Option {
	private String option; 
	private String help;
	private int params;
	public Option(String option_name){
		this(option_name, option_name, 0);
	}
	public Option(String option_name, String helptext){
		this(option_name, helptext, 0);
	}
	public Option(String option_name, String helptext, int nb_params){
		this.option = option_name;
		this.help = helptext;
		this.params = nb_params;
	}
	public String toString() {
		return option;
	}
	public String getOption() {
		return option;
	}
	public String getHelp() {
		return help;
	}
	public int getParams() {
		return params;
	}
	public boolean has_option(String arg) {
		return option.equals(arg);
	}
	public String format_help_text(){
		return getOption() +
		(getParams()>0? " <"+getParams()+">" :"") +
		"\t" + getHelp();
	}
	abstract protected void process_option(String name, String opts[]);

	public static String[] process_command_line(String[] args, Option[] aviable_options){
		ArrayList<String> todo = new ArrayList<String>(args.length);

		for(int i = 0; i < args.length; i ++){
			String arg = args[i];
			boolean found = false;
			for(int j = 0; j < aviable_options.length && !found; j ++)
				if(aviable_options[j].has_option(arg)){
					int nbParams = aviable_options[j].getParams();
					String opts[] = new String[nbParams];
					int currentParam = 0;
					while(currentParam < nbParams)
						try {
							opts[currentParam++] = args[++i];
						} catch (ArrayIndexOutOfBoundsException e) {
							System.err.println("Option '"+arg+"' expects some parameters, using null");
							opts[currentParam - 1] = null;
						}
					aviable_options[j].process_option(arg, opts);
					found = true;
				}
			if(!found)
				todo.add(arg);
		}
		String[] rest = new String[todo.size()];
		todo.toArray(rest);
		return rest;
	}
	
	public abstract static class Help extends Option{
		public Help(){
			super("-?", "Help (this screen)", 0);
		}
		public boolean has_option(String arg) {
			return "--help".equals(arg) || super.has_option(arg);
		}
		static public void display_help(PrintStream out, Option[] opts, int exitValue) {
			if(opts != null)
				for(Option opt: opts)
					out.println(opt.format_help_text());
			System.exit(exitValue);
		}
	}
	
	public static class Text extends Option{
		public Text(String text){
			super("", text, 0);
		}
		protected void process_option(String name, String opts[]) {}
		public boolean has_option(String opt) { return false;}
		public String format_help_text(){
			return getHelp();
		}
	}
	
	public static class Verbose extends Option{
		public static boolean verbose = false;
		public Verbose(){
			super("-v", "Be more verbose", 0);
		}
		protected void process_option(String name, String opts[]) {
			verbose = true;
		}
	}
	
	public static class Quiet extends Option{
		// This class is only for convenience, it must not be instanciated in
		// a static context to avoid conflict with verbose.
		public Quiet(){
			super("--quiet", "Be quiet", 0);
			Verbose.verbose = true;
		}
		protected void process_option(String name, String opts[]) {
			Verbose.verbose = false;
		}
	}
}
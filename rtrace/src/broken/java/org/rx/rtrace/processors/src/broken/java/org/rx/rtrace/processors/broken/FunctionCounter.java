package org.rx.rtrace.processors;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rx.FileTools;
import org.rx.FileTools.ListFileReader;
import org.rx.rtrace.OldFunctionMap;
import org.rx.rtrace.RTrace;
import org.rx.rtrace.Node.FunctionCall;


public class FunctionCounter extends BasicProcessor {
	protected Map<Integer,Integer> func_map;
	String map_file;
	
	public void initialize_processor(String[] options) throws Exception {
		map_file = options[0];
	}
	
	@Override
	public boolean initialize_trace() throws Exception { 
		func_map = new LinkedHashMap<Integer, Integer>();
		read_map_file(map_file);
		return func_map.size() > 0;
	}
	
	protected void read_map_file(String map_file) {
		try {
			new FileTools.ListFileReader(map_file) {
				@Override
				protected void parse_line(String line) {
					put(line);
				}
			};
		} catch (IOException e) {
				System.err.println("Unable to load function to counts from '"+map_file+": "+e.getMessage());
		}
	}
	
	public void finalize_trace() {
		for(int key: func_map.keySet())
			out.println(format_name(key)+": "+func_map.get(key));
	}

	protected String format_name(int fun_id){
		return "0x"+Integer.toHexString(fun_id);
	}

	protected void put(String fun_name) {
		try {
			func_map.put(ListFileReader.parseInt(fun_name), 0);
		} catch (NumberFormatException e) {
			System.err.println("Bad number: "+fun_name);
		}
	}
	@Override
	protected NodeVisitor make_visitor(){
		return new BasicNodeVisitor() {	
			@Override
			public void visit_apply_call(FunctionCall node) throws Exception {
				int id = node.getID();
				Integer value = func_map.get(id);
				if(value != null)
					func_map.put(id, value + 1);
				super.visit_apply_call(node);
			}
		};
	}

	public static class FunctionCounterByName extends FunctionCounter {
		@Override
		public boolean initialize_trace() throws Exception { 
			if(!OldFunctionMap.is_initialized())
				throw new Exception("FunctionMap must be loaded to use this processor !");
			return super.initialize_trace();
		}
		
		protected String format_name(int fun_id){
			String name = OldFunctionMap.get(fun_id);
			if(name == null)
				return super.format_name(fun_id);
			return name;
		}

		protected void put(String fun_name) {
			int id = OldFunctionMap.resolve(fun_name);
			if(id == -1)
				System.err.println("No function id for: "+fun_name);
			else
				func_map.put(id, 0);
		}
	}
}

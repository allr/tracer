package org.rx.rtrace.processors;


import org.rx.rtrace.FunctionMap;
import org.rx.rtrace.FunctionMap.SpecialName;
import org.rx.rtrace.Node.FunctionCall;


public class TypesOfCalls  extends BasicProcessor {
	protected int results[] = {0, 0, 0, 0};
	static final public int SPECIAL = 0;
	static final public int BUILTIN = 1;
	static final public int FUNCTION = 2;
	static final public int CLOSURE = 3;
	String map_file;
	
	public void finalize_trace() {
		out.println("SPECIAL "+results[SPECIAL]);
		out.println("BUILTIN "+results[BUILTIN]);
		out.println("FUNCTION "+results[FUNCTION]);
		out.println("CLOSURE "+results[CLOSURE]);
	}
	@Override
	protected NodeVisitor make_visitor(){
		return new NodeVisitorAdapter() {
			@Override
			public void visit_apply_call(FunctionCall node) throws Exception {
				int id = node.getID();
				int type = FUNCTION;
				if(id < FunctionMap.FIRST_ID) {
					type = FunctionMap.primitives_names[id] instanceof SpecialName ? SPECIAL : BUILTIN; 
				}
				results[type] ++;
			}
		};
	}
}
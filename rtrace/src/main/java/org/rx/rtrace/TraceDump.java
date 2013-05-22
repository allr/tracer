package org.rx.rtrace;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rx.FileTools;

public class TraceDump {	
	
	static Map<Integer, OpCode> map = new LinkedHashMap<Integer, OpCode>();
	static Map<OpCode, Integer> counts = new LinkedHashMap<OpCode, Integer>();
	static final OpCode unknown =  new OpCode(0xFF, 0, "xxxx");
	static int cmpt = 0;
	static final int LINELENGHT = Integer.parseInt(System.getProperty("TraceDump.lenght", "16"));
	static final private boolean old_header = Boolean.parseBoolean(System.getProperty("RTrace.NodeBuilder.oldheader", "false"));
	
	static byte[] buffer = new byte[32*1024];
	static int pos = 0;
	static int read = 0;
	
	public static void main(String[] args) throws IOException {
		build_table();
		
		InputStream istream = FileTools.open_file((args.length > 0) ? args[0] : null, null);
		int skip = 0;
		if(args.length > 1)
			skip = FileTools.parseInt(args[1]);
		
		DataInputStream stream = new DataInputStream(istream);//new BufferedInputStream(istream, 8192));
		cmpt = old_header ? 9 : 12;
		stream.skipBytes(cmpt); // Skip the header
		
		try {
			while(skip > 0){
				int current = (readByte(stream) & 0xFF);
				OpCode opcode = get_opcode(current);
				int followed = opcode.followed;
				stream.skipBytes(followed);
				cmpt += 1 + followed;
				skip -= 1 + followed;
			}
			System.out.print("0x"+Integer.toHexString(cmpt)+"\t");
			skip = cmpt % LINELENGHT;
			while((-- skip) >= 0)
				System.out.print("\t");

			do{
				int current = (readByte(stream) & 0xFF);
				OpCode opcode = get_opcode(current);
				display(opcode.name);
				skip = opcode.followed;
				while(skip-- > 0){
					display("0x"+
							Integer.toHexString(readByte(stream) & 0xFF));
				}
			}while(true);
		} catch (EOFException e) {}
		System.out.println();
		for(OpCode op: counts.keySet())
			System.err.println(op.name+" "+counts.get(op));
	}
	
	private static void display(String text) {
		System.out.print(text+"\t");
		cmpt ++;
		if((cmpt%LINELENGHT) == 0){
			System.out.println("");
			System.out.print("0x"+Integer.toHexString(cmpt)+"\t");
		}
	}

	private static OpCode get_opcode(int current) {
		OpCode opcode = map.get(current);
		if(opcode == null)
			opcode = unknown;
		Integer val = counts.get(opcode);
		if(val == null)
			val = 0;
		counts.put(opcode, val + 1);
		return opcode;
	}
	
	static int readByte(DataInputStream is) throws IOException{
		return is.readByte();
		/*
		if(pos == read){
			read = is.read(buffer) - 1;
			pos = 0;
			if(read <= 0)
				throw new EOFException();
		}
		return buffer[pos ++];
		*/
	}
	
	static void build_table(){
		add(new OpCode(Node.NIL_SXP, 0, "NIL"));
		add(new OpCode(Node.SYMBOL_SXP, 0, "SYM"));
		add(new OpCode(Node.SYMBOL_SXP | Node.S3FLAG, 0, "SYM3"));
		add(new OpCode(Node.LIST_SXP, 0, "LIST"));
		add(new OpCode(Node.LIST_SXP | Node.S3FLAG, 0, "LIST3"));
		add(new OpCode(Node.CLOS_SXP, 0, "CLOS"));
		add(new OpCode(Node.CLOS_SXP | Node.S3FLAG, 0, "CLOS3"));
		add(new OpCode(Node.ENV_SXP, 0, "ENV"));
		add(new OpCode(Node.ENV_SXP | Node.S3FLAG, 0, "ENV3"));
		add(new OpCode(Node.LANG_SXP, 0, "LANG"));
		add(new OpCode(Node.LANG_SXP | Node.S3FLAG, 0, "LANG3"));
		add(new OpCode(Node.SPECIAL_SXP, 0, "SPEC"));
		add(new OpCode(Node.SPECIAL_SXP | Node.S3FLAG, 0, "SPEC3"));
		add(new OpCode(Node.BUILTIN_SXP, 0, "BUIL"));
		add(new OpCode(Node.BUILTIN_SXP | Node.S3FLAG, 0, "BUIL3"));
		add(new OpCode(Node.CHAR_SXP, 0, "CHAR"));
		add(new OpCode(Node.CHAR_SXP | Node.S3FLAG, 0, "CHAR3"));
		add(new OpCode(Node.LOGICAL_SXP, 0, "LOG"));
		add(new OpCode(Node.LOGICAL_SXP | Node.S3FLAG, 0, "LOG3"));
		add(new OpCode(Node.INT_SXP, 2, "INT"));
		add(new OpCode(Node.INT_SXP | Node.S3FLAG, 2, "INT3"));
		add(new OpCode(Node.REAL_SXP, 2, "REAL"));
		add(new OpCode(Node.REAL_SXP | Node.S3FLAG, 2, "REAL3"));
		add(new OpCode(Node.CPLX_SXP, 2, "CPLX"));
		add(new OpCode(Node.CPLX_SXP | Node.S3FLAG, 2, "CPLX3"));
		add(new OpCode(Node.STRING_SXP, 2, "STR"));
		add(new OpCode(Node.STRING_SXP | Node.S3FLAG, 2, "STR3"));
		add(new OpCode(Node.DOT_SXP, 0, "DOT"));
		add(new OpCode(Node.DOT_SXP | Node.S3FLAG, 0, "DOT3"));
		add(new OpCode(Node.ANY_SXP, 0, "ANY"));
		add(new OpCode(Node.ANY_SXP | Node.S3FLAG, 0, "ANY3"));
		add(new OpCode(Node.VECTOR_SXP, 2, "VEC"));
		add(new OpCode(Node.VECTOR_SXP | Node.S3FLAG, 2, "VEC3"));
		add(new OpCode(Node.EXPR_SXP, 0, "EXP"));
		add(new OpCode(Node.EXPR_SXP | Node.S3FLAG, 0, "EXP3"));
		add(new OpCode(Node.BCODE_SXP, 0, "BCODE"));
		add(new OpCode(Node.BCODE_SXP | Node.S3FLAG, 0, "BCODE3"));
		add(new OpCode(Node.EPTR_SXP, 0, "EPTR"));
		add(new OpCode(Node.EPTR_SXP | Node.S3FLAG, 0, "EPTR3"));
		add(new OpCode(Node.WREF_SXP, 0, "WREF"));
		add(new OpCode(Node.WREF_SXP | Node.S3FLAG, 0, "WREF3"));
		add(new OpCode(Node.RAW_SXP, 0, "RAW"));
		add(new OpCode(Node.RAW_SXP | Node.S3FLAG, 0, "RAW3"));
		add(new OpCode(Node.S4_SXP, 0, "S4"));
		add(new OpCode(Node.SPECIAL_EVENT, 0, "sevnt"));
		add(new OpCode(Node.UNIT_SXP, 0, "UNIT"));

		add(new OpCode(Node.EVAL_BUILTIN, 2, "buil["));
		add(new OpCode(Node.EVAL_BUILTIN | Node.NO_PROLOGUE_MOD, 2, "bui'["));
		add(new OpCode(Node.EVAL_SPECIAL, 2, "spec["));
		add(new OpCode(Node.EVAL_SPECIAL | Node.NO_PROLOGUE_MOD, 2, "spe'["));
		
		add(new OpCode(Node.EVAL_CLOSURE, 11, "clos["));
		add(new OpCode(Node.EVAL_CLOSURE | Node.NO_PROLOGUE_MOD, 11, "clo'["));
		
		add(new OpCode(Node.PROLOGUE, 0, "prol ["));
		add(new OpCode(Node.END_PROLOGUE, 0, "] prol"));
		
		add(new OpCode(Node.END_EVAL_CALL, 0, "] ret"));
		
		add(new OpCode(Node.EVAL_BND_PROM, 8, "bprom["));
		add(new OpCode(Node.EVAL_UNBND_PROM, 8, "uprom["));
		add(new OpCode(Node.END_EVAL_PROM, 0, "]prom"));
		
		add(new OpCode(Node.UNK_TYPE, 0, "utype"));
		add(new OpCode(Node.UNK_EVENT, 0, "uerror"));
		add(new OpCode(Node.R_ERROR, 0, "rerror"));
		add(new OpCode(Node.LOG_ERROR, 0, "lerror"));

		add(new OpCode(Node.UNBND_PROM, 8, "unbnd"));
		add(new OpCode(Node.BND_PROM, 8, "bound"));
		
		add(new OpCode(Node.BND_PROM | Node.PROM_NEW_MOD, 8, "boun'"));
		add(new OpCode(Node.UNBND_PROM | Node.PROM_NEW_MOD, 8, "unbn'"));
	}
	
	static void add(OpCode bcode){
		map.put(bcode.id, bcode);
	}
	
	static class OpCode {
		public final int id;
		public final int followed;
		public final String name;
		public OpCode(int id, int followed, String name) {
			this.id = id;
			this.followed = followed;
			this.name = name;
		}
	}
}

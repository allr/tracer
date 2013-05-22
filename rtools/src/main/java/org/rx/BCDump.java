package org.rx;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

public class BCDump {
	static final OpCode[] OPCODES = OpCode.values();
	static final Map<OpCode, Integer> counts = new EnumMap<OpCode, Integer>(OpCode.class);
	static int cmpt = 0, bytes_read;
	final static private boolean littleEndian = Boolean.parseBoolean(System.getProperty("RTrace.NodeBuilder.littleendian", "true"));

	static final int LINELENGHT = Integer.parseInt(System.getProperty("TraceDump.lenght", "16"));
	
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
		cmpt = read_header(stream) ;// Skip the header
		
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
				display(opcode.name());
				skip = opcode.followed;
				while(skip-- > 0){
					display("0x"+
							Integer.toHexString(readByte(stream) & 0xFF));
				}
			}while(true);
		} catch (EOFException e) {}
		System.out.println();
		for(Entry<OpCode, Integer> op_cnt: counts.entrySet())
			System.err.println(op_cnt.getKey().name()+" "+op_cnt.getValue());
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
		OpCode opcode = (current < OPCODES.length ? OPCODES[current] : OpCode.UNKNOWN);
		Integer val = counts.get(opcode);
		counts.put(opcode, val != null ? val + 1 : 1);
		return opcode;
	}
	
	@SuppressWarnings("unused")
	static private int readNumber(DataInputStream stream, int size) throws IOException { // Size in bytes
		int value = 0;
		int current = 0; // FIXME Why using a byte is buggy ?
		for(int i = 0; i < size; i ++){
			current = stream.readByte() & 0xFF; // make a byte;
			if(littleEndian)
				value = (value << 8) | current;
			else
				value |= (current << (8*i));
		}
		bytes_read += size;
		return value;
	}
	static private int readInt(DataInputStream stream) throws IOException {
		// return readNumber(stream, 4);
		int v = stream.readInt();
		bytes_read += 4;
		if(littleEndian)
			return v;
		return (v >>> 24) | (v << 24) | 
	      ((v << 8) & 0x00FF0000) | ((v >> 8) & 0x0000FF00);
	}
	static private int readByte(DataInputStream stream) throws IOException {
		int b = stream.readByte() & 0xFF;
		bytes_read ++;
		return b;
	}
	static private int readShort(DataInputStream stream) throws IOException {
	//	return readNumber(stream, 2) & 0xFFFF;
		int v = stream.readShort();
		bytes_read += 2;
		return (v&0xFF) << 8 | (v & 0xFF00) >> 8; // & 0xFFFF;*/
	}
	static private int readPtr(DataInputStream stream) throws IOException {
		//return readNumber(stream, 8);
		int v = readInt(stream);
		readInt(stream); // discards 0
		return v;
	}
	
	static int read_header(DataInputStream is) throws IOException{
		byte[] magic = new byte[5];
		int cmpt = is.read(magic);
		if(!new String(magic).equals("RDX2\n"))
			System.err.println("Magic not handled: "+new String(magic));
		magic = new byte[2];
		cmpt += is.read(magic);
		if(!new String(magic).equals("X\n"))
			System.err.println("Only binary (X) mode supported: "+new String(magic));
		bytes_read = cmpt;
		int version = readInt(is);
		int w_version = readInt(is); 
		int r_version = readInt(is); 
		System.err.println("Reading version: "+version+" ("+w_version+"/"+r_version+")");
		return cmpt;
	}
	
	static void build_table(){

	}
	
	enum OpCode {
		BCMISMATCH(0),
		RETURN(0),
		GOTO(1),
		BRIFNOT(2),
		POP(0),
		DUP(0),
		PRINTVALUE(0),
		STARTLCNTXT(1),
		ENDLOOPCNTXT(0),
		DOLOOPNEXT(0),
		DOLOOPBREAK(0),
		STARTFOR(3),
		STEPFOR(1),
		ENDFOR(0),
		SETLOOPVAL(0),
		INVISIBLE(0),
		LDCONST(1),
		LDNULL(0),
		LDTRUE(0),
		LDFALSE(0),
		GETVAR(1),
		DDVAL(1),
		SETVAR(1),
		GETFUN(1),
		GETGLOBFUN(1),
		GETSYMFUN(1),
		GETBUILTIN(1),
		GETINTLBUILTIN(1),
		CHECKFUN(0),
		MAKEPROM(1),
		DOMISSING(0),
		SETTAG(1),
		DODOTS(0),
		PUSHARG(0),
		PUSHCONSTARG(1),
		PUSHNULLARG(0),
		PUSHTRUEARG(0),
		PUSHFALSEARG(0),
		CALL(1),
		CALLBUILTIN(1),
		CALLSPECIAL(1),
		MAKECLOSURE(1),
		UMINUS(1),
		UPLUS(1),
		ADD(1),
		SUB(1),
		MUL(1),
		DIV(1),
		EXPT(1),
		SQRT(1),
		EXP(1),
		EQ(1),
		NE(1),
		LT(1),
		LE(1),
		GE(1),
		GT(1),
		AND(1),
		OR(1),
		NOT(1),
		DOTSERR(0),
		STARTASSIGN(1),
		ENDASSIGN(1),
		STARTSUBSET(2),
		DFLTSUBSET(0),
		STARTSUBASSIGN(2),
		DFLTSUBASSIGN(0),
		STARTC(2),
		DFLTC(0),
		STARTSUBSET2(2),
		DFLTSUBSET2(0),
		STARTSUBASSIGN2(2),
		DFLTSUBASSIGN2(0),
		DOLLAR(2),
		DOLLARGETS(2),
		ISNULL(0),
		ISLOGICAL(0),
		ISINTEGER(0),
		ISDOUBLE(0),
		ISCOMPLEX(0),
		ISCHARACTER(0),
		ISSYMBOL(0),
		ISOBJECT(0),
		ISNUMERIC(0),
		NVECELT(0),
		NMATELT(0),
		SETNVECELT(0),
		SETNMATELT(0),
		AND1ST(2),
		AND2ND(1),
		OR1ST(2),
		OR2ND(1),
		GETVAR_MISSOK(1),
		DDVAL_MISSOK(1),
		VISIBLE(0),
		SETVAR2(1),
		STARTASSIGN2(1),
		ENDASSIGN2(1),
		SETTER_CALL(2),
		GETTER_CALL(1),
		SWAP(0),
		DUP2ND(0),
		SWITCH(4),
		RETURNJMP(0),
		UNKNOWN(0);
		
		final int followed;
		
		OpCode(int followed) {
			this.followed = followed;
		}
		
		public int id() {
			return ordinal();
		}
		
		@Override
		public String toString() {
			return "OpCode(" + name() + ")[id = " + ordinal() + "; followed = " +
					followed + "]";
		}
	}
}

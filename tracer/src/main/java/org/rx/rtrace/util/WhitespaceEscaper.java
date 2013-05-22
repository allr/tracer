package org.rx.rtrace.util;


public class WhitespaceEscaper extends CharacterEscaper {

	private static final char[] FROM = new char[]{
		' ', '\n', '\t', '\\', '\r'
	};
	private static final char[] TO = new char[]{
		'_',  'n',  't', '\\', 'r'
	};
	
	private static final WhitespaceEscaper INSTANCE
	= new WhitespaceEscaper();

	private WhitespaceEscaper() {
		super(FROM, TO);
	}

	public static WhitespaceEscaper getInstance() {
		return INSTANCE;
	}

}

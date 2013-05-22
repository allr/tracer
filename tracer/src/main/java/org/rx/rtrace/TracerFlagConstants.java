package org.rx.rtrace;

public abstract class TracerFlagConstants {

	public static final int TIME = 0x1;
	public static final int TRACE = 0x2;
	
	public static final int TIME_AND_TRACE = TIME | TRACE;
	
	private TracerFlagConstants() {
	}
}

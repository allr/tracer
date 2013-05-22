package org.rx.rtrace;

import java.io.File;

public class SimplePathMangler implements PathMangler {

	@Override
	public String mangle(File path) {
		try {  // XXX
			path = path.getAbsoluteFile();
			return path.getParent() + "-" + path.getName();
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("Mangling failed: Invalid file path.");
		}
	}
	
}

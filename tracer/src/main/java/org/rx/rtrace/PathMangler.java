package org.rx.rtrace;

import java.io.File;

public interface PathMangler extends Mangler<File, String> {
	@Override
	String mangle(File path);
}

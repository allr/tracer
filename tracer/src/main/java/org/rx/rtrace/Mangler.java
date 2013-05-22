package org.rx.rtrace;

public interface Mangler<T, R> {
	R mangle(T what);
}

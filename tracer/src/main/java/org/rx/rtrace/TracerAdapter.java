package org.rx.rtrace;

public class TracerAdapter implements TracerListener {
	@Override
	public void tracingStarted(Tracer t) {
	}

	@Override
	public void tracingDone(Tracer t) {
	}

	@Override
	public void traceRegistered(Tracer tracer, Trace trace) {
	}

	@Override
	public void rTraceStarted(Tracer tracer, Trace trace) {
	}

	@Override
	public void rTraceDone(Tracer tracer, Trace trace) {
	}

	@Override
	public void processingStarted(Tracer tracer, Trace trace) {
	}

	@Override
	public void processingDone(Tracer tracer, Trace trace) {
	}

	@Override
	public void exceptionThrown(Tracer t, TracerException e) {
	}

	@Override
	public void traceFailed(Tracer t, Trace trace, Throwable cause) {
	}
}

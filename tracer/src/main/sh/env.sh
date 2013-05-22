#!/bin/sh

makedir () {
	[ -z "$1" ] && error "need a folder to create"
	mkdir -p $1
	chgrp r $1
	chmod g+rw $1
}

error () {
	echo $*
	exit 1
}


[ "$SOURCED" ] && return

TRACER_PROFILE="~/.tracer"
[ -z "$SOURCED" ] && [ -f "$TRACER_PROFILE" ] && source $TRACER_PROFILE

[ -z "$JAVA" ] && JAVA=java

[ -z "$RGROUPHOME" ] && RGROUPHOME="/opt/r"
[ -z "$SCRIPTDIR" ] && SCRIPTDIR="$RGROUPHOME/tracer"
[ -z "$BIOCDIR" ] && BIOCDIR="$RGROUPHOME/bioc-2.7"
[ -z "$BIOCSOURCE" ] && BIOCSOURCE="$BIOCDIR-source"
[ -z "$TRACEDIR" ] && TRACEDIR="$RGROUPHOME/traces"
[ -z "$RESULT_DIR" ] && RESULT_DIR="$TRACEDIR/results"
[ -z "$PROCESSORS" ] && PROCESSORS=2
[ -z "$JAVA_OPTS" ] && JAVA_OPTS="-Xmx4092m -Xss50m"
[ -z "$TAG" ] && TAG="`date '+%Y%m%d-%H%M'`"
[ -z "$R_LIBS_SITE" ] && R_LIBS_SITE="$BIOCDIR"

[ -z "$JARPATH" ] && JARPATH="$RGROUPHOME/jars"
[ -z "$CLASSPATH" ] && CLASSPATH="/usr/share/java/stringtemplate.jar:`ls "$JARPATH"/*.jar |xargs |tr ' ' :`"
#[ -z "$R_LIBRARY_PATH" ] && R_LIBRARY_PATH="/home/bghill/r-instrumented-hg/src/library:/usr/local/lib64/R/library:$BIOCSOURCE:$BIOCDIR"
[ -z "$R_LIBRARY_PATH" ] && R_LIBRARY_PATH="/opt/r/r-instrumented-hg/src/library:$BIOCSOURCE:/usr/local/lib64/R/library:$BIOCDIR"

[ -z "$STATIC_OPTS" ] && STATIC_OPTS="--keywords --recursive --tokens --resolv"
[ -z "$PARSE_OPTS" ] && PARSE_OPTS="--allcalls --recursive --hidden --foreign --counters --promise --size --args"
[ -z "$DEFAULT_TRACE_LIST" ] && DEFAULT_TRACE_LIST=all-rev.list
[ -z "$R_INSTRUMENTED" ] && R_INSTRUMENTED="/usr/local/bin/R-trace"
[ -z "$R_TIMED" ] && R_TIMED="/usr/local/bin/R-timed"
[ -z "$R_2_12" ] && R_2_12="/usr/local/bin/R-2_12_1"
[ -z "$R_2_13" ] && R_2_13="/usr/local/bin/R-2_13_1"
[ -z "$R" ] && R=$R_2_12

PATH="$SCRIPTDIR:/usr/lib64/qt-3.3/bin:/usr/kerberos/sbin:/usr/kerberos/bin:/usr/lib64/ccache:/usr/local/bin:/bin:/usr/bin:/home/morandat/bin:/usr/local/sbin:/usr/sbin:/sbin"

export SOURCED=true RGROUPHOME SCRIPTDIR JARPATH CLASSPATH BIOCDIR TRACEDIR RESULT_DIR PROCESSORS JAVA JAVA_OPTS PATH R_LIBRARY_PATH TAG STATIC_OPTS PARSE_OPTS R_LIBS_SITE R_INSTRUMENTED R_TIMED R_2_12 R_2_13

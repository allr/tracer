#!/bin/sh

source /opt/r/tracer/env.sh

[ -z "$DIR" ] && DIR=$RESULT_DIR/$TAG/parse
makedir $DIR

while read i ; do
	echo $i | tee -a $RESULT_DIR/$TAG/process-trace.log
	TRACE_FILE="$TRACEDIR/$TAG/$i/trace.gz"
	if [ -z "$SOURCE_MAP" ] ; then
		if [ -f "`dirname $TRACE_FILE`/source.map.gz.patch" ] ; then
			SOURCE_MAP="--map-file-name source.map.gz.patch"
		else if [ -f "`dirname $TRACE_FILE`/source.map.patch" ] ; then
			SOURCE_MAP="--map-file-name source.map.patch"
		else if [ -f "`dirname $TRACE_FILE`/source.map.gz" ] ; then
			SOURCE_MAP="--map-file-name source.map.gz"
		else if [ -f "`dirname $TRACE_FILE`/source.map" ] ; then
			SOURCE_MAP="--map-file-name source.map"
		fi fi fi fi
	fi
	RESULT_FILE=$i
	timed_java_exec $DIR/process-time.log org.rx.rtrace.RTrace --db $RESULT_DIR/$TAG/ $SOURCE_MAP $OPTS $TRACE_FILE $* 2>&1 | tee -a $DIR/process.log
	# The '/' after DIR is not mandatory, but it seems more easy to not forget if we copy paste this line & the dir !exists
done 

#!/bin/sh
source /opt/r/tracer/env.sh

[ -z "$DIR" ] && DIR=$RESULT_DIR/$TAG/static
makedir $DIR

while read f ; do
	i=$TRACEDIR/$TAG/$f
	echo $f | tee -a $RESULT_DIR/$TAG/process-static.log
	cat "$i/alldeps" |xargs java_exec org.rx.analyser.AnalyseR -q --outdir $DIR --prefix $f $*
done

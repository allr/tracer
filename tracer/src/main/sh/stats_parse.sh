#!/bin/sh
source /opt/r/tracer/env.sh

[ "$1" ] && TAG=$1

qalc `cat $RESULT_DIR/$TAG/parse/process.log | grep 'Java Error' | wc -l` / `cat $RESULT_DIR/$TAG/process-trace.log | wc -l`

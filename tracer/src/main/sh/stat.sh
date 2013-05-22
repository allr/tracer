#!/bin/sh

DIR="$RESULT_DIR/$TAG"
[ "$1" ] && DIR=$1

PROCEED="`grep -e \\.gz$ $DIR/process-trace.log | wc -l`"
MEM="`grep -e ^Out $DIR/process-trace.log | wc -l`"
ERR="`grep -B2 -e ^Expected $DIR/process-trace.log | grep -e gz$ | wc -l`"

div () {
	echo "scale=3; $1/$2*100 "| bc -q
}

echo "Parsed: $PROCEED Out of Memory $MEM (`div $MEM $PROCEED`%) Parse error $ERR (`div $ERR $PROCEED`%)"
grep process- $DIR/process-time.log 

#qalc "` cat $DIR/parse/process.log | grep '^Java' | wc -l ` / (` ls $DIR/parse/ | wc -l ` / 2 - 1) * 100"

#!/bin/sh

source /opt/r/tracer/env.sh

[ -z "$DIR" ] && DIR=$RESULT_DIR/$TAG/size
makedir $DIR

while read i ; do
	MAP_FILE="$TRACEDIR/$TAG/$i/source.map"

	echo $MAP_FILE
	cat $MAP_FILE | cut -d\  -f 3,4,6  | sort -u |awk '{ i += strtonum($3) - strtonum($2) + 1; } END { print i;}' | tee $DIR/$i
done

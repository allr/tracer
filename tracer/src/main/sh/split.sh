#!/bin/bash
NB=5
FILE="part"

[ "$1" ] && NB="$1"
[ "$2" ] && FILE="$2"

i=0
while expr $i '<' $NB > /dev/null; do
	rm -f "$FILE.$i"
	i=$((i+1))
done
while read v ; do
	echo $v >> "$FILE.`expr $i % $NB`"
	i=$((i+1))
done

#!/bin/sh
[ "$1" = ls ] && shift || MASK=0x7F
[ "$1" ] && MASK="$1" && shift

for i in `ps -A | sed 's/^ *//' | tail -n +2 | cut '-d '  -f 1` ; do
	sudo taskset -p $MASK $i 
done

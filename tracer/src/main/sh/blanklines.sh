#!/bin/sh
[ "$1" == "-v" ] && GOPTS="-v" && shift

exec egrep $GOPTS '^\s*(#|$)' $* 

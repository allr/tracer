#!/bin/sh
source /opt/r/tracer/env.sh

SRC="/home/bghill/R-Library/Bioc-2.7-tgz"
[ "$1" ] && SRC="$1"

DST="$BIOCSOURCE"

makedir $DST
find $SRC -name '*.tar.gz' > $DST/archives.tmp
find $SRC -name '*.tar.gz' -exec tar xvzf {} --ignore-command-error -C $DST --wildcards '*.R' '*.r' '*.q' '*.s' '*.S' '*.Q' '*.h' '*.hpp' '*.hh' '*.hxx' '*.h++' '*.c' '*.C' '*.cc' '*.cpp' '*.cxx' '*.c++' '*.f' '*.f90' '*.for' '*.F' '*.java'  '*.py' \; > $DST/extracted.tmp 

#!/bin/sh
R=~morandat/src/R-2.13.1/bin/R
TIME=/usr/bin/time
R_LIB="~morandat/src/R-2.13.1/library"
OUTPUT=/dev/null

export R_LIBRARY_PATH="$R_LIB" R_LIBS_SITE="$R_LIB"

[ -z "$1" ] && echo "Syntax: need file to bench." && exit 1
F=$1
shift

echo "Compiling $F ..."
$TIME -o $F.compiler.time $R --slave --no-save --no-restore -f - --args $* <<EOF
library("compiler", verbose=TRUE, lib.loc="$R_LIB")
cmpfile("$F", "${F}c")
EOF

echo "Benching source ..."
$TIME -o $F.base.time $R --slave --no-save --no-restore -f - --args $*  > $OUTPUT <<EOF
library("compiler", verbose=TRUE, lib.loc="$R_LIB")
source("$F")
EOF
echo "Benching compiled ..."
$TIME -o $F.compiled.time $R --slave --no-save --no-restore -f - --args $* > $OUTPUT << EOF
library("compiler", verbose=TRUE, lib.loc="$R_LIB")
loadcmp("${F}c")
EOF

echo =-===== $F =====-=
head -n 1 $F.compiler.time 
echo ------------------------
head -n 1 $F.base.time 
head -n 1 $F.compiled.time 
echo ------------------------
echo -n 'Gain (in %) : '
qalc -t \( `head -n 1 $F.base.time | sed "s/user.*$//"` - `head -n 1 $F.compiled.time | sed "s/user.*$//"` \) / `head -n 1 $F.base.time | sed "s/user.*$//"` \* 100 
chmod g+rw $F.compiler.time $F.base.time $F.compiled.time ${F}c 
chgrp r $F.compiler.time $F.base.time $F.compiled.time ${F}c

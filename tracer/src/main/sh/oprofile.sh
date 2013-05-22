#!/bin/bash

source /opt/r/tracer/env.sh
[ -z "$R_CMD" ] && R_CMD="/opt/r/r-timed/bin/R"

DIR="$RESULT_DIR/$TAG"
makedir $DIR

echo $DIR

[ -z "$USER" ] && USER=`whoami`
[ -z "$WRAPPER" ] && WRAPPER="taskset -c 7"

while read i
do
  v="`echo $i |cut '-d ' -f 1`"
  p="`echo $i |cut '-d ' -s -f 2-`"
  DNAME=`dirname $v`
  PNAME=`vignette_name $v`
  FNAME=`basename $v`

  pushd $BIOCDIR/$DNAME
  # run each vignette 
  [ -f "$SCRIPTDIR/action/$PNAME.start" ] && source "$SCRIPTDIR/action/$PNAME.start"

	opcontrol --reset
	opcontrol --start
	$WRAPPER sudo -u morandat $R_CMD --no-restore --no-save --slave -f $FNAME --args $p 2>&1| sudo -u morandat tee $DIR/$PNAME.out 
	opcontrol --stop
	opcontrol --dump 
	opreport --session-dir=/tmp/prof -l | sudo -u morandat tee $DIR/$PNAME.oprof.l
	opreport --session-dir=/tmp/prof --callgraph | sudo -u morandat tee  $DIR/$PNAME.oprof.cg

  [ -f "$SCRIPTDIR/action/$PNAME.stop" ] && source "$SCRIPTDIR/action/$PNAME.stop"
  #/usr/bin/time -o $DIR/\$PNAME.stime $OLDWRAPPER $R_CMD --time=$DIR/\$PNAME.time -f \$FNAME 2>&1| tee $DIR/\$PNAME.out 
  popd > /dev/null
done

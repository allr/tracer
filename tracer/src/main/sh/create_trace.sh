#!/bin/sh

source /opt/r/tracer/env.sh

[ -z "$TRACETYPE" ] && TRACETYPE=all
[ -z "$DIR" ] && DIR=$RESULT_DIR/$TAG/trace
makedir $DIR
makedir $TRACEDIR/$TAG

while read i
do
	echo $i | tee -a $RESULT_DIR/$TAG/create-trace.log
	v="`echo $i |cut '-d ' -f 1`"
	p="`echo $i |cut '-d ' -s -f 2-`"
	DNAME=$(dirname $v)
	FNAME=$(basename $v)
	PNAME=`vignette_name $v`
	makedir $TRACEDIR/$TAG/$PNAME
	pushd $BIOCDIR/$DNAME
	# run each vignette 
	[ -f "$SCRIPTDIR/action/$PNAME.start" ] && source "$SCRIPTDIR/action/$PNAME.start"
	timed_command $DIR/create-time.log $R_INSTRUMENTED --no-restore --no-save --slave --trace $TRACETYPE --tracedir $TRACEDIR/$TAG/$PNAME -f $FNAME --args $p 2>&1 | tee -a $TRACEDIR/$TAG/$PNAME/r.out
	# sh -c "sleep 5s; gzip $TRACEDIR/$TAG/$PNAME/trace; gzip $TRACEDIR/$TAG/$PNAME/source.map" &
	[ -f "$SCRIPTDIR/action/$PNAME.stop" ] && source "$SCRIPTDIR/action/$PNAME.stop"
	popd	
done 
wait

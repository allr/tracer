#!/bin/bash

source /opt/r/tracer/env.sh

[ -z "$R_CMD" ] && R_CMD="/opt/r/r-timed/bin/R"

mkdir -p $RESULT_DIR/$TAG

export R_LIBS_SITE
while read i
do
	v="`echo $i |cut '-d ' -f 1`"
	p="`echo $i |cut '-d ' -f 2-`"
  DNAME=`dirname $v`
  PNAME=`vignette_name $v`
  FNAME=`basename $v`
  pushd $BIOCDIR/$DNAME
  # run each vignette 
  [ -f "$SCRIPTDIR/action/$PNAME.start" ] && source "$SCRIPTDIR/action/$PNAME.start"
  $R_CMD --externalcalls=$RESULT_DIR/$TAG/$PNAME.external --time=$RESULT_DIR/$TAG/$PNAME.time --no-restore --no-save --slave -f $FNAME --args $p 2>&1| tee $RESULT_DIR/$TAG/$PNAME.ext.out 
  ( sleep 3 && rm -f $RESULT_DIR/$TAG/$PNAME.external.gz && gzip $RESULT_DIR/$TAG/$PNAME.external  ) &
  [ -f "$SCRIPTDIR/action/$PNAME.stop" ] && source "$SCRIPTDIR/action/$PNAME.stop"
  popd
done

wait

#!/bin/bash

source /opt/r/tracer/env.sh
[ -z "$R_CMD" ] && R_CMD="/opt/r/r-timed/bin/R"

DIR="$RESULT_DIR/$TAG"
makedir $DIR

[ -z "$USER" ] && USER=`whoami`

[ -z "$WRAPPER" ] && WRAPPER="taskset -c 7"
#[ -z "$WRAPPER" ] && WRAPPER="schedtool -a 7 -F -p 99  -e sudo -E -u $USER "

cp /opt/r/bioc-2.7/RCurl/libs/RCurl.so.time /opt/r/bioc-2.7/RCurl/libs/RCurl.so
chgrp r /opt/r/bioc-2.7/RCurl/libs/RCurl.so
DISPLAY=:9
export DISPLAY
xpra start $DISPLAY
#sudo bash -c "
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
  echo /usr/bin/time -o $DIR/$PNAME.stime $WRAPPER $R_CMD --time=$DIR/$PNAME.time --no-restore --no-save --slave -f $FNAME --args $p 
  /usr/bin/time -o $DIR/$PNAME.stime $WRAPPER $R_CMD --time=$DIR/$PNAME.time --no-restore --no-save --slave -f $FNAME --args $p 2>&1 > /dev/null | tee $DIR/$PNAME.err
  [ -f "$SCRIPTDIR/action/$PNAME.stop" ] && source "$SCRIPTDIR/action/$PNAME.stop"
  popd > /dev/null
done
#done" < $1
sleep 3
xpra stop $DISPLAY

#!/bin/bash

# NOTE: Update this for your installation!
BASEDIR="$HOME/some/dir/"

# prepare tracer default configuration
TRACER_CONFIG="$HOME/.tracer/default.prof"

mkdir -p $(dirname $TRACER_CONFIG)
echo "r.instrumented.path=$BASEDIR/instrumented/bin/R" > $TRACER_CONFIG
echo "r.timed.path=$BASEDIR/timed/bin/R" >> $TRACER_CONFIG

TRACER_JAR="$BASEDIR/tracer.jar"
TRACER="java -jar $TRACER_JAR"
SQLITE3="sqlite3"

SOURCE="$*"

if [ -z "$SOURCE" ]
then
  echo "$0 <source file> <args>"
  exit 1
fi

SOURCE=$1
shift 1

# set up names
  TRACENAME=$(basename $SOURCE .R)
  TASKNAME=$TRACENAME
  
# create the tracer task  
  $TRACER create-task $TASKNAME
  
# create the trace output directory  
  OUTPUT="traces/$TRACENAME"
  mkdir -p $OUTPUT
  
# add one trace to the task that was created  
  $TRACER add-trace $TASKNAME --name $TRACENAME --output-dir $OUTPUT $SOURCE $*
  
# run the created task using all available analysis options  
  $TRACER run-task $TASKNAME --force --trace-allcalls --trace-foreign --trace-promise --trace-hidden --trace-size --trace-recursive --analyze-keywords --analyze-recursive --analyze-tokens --analyze-resolv --analyze-class --analyze-promside
## --analyze-names

# install analysis views into the created database

if [ -d $BASEDIR/queries ]; then
    for f in $(ls $BASEDIR/queries/*.sql | sort)
    do
	$SQLITE3 db.db < $f
    done
fi

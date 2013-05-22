#!/bin/sh

source /opt/r/tracer/env.sh

EMPTY_SIZE=35
DIR="$TRACEDIR/$TAG"
[ "$1" ] && DIR=$1
FILTER_NAME='s:^.*/\([^/]*\)/trace.gz$:\1:'

ls -1dS $DIR/*/trace.gz | sed $FILTER_NAME |tee  $DIR/all.list | tac > $DIR/all-rev.list
ls -1dsS $DIR/*/trace.gz --block-size 1  | sed 's/^ *//'|grep -v "^$EMPTY_SIZE " | cut -f 2 "-d " |sed $FILTER_NAME | tee $DIR/nonempty.list | tac >  $DIR/nonempty-rev.list
ls -1dsS $DIR/*/trace.gz --block-size 1  | sed 's/^ *//'|grep "^$EMPTY_SIZE " | cut -f 2 "-d " |sed $FILTER_NAME > $DIR/empty.list
sort -R $DIR/nonempty.list | sort -R | sort -R > $DIR/random.list
#find $DIR -name 'source.map' -exec sh -c "(echo {} ; zcat {} | tail -n 2 |cut -d\   -f 5 | grep '^$') | grep -B1 '^$' | grep -v '^$' | sed 's:$DIR/\?::' | sed 's:/source.map:'" \; > $DIR/corrupted.map

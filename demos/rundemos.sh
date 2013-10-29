#!/bin/sh

# NOTE: Update this for your installation!
BASEDIR="$HOME/some/dir/"

if [ -e db.db ]; then
    echo ERROR: db.db file already exists, please remove before running!
    exit 2
fi

if [ -d traces ]; then
    echo ERROR: traces subdirectory already exists, please remove before running!
    exit 2
fi

for scr in $BASEDIR/demos/*.R; do
    $BASEDIR/tracer.sh $scr
done

$BASEDIR/scripts/plotall.sh

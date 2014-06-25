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
    $BASEDIR/tracer.pl $scr || exit 2
done

$BASEDIR/tracer.pl --autopivot --sqldir $BASEDIR/queries

$BASEDIR/scripts/plotall.sh

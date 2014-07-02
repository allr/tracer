#!/bin/bash

# NOTE: Update this for your installation!
BASEDIR="$HOME/some/dir/"
DATABASE=db.db
PLOTDIR=plots

if [ .$1 != . ]; then
    DATABASE=$1
    PLOTDIR=plots-$DATABASE
fi

PLOTTABLES_PCT="runtime_details_pct memory_used_pct vector_sizes_pct vector_counts"
PLOTTABLES_NORM="memory_used_vs_alloc total_runtimes"

mkdir -p $PLOTDIR

for table in $PLOTTABLES_PCT; do
    echo Generating plot for $table
    sqlite3 -csv -header $DATABASE "select * from $table" > $PLOTDIR/$table.csv
    $BASEDIR/scripts/plotcsv.pl --stacked --maxrange 100 $PLOTDIR/$table.csv $PLOTDIR/$table.pdf
done

for table in $PLOTTABLES_NORM; do
    echo Generating plot for $table
    sqlite3 -csv -header $DATABASE "select * from $table" > $PLOTDIR/$table.csv
    $BASEDIR/scripts/plotcsv.pl $PLOTDIR/$table.csv $PLOTDIR/$table.pdf
done

$BASEDIR/scripts/memoryplot.pl $DATABASE $PLOTDIR/memtime.pdf

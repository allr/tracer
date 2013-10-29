#!/bin/bash

# NOTE: Update this for your installation!
BASEDIR="$HOME/some/dir/"

PLOTTABLES_PCT="running_time_details_pct_new memory_used_pct_new vector_sizes vector_counts"
PLOTTABLES_NORM="memory_used_vs_alloc total_runtimes"

mkdir -p plots
for table in $PLOTTABLES_PCT; do
    echo Generating plot for $table
    sqlite3 -csv -header db.db "select * from $table" > plots/$table.csv
    $BASEDIR/timed/bin/Rscript $BASEDIR/scripts/plotcsv.R plots/$table.csv plots/$table.pdf pctstack
done

for table in $PLOTTABLES_NORM; do
    echo Generating plot for $table
    sqlite3 -csv -header db.db "select * from $table" > plots/$table.csv
    $BASEDIR/timed/bin/Rscript $BASEDIR/scripts/plotcsv.R plots/$table.csv plots/$table.pdf
done

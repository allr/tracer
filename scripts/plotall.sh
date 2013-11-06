#!/bin/bash

# NOTE: Update this for your installation!
BASEDIR="$HOME/some/dir/"

PLOTTABLES_PCT="runtime_details_pct memory_used_pct vector_sizes_pct vector_counts"
PLOTTABLES_NORM="memory_used_vs_alloc total_runtimes"

mkdir -p plots
for table in $PLOTTABLES_PCT; do
    echo Generating plot for $table
    sqlite3 -csv -header db.db "select * from $table" > plots/$table.csv
    $BASEDIR/scripts/plotcsv.pl --stacked --maxrange 100 plots/$table.csv plots/$table.pdf
done

for table in $PLOTTABLES_NORM; do
    echo Generating plot for $table
    sqlite3 -csv -header db.db "select * from $table" > plots/$table.csv
    $BASEDIR/scripts/plotcsv.pl plots/$table.csv plots/$table.pdf
done

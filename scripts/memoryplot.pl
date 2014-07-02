#!/usr/bin/env perl
#
# Warning: Quickly-hacked script ahead

use strict;
use warnings;
use feature ':5.10';
use DBI;
use Data::Dumper;
use POSIX;

sub avg {
    my $sum = 0;
    my $count = 0;

    while (@_) {
        $count += 1;
        $sum += shift;
    }

    if ($count == 0) {
        return 0;
    } else {
        return $sum / $count;
    }
}

sub max {
    my $m = shift;

    while (@_) {
        my $v = shift;
        $m = $v if $v > $m;
    }

    return $m;
}

sub fetch_memory_for_script {
    my $dbh      = shift;
    my $trace_id = shift;
    my @result;

    my $sqlres = $dbh->selectall_arrayref("SELECT time, memory FROM MemoryOverTime WHERE trace_id=? ORDER BY time", undef, $trace_id);

    foreach my $row (@$sqlres) {
        $result[$$row[0]] = $$row[1];
    }

    return \@result;
}

sub plot_memory {
    my $plotfh     = shift;
    my $data       = shift;
    my $scriptname = shift;

    say $plotfh "set title \"memory over time for $scriptname\"";

    # build plot command
    my $flag = 0;
    my $i = 1;

    print $plotfh "plot ";

    if (scalar(@$data) < 10) {
        # add points on the lines if very few data points are available
        print $plotfh "'-' using 1:2 with linespoints ls $i notitle";
    } else {
        print $plotfh "'-' using 1:2 with lines ls $i notitle";
    }

    # peak
    printf $plotfh ", %.2f with lines ls $i notitle", max(@$data)/1024.0/1024.0;

    # average
    printf $plotfh ", %.2f with dots ls $i notitle\n", avg(@$data)/1024.0/1024.0;

    # output data blocks
    for (my $i = 0; $i < scalar(@$data); $i++) {
        printf $plotfh "%d, %.2f\n", $i, $$data[$i]/1024.0/1024.0;
    }
    say $plotfh "EOF";
}

# -------------

if (scalar(@ARGV) != 2) {
    say "Usage: $0 database pdfoutput";
    say "         creates a PDF plot of all memory curves from the given database";
    exit 1;
}

my $DBFile  = $ARGV[0];
my $pdffile = $ARGV[1];

# connect to database
my $dbh = DBI->connect("DBI:SQLite:dbname=$DBFile", "", "",
    { RaiseError => 1 } );
$dbh->do("PRAGMA foreign_keys = ON");

my $plotfh;
open $plotfh, "|-", "gnuplot" or die "ERROR: Failed to run gnuplot: $!";

# FIXME: Label assumes that the time quantum is 1
print $plotfh <<EOT;
set terminal pdf size 20cm,15cm
set output "$pdffile"
set datafile separator ","
set xlabel "time [s]"
set ylabel "allocated memory [MiB]"
set boxwidth 0.9
set key off
EOT
    
my $res = $dbh->selectall_arrayref("SELECT id, name FROM Traces ORDER BY name");

foreach my $row (@$res) {
    my $data  = fetch_memory_for_script($dbh, $$row[0]);

    plot_memory($plotfh, $data, "$$row[1] ($$row[0])");
}

close $plotfh;

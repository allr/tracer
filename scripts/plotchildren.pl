#!/usr/bin/env perl

use strict;
use warnings;
use feature ':5.10';
use Data::Dumper;
use DBI;
use POSIX;

#my @RGB_GRADIENT_NODES = ([0,0,255], [0,255,0], [255,255,0], [255,0,0]);
my @RGB_GRADIENT_NODES = ([255,0,0], [240,240,0], [0,255,0]);

my %fieldmapping = (
    "EndTimeUsec"   => "end_time",
    "StartTimeUsec" => "start_time",
    "UserTime"      => "user_time",
    "SystemTime"    => "system_time",
    "Hostname"      => "hostname",
    );

my $verbose = 0;

sub linear_rgb_gradient {
    my $percentage = shift;

    if ($percentage < 0 || $percentage > 1) {
        say STDERR "WARNING: Invalid percentage $percentage" if $percentage < 0.01 || $percentage > 1.01;
        $percentage = 0 if $percentage < 0;
        $percentage = 1 if $percentage > 1;
    }

    my $groupsize = 1.0 / (scalar(@RGB_GRADIENT_NODES) - 1);
    my $group = floor($percentage / $groupsize);
    $group-- if $percentage == 1; # FIXME: HACK

    my $left  = $RGB_GRADIENT_NODES[$group];
    my $right = $RGB_GRADIENT_NODES[$group + 1];
    my $remaining_pct = ($percentage - $group * $groupsize) / $groupsize;

    my @resultcolor;

    for (my $i = 0; $i < scalar(@$left); $i++) {
        push @resultcolor, floor($left->[$i] * (1 - $remaining_pct) + $right->[$i] * $remaining_pct);
    }

    return $resultcolor[0] * 256 * 256 +
        $resultcolor[1] * 256 +
        $resultcolor[2]
}

sub fetch_memory {
    my $dbh      = shift;
    my $trace_id = shift;
    my $table    = shift;
    my $quantum  = shift;
    my @result;

    # fetch time factor
    my $sqlres = $dbh->selectall_arrayref("SELECT value FROM TraceResults WHERE trace_id=? and key=?", undef, $trace_id, $quantum);
    if (@$sqlres > 0) {
        # avoid warning if no freemem data is available
        $quantum = 0 + $$sqlres[0][0];
    }

    # fetch data
    $sqlres = $dbh->selectall_arrayref("SELECT time, memory FROM $table WHERE trace_id=? ORDER BY time", undef, $trace_id);

    foreach my $row (@$sqlres) {
        # replicate data to second-resolution, simplifies merging
        for (my $i = 0; $i < $quantum; $i++) {
            $result[$$row[0] * $quantum + $i] = $$row[1];
        }
    }

    return \@result;
}

# FIXME: function only used once
sub fetch_rundata {
    # FIXME: This is a really bad way to handle database queries
    # (the child query is a bit better...)
    my $dbh = shift;
    my $id  = shift;
    my %result;

    my $res = $dbh->selectall_arrayref("SELECT name FROM Traces WHERE id=?", undef, $id);
    $result{name} = $$res[0][0];
    $result{id}   = $id;

    $res = $dbh->selectall_arrayref("SELECT key,value FROM TraceResults WHERE trace_id=? AND key IN ('" . join("','", keys %fieldmapping) . "') ORDER BY key", undef, $id);

    foreach my $row (@$res) {
        if (!exists($fieldmapping{$$row[0]})) {
            die "Got unknown field $$row[0] from database?!";
        }
        $result{$fieldmapping{$$row[0]}} = $$row[1];
    }

    # fetch idletimes
    # FIXME: Must be skipped if the table does not exist!
    #$result{idletimes} ||= [];
    #$res = $dbh->selectall_arrayref("SELECT start, end from IdleTimes WHERE trace_id=? ORDER BY num", undef, $id);
    #foreach my $row (@$res) {
    #    push @{$result{idletimes}}, [$$row[0], $$row[1]];
    #}

    return \%result;
}


# -------------

if (scalar(@ARGV) != 2 && scalar(@ARGV) != 3) {
    say "Usage: $0 [-v] database pdfoutput";
    say "         creates a PDF plot of all memory curves from the given database";
    exit 1;
}

if ($ARGV[0] eq "-v") {
    $verbose = 1;
    shift;
}

my $DBFile  = $ARGV[0];
my $pdffile = $ARGV[1];

# connect to database
my $dbh = DBI->connect("DBI:SQLite:dbname=$DBFile", "", "",
    { RaiseError => 1 } );
$dbh->do("PRAGMA foreign_keys = ON");

# get a list of traces that have children
my $parentres = $dbh->selectall_arrayref("SELECT DISTINCT parent_id FROM Traces WHERE parent_id IS NOT NULL ORDER BY parent_id");

if (scalar(@$parentres) == 0) {
    say "No traces with child processes found, exiting";
    exit 1;
}

my $plotfh;
open $plotfh, "|-", "gnuplot" or die "ERROR: Failed to run gnuplot: $!";
#open $plotfh, ">", "gnuplot.txt" or die "ERROR: Failed to run gnuplot: $!";

print $plotfh <<EOT;
set terminal pdf size 20cm,15cm
set output "$pdffile"
set style arrow 1 lw 2 nohead
set style arrow 2 lw 2 nohead
EOT

foreach my $pidref (@$parentres) {
    print $plotfh <<EOT;
set ylabel "processes"
set y2label "memory [MB]"
set xtics font ",10"
set y2tics font ",9"
unset ytics
set autoscale y2
unset label
EOT

    # get data for parent
    my $parent_id = $$pidref[0];
    say $parent_id;
    my %parentdata = %{fetch_rundata($dbh, $parent_id)};

    my @parent_memory = @{fetch_memory($dbh, $parent_id, "MemoryOverTime", "MallocmeasureQuantum")};
    my @parent_free   = @{fetch_memory($dbh, $parent_id, "FreeMemoryOverTime", "FreememQuantum")};

    # get children data
    # FIXME: This is a bit hack-ish because it creates each child data hash in multiple consecutive loops,
    #        based on the (sort-guranteed) result order
    # Note: This uses the timing information from r-instrumented to ensure it matches the memory data
    my $res = $dbh->selectall_arrayref("SELECT Traces.id,name,key,value FROM Traces JOIN TraceResults ON Traces.id=TraceResults.trace_id WHERE parent_id=? AND key IN ('" . join("','", keys %fieldmapping) . "') ORDER BY Traces.id,key", undef, $parent_id);
    my @childdata;
    my $prev_id = -1;
    my $curdata;
    foreach my $childref (@$res) {
        if ($prev_id != $$childref[0]) {
            $prev_id = $$childref[0];
            push @childdata, $curdata if defined($curdata);
            $curdata = {"id"   => $$childref[0],
                        "name" => $$childref[1]};
        }

        if (!exists($fieldmapping{$$childref[2]})) {
            die "Got unknown field $$childref[2] from database?!";
        }
        $curdata->{$fieldmapping{$$childref[2]}} = $$childref[3];
    }
    push @childdata, $curdata if defined($curdata);

    my %hosts;

    foreach my $child (@childdata) {
        $child->{"memory"}  = fetch_memory($dbh, $child->{"id"}, "MemoryOverTime", "MallocmeasureQuantum");
        $child->{"freemem"} = fetch_memory($dbh, $child->{"id"}, "FreeMemoryOverTime", "FreememQuantum");
        $hosts{$child->{"hostname"}} ||= 0;
        $hosts{$child->{"hostname"}}  += 1;
    }

    my $hostcount = scalar(keys %hosts);

    # make times relative to parent start
    my $parent_start = $parentdata{start_time};
    $parentdata{start_time} = 0;
    $parentdata{end_time}  -= $parent_start;
    my $parent_util = ($parentdata{user_time} + $parentdata{system_time}) / ($parentdata{end_time} / 1e6);
    #foreach my $it (@{$parentdata{idletimes}}) {
    #    $it->[0] -= $parent_start;
    #    $it->[1] -= $parent_start;
    #}

    for (my $i = 0; $i < @childdata; $i++) {
        my $child = $childdata[$i];
        $child->{start_time} -= $parent_start;
        $child->{end_time}   -= $parent_start;
        $child->{line}        = $i + 2;
        $child->{offset}      = 0;

        my $childtime = ($child->{end_time} - $child->{start_time}) / 1e6;
        $child->{utilization} = ($child->{user_time} + $child->{system_time}) / $childtime;
    }

    # sort children by start time
    @childdata = sort { $a->{start_time} <=> $b->{start_time} } @childdata;

    # prepare multiplot
    say $plotfh "set multiplot layout $hostcount, 1 title \"$parentdata{name}\" font \",14\"";

    # plot one diagram per host
    my @hostlist = sort keys %hosts;
    for (my $i = 0; $i < scalar(keys %hosts); $i++) {
        my $host = $hostlist[$i];

        if ($i == 0) {
            say $plotfh "set key on at screen 0.8, screen 0.95 center";
        } else {
            say $plotfh "set key off";
        }

        if ($i < scalar(keys %hosts)-1) {
            say $plotfh "unset xlabel";
        } else {
            say $plotfh "set xlabel \"time [s]\"";
        }

        # sum memory
        my @free_memory;
        my @memory_sum;
        if ($host eq $parentdata{hostname}) {
            @memory_sum  = @parent_memory;
            @free_memory = @parent_free;
        } else {
            @memory_sum  = (0) x scalar(@parent_memory);
            @free_memory = (-99999999) x scalar(@parent_free); # must be large because it's scaled later
        }
        my @current_children;
        foreach my $child (@childdata) {
            next if $child->{hostname} ne $host;
            push @current_children, $child;

            say "Processing child id $child->{id}" if $verbose;

            my $offset = floor($child->{start_time} / 1e6);
            my $warnings = 0;

            for (my $i = 0; $i < scalar(@{$child->{"memory"}}); $i++) {
                if ($i + $offset >= @memory_sum) {
                    if ($warnings == 0) {
                        say "WARNING: child data beyond parent (",scalar(@memory_sum),") at $i + $offset (upto ", scalar(@{$child->{"memory"}}), ")";
                        say "  start $child->{start_time} end $child->{end_time} pend $parentdata{end_time}";
                    }
                    $warnings++;
                }
                $memory_sum[$i + $offset] += $child->{"memory"}->[$i];
            }

            if ($warnings) {
                say "(dropped $warnings data points)";
            }

            for (my $i = 0; $i < scalar(@{$child->{"freemem"}}); $i++) {
                $free_memory[$i + $offset] = $child->{"freemem"}->[$i];
            }
        }
        my $childcount = scalar(@current_children);

        # plot data
        my $linecount  = 0;
        my (@limit_low, @limit_high, @childs_in_line);

        say "Forwarding data to gnuplot" if $verbose;

        say $plotfh "set title \"$host\"";

        if (0) {
            # pack multiple children on a line
            # This only works "gap-free" if the child process data is sorted by start time,
            # but due to the way the data is captured this should be true for now.

            foreach my $child (@current_children) {
                my $found = 0;
                for (my $i = 0; $i < $linecount; $i++) {
                    # search for a line that does not overlap the current child
                    # (extended: search for the line with maximum end time -> minimal distance)
                    if ($child->{start_time} > ($limit_high[$i] - 20000) || # FIXME: Fudge factor
                        $child->{end_time}   < $limit_low[$i]) {
                        $child->{line} = $i + 2; # 1-based, first is parent
                        if ((++$childs_in_line[$i]) % 2) {
                            $child->{offset} = 1;
                        } else {
                            $child->{offset} = -1;
                        }
                        $found = 1;
                        $limit_low[$i]  = $child->{start_time} if $child->{start_time} < $limit_low[$i];
                        $limit_high[$i] = $child->{end_time}   if $child->{end_time} > $limit_high[$i];
                        last;
                    }
                }

                if (!$found) {
                    # open a new line
                    $linecount++;
                    push @limit_low, $child->{start_time};
                    push @limit_high, $child->{end_time};
                    push @childs_in_line, 1;
                    $child->{offset} = 1;
                    $child->{line}   = $linecount + 1;
                }
                $child->{name} = sprintf("Group %d", $child->{line} - 1);
            }

            # generate new Y labels for child groups
            print $plotfh "set ytics (\"Master\" -1";
            for (my $i = 1; $i <= $linecount; $i++) {
                printf $plotfh ",\"Group %d\" %d", $i, -$i-1;
            }
            say $plotfh ")";

            printf $plotfh "plot [0:%d] [-%d:0] '-' using (\$3/1e6):(-\$2 - 0.1 * \$5):((\$4 - \$3)/1e6):(0):6 axes x1y1 notitle with vectors nohead lw 3 lc rgb variable, '-' using 1:2 axes x1y2 title \"Used memory\" with lines lw 2 lc rgb 0, '-' using 1:(\$2 < 0 ? NaN : \$2) axes x1y2 title \"Free memory\" with lines lc rgb 255\n",
            $parentdata{end_time} / 1e6 * 1.02, $linecount + 2;

        } else {
            printf $plotfh "plot [0:%d] [-%d:0] '-' using (\$3/1e6):(-\$0 - 1):((\$4 - \$3)/1e6):(0):6 axes x1y1 notitle with vectors nohead lw 3 lc rgb variable, '-' using 1:2 axes x1y2 title \"Used memory\" with lines lc rgb 0, '-' using 1:(\$2 < 0 ? NaN : \$2) axes x1y2 title \"Free memory\" with lines lc rgb 255\n",
            $parentdata{end_time} / 1e6 * 1.02, $childcount + 2;
        }

        say $plotfh "Master 1 $parentdata{start_time} $parentdata{end_time} 0 ",linear_rgb_gradient($parent_util)
            if $host eq $parentdata{hostname};
        # plot idletimes on top of parent data
        #foreach my $it (@{$parentdata{idletimes}}) {
        #    say $plotfh "Master-idle 1 $$it[0] $$it[1] 0 65280"; # pure green
        #}

        foreach my $child (@current_children) {
            # FIXME: Strip parent name prefix from child name
            say $plotfh "\"$child->{name}\" $child->{line} $child->{start_time} $child->{end_time} $child->{offset} ",
            linear_rgb_gradient($child->{utilization});
        }

        say $plotfh "e";

        # now dump memory data to gnuplot
        for (my $i = 0; $i < @memory_sum; $i++) {
            printf $plotfh "%d %.2f\n", $i, $memory_sum[$i] / 1024.0 / 1024.0;
        }
        say $plotfh "e";

        # and finally free memory data
        for (my $i = 0; $i < @free_memory; $i++) {
            printf $plotfh "%d %.2f\n", $i, $free_memory[$i] / 1024.0;
        }
        say $plotfh "e";
    }

    # plot the color key
    print $plotfh <<EOI;
set origin 0.05,0.9
set size 0.3,0.1
set key off
unset xlabel
unset ylabel
unset y2label
unset xtics
unset ytics
unset y2tics
unset title
set style fill solid 1.0
set label "CPU utilization" at graph 0.5, graph 0.5 center front
set label "low" at graph 0.02, graph 0.5 left front
set label "high" at graph 0.98, graph 0.5 right front
plot [] [0:1] '-' using 0:(1):1 with boxes lc rgb variable
EOI
;
    #
    for (my $i = 0; $i < 100; $i++) {
        say $plotfh linear_rgb_gradient($i / 100.0);
    }
    say $plotfh "e";

}

close $plotfh;

$dbh->disconnect();

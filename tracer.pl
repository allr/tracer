#!/usr/bin/env perl
#
# traceR: Frontend for timeR and r-instrumented
# Copyright (C) 2014  TU Dortmund Informatik LS XII
# Inspired by r-timed from the Reactor group at Purdue,
#   http://r.cs.purdue.edu/
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, a copy is available at
# http://www.r-project.org/Licenses/
#

use warnings;
use strict;
use feature ':5.10';
use Cwd;
use DBI;
use Data::Dumper;
use File::Basename;
use File::Spec::Functions qw(catfile rel2abs file_name_is_absolute);
use FileHandle;
use FindBin qw($Bin);
use Getopt::Long qw(:config require_order);
use IPC::Open3;
use Pod::Usage;
use POSIX ':sys_wait_h';

use constant TracesTableVersion        => 1;
use constant TimingResultsTableVersion => 1;
use constant TraceResultsTableVersion  => 1;

use constant TimingResultsTable => "TimingResults";
use constant TraceResultsTable  => "TraceResults";

# globals
my ($TimeRBase, $RInstrBase);


### misc stuff ###

# DJB hash, used to checksum the column names for extra tables
sub string_checksum {
    my $checksum = 5381;

    while (my $string = shift) {
        for (my $i = 0; $i < length($string); $i++) {
            $checksum = (0xffffffff & (33 * $checksum)) ^
                ord(substr($string, $i, 1));
        }
    }
    return $checksum;
}

# "mkdir -p"
sub create_dirs {
    my $dir     = shift;
    my @dirs    = File::Spec->splitdir($dir);
    my $lastdir = "";
    my $curdir;

    $lastdir = pop @dirs while $lastdir eq "" && scalar(@dirs) != 0;

    foreach my $d (@dirs) {
        if (!defined($curdir)) {
            # catdir(undef, $d) would result in "/$d"
            $curdir = File::Spec->catdir($d);
        } else {
            $curdir = File::Spec->catdir($curdir, $d);
        }
        mkdir $curdir; # ignore errors
    }

    if (!defined($curdir)) {
        return mkdir File::Spec->catdir($lastdir);
    } else {
        return mkdir File::Spec->catdir($curdir, $lastdir);
    }
}

# generic key=value config file reader
sub read_config {
    my $filename = shift;
    my $values   = shift;

    return unless -e $filename;

    open IN, "<", $filename or die "Can't open $filename for reading: $!";
    while (<IN>) {
        chomp; s/\r//;      # eliminate newlines
        s/^\s+//; s/\s+$//; # eliminate leading/trailing whitespace
        next if $_ eq "";   # skip empty lines
        next if /^#/;       # skip comment lines

        if (/^([^ \t]+)\s*=\s*(.*)$/) {
            $$values{lc $1} = $2;
        } else {
            # line is not empty, but does not contain a = either
            say STDERR "WARNING: Could not parse line $. of $filename";
        }
    }
    close IN;
}

sub get_config {
    my $key     = lc shift;
    my $default = shift;
    my $values  = shift;

    if (exists($$values{$key})) {
        my $res = $$values{$key};
        delete $$values{$key};
        return $res;
    } else {
        return $default;
    }
}

sub get_config_bool {
    my $key     = shift;
    my $default = shift;
    my $values  = shift;

    my $result  = lc(get_config($key, $default, $values));

    if ($result eq "0"     ||
        $result eq "false" ||
        $result eq "no"    ||
        $result eq "off") {
        return 0;
    } elsif ($result eq "1"    ||
             $result eq "true" ||
             $result eq "yes"  ||
             $result eq "on") {
        return 1;
    } else {
        say STDERR "WARNING: Invalid boolean value for key $key, using default $default";
        return $default;
    }
}
    

### interface to database ###

sub check_table_version {
    my $dbh    = shift;
    my $name   = shift;
    my $want   = shift;
    my $create = shift;

    my $result = $dbh->selectall_arrayref("SELECT version FROM schema_versions WHERE name = ?",
                                          undef, $name);
    if (scalar(@$result) == 0) {
        # no data, create table
        $dbh->do("CREATE TABLE $name ($create)");
        $dbh->do("INSERT INTO schema_versions (name, version) VALUES (?,?)",
                 undef, $name, $want);
    } else {
        # check version number
        if ($$result[0][0] != $want) {
            say STDERR "ERROR: Version number of table $name does not match!";
            say STDERR "       Please specify a different database or remove the current one.";
            exit 2;
        }
    }
}

sub create_tables {
    my $dbh = shift;

    # check if the database is empty
    my $sth = $dbh->table_info("", undef, "schema_versions", "TABLE");
    if (!$sth->fetch) {
        # create schema_version table
        # FIXME: Allow md5 checksums of table schemas?
        $dbh->do("CREATE TABLE schema_versions (name VARCHAR NOT NULL, version INTEGER NOT NULL)");
    }

    # check/create tables
    check_table_version($dbh, "Traces", TracesTableVersion,
                        "id INTEGER NOT NULL PRIMARY KEY," .
                        "scriptname VARCHAR NOT NULL," .
                        "args VARCHAR," .
                        "name VARCHAR," .
                        "tracedir VARCHAR," .
                        "tracetime INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP");

    check_table_version($dbh, TimingResultsTable, TimingResultsTableVersion,
                        "id INTEGER NOT NULL PRIMARY KEY," .
                        "trace_id INTEGER NOT NULL REFERENCES Traces(id)," .
                        "key VARCHAR NOT NULL," .
                        "value INTEGER NOT NULL");

    check_table_version($dbh, TraceResultsTable, TraceResultsTableVersion,
                        "id INTEGER NOT NULL PRIMARY KEY," .
                        "trace_id INTEGER NOT NULL REFERENCES Traces(id)," .
                        "key VARCHAR NOT NULL," .
                        "value INTEGER NOT NULL");
}

sub create_pivot {
    my $dbh            = shift;
    my $tablename      = shift;
    my $do_materialize = shift;

    my $pivotname = $tablename . "_pivot";
    my $sth;

    # remove old table/pivot
    $sth = $dbh->table_info("", undef, $pivotname, "TABLE");
    if ($sth->fetch) {
        say STDERR "NOTICE: Removing previous pivot table for $tablename";
        $dbh->do("DROP TABLE IF EXISTS $pivotname");
    }

    $sth = $dbh->table_info("", undef, $pivotname, "VIEW");
    if ($sth->fetch) {
        say STDERR "NOTICE: Removing previous pivot view for $tablename";
        $dbh->do("DROP VIEW IF EXISTS $pivotname");
    }

    # gather column names
    my @columns;

    $sth = $dbh->prepare("SELECT DISTINCT key FROM $tablename ORDER BY id");
    $sth->execute;
    while (my @row = $sth->fetchrow_array) {
        my $k = $row[0];
        if ($k =~ /^[a-zA-Z_0-9]+$/) {
            push @columns, $k;
        }
    }

    # generate select statement
    my $statement = "SELECT $tablename.trace_id, traces.name";
    foreach my $col (@columns) {
        $statement .= ",GROUP_CONCAT(CASE WHEN $tablename.key='$col' THEN $tablename.value ELSE NULL END) AS $col";
    }
    $statement .= " FROM $tablename LEFT JOIN traces ON $tablename.trace_id=traces.id " .
        "GROUP BY trace_id";

    # create table if materialized
    if ($do_materialize) {
        my $tabledef = "trace_id INTEGER NOT NULL PRIMARY KEY, name VARCHAR NOT NULL";
        foreach my $col (@columns) {
            $tabledef .= ", $col INTEGER";
        }
        $dbh->do("CREATE TABLE ${tablename}_pivot ($tabledef)");
    }

    # create view or populate table
    if ($do_materialize) {
        $statement = "INSERT INTO ${tablename}_pivot ".$statement;
    } else {
        $statement = "CREATE VIEW ${tablename}_pivot AS ".$statement;
    }

    $dbh->do($statement);
}


### interface to R interpreters ###

sub run_R {
    my $datadir = shift;
    my $interp_base = shift;
    my @args = (
        catfile($interp_base,"bin/R"),
        "--vanilla", # FIXME: Configurable?
        "--slave",
        @_);
    my $stdout     = FileHandle->new;
    my $stdoutfile = FileHandle->new;

    open $stdoutfile, ">", catfile($datadir, "output.txt") or die "Can't open output.txt: $!";

    say "Running ", join(" ", @args);

    my $pid = open3("<&", $stdout, 0, @args);

    do {
        my ($rin, $rout, $nfound);

        $rin = '';
        vec($rin, fileno($stdout), 1) = 1;

        $nfound = select($rout = $rin, undef, undef, undef);

        if ($nfound < 0) {
            say STDERR "Error while waiting for R interpreter:\n$!";
            exit 2;
        } elsif ($nfound > 0) {
            if (vec($rout, fileno($stdout), 1)) {
                my ($buf, $nbyte);

                $nbyte = sysread($stdout, $buf, 1024);
                if ($nbyte < 0) {
                    # Error
                    say STDERR "Error reading R interpreter output: $!";
                    goto LOOPEND; # can't "last" in do {}
                }
                print $stdoutfile $buf;
                print $buf;
            }
        } else {
            say STDERR "Huh, select returned with nfound=0?";
        }
      LOOPEND:
    } while (waitpid($pid, WNOHANG) == 0);

    waitpid $pid, 0; # wait for termination

    close $stdoutfile;
    close $stdout;
}

sub run_timeR {
    my $tracedir   = shift;
    my $scriptname = shift;
    my @scriptargs = @_;

    run_R($tracedir, $TimeRBase, "--timeR-raw=".catfile($tracedir, "script.time"),
          "--timeR-verbose", "-f", $scriptname, "--args", @scriptargs);
}

sub run_instrumented {
    my $tracedir   = shift;
    my $scriptname = shift;
    my @scriptargs = @_;

    run_R($tracedir, $RInstrBase, "--tracedir", $tracedir,
          "-f", $scriptname, "--args", @scriptargs);
}


# read a tab-separated data file from timeR/r-instrumented
sub read_datafile {
    my $filename = shift;
    my @labels = ();
    my %data;
    my @dataorder;
    my %tables;

    open IN, "<", $filename or die "Can't open $filename: $!";
    while (<IN>) {
        chomp;
        next if /^(#[^!]|\s+#)/;
        my @words = split /\t/, $_;

        if ($words[0] =~ /^#!(.*)$/) {
            # found a marker line
            if ($1 eq "LABEL") {
                # label marker
                # arguments: labels for the subelements of the following data line(s)
                @labels = @words;
                shift @labels;

            } elsif ($1 eq "TABLE") {
                # output table marker
                # arguments: field name, table name
                # must come after a label line that defines the column names
                if (exists($tables{$words[1]})) {
                    say STDERR "WARNING: Table for $words[1] redefined in $filename line $.";
                    next;
                }

                $tables{$words[1]} = {
                    tablename => $words[2],
                    columns   => [@labels], # forces a copy
                    data      => []
                };

            } else {
                # unknown marker
                say STDERR "WARNING: Unknown marker \"$1\" ignored in $filename line $.";
            }

        } else {
            # found a data line

            if (exists($tables{$words[0]})) {
                # data is for a seperate table
                my $table = $words[0];

                if (scalar(@{$tables{$table}{columns}}) != scalar(@words)-1) {
                    say STDERR "ERROR: Column count mismatch in $filename line $.";
                    say STDERR "       Expected ", scalar(@{$tables{$table}{columns}}),
                               " columns, found ", scalar(@words)-1;
                    #say join("/", @{$tables{$table}{columns}}); # Debug
                    #say join("/", @words); # Debug
                    exit 2;
                }

                shift @words;
                push @{$tables{$table}{data}}, \@words;

            } else {
                # data is a normal data line
                if (exists($data{$words[0]})) {
                    if ($words[0] =~ /^<\.Internal>:La_rs/) {
                        # workaround for two duplicate entries in names.c
                        $words[0] .= "!5";
                    } else {
                        say STDERR "WARNING: Ignoring duplicate entry $words[0] in $filename line $.";
                        next;
                    }
                }

                push @dataorder, $words[0];

                if (scalar(@words) == 1) {
                    # no data values, error
                    say STDERR "ERROR: No value found in $filename line $.";
                    exit 2;
                } elsif (scalar(@words) == 2) {
                    # one data value, no suffix
                    $data{$words[0]} = $words[1];
                } else {
                    # multiple data values, add as hash of arrays (labels+data)
                    if (scalar(@words) != scalar(@labels) + 1) {
                        # difference between label count and value count
                        say STDERR "ERROR: Incorrect number of values found in $filename line $.";
                        say STDERR "(expected ", scalar(@labels),", found ", scalar(@words)-1, ")";
                        #say join("/",@labels); # Debug
                        #say join("/",@words);  # Debug
                        exit 2;
                    }

                    # generate value-hash
                    my $key = shift @words;
                    my %e = (
                        "labels" => [@labels],
                        "values" => [@words]
                        );

                    $data{$key} = \%e;
                }
            }
        }
    }
    close IN;

    return {
        order       => \@dataorder,
        data        => \%data,
        extratables => \%tables
    };
}

sub insert_results {
    my $dbh      = shift;
    my $trace_id = shift;
    my $table    = shift;
    my $data     = shift;
    my @order = @{$data->{order}};

    $dbh->begin_work();
    my $sth = $dbh->prepare("INSERT INTO $table (trace_id, key, value) VALUES (?,?,?)");
    foreach (@order) {
        my $value = $data->{data}{$_};

        if (ref($value)) {
            # value with multiple fields
            my @keys;
            foreach my $k (@{$$value{labels}}) {
                push @keys, "${_}_$k";
            }

            $sth->bind_param_array(1, $trace_id);
            $sth->bind_param_array(2, \@keys);
            $sth->bind_param_array(3, $$value{values});
            $sth->execute_array({});
        
        } else {
            # value with a single field
            $sth->execute($trace_id, $_, $value);
        }
    }
    $dbh->commit();
}

sub insert_extratables {
    my $dbh       = shift;
    my $trace_id  = shift;
    my $tablehash = shift;

    foreach my $tab (keys %$tablehash) {
        my $tablename = $$tablehash{$tab}{tablename};
        my @columns   = @{$$tablehash{$tab}{columns}};
        my $data      = $$tablehash{$tab}{data};

        # check/create table
        my $tabledef = "id INTEGER PRIMARY KEY NOT NULL," .
            "trace_id INTEGER NOT NULL REFERENCES Traces(id)," .
            join(",", map { "$_ INTEGER" } @columns);

        check_table_version($dbh, $tablename,
                            string_checksum(@columns), $tabledef);

        # insert data
        $dbh->begin_work();
        my $sth = $dbh->prepare("INSERT INTO $tablename (trace_id," .
                                join(",", @columns) . ") VALUES (?" .
                                (",?" x scalar(@columns)) . ")");

        foreach (@$data) {
            $sth->execute($trace_id, @$_);
        }            

        $dbh->commit();
    }
}


### main ###

# read config file
my %config_values;

read_config(catfile($ENV{HOME}, ".tracer.conf"), \%config_values);
read_config("tracer.conf", \%config_values);

$TimeRBase    = get_config("TimeRBase", catfile($Bin, "timed"), \%config_values);
$RInstrBase   = get_config("RInstrumentedBase", catfile($Bin, "instrumented"), \%config_values);
my $TraceBase = get_config("TracesPrefix", "traces", \%config_values); 
my $DBFile    = get_config("Database",  "db.db",  \%config_values);
my $SQLiteCmd = get_config("sqlite_command", "sqlite3", \%config_values);
my $sqldir    = get_config("sqldir", undef, \%config_values);
my $do_autopivot   = get_config_bool("AutoPivot", 0, \%config_values);
my $do_materialize = get_config_bool("MaterializePivots", 0, \%config_values);


if (scalar(keys %config_values) != 0) {
    say "WARNING: Unknown entries found in configuration file(s):";
    foreach (sort keys %config_values) {
        say "  $_";
    }
}

$TraceBase = rel2abs($TraceBase) unless file_name_is_absolute($TraceBase);
$DBFile    = rel2abs($DBFile)    unless file_name_is_absolute($DBFile);
# use relative to this script unless the directory is already accessible
$sqldir    = catfile($Bin, $sqldir) unless defined($sqldir) && -d $sqldir;

# parse the command line options
my $scriptname;
my @scriptargs;
my $tracename;
my $show_help = 0;
my $tracedir;
my $skip_sql  = 0;

GetOptions(
    "help"                => \$show_help,
    "name=s"              => \$tracename,
    "database|db=s"       => \$DBFile,
    "tracedir=s"          => \$tracedir,
    "prefix=s"            => \$TraceBase,
    "autopivot!"          => \$do_autopivot,
    "materialize-pivots!" => \$do_materialize,
    "sqldir=s"            => \$sqldir,
    "skip-sql"            => \$skip_sql,
    ) or pod2usage(1);

pod2usage(0) if $show_help || (scalar(@ARGV) == 0 && !$do_autopivot &&
                               !(defined($sqldir) && !$skip_sql));

if (scalar(@ARGV) != 0) {
    $scriptname  = shift @ARGV;
    @scriptargs  = @ARGV;
    $tracename ||= basename($scriptname, qw/.r .R/);
}

# connect to database
my $dbh = DBI->connect("DBI:SQLite:dbname=$DBFile", "", "",
    { RaiseError => 1 } );
$dbh->do("PRAGMA foreign_keys = ON");

# prepare database
create_tables($dbh);


if (defined($scriptname)) {
    # create directory for trace files
    if (defined($tracedir)) {
        # user-specified tracedir name, must not exist
        $tracedir = rel2abs($tracedir);

        if (!create_dirs($tracedir)) {
            say STDERR "ERROR: Unable to create trace directory: $!";
            exit 2;
        }
    } else {
        # auto-generated tracedir name, try to find an alternative if it exists
        $tracedir = catfile($TraceBase, $tracename) unless defined($tracedir);

        if (-e $tracedir || !create_dirs($tracedir)) {
            my $tmp;

            # try a few suffixes to find an unused directory
            for (my $i = 0; $i < 100; $i++) {
                $tmp = $tracedir . "-$i";
                if (! -e $tmp) {
                    # retry if the creation fails
                    next unless create_dirs($tmp);

                    $tracedir = $tmp;
                    $tmp = undef;
                    last;
                }
            }

            if (defined($tmp)) {
                say STDERR "ERROR: Could not generate an unused trace directory name!";
                exit 2;
            }
        }
    }

    say "Saving trace data to $tracedir";

    # run with timeR
    run_timeR($tracedir, $scriptname, @scriptargs);

    # run with r-instrumented
    run_instrumented($tracedir, $scriptname, @scriptargs);

    # read data files
    my $timingdata = read_datafile(catfile($tracedir, "script.time"));
    my $tracedata  = read_datafile(catfile($tracedir, "trace_summary"));

    # create trace information entry
    $dbh->do("INSERT INTO Traces (scriptname, args, name, tracedir) VALUES (?,?,?,?)",
             undef, $scriptname, join(" ", @scriptargs), $tracename, $tracedir);

    my $trace_id = $dbh->sqlite_last_insert_rowid();

    # put base results into database
    insert_results($dbh, $trace_id, TimingResultsTable, $timingdata);
    insert_results($dbh, $trace_id, TraceResultsTable,  $tracedata);

    # put extra tables into database
    insert_extratables($dbh, $trace_id, $timingdata->{extratables});
    insert_extratables($dbh, $trace_id, $tracedata->{extratables});
}

# create pivot tables
if ($do_autopivot) {
    create_pivot($dbh, TimingResultsTable, $do_materialize);
    create_pivot($dbh, TraceResultsTable,  $do_materialize);
}

# apply .sql files
$dbh->disconnect;

if (defined($sqldir)) {
    if (!-d $sqldir) {
        say STDERR "ERROR: $sqldir is not a directory";
        exit 2;
    }

    my @files;
    opendir DIR, $sqldir or die "Cannot read list of files from $sqldir: $!";
    while (my $f = readdir DIR) {
        next unless $f =~ /\.sql$/i;
        my $fn = catfile($sqldir, $f);

        if (-f $fn && -r $fn) {
            push @files, $fn;
        }
    }
    closedir DIR;

    # apply by calling sqlite3 to avoid parsing SQL statements
    # in here (lines need to be joined)
    open OUT, "|-", $SQLiteCmd, $DBFile or die "Can't run sqlite3: $!";

    foreach my $f (sort @files) {
        open IN, "<", $f or die "Can't open $f for reading: $!";
        say "Applying $f";

        while (<IN>) {
            print OUT $_;
        }

        close IN;
        print OUT "\n";
    }

    close OUT;
}

say "Done!";



### command line help ###

=head1 SYNOPSIS

tracer.pl [options] script [scriptargs]

=head1 OPTIONS

=over 8

=item B<--help>

prints this help message

=item B<--name> string

sets the name for the current trace

=item B<--database> file

set database file name

=item B<--tracedir> dir

set trace directory (default traces/<name>)

=item B<--prefix> dir

sets the trace directory prefix (default traces)

=item B<--autopivot>

automatically generate pivot views

=item B<--materialize-pivots>

create pivot tables instead of views

=item B<--sqldir> directory

apply *.sql files from this directory to the database

=item B<--skip-sql>

do not apply *.sql files

=back

=cut

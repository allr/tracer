Intro
=====
traceR is a frontend script that runs an R program with both timeR and
r-instrumented and imports their results into an sqlite3 database for
further analysis.

Installation
============
It is strongly recommended to use the [traceR installer][] to install
traceR as it will automatically install both timeR and r-instrumented
in a directory of your choice with minimum hassle.

[traceR installer]: https://github.com/allr/traceR-installer

If you want to install traceR manually anyway, just use
`make install PREFIX=/where/you/want/it` to install traceR in a
directory of your choice. It will assume that timeR and r-instrumented
are installed in the directories "timed" and "instrumented" in the
destination directory, although you can override this in the
configuration file.

If there is no `tracer.conf` file in the target directory, the
installation process will copy a sample file.

The installation process also creates a symlink named `tracer.sh` in
the target directory that points to `tracer.pl`. This symlink is added
for backwards compatibility and its creation may be removed in a
future version.


Demonstration
=============
If you want to see a small demonstration of the data gathered by
traceR, run the "rundemos.sh" script in the demos subdirectory after
installation. It runs traceR on a few included example R programs and
creates a number of PDF plots from the gathered data in a new
subdirectory "plots".


Usage
=====
To run an R program with traceR, just give the name of the R program
and any command line arguments it needs to `tracer.pl`, similar to the
way Rscript is used. Any traceR-specific command line options (see
below) must come before the name of the R program. traceR will then
run the program under both modifed R interpreters, whose outputs are
stored in `traces/<scriptname>` (unless overridden) and parsed into
`db.db` in the current directory. You can run traceR multiple times
from within the same directory (or explicitly specifying the same
database file) to accumulate data for multiple scripts and/or multiple
runs of the same script into one database for analysis.

If your R program needs special packages, you need to make sure that
these packages are installed in both the timeR and r-instrumented R
interpreters. There is currently no automation for this, so you need
to run the installation manually for both timeR (by default assumed to
be in `<Installdir>/timed`) and r-instrumented (in
`<Installdir>/instrumented`).

In addition to running an R program, traceR can also apply a set of
SQL files from a directory and create pivot views in the
database. Both of these options are enabled in the default
configuration file, scanning the `queries` subdirectory of the
installation for *.sql files. Both of these functions can be run
without specifying an R program on the command line or they can be
explicitly disabled using the `--skip-sql` and `--no-autopivot`
options.


Command line options
--------------------
### --help ###
Prints a message listing the known command line options

### --name ###
This option specifies the name for this tracing run. By default traceR
uses the name of the R program as name, but this option can be useful
if you want to trace the same R program with diffent command line
options. The name of a tracing run also influences the name of the
tracing directory unless that is also explicitly specified.

### --database / --db ###
These options specify the database file, overriding any such setting
in the configuration file. The default is to use `db.db` in the
current directory.

### --tracedir ###
This option specifies the directory where the outputs of timeR and
r-instrumented are to be stored. If this option is given, traceR
assumes that the user wants it to use exactly the given directory, so
any failure while creating it causes traceR to abort.

### --prefix ###
This option sets the prefix of the trace directory. If no `--tracedir`
option is given, traceR will attempt to generate a not yet used
directory name below this prefix based on the name of the tracing run
to store the outputs of timeR and r-instrumented. If the initial
attempt using `<prefix>/<tracename>` fails, tracerR makes up to 100
attempts to generate a not-yet-existing directory name by adding a
dash followed by a number to the trace name. If all of these attempts
fail, traceR aborts.

### --autopivot ###
This option enables autopivoting. It can be disabled using
`--no-autopivot`. Please see the autopivot section below for more
details.

### --materialize-pivots ###
This option tells traceR to generate materialized autopivots. It can
be disabled using `--no-materialize-pivots`. Please see the autopivot
section below for more details.

### --sqldir ###
This option specifies a directory which is scanned for files matching
`*.sql` which are applied to the database after the end of the tracing
run. By default it is not set.

### --skip-sql ###
This option tells traceR not to apply the files found in the directory
given by the sqldir option even if that option is present. It is
useful if you have set a default sqldir in a configuration file and
want to disable the functionality temporarily.


Configuration
=============
traceR looks for two configuration files, `$HOME/.tracer.conf` and
`tracer.conf` in the current directory. The latter file may override
settings from the first file and command line options may override
settings from either of these files. traceR uses a rather simplicistic
configuration file parser which accepts lines of the form
`key=value`, using case-insensitive keys. It skips blank lines and
lines starting with a hash mark (`#`). The currently accepted
configuration keys are:

TimeRBase
---------
This key sets the root directory of the timeR installation. If it is
not specified, the default is the subdirectory "timed" in the
directory where `tracer.pl` is located. If you use a relative
directory here (i.e. not starting with "/"), please be aware that it
is interpreted relative to the current directory from where timer.pl
is started.

RInstrumentedBase
-----------------
This key sets the root directory of the r-instrumented
installation. If it is not specified, the default is the directory
"instrumented" in the directory where `tracer.pl` is located. The same
caveat about relative directories that was mentioned for TimeRBase
also applies here.

TracesPrefix
------------
This key sets the prefix used for the auto-generated trace directory
name. If you use a relative directory here (i.e. not starting with
"/"), it is interpreted as relative to the current directory where
tracer.pl was started. The default value is "traces".

Database
--------
This key sets the name of the database file where the information
gathered during a tracing run is gathered. The default value is
"db.db", non-absolute values are interpreted relative to the current
directory where tracer.pl was started.

sqlite_command
--------------
This key sets the name of the sqlite3 command-line executable which is
used to apply SQL files to the database after the tracing run is
complete. If you have `sqlite3` available in your path, you can just
accept the default which is "sqlite3".

sqldir
------
This key sets the name of a directory which is scanned for files named
*.sql after the tracing run has completed which are then applied to
the database. By default this key is unset, which means this step is
skipped completely. If traceR cannot access the given directory
relative to the current directory, it will assume that the value is
relative to the directory where tracer.pl is located.

AutoPivot
---------
This key is used to enable or disable autopivoting. If set to "1",
"yes", "on" or "true", autopivoting is enabled. If set to "0", "no",
"off" or "false", autopivoting is disabled. Please check the
autopivoting section for more information.

MaterializePivots
-----------------
This key is used to enable or disable materialization of
auto-pivots. If set to "1", "yes", "on" or "true", materialization is
enabled. If set to "0", "no", "off" or "false", materialization is
disabled. Please check the autopivoting section for more information.


Database structure
==================
FIXME

Autopivoting
------------
traceR imports the data gathered by timeR and r-instrumented in a
row-oriented format (one row per data point) into the database as this
allows varying amounts of input data without changing the database
format (e.g. user functions from multiple R scripts). On the other
hand, a column-oriented format (one column per data point, one row per
tracing run) is more convenient for writing simple queries. To achieve
both of these goals, traceR can automatically generate pivot
views from the original data tables. traceR calculates a list
of distinct keys from the original table, throws out any key that
would be an invalid or problematic column name (anything that contains
more than just letters, numbers or underscores) and creates a view
using these keys as columns and one row per tracing run.

Depending on the amount of tracing runs in the data base, accessing
these views may be slow as SQLite recalculates their contents
dynamically. To avoid this, traceR offers the `--materialize-pivots`
option that duplicates the original data into new pivot tables instead
of generating a view for this purpose. This increases the size of the
database, but speeds up accesses to the pivoted data. You can switch
between materialized and normal views at any time by re-running
tracer.pl with the `--autopivot` option and with or without
`--materialize-pivots` - traceR will automatically delete the old
pivots and generate new ones according to your options. Obviously this
is strongly recommended if you use materialized views and add new
trace runs to the database as the views will not be updated with the
new data otherwise.


Other included programs
=======================
A few helper scripts are installed along with traceR.

scripts/plotall.sh
------------------
The `plotall.sh` script generates a number of CSV and PDF files for
a selected subset of the data collected by traceR. The output
files are stored in a subdirectory `plots` of the current directory.
The script assumes that the `db.db` file is located in
the currect directory. gnuplot version 4.4 or later and Perl 5.10
or later are required to generate the plots as I was unable to
create a sufficiently nice-looking output with just plain R (no
packages not included with the R distribution).

Plots are generated for the runtime_details_pct, memory_used_pct,
vector_sizes_pct, vector_counts, memory_used_vs_alloc and
total_runtimes views. A more detailed description of the contents
of these views can be found in
[queries/README.md](queries/README.md).

scripts/plotcsv.pl
------------------
`plotcsv.pl` is a Perl script that parses a CSV file and generates a
PDF- or PNG-format plot from it using gnuplot. The default plot
style is to plot all columns from a line in the CSV beside each
other, but it can be changed to a stacked bar graph output using
the `--stacked` parameter. The script assumes that the first line
in the file holds names for the columns and the first column of
the following lines holds a label for the current line. The CSV
parser in this script is extremely rudimentary, but it is
sufficient to read files generated by sqlite3 with the
`-csv -header` options.

demos/rundemos.sh
-----------------
`rundemos.sh` in the `demos` subdirectory runs traceR against a few
included demonstration R programs and generates plots from the
data gathered during these runs. Please check
[demos/README.md](demos/README.md) for
more details about the demonstration programs and their expected
behaviour.


Legalese
========
Copyright (C) 2013-2014 TU Dortmund Informatik LS XII
Inspired by r-timed from the [Reactor group](http://r.cs.purdue.edu/)
at Purdue University

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, a copy is available at
[http://www.r-project.org/Licenses/](http://www.r-project.org/Licenses/)

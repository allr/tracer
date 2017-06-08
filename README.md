Intro
=====
traceR is a profiling framework for the R language to analyze the
resource usage of an R application to locate bottlenecks.
traceR consists of two modified R interpreters, one
for runtime measurements called [timeR][] and [r-instrumented][] for
analyzing runtime and memory behavior. The results are gathered in an SQLite
database for convenient analysis.

[timeR]: https://github.com/allr/timeR
[r-instrumented]: https://github.com/allr/r-instrumented

The current version of traceR was inspired by the original traceR from
the [Reactor group](http://r.cs.purdue.edu/) at Purdue University.
This version has improved usability and analysis capability compared
to the original. We added profiling for vector data structures,
dynamic memory and CPU utilization profiles and profiling for parallel
R programs.


Installation
============
Currently, traceR has only been tested on a Linux system. It can
likely be compiled on other Unix-like systems, but chances of it working
in Windows are relatively slim.

It is strongly recommended to use the [traceR installer][] to install
traceR as it will automatically install both timeR and r-instrumented
in a directory of your choice with minimum hassle.

[traceR installer]: https://github.com/allr/traceR-installer

If you want to install traceR manually anyway (NOT recommended), just use
`make install PREFIX=/where/you/want/it` to install traceR in a
directory of your choice. It will assume that timeR and r-instrumented
are installed in the directories "timed" and "instrumented" in the
destination directory, although you can override this in the
configuration file.

If there is no `tracer.conf` file in the target directory, the
installation process will copy a sample file.

Dependencies
------------
* Perl version 5.20 or later
* Perl modules DBI and DBD::SQLite3
* SQLite 3
* everything needed to compile R itself, e.g. a Fortran compiler
* Gnuplot 5.x (if you want to use the included plot scripts or run the
    demo)



Demonstration
=============
If you want to see a small demonstration of the data gathered by
traceR, run the `rundemos.sh` script in the demos subdirectory after
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
runs of the same script into a single database for analysis.

If your R program needs additional packages, you need to make sure that
these packages are
[installed](https://cran.r-project.org/doc/manuals/r-release/R-admin.html#Installing-packages)
in both the timeR and r-instrumented R
interpreters. There is currently no automation for this, so you need
to run the installation manually for both timeR (by default assumed to
be in `<Installdir>/timed`) and r-instrumented (in
`<Installdir>/instrumented`). Since the two R interpreters are not
installed as the system's default R, they must be run with the correct
path, e.g. `<Installdir>/timed/bin/R CMD INSTALL ...` or
`<Installdir>/instrumented/bin/R CMD INSTALL ...`.

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


Configuration File
==================
traceR looks for three configuration files, `$HOME/.tracer.conf`,
`tracer.conf` in the directory of tracer.pl and
`tracer.conf` in the current directory. The later files may override
settings from the earlier files and command line options may override
settings from any of these files. traceR uses a rather simplicistic
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


Database Notes
==============

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


Sample Plot Scripts
===================
A few helper scripts to generate plots from the gathered data are
installed along with traceR. To use then, Gnuplot version 5.0 or later
must be installed.

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

scripts/memoryplot.pl
---------------------
`memoryplot.pl` is a Perl script that reads a traceR SQLite database
and produces memory-over-time plots in PDF format from it using
gnuplot. In the output file, one page is written for each script in
the database, showing the amount of allocated memory in the R
interpreter over time. Since the data is sampled only once per second,
plots with 10 data points or less indicate the sample points with an
"+" mark. In addition to the over-time curve, the peak is marked with
a solid line and the arithmetic mean of all samples is marked with a
dashed line.

scripts/plotchildren.pl
-----------------------
`plotchildren.pl` is a Perl script that reads a traceR SQLite database
and produces plots in PDF format from it using gnuplot. It only
creates plots for scripts that use the parallel package to run on
multiple cores or systems. The output can consist of multiple subplots
if the work was distributed over multiple systems, in this case the
master R process will only be shown in the subplot for the system it was
running on.

For each subplot, the X axis shows the time since
the start of the master R script in seconds and the right Y axis shows
the total amount of free/allocated memory (blue and black curves).
The horizontal lines in the plot whose color varies from red via
yellow to green indicate the times during which an R process was
running. The master process is shown on the top, the child processes
are shown below it, sorted by starting time. The color of each process
indicates the relative CPU usage of this process, calculated over its
entire runtime. In the best case, all processes except the master are
bright green, indicating that they have received the maximum amount of
CPU time. If the color shifts to yellow or red, this means that
the process has consumed less CPU time than its
wallclock-runtime. This can be caused by other concurrent processes
running on the same system, by running more R processes on a system
than the number of CPU cores it has or just processing delays caused
by I/O operations. The master process will usually show very low CPU
utilization since its only job is to coordinate the child processes
without running any calculations itself.

The black and blue curves show the amount of memory allocated by all R
interpreters on the system (black) and the amount of free memory
reported by the Linux kernel (blue). Their values are sampled once per
second. The amount of allocated memory has some slight inaccuracies
due to the way it is measured (it does not detect memory shared
between multiple R interpreters), but in general it should tend to
overestimate rather than underestimate the actual amount of memory
required. In the case of a parallel computation distributed over
multiple systems, distinct curves are calculated for each
system. Obviously parts of the curve will be missing during the times
when no R interpreter is running on such a system since measurements
are only available while R is running.

demos/rundemos.sh
-----------------
`rundemos.sh` in the `demos` subdirectory runs traceR against a few
included demonstration R programs and generates plots from the
data gathered during these runs. Please check
[demos/README.md](demos/README.md) for
more details about the demonstration programs and their expected
behaviour.


Community Guidelines
====================
If you happen to find a bug, want to contribute or just have some kind
of issue, please report it in this project's issue tracker on Github.


Legalese
========
Copyright (C) 2013-2017 TU Dortmund Informatik LS XII

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

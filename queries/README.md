This directory contains a few SQL files that create a lot of views
in the database to summarize the data collected by traceR.
Some of them are explained in more detail in the comments within the
SQL files.

The file names matter because the sample tracer.sh applies all *.sql
files it finds in alphabetical order and some of the views rely
on others that were defined previously.

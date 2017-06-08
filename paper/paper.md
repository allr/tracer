---
title: 'traceR: Profiling Framework for the R Language'
tags:
  - R language
  - profiling
  - memory analysis
authors:
 - name: Ingo Korb
   orcid: 0000-0002-0041-9894
   affiliation: 1
 - name: Helena Kotthaus
   affiliation: 1
 - name: Peter Marwedel
   affiliation: 1
affiliations:
 - name: Department of Computer Science 12, TU Dortmund University
   index: 1
date: 08 June 2017
bibliography: paper.bib
---

# Summary

traceR is a profiling framework for the R language to analyze the
resource usage of an R application to locate bottlenecks and develop
new optimizations for the R interpreter.

Statistical machine learning algorithms are one of the main use cases
suffering from performance deficiencies of the R interpreter due to
typically large input sets. traceR can be used to profile such
applications to attempt to gain an insight on their bottlenecks.

traceR consists of two parts, one for runtime measurements called
timeR and one for runtime and memory behavior analysis called
r-instrumented.
The original traceR was developed at Purdue University for R 2.12. The
current version has been rewritten to work with R 3.4 and features
improved usability and analysis capabilities.

The new traceR also added profiling for vector data structures,
dynamic memory and CPU utilization profiles and profiling for parallel
R programs. Unlike the Rprof tool which comes with R, traceR uses
a deterministic profiling, so it can generate more detailed and
accurate function usage statistics and can even profile the execution
time spent in external C/Fortran code supplied by R packages.

# References

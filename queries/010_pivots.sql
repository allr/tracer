-- Pivot for allocation summary
-- converts the memory allocation fields in the summary table from
-- "one row per program, one column per value" to
-- "one row per value times number of programs" format
-- which can simplify certain queries
-- (derived value "allocatedvectors" not included!)

DROP VIEW IF EXISTS alloc_summary_pivot;
CREATE VIEW IF NOT EXISTS alloc_summary_pivot AS

SELECT
summary.id as summary_id,
name as name,
"list" as label,
allocatedlist_tl as total,
allocatedlist_elts as elements,
0 as size, -- lists consist of cons elements which have a header but no data
0 as asize
FROM summary left join tasks on tasks.id=summary.id

UNION ALL

SELECT
summary.id,
name,
"smallvectors",
allocatedsmallvectors_tl,
allocatedsmallvectors_elts,
allocatedsmallvectors_size,
allocatedsmallvectors_asize
FROM summary left join tasks on tasks.id=summary.id

UNION ALL

SELECT
summary.id,
name,
"largevectors",
allocatedlargevectors_tl,
allocatedlargevectors_elts,
allocatedlargevectors_size,
allocatedlargevectors_asize
FROM summary left join tasks on tasks.id=summary.id

UNION ALL

SELECT
summary.id,
name,
"onevectors",
allocatedonevectors_tl,
allocatedonevectors_elts,
allocatedonevectors_size,
allocatedonevectors_asize
FROM summary left join tasks on tasks.id=summary.id

UNION ALL

SELECT
summary.id,
name,
"nullvectors",
allocatednullvectors_tl,
allocatednullvectors_elts,
allocatednullvectors_size,
allocatednullvectors_asize
FROM summary left join tasks on tasks.id=summary.id

UNION ALL

SELECT
summary.id,
name,
"stringbuffer",
allocatedstringbuffer_tl,
allocatedstringbuffer_elts,
allocatedstringbuffer_size,
allocatedstringbuffer_size  -- size is always exact for stringbuffers
FROM summary left join tasks on tasks.id=summary.id

-- synthesize values for class-0 allocators by using
-- just their count, zero elements and a fixed size of 0
-- because they all consist just of a header with no data
-- (note: changing the number of elements to a nonzero value
--  will skew the values in the alloc_summary_pct view)

UNION ALL SELECT
summary.id, name,
"cons", allocatedcons / 56, 0, 0, 0
FROM summary left join tasks on tasks.id=summary.id

UNION ALL SELECT
summary.id, name,
"sexp", allocatedsxp / 56, 0, 0, 0
FROM summary left join tasks on tasks.id=summary.id

UNION ALL SELECT
summary.id, name,
"sexpNonCons", allocatednoncons / 56, 0, 0, 0
FROM summary left join tasks on tasks.id=summary.id

UNION ALL SELECT
summary.id, name,
"env", allocatedenv / 56, 0, 0, 0
FROM summary left join tasks on tasks.id=summary.id

UNION ALL SELECT
summary.id, name,
"promises", allocatedpromises / 56, 0, 0, 0
FROM summary left join tasks on tasks.id=summary.id

-- synthesize a value for external memory allocations
-- by using a count of 0 and just their size
-- FIXME: This should be changed to use the real count when
-- r-instrumented is updated to track and print it!

UNION ALL SELECT
summary.id, name,
"external", 0, 0, allocatedexternal, allocatedexternal
FROM summary left join tasks on tasks.id=summary.id

;


-- pivot for duplicate summary
-- the 'duplicate*' columns have other suffixes so they are not pivoted with the other columns of the allocation summary
DROP VIEW IF EXISTS dupl_summary_pivot;
CREATE VIEW IF NOT EXISTS dupl_summary_pivot AS

SELECT
"duplicate" as summary_name,
duplicate_tl as tl,
duplicate_elts as elts,
duplicate_elts1 as elts1
FROM summary;


-- Pivot for time summary (warning, long query)
-- time_summary is presented in a very inconvenient format for analyzing, so the table is pivoted
-- that means column names are transformed to labels and related columns for one label are presented as neighboring columns in the output.
DROP VIEW IF EXISTS time_summary_pivot;
CREATE VIEW IF NOT EXISTS time_summary_pivot AS

SELECT
  time_summary.id as summary_id, name as name,
  "startup" as label,
  startup_self as self,
  startup_total as total,
  startup_starts as starts,
  startup_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "userfuncfallback" as label,
  userfuncfallback_self as self,
  userfuncfallback_total as total,
  userfuncfallback_starts as starts,
  userfuncfallback_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "cons" as label,
  cons_self as self,
  cons_total as total,
  cons_starts as starts,
  cons_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "allocvector" as label,
  allocvector_self as self,
  allocvector_total as total,
  allocvector_starts as starts,
  allocvector_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "alloclist" as label,
  alloclist_self as self,
  alloclist_total as total,
  alloclist_starts as starts,
  alloclist_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "allocs4" as label,
  allocs4_self as self,
  allocs4_total as total,
  allocs4_starts as starts,
  allocs4_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "gcinternal" as label,
  gcinternal_self as self,
  gcinternal_total as total,
  gcinternal_starts as starts,
  gcinternal_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "doarith" as label,
  doarith_self as self,
  doarith_total as total,
  doarith_starts as starts,
  doarith_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "domatprod" as label,
  domatprod_self as self,
  domatprod_total as total,
  domatprod_starts as starts,
  domatprod_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "gzfile" as label,
  gzfile_self as self,
  gzfile_total as total,
  gzfile_starts as starts,
  gzfile_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "bzfile" as label,
  bzfile_self as self,
  bzfile_total as total,
  bzfile_starts as starts,
  bzfile_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "xzfile" as label,
  xzfile_self as self,
  xzfile_total as total,
  xzfile_starts as starts,
  xzfile_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "onexits" as label,
  onexits_self as self,
  onexits_total as total,
  onexits_starts as starts,
  onexits_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dotexternalfull" as label,
  dotexternalfull_self as self,
  dotexternalfull_total as total,
  dotexternalfull_starts as starts,
  dotexternalfull_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dotexternal" as label,
  dotexternal_self as self,
  dotexternal_total as total,
  dotexternal_starts as starts,
  dotexternal_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dotcallfull" as label,
  dotcallfull_self as self,
  dotcallfull_total as total,
  dotcallfull_starts as starts,
  dotcallfull_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dotcall" as label,
  dotcall_self as self,
  dotcall_total as total,
  dotcall_starts as starts,
  dotcall_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dotcfull" as label,
  dotcfull_self as self,
  dotcfull_total as total,
  dotcfull_starts as starts,
  dotcfull_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dotc" as label,
  dotc_self as self,
  dotc_total as total,
  dotc_starts as starts,
  dotc_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dotfortranfull" as label,
  dotfortranfull_self as self,
  dotfortranfull_total as total,
  dotfortranfull_starts as starts,
  dotfortranfull_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dotfortran" as label,
  dotfortran_self as self,
  dotfortran_total as total,
  dotfortran_starts as starts,
  dotfortran_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dounzip" as label,
  dounzip_self as self,
  dounzip_total as total,
  dounzip_starts as starts,
  dounzip_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "zipread" as label,
  zipread_self as self,
  zipread_total as total,
  zipread_starts as starts,
  zipread_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "duplicate" as label,
  duplicate_self as self,
  duplicate_total as total,
  duplicate_starts as starts,
  duplicate_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "findvarinframe3other" as label,
  findvarinframe3other_self as self,
  findvarinframe3other_total as total,
  findvarinframe3other_starts as starts,
  findvarinframe3other_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "symlookup" as label,
  symlookup_self as self,
  symlookup_total as total,
  symlookup_starts as starts,
  symlookup_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "funlookup" as label,
  funlookup_self as self,
  funlookup_total as total,
  funlookup_starts as starts,
  funlookup_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "funlookupeval" as label,
  funlookupeval_self as self,
  funlookupeval_total as total,
  funlookupeval_starts as starts,
  funlookupeval_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "match" as label,
  match_self as self,
  match_total as total,
  match_starts as starts,
  match_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "evallist" as label,
  evallist_self as self,
  evallist_total as total,
  evallist_starts as starts,
  evallist_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "download" as label,
  download_self as self,
  download_total as total,
  download_starts as starts,
  download_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dologic" as label,
  dologic_self as self,
  dologic_total as total,
  dologic_starts as starts,
  dologic_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dologic2" as label,
  dologic2_self as self,
  dologic2_total as total,
  dologic2_starts as starts,
  dologic2_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "repl" as label,
  repl_self as self,
  repl_total as total,
  repl_starts as starts,
  repl_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "setupmainloop" as label,
  setupmainloop_self as self,
  setupmainloop_total as total,
  setupmainloop_starts as starts,
  setupmainloop_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "endmainloop" as label,
  endmainloop_self as self,
  endmainloop_total as total,
  endmainloop_starts as starts,
  endmainloop_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "install" as label,
  install_self as self,
  install_total as total,
  install_starts as starts,
  install_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dotspecial2" as label,
  dotspecial2_self as self,
  dotspecial2_total as total,
  dotspecial2_starts as starts,
  dotspecial2_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dorelop" as label,
  dorelop_self as self,
  dorelop_total as total,
  dorelop_starts as starts,
  dorelop_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dosubset" as label,
  dosubset_self as self,
  dosubset_total as total,
  dosubset_starts as starts,
  dosubset_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dosubset2" as label,
  dosubset2_self as self,
  dosubset2_total as total,
  dosubset2_starts as starts,
  dosubset2_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "dosubset3" as label,
  dosubset3_self as self,
  dosubset3_total as total,
  dosubset3_starts as starts,
  dosubset3_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "insockread" as label,
  insockread_self as self,
  insockread_total as total,
  insockread_starts as starts,
  insockread_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "insockwrite" as label,
  insockwrite_self as self,
  insockwrite_total as total,
  insockwrite_starts as starts,
  insockwrite_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "insockopen" as label,
  insockopen_self as self,
  insockopen_total as total,
  insockopen_starts as starts,
  insockopen_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "insockconnect" as label,
  insockconnect_self as self,
  insockconnect_total as total,
  insockconnect_starts as starts,
  insockconnect_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "sleep" as label,
  sleep_self as self,
  sleep_total as total,
  sleep_starts as starts,
  sleep_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

UNION ALL SELECT
  time_summary.id as summary_id, name as name,
  "system" as label,
  system_self as self,
  system_total as total,
  system_starts as starts,
  system_aborts as aborts
FROM time_summary left join tasks on tasks.id=time_summary.id

ORDER BY summary_id

;
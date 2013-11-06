-- memory usage, slightly reorganized for better readability
DROP VIEW IF EXISTS memory_used;
CREATE VIEW IF NOT EXISTS memory_used AS

SELECT
    name,
    rusagemaxresidentmemoryset * 1024 as RUsageMemory_bytes,
    (allocatedcons - allocatedlist_elts * 56) as cons_bytes,
    allocatedpromises as promises_bytes,
    allocatedenv as env_bytes,
    allocatedexternal as external_bytes,
    allocatedsxp + allocatednoncons as sexp_bytes,
    allocatedlist_elts * 56 as lists_bytes,
    allocatedlist_tl as lists_count,
    ROUND(allocatedlist_elts / CAST(allocatedlist_tl AS REAL), 4) as lists_avg_elts,

    allocatedlargevectors_size as largevec_bytes,
    allocatedlargevectors_tl   as largevec_count,
    allocatedlargevectors_elts as largevec_elements,

    allocatedsmallvectors_size as smallvec_bytes,
    allocatedsmallvectors_tl   as smallvec_count,
    allocatedsmallvectors_elts as smallvec_elements,

    allocatedonevectors_size   as onevec_bytes,
    allocatedonevectors_tl     as onevec_count,
    allocatedonevectors_elts   as onevec_elements,

    allocatednullvectors_size  as nullvec_bytes,
    allocatednullvectors_tl    as nullvec_count,
    allocatednullvectors_elts  as nullvec_elements,

    allocatedstringbuffer_size as stringbuffer_bytes_roundedup,
    allocatedstringbuffer_elts as stringbuffer_bytes_needed,
    allocatedstringbuffer_tl   as stringbuffer_count,

    -- sum of all byte values to simplify the percentage view
    allocatedcons + allocatedpromises + allocatedenv +
    allocatedsxp + allocatednoncons +
    -- calculate size of list headers
    (allocatedlargevectors_tl + allocatedsmallvectors_tl +
     allocatedonevectors_tl + allocatednullvectors_tl) * 56 +
    allocatedexternal + allocatedlargevectors_size +
    allocatedsmallvectors_size + allocatedonevectors_size +
    allocatednullvectors_size + allocatedstringbuffer_size
      AS total_bytes
FROM summary
left join traces
where traces.id = summary.id
order by summary.id;


--
-- detail view for vector sizes and counts
--
-- Note: Bytes only for the data itself, NO HEADERS!
DROP VIEW IF EXISTS vector_details;
CREATE VIEW vector_details AS
  SELECT
    traces.name,

    ROUND(allocatedvectors_elts / CAST(allocatedvectors_tl AS REAL), 4)
      AS all_avg_elements_per_vector,
    ROUND(allocatedvectors_size / CAST(allocatedvectors_tl AS REAL), 4)
      AS all_avg_bytes_per_vector,
    ROUND(allocatedvectors_size / CAST(allocatedvectors_elts AS REAL), 4)
      AS all_avg_bytes_per_element,

    ROUND(allocatedonevectors_elts / CAST(allocatedonevectors_tl AS REAL), 4)
      AS one_avg_elements_per_vector,
    ROUND(allocatedonevectors_size / CAST(allocatedonevectors_tl AS REAL), 4)
      AS one_avg_bytes_per_vector,
    ROUND(allocatedonevectors_size / CAST(allocatedonevectors_elts AS REAL), 4)
      AS one_avg_bytes_per_element,

    ROUND(allocatedsmallvectors_elts / CAST(allocatedsmallvectors_tl AS REAL), 4)
      AS small_avg_elements_per_vector,
    ROUND(allocatedsmallvectors_size / CAST(allocatedsmallvectors_tl AS REAL), 4)
      AS small_avg_bytes_per_vector,
    ROUND(allocatedsmallvectors_size / CAST(allocatedsmallvectors_elts AS REAL), 4)
      AS small_avg_bytes_per_element,

    ROUND(allocatedlargevectors_elts / CAST(allocatedlargevectors_tl AS REAL), 4)
      AS large_avg_elements_per_vector,
    ROUND(allocatedlargevectors_size / CAST(allocatedlargevectors_tl AS REAL), 4)
      AS large_avg_bytes_per_vector,
    ROUND(allocatedlargevectors_size / CAST(allocatedlargevectors_elts AS REAL), 4)
      AS large_avg_bytes_per_element,

    CAST(allocatedvectors_size AS REAL)      as all_totalsize_bytes,
    CAST(allocatedonevectors_size AS REAL)   as one_totalsize_bytes,
    CAST(allocatedsmallvectors_size AS REAL) as small_totalsize_bytes,
    CAST(allocatedlargevectors_size AS REAL) as large_totalsize_bytes,

    100 * allocatedonevectors_size / CAST(allocatedvectors_size AS REAL)
      AS one_byte_pct_of_allvectors,
    100 * allocatedsmallvectors_size / CAST(allocatedvectors_size AS REAL)
      AS small_byte_pct_of_allvectors,
    100 * allocatedlargevectors_size / CAST(allocatedvectors_size AS REAL)
      AS large_byte_pct_of_allvectors,

    ROUND(100 * allocatednullvectors_tl / CAST(allocatedvectors_tl AS REAL), 4)
      AS null_count_pct_of_allvectors,
    ROUND(100 * allocatedonevectors_tl / CAST(allocatedvectors_tl AS REAL), 4)
      AS one_count_pct_of_allvectors,
    ROUND(100 * allocatedsmallvectors_tl / CAST(allocatedvectors_tl AS REAL), 4)
      AS small_count_pct_of_allvectors,
    ROUND(100 * allocatedlargevectors_tl / CAST(allocatedvectors_tl AS REAL), 4)
      AS large_count_pct_of_allvectors

  FROM summary
  LEFT JOIN traces ON traces.id = summary.id
  ORDER BY summary.id
;

--
-- external C code vs. stuff in the interpreter
--
DROP VIEW IF EXISTS external_vs_interpreter;
CREATE VIEW external_vs_interpreter AS
  SELECT
    traces.name,
    dotexternal_self + dotcall_self +
      dotc_self + dotfortran_self AS external_code_time,
    totalruntime - dotexternal_self - dotcall_self -
      dotc_self - dotfortran_self AS interpreter_time
  FROM time_summary
  LEFT JOIN traces on traces.id = time_summary.id
  ORDER BY time_summary.id
;    

DROP VIEW IF EXISTS external_vs_interpreter_pct;
CREATE VIEW external_vs_interpreter_pct AS
SELECT
  name,
  ROUND(100 * interpreter_time / CAST( (interpreter_time + external_code_time) AS REAL ), 4) AS R_Code,
  ROUND(100 * external_code_time / CAST( (interpreter_time + external_code_time)  AS REAL ), 4) AS Ex_Code
  FROM external_vs_interpreter
;

-- categorized runtimes as percentage of total
--
DROP VIEW IF EXISTS runtime_details_pct;
CREATE VIEW runtime_details_pct AS
  SELECT
    name,
    ROUND(100 * (dotC_self + dotFortran_self + dotCall_self +
      dotExternal_self ) / CAST(TotalRuntime AS REAL), 4) AS External,
    ROUND(100 * ( FunLookup_self + SymLookup_self +
      FindVarInFrame3other_self ) / CAST(TotalRuntime AS REAL), 4) AS Lookup,
    ROUND(100 * Match_self / CAST(TotalRuntime AS REAL), 4) AS Match,
    ROUND(100 * Duplicate_self / CAST(TotalRuntime AS REAL), 4) AS Duplicate,
    ROUND(100 * gcinternal_self / CAST(TotalRuntime AS REAL), 4) AS GC,
    ROUND(100 * (cons_self + allocList_self + allocVector_self ) /
      CAST(TotalRuntime AS REAL), 4) AS MemAlloc,
    ROUND(100 * (dosubset_self + dosubset2_self + dosubset3_self) /
      CAST(TotalRuntime AS REAL), 4) AS Subset,
    ROUND(100 * EvalList_self / CAST(TotalRuntime AS REAL), 4) AS Eval,
    ROUND(100 * (doarith_self + domatprod_self + dologic_self +
      dologic2_self + dorelop_self ) / CAST(TotalRuntime AS REAL), 4)
      AS Arith,
    ROUND(100 * (builtinsum_self + specialsum_self + dotSpecial2_self) /
      CAST(TotalRuntime AS REAL), 4) AS BuiltIn_Special,
    ROUND(100 * (startup_self + install_self + repl_self + userfunctionsum_self +
      setupMainLoop_self + endMainLoop_self + gzFile_self) / CAST(TotalRuntime AS REAL), 4)
      AS Other
  FROM time_summary
  left join traces
  where traces.id=time_summary.id

  UNION ALL SELECT
    " Average",
    ROUND(100 * (SUM(dotC_self) + SUM(dotFortran_self) + SUM(dotCall_self) +
      SUM(dotExternal_self) ) / CAST(SUM(TotalRuntime) AS REAL), 4) AS External,
    ROUND(100 * ( SUM(FunLookup_self) + SUM(SymLookup_self) +
      SUM(FindVarInFrame3other_self) ) / CAST(SUM(TotalRuntime) AS REAL), 4) AS Lookup,
    ROUND(100 * SUM(Match_self) / CAST(SUM(TotalRuntime) AS REAL), 4) AS Match,
    ROUND(100 * SUM(Duplicate_self) / CAST(SUM(TotalRuntime) AS REAL), 4) AS Duplicate,
    ROUND(100 * SUM(gcinternal_self) / CAST(SUM(TotalRuntime) AS REAL), 4) AS GC,
    ROUND(100 * (SUM(cons_self) + SUM(allocList_self) + SUM(allocVector_self) ) /
      CAST(SUM(TotalRuntime) AS REAL), 4) AS MemAlloc,
    ROUND(100 * (SUM(dosubset_self) + SUM(dosubset2_self) + SUM(dosubset3_self)) /
      CAST(SUM(TotalRuntime) AS REAL), 4) AS Subset,
    ROUND(100 * SUM(EvalList_self) / CAST(SUM(TotalRuntime) AS REAL), 4) AS Eval,
    ROUND(100 * (SUM(doarith_self) + SUM(domatprod_self) + SUM(dologic_self) +
      SUM(dologic2_self) + SUM(dorelop_self)) / CAST(SUM(TotalRuntime) AS REAL), 4)
      AS Arith,
    ROUND(100 * (SUM(builtinsum_self) + SUM(specialsum_self) + SUM(dotSpecial2_self)) /
      CAST(SUM(TotalRuntime) AS REAL), 4) AS BuiltIn_Special,
    ROUND(100 * (SUM(startup_self) + SUM(install_self) + SUM(repl_self) + SUM(userfunctionsum_self) +
      SUM(setupMainLoop_self) + SUM(endMainLoop_self) + SUM(gzFile_self)) / CAST(SUM(TotalRuntime) AS REAL), 4)
      AS Other
  FROM time_summary
order by name;

--
-- memory allocations: total and os-reported maximum
--
DROP VIEW IF EXISTS memory_used_vs_alloc;
CREATE VIEW memory_used_vs_alloc AS
SELECT
    name,
    rusagemaxresidentmemoryset / 1024.0 as RUsageMemory_mbytes,
    -- sum of all byte values to simplify the percentage view
    (allocatedcons + allocatedpromises + allocatedenv +
    allocatedsxp + allocatednoncons +
    -- calculate size of list headers
    (allocatedlargevectors_tl + allocatedsmallvectors_tl +
     allocatedonevectors_tl + allocatednullvectors_tl) * 56 +
    allocatedexternal + allocatedlargevectors_size +
    allocatedsmallvectors_size + allocatedonevectors_size +
    allocatednullvectors_size + allocatedstringbuffer_size) / 1024.0 / 1024.0
      AS total_mbytes
  FROM summary
  left join traces
  where traces.id = summary.id

UNION ALL SELECT
  " Average",
    sum(rusagemaxresidentmemoryset) / 1024.0 / cast(count(rusagemaxresidentmemoryset) as real)
      as RUsageMemory_mbytes,
    (sum(allocatedcons) + sum(allocatedpromises) + sum(allocatedenv) +
    sum(allocatedsxp) + sum(allocatednoncons) +
    -- calculate size of list headers
    (sum(allocatedlargevectors_tl) + sum(allocatedsmallvectors_tl) +
     sum(allocatedonevectors_tl) + sum(allocatednullvectors_tl)) * 56 +
    sum(allocatedexternal) + sum(allocatedlargevectors_size) +
    sum(allocatedsmallvectors_size) + sum(allocatedonevectors_size) +
    sum(allocatednullvectors_size) + sum(allocatedstringbuffer_size)) / 1024.0 / 1024.0
      / cast(count(allocatedcons) as real)
      AS total_mbytes
  FROM summary

order by name;

--
-- memory usage in percent of total bytes used
--
DROP VIEW IF EXISTS memory_used_pct;
CREATE VIEW memory_used_pct AS

  SELECT
    name,
    ROUND(100 * ( cons_bytes +  lists_bytes ) / CAST(total_bytes AS REAL),
4) AS Lists,
    ROUND(100 * ((nullvec_bytes + 56 * nullvec_count)
    +   (onevec_bytes + 56 * onevec_count)
    +   (smallvec_bytes + 56 * smallvec_count)
    +   (largevec_bytes + 56 * largevec_count))  / CAST(total_bytes AS
REAL), 4) AS Vectors,
    ROUND(100 * promises_bytes / CAST(total_bytes AS REAL), 4) AS Promises,
    ROUND(100 * env_bytes / CAST(total_bytes AS REAL), 4) AS Environments,
    ROUND(100 * external_bytes / CAST(total_bytes AS REAL), 4)  AS External,
    ROUND(100 * (stringbuffer_bytes_needed + sexp_bytes) /
      CAST(total_bytes AS REAL), 4) AS Other
  FROM memory_used

UNION ALL
  SELECT
    " Average",
    ROUND(100 * (SUM(cons_bytes) + SUM(lists_bytes)) / CAST(SUM(total_bytes) as real), 4),
    ROUND(100 * ((SUM(nullvec_bytes) + 56 * SUM(nullvec_count))
    +   (SUM(onevec_bytes) + 56 * SUM(onevec_count))
    +   (SUM(smallvec_bytes) + 56 * SUM(smallvec_count))
    +   (SUM(largevec_bytes) + 56 * SUM(largevec_count)))  / CAST(SUM(total_bytes) AS
REAL), 4),
    ROUND(100 * SUM(promises_bytes) / CAST(SUM(total_bytes) AS REAL), 4),
    ROUND(100 * SUM(env_bytes) / CAST(SUM(total_bytes) AS REAL), 4),
    ROUND(100 * SUM(external_bytes) / CAST(SUM(total_bytes) AS REAL), 4),
    ROUND(100 * (SUM(stringbuffer_bytes_needed) + SUM(sexp_bytes)) /
      CAST(SUM(total_bytes) AS REAL), 4)
  FROM memory_used ORDER BY name;

-- helper view to simplify the formulation of vector_sizes
drop view if exists vector_sizes_helperview;
create view vector_sizes_helperview as
  select
    name,
    allocatedlargevectors_size + 56 * allocatedlargevectors_tl as large_bytes,
    allocatedsmallvectors_size + 56 * allocatedsmallvectors_tl as small_bytes,
    allocatedonevectors_size + 56 * allocatedonevectors_tl as one_bytes,
    allocatednullvectors_size + 56 * allocatednullvectors_tl as null_bytes,
    allocatedvectors_size + 56 * allocatedvectors_tl - allocatednullvectors_size - 56 * allocatednullvectors_tl as total_bytes
  from summary left join traces
  where traces.id = summary.id
  order by summary.id;

-- relative vector sizes, by allocated memory
drop view if exists vector_sizes;
create view vector_sizes as
  select
    name,
    round(100 * null_bytes / cast(total_bytes as real), 2) as null_pct,
    round(100 * one_bytes / cast(total_bytes as real), 2) as one_pct,
    round(100 * small_bytes / cast(total_bytes as real), 2) as small_pct,
    round(100 * large_bytes / cast(total_bytes as real), 2) as large_pct
  from vector_sizes_helperview
UNION ALL
  SELECT
    " Average",
    round(100 * sum(null_bytes) / cast(sum(total_bytes) as real), 2),
    round(100 * sum(one_bytes) / cast(sum(total_bytes) as real), 2),
    round(100 * sum(small_bytes) / cast(sum(total_bytes) as real), 2),
    round(100 * sum(large_bytes) / cast(sum(total_bytes) as real), 2)
  from vector_sizes_helperview
order by name;

-- relative vector counts (i.e. number of vectors, not elements)
DROP VIEW IF EXISTS vector_counts;
CREATE VIEW vector_counts AS
  SELECT
    name,
    null_count_pct_of_allvectors AS null_count_pct,
    one_count_pct_of_allvectors AS one_count_pct,
    small_count_pct_of_allvectors AS small_count_pct,
    large_count_pct_of_allvectors AS large_count_pct
  FROM vector_details
  ORDER BY name;


-- total runtimes, converted from nanoseconds to seconds
DROP VIEW IF EXISTS total_runtimes;
CREATE VIEW total_runtimes AS
  SELECT
    name,
    totalruntime / 1e9 as totalruntime_seconds
    FROM time_summary LEFT JOIN traces
    WHERE traces.id = time_summary.id
  UNION ALL SELECT
    " Average",
    SUM(totalruntime / 1e9) / cast(count(totalruntime) AS REAL)
    FROM time_summary
  ORDER BY NAME;

-- runtime as percentage of total
--
DROP VIEW IF EXISTS running_time_details_pct_new;
CREATE VIEW running_time_details_pct_new AS
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

--order by time_summary.id;

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
    -- sum of all byte values to simplify the percentage view
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

-- memory usage in percent of total bytes used
--
DROP VIEW IF EXISTS memory_used_pct_new;
CREATE VIEW memory_used_pct_new AS

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

-- helper view to simplify the formulation of vector_ratios
drop view if exists vector_ratios_temp;
create view vector_ratios_temp as
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

drop view if exists vector_ratios;
create view vector_ratios as
  select
    name,
    -- round(100 * null_bytes / cast(total_bytes as real), 2) as null_percentage,
    round(100 * one_bytes / cast(total_bytes as real), 2) as one_percentage,
    round(100 * small_bytes / cast(total_bytes as real), 2) as small_percentage,
    round(100 * large_bytes / cast(total_bytes as real), 2) as large_percentage
  from vector_ratios_temp
UNION ALL
  SELECT
    " Average",
    round(100 * sum(one_bytes) / cast(sum(total_bytes) as real), 2),
    round(100 * sum(small_bytes) / cast(sum(total_bytes) as real), 2),
    round(100 * sum(large_bytes) / cast(sum(total_bytes) as real), 2)
  from vector_ratios_temp
order by name;


-- Neuer View für Berechnung des Mittelwertes
DROP VIEW IF EXISTS runtime_average;
CREATE VIEW runtime_average AS
  SELECT
    name,
    ROUND((dotC_self + dotFortran_self + dotCall_self +
      dotExternal_self ), 4) AS External,
    ROUND(( FunLookup_self + SymLookup_self +
      FindVarInFrame3other_self ) , 4) AS Lookup,
    ROUND( Match_self , 4) AS Match,
    ROUND( Duplicate_self , 4) AS Duplicate,
    ROUND( gcinternal_self, 4) AS GC,
    ROUND( (cons_self + allocList_self + allocVector_self ), 4) AS MemAlloc,
    ROUND( (dosubset_self + dosubset2_self + dosubset3_self) , 4) AS Subset,
    ROUND( EvalList_self , 4) AS Eval,
    ROUND( (doarith_self + domatprod_self + dologic_self +
     dologic2_self + dorelop_self ), 4) AS Arith,
    ROUND( (builtinsum_self + specialsum_self + dotSpecial2_self) , 4) AS BuiltIn_Special,
    ROUND( (startup_self+install_self+Repl_self+userfunctionsum_self+setupmainloop_self+endmainloop_self+gzfile_self), 4) AS Other,
    totalruntime
  FROM time_summary
  left join traces
  where traces.id=time_summary.id
  order by external desc;


-- Vector allocation counts, relative
DROP VIEW IF EXISTS relative_vector_alloc_counts;
CREATE VIEW relative_vector_alloc_counts AS
  select
    name,
    round(100 * allocatedonevectors_tl / cast((allocatedsmallvectors_tl + allocatedonevectors_tl + allocatedlargevectors_tl) as real), 2) as one_vec_count,
    round(100 * allocatedsmallvectors_tl / cast((allocatedsmallvectors_tl + allocatedonevectors_tl + allocatedlargevectors_tl) as real), 2) as small_vec_count,
    round(100 * allocatedlargevectors_tl / cast((allocatedsmallvectors_tl + allocatedonevectors_tl + allocatedlargevectors_tl) as real), 2) as large_vec_count

  from summary left join traces
  where traces.id = summary.id
UNION ALL
  select
    " Average",
    round(100 * sum(allocatedonevectors_tl) / cast(sum(allocatedsmallvectors_tl + allocatedonevectors_tl + allocatedlargevectors_tl) as real), 2),
    round(100 * sum(allocatedsmallvectors_tl) / cast(sum(allocatedsmallvectors_tl + allocatedonevectors_tl + allocatedlargevectors_tl) as real), 2),
    round(100 * sum(allocatedlargevectors_tl) / cast(sum(allocatedsmallvectors_tl + allocatedonevectors_tl + allocatedlargevectors_tl) as real), 2)
  from summary
 order by name;

-- drop old view
DROP VIEW IF EXISTS relative_vector_alloc_counts_overall;


-- number of args as percentages
DROP VIEW IF EXISTS number_of_args_pct;
CREATE VIEW number_of_args_pct AS
  select
    value as number_of_args,
    100 * param / cast((SELECT sum(param) from number_of_args) as real) as percentage_of_calls
  from number_of_args
  order by number_of_args;
-- to get values for "X to Y args", use
-- SELECT sum(percentage_of_calls) FROM number_of_args_pct WHERE number_of_args >= X AND number_of_args <= Y;


-- reduced version of vector_details for demo purposes
DROP VIEW IF EXISTS vector_details_lite;
CREATE VIEW vector_details_lite AS
  SELECT
    name,
    one_count_percentage_of_allvectors AS one_count_pct,
    small_count_percentage_of_allvectors AS small_count_pct,
    large_count_percentage_of_allvectors AS large_count_pct
  FROM vector_details
  ORDER BY name;

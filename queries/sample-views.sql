-- memory usage, slightly reorganized for better readability
DROP VIEW IF EXISTS memory_used;
CREATE VIEW IF NOT EXISTS memory_used AS

SELECT
    trace_id,
    name,
    rusagemaxresidentmemoryset * 1024 as RUsageMemory_bytes,
    (allocatedcons - allocatedlist_elements * 56) as cons_bytes,
    allocatedpromises as promises_bytes,
    allocatedenv as env_bytes,
    allocatedexternal as external_bytes,
    allocatedsxp + allocatednoncons as sexp_bytes,
    allocatedlist_elements * 56 as lists_bytes,
    allocatedlist_allocs as lists_count,
    ROUND(allocatedlist_elements / CAST(allocatedlist_allocs AS REAL), 4) as lists_avg_elements,

    allocatedlargevectors_size as largevec_bytes,
    allocatedlargevectors_allocs   as largevec_count,
    allocatedlargevectors_elements as largevec_elements,

    allocatedsmallvectors_size as smallvec_bytes,
    allocatedsmallvectors_allocs   as smallvec_count,
    allocatedsmallvectors_elements as smallvec_elements,

    allocatedonevectors_size   as onevec_bytes,
    allocatedonevectors_allocs     as onevec_count,
    allocatedonevectors_elements   as onevec_elements,

    allocatedzerovectors_size  as zerovec_bytes,
    allocatedzerovectors_allocs    as zerovec_count,
    allocatedzerovectors_elements  as zerovec_elements,

    allocatedstringbuffer_size as stringbuffer_bytes_roundedup,
    allocatedstringbuffer_elements as stringbuffer_bytes_needed,
    allocatedstringbuffer_allocs   as stringbuffer_count,

    -- sum of all byte values to simplify the percentage view
    allocatedcons + allocatedpromises + allocatedenv +
    allocatedsxp + allocatednoncons +
    -- calculate size of list headers
    (allocatedlargevectors_allocs + allocatedsmallvectors_allocs +
     allocatedonevectors_allocs) * 40 + allocatedzerovectors_allocs * 56 +
    allocatedexternal + allocatedlargevectors_size +
    allocatedsmallvectors_size + allocatedonevectors_size +
    allocatedzerovectors_size + allocatedstringbuffer_size
      AS total_bytes
FROM TraceResults_pivot
order by name;

--
-- detail view for vector sizes and counts
--
-- Note: Bytes only for the data itself, NO HEADERS!
DROP VIEW IF EXISTS vector_details;
CREATE VIEW vector_details AS
  SELECT
    traces.name,

    -- elem/vec, byte/vec, byte/elem

    ROUND(allocatedvectors_elements / CAST(allocatedvectors_allocs AS REAL), 4)
      AS all_avg_elements_per_vector,
    ROUND(allocatedvectors_size / CAST(allocatedvectors_allocs AS REAL), 4)
      AS all_avg_bytes_per_vector,
    ROUND(allocatedvectors_size / CAST(allocatedvectors_elements AS REAL), 4)
      AS all_avg_bytes_per_element,

    ROUND(allocatedonevectors_elements / CAST(allocatedonevectors_allocs AS REAL), 4)
      AS one_avg_elements_per_vector,
    ROUND(allocatedonevectors_size / CAST(allocatedonevectors_allocs AS REAL), 4)
      AS one_avg_bytes_per_vector,
    ROUND(allocatedonevectors_size / CAST(allocatedonevectors_elements AS REAL), 4)
      AS one_avg_bytes_per_element,

    ROUND(allocatedsmallvectors_elements / CAST(allocatedsmallvectors_allocs AS REAL), 4)
      AS small_avg_elements_per_vector,
    ROUND(allocatedsmallvectors_size / CAST(allocatedsmallvectors_allocs AS REAL), 4)
      AS small_avg_bytes_per_vector,
    ROUND(allocatedsmallvectors_size / CAST(allocatedsmallvectors_elements AS REAL), 4)
      AS small_avg_bytes_per_element,

    ROUND(allocatedlargevectors_elements / CAST(allocatedlargevectors_allocs AS REAL), 4)
      AS large_avg_elements_per_vector,
    ROUND(allocatedlargevectors_size / CAST(allocatedlargevectors_allocs AS REAL), 4)
      AS large_avg_bytes_per_vector,
    ROUND(allocatedlargevectors_size / CAST(allocatedlargevectors_elements AS REAL), 4)
      AS large_avg_bytes_per_element,

    --- percentages

    ROUND(100 * allocatedonevectors_size / CAST(allocatedvectors_size AS REAL), 4)
      AS one_byte_pct_of_allvectors,
    ROUND(100 * allocatedsmallvectors_size / CAST(allocatedvectors_size AS REAL), 4)
      AS small_byte_pct_of_allvectors,
    ROUND(100 * allocatedlargevectors_size / CAST(allocatedvectors_size AS REAL), 4)
      AS large_byte_pct_of_allvectors,

    ROUND(100 * allocatedzerovectors_allocs / CAST(allocatedvectors_allocs AS REAL), 4)
      AS zero_count_pct_of_allvectors,
    ROUND(100 * allocatedonevectors_allocs / CAST(allocatedvectors_allocs AS REAL), 4)
      AS one_count_pct_of_allvectors,
    ROUND(100 * allocatedsmallvectors_allocs / CAST(allocatedvectors_allocs AS REAL), 4)
      AS small_count_pct_of_allvectors,
    ROUND(100 * allocatedlargevectors_allocs / CAST(allocatedvectors_allocs AS REAL), 4)
      AS large_count_pct_of_allvectors

  FROM TraceResults_pivot
  LEFT JOIN traces ON traces.id = TraceResults_pivot.trace_id
  ORDER BY TraceResults_pivot.trace_id
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
  FROM TimingResults_pivot
  LEFT JOIN traces on traces.id = TimingResults_pivot.trace_id
  ORDER BY TimingResults_pivot.trace_id
;    

DROP VIEW IF EXISTS external_vs_interpreter_pct;
CREATE VIEW external_vs_interpreter_pct AS
SELECT
  name,
  ROUND(100 * interpreter_time / CAST( (interpreter_time + external_code_time) AS REAL ), 4) AS R_Code,
  ROUND(100 * external_code_time / CAST( (interpreter_time + external_code_time)  AS REAL ), 4) AS Ex_Code
  FROM external_vs_interpreter
;

-- categorized runtimes as absolute times
DROP VIEW IF EXISTS runtime_details_abs;
CREATE VIEW runtime_details_abs AS
  SELECT
    name,

    dotC_self            +
    dotCFull_self        +
    dotFortran_self      +
    dotFortranFull_self  +
    dotCall_self         +
    dotCallFull_self     +
    dotExternal_self     +
    dotExternalFull_self
      AS External,

    FunLookup_self            +
    SymLookup_self            +
    FindVarInFrame3other_self +
    bcEvalGetvar_self
      AS Lookup,

    Match_self
      AS Match,

    Duplicate_self
      AS Duplicate,

    GCInternal_self
      AS GC,

    cons_self        +
    allocList_self   +
    allocS4_self     +
    allocVector_self
      AS MemAlloc,

    doSubset_self     +
    doSubset2_self    +
    doSubset3_self    +
    doSubassign_self  +
    doSubassign2_self +
    doSubassign3_self
      AS Subset,

    EvalList_self
      AS EvalList,

    doArith_self      +
    doMatprod_self    +
    doLogic_self      +
    doLogic2_self     +
    doLogic3_self     +
    doRelop_self      +
    bcEvalArith1_self +
    bcEvalArith2_self +
    bcEvalMath1_self  +
    bcEvalRelop_self  +
    bcEvalLogic_self
      AS Arith,

    bcEval_self           + -- FIXME?
    BuiltinSum_self  +
    SpecialSum_self  +
    do_internal_self
      AS BuiltIn_Special,

    Startup_self          +
    Install_self          +
    Repl_self             +
    UserFunctionSum_self  +
    UserFuncFallback_self +
    setupMainLoop_self    +
    endMainLoop_self      +
    onExits_self          +
    gzFile_self           +
    bzFile_self           +
    xzFile_self           +
    doUnzip_self          +
    zipRead_self          +
    Download_self         +
    Rsock_self            +
    Sleep_self            +
    System_self
      AS Other,

    TotalRuntime
      AS TotalRuntime

  FROM TimingResults_pivot

  UNION ALL SELECT
    " Average",

    ROUND(SUM(
      dotC_self            +
      dotCFull_self        +
      dotFortran_self      +
      dotFortranFull_self  +
      dotCall_self         +
      dotCallFull_self     +
      dotExternal_self     +
      dotExternalFull_self
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS External,

    ROUND(SUM(
      FunLookup_self            +
      SymLookup_self            +
      FindVarInFrame3other_self +
      bcEvalGetvar_self
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS Lookup,

    ROUND(SUM(
      Match_self
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS Match,

    ROUND(SUM(
      Duplicate_self
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS Duplicate,

    ROUND(SUM(
      GCInternal_self
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS GC,

    ROUND(SUM(
      cons_self        +
      allocList_self   +
      allocS4_self     +
      allocVector_self
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS MemAlloc,

    ROUND(SUM(
      doSubset_self     +
      doSubset2_self    +
      doSubset3_self    +
      doSubassign_self  +
      doSubassign2_self +
      doSubassign3_self
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS Subset,

    ROUND(SUM(
      EvalList_self
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS EvalList,

    ROUND(SUM(
      doArith_self      +
      doMatprod_self    +
      doLogic_self      +
      doLogic2_self     +
      doLogic3_self     +
      doRelop_self      +
      bcEvalArith1_self +
      bcEvalArith2_self +
      bcEvalMath1_self  +
      bcEvalRelop_self  +
      bcEvalLogic_self
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS Arith,

    ROUND(SUM(
      bcEval_self           + -- FIXME?
      BuiltinSum_self  +
      SpecialSum_self  +
      do_internal_self
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS BuiltIn_Special,

    ROUND(SUM(
      Startup_self          +
      Install_self          +
      Repl_self             +
      UserFunctionSum_self  +
      UserFuncFallback_self +
      setupMainLoop_self    +
      endMainLoop_self      +
      onExits_self          +
      gzFile_self           +
      bzFile_self           +
      xzFile_self           +
      doUnzip_self          +
      zipRead_self          +
      Download_self         +
      Rsock_self            +
      Sleep_self            +
      System_self
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS Other,

    ROUND(SUM(
      TotalRuntime
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4) AS TotalRuntime
  FROM TimingResults_pivot
ORDER BY NAME;

--
-- categorized runtimes as percentage of total
--
DROP VIEW IF EXISTS runtime_details_pct;
CREATE VIEW runtime_details_pct AS
  SELECT
    name,
    ROUND(100 * External        / CAST(TotalRuntime AS REAL), 4) AS External,
    ROUND(100 * Lookup          / CAST(TotalRuntime AS REAL), 4) AS Lookup,
    ROUND(100 * Match           / CAST(TotalRuntime AS REAL), 4) AS Match,
    ROUND(100 * Duplicate       / CAST(TotalRuntime AS REAL), 4) AS Duplicate,
    ROUND(100 * GC              / CAST(TotalRuntime AS REAL), 4) AS GC,
    ROUND(100 * MemAlloc        / CAST(TotalRuntime AS REAL), 4) AS MemAlloc,
    ROUND(100 * Subset          / CAST(TotalRuntime AS REAL), 4) AS Subset,
    ROUND(100 * EvalList        / CAST(TotalRuntime AS REAL), 4) AS EvalList,
    ROUND(100 * Arith           / CAST(TotalRuntime AS REAL), 4) AS Arith,
    ROUND(100 * BuiltIn_Special / CAST(TotalRuntime AS REAL), 4) AS BuiltIn_Special,
    ROUND(100 * Other           / CAST(TotalRuntime AS REAL), 4) AS Other
  FROM runtime_details_abs
ORDER BY NAME;

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
    (allocatedlargevectors_allocs + allocatedsmallvectors_allocs +
     allocatedonevectors_allocs) * 40 + allocatedzerovectors_allocs * 56 +
    allocatedexternal + allocatedlargevectors_size +
    allocatedsmallvectors_size + allocatedonevectors_size +
    allocatedzerovectors_size + allocatedstringbuffer_size) / 1024.0 / 1024.0
      AS total_mbytes
  FROM TraceResults_pivot

UNION ALL SELECT
  " Average",
    sum(rusagemaxresidentmemoryset) / 1024.0 / cast(count(rusagemaxresidentmemoryset) as real)
      as RUsageMemory_mbytes,
    (sum(allocatedcons) + sum(allocatedpromises) + sum(allocatedenv) +
    sum(allocatedsxp) + sum(allocatednoncons) +
    -- calculate size of list headers
    (sum(allocatedlargevectors_allocs) + sum(allocatedsmallvectors_allocs) +
     sum(allocatedonevectors_allocs)) * 40 + sum(allocatedzerovectors_allocs) * 56 +
    sum(allocatedexternal) + sum(allocatedlargevectors_size) +
    sum(allocatedsmallvectors_size) + sum(allocatedonevectors_size) +
    sum(allocatedzerovectors_size) + sum(allocatedstringbuffer_size)) / 1024.0 / 1024.0
      / cast(count(allocatedcons) as real)
      AS total_mbytes
  FROM TraceResults_pivot

order by name;

--
-- memory usage in bytes, categorized
--
DROP VIEW IF EXISTS memory_used_abs;
CREATE VIEW memory_used_abs AS
  SELECT
    name,

    cons_bytes +
    lists_bytes
      AS Lists,

    -- include header sizes in this value
    (zerovec_bytes  + 56 * zerovec_count ) +
    (onevec_bytes   + 40 * onevec_count  ) +
    (smallvec_bytes + 40 * smallvec_count) +
    (largevec_bytes + 40 * largevec_count)
      AS Vectors,

    promises_bytes
      AS Promises,

    env_bytes
      AS Environments,

    external_bytes
      AS External,

    stringbuffer_bytes_needed +
    sexp_bytes
      AS Other,

    total_bytes
      AS Total_Bytes

  FROM memory_used

UNION ALL

  SELECT
    " Average",

    ROUND(SUM(
      cons_bytes +
      lists_bytes
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4),

    ROUND(SUM(
      zerovec_bytes  + 56 * zerovec_count  +
      onevec_bytes   + 40 * onevec_count   +
      smallvec_bytes + 40 * smallvec_count +
      largevec_bytes + 40 * largevec_count
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4),

    ROUND(SUM(
      promises_bytes
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4),

    ROUND(SUM(
      env_bytes
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4),

    ROUND(SUM(
      external_bytes
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4),

    ROUND(SUM(
      stringbuffer_bytes_needed +
      sexp_bytes
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4),

    ROUND(SUM(
      total_bytes
    ) / CAST(COUNT(DISTINCT trace_id) AS REAL), 4)

  FROM memory_used ORDER BY name;


--
-- memory usage in percent of total bytes used
--
DROP VIEW IF EXISTS memory_used_pct;
CREATE VIEW memory_used_pct AS
  SELECT
    name,
    ROUND(100 * Lists        / CAST(Total_Bytes AS REAL), 4) AS Lists,
    ROUND(100 * Vectors      / CAST(Total_Bytes AS REAL), 4) AS Vectors,
    ROUND(100 * Promises     / CAST(Total_Bytes AS REAL), 4) AS Promises,
    ROUND(100 * Environments / CAST(Total_Bytes AS REAL), 4) AS Environments,
    ROUND(100 * External     / CAST(Total_Bytes AS REAL), 4) AS External,
    ROUND(100 * Other        / CAST(Total_Bytes AS REAL), 4) AS Other
  FROM memory_used_abs
ORDER BY NAME;


--
-- memory allocated to the four vector classes
--
drop view if exists vector_sizes;
create view vector_sizes as
  select
    name,
    allocatedlargevectors_size + 40 * allocatedlargevectors_allocs as large_bytes,
    allocatedsmallvectors_size + 40 * allocatedsmallvectors_allocs as small_bytes,
    allocatedonevectors_size + 40 * allocatedonevectors_allocs as one_bytes,
    allocatedzerovectors_size + 56 * allocatedzerovectors_allocs as zero_bytes,
    allocatedlargevectors_size + allocatedsmallvectors_size + allocatedonevectors_size +
    40 * (allocatedlargevectors_allocs + allocatedsmallvectors_allocs + allocatedonevectors_allocs) as total_bytes_nozero,
    allocatedlargevectors_size + allocatedsmallvectors_size + allocatedonevectors_size + allocatedzerovectors_size +
    40 * (allocatedlargevectors_allocs + allocatedsmallvectors_allocs + allocatedonevectors_allocs) +
    56 * allocatedzerovectors_allocs as total_bytes
  from TraceResults_pivot
  order by name;

-- relative vector sizes, by allocated memory
drop view if exists vector_sizes_pct;
create view vector_sizes_pct as
  select
    name,
    round(100 * zero_bytes / cast(total_bytes as real), 2) as zero_pct,
    round(100 * one_bytes / cast(total_bytes as real), 2) as one_pct,
    round(100 * small_bytes / cast(total_bytes as real), 2) as small_pct,
    round(100 * large_bytes / cast(total_bytes as real), 2) as large_pct
  from vector_sizes
UNION ALL
  SELECT
    " Average",
    round(100 * sum(zero_bytes) / cast(sum(total_bytes) as real), 2),
    round(100 * sum(one_bytes) / cast(sum(total_bytes) as real), 2),
    round(100 * sum(small_bytes) / cast(sum(total_bytes) as real), 2),
    round(100 * sum(large_bytes) / cast(sum(total_bytes) as real), 2)
  from vector_sizes
order by name;

-- relative vector counts (i.e. number of vectors, not elements)
DROP VIEW IF EXISTS vector_counts;
CREATE VIEW vector_counts AS
  SELECT
    name,
    zero_count_pct_of_allvectors AS zero_count_pct,
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
    FROM TimingResults_pivot
  UNION ALL SELECT
    " Average",
    SUM(totalruntime / 1e9) / cast(count(totalruntime) AS REAL)
    FROM TimingResults_pivot
  ORDER BY name;


-- percentage of calls with n arguments
DROP VIEW IF EXISTS argument_count_pct;
CREATE VIEW argument_count_pct AS
  SELECT
    Traces.name AS name,
    ArgumentCounts.count,
    ROUND(100 * CAST(ArgumentCounts.calls AS REAL) / TotalCalls, 2) AS CallPercentage
  FROM
    ArgumentCounts JOIN Traces JOIN (
      SELECT
        trace_id,
        SUM(calls) AS TotalCalls
      FROM
        ArgumentCounts GROUP BY trace_id
    ) AS SumTable ON
      ArgumentCounts.trace_id = Traces.id AND
      SumTable.trace_id = ArgumentCounts.trace_id
  UNION ALL SELECT
    " Average",
    ArgumentCounts.count,
    ROUND(100 * CAST(SUM(ArgumentCounts.calls) AS REAL) / TotalCalls, 2)
  FROM
    ArgumentCounts JOIN (
      SELECT
        SUM(calls) AS TotalCalls
      FROM
        ArgumentCounts
    ) AS SumTable
  GROUP BY count
  ORDER BY name;

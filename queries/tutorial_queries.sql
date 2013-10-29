-- Find prim calls by names:

DROP VIEW IF EXISTS prim_calls_by_name;
CREATE VIEW IF NOT EXISTS prim_calls_by_name AS

SELECT name, id, sum(nb)
FROM locations left join calls on id=location_id
WHERE name in ('eval', 'eval.with.vis')
GROUP by location_id ;

--    Full running time by arguments :

DROP VIEW IF EXISTS running_time_full;
-- CREATE VIEW IF NOT EXISTS running_time_full AS

-- SELECT
-- name,
-- run_cmd_args,
-- -- note: timerunit not available in time_summary
-- -- mainloop_total / timerunit 
-- TotalRuntime
-- FROM time_summary left join traces on time_summary.id = traces.id;
-- -- Note that the time unit is weird and is computed through an idle loop.

--    Breakdown of runtimes (i.e. the fig 8 of ecoop)

DROP VIEW IF EXISTS running_time_details;
CREATE VIEW IF NOT EXISTS running_time_details AS

SELECT
  name,
  TotalRuntime,
  FunLookup_self,
  SymLookup_self,
  FindVarInFrame3other_self,
  -- Eval_self + bcEval_self + dotBuiltIn_self AS Eval_withbc_self,
  EvalList_self,
  Match_self,
  Duplicate_self,
  Duplicate_total,
  dotC_self,
  dotFortran_self,
  dotCall_self,
  dotExternal_self,
  dotC_self + dotFortran_self + dotCall_self + dotExternal_self
    AS ExternalCode_selfsum,
  gcinternal_self,
  install_self,
  allocVector_self,
  allocList_self,
  cons_self,
  dosubset_self + dosubset2_self + dosubset3_self as Subsets_selfsum,
  doarith_self + domatprod_self + dologic_self + dologic2_self +
    dorelop_self as arith_selfsum,
  builtinsum_self + specialsum_self + 
    dotSpecial2_self
    AS otherbuiltinspecial_selfsum,
  startup_total,
  startup_self
  
FROM time_summary
left join traces
where traces.id=time_summary.id
order by time_summary.id;

--
-- runtime as percentage of total
--
DROP VIEW IF EXISTS running_time_details_pct;
CREATE VIEW IF NOT EXISTS running_time_details_pct AS

SELECT
  name,
  ROUND(100 * FunLookup_self / CAST(TotalRuntime AS REAL), 4) AS FunLookup,
  ROUND(100 * SymLookup_self / CAST(TotalRuntime AS REAL), 4) AS SymLookup,
  ROUND(100 * FindVarInFrame3other_self / CAST(TotalRuntime AS REAL), 4) AS FindVarInFrame3other,
  ROUND(100 * EvalList_self / CAST(TotalRuntime AS REAL), 4) AS EvalList,
  ROUND(100 * Match_self / CAST(TotalRuntime AS REAL), 4) AS Match,
  ROUND(100 * Duplicate_self / CAST(TotalRuntime AS REAL), 4) AS Duplicate_self,
  ROUND(100 * Duplicate_total / CAST(TotalRuntime AS REAL), 4) AS Duplicate_total,
  ROUND(100 * dotC_self / CAST(TotalRuntime AS REAL), 4) AS dotC,
  ROUND(100 * dotFortran_self / CAST(TotalRuntime AS REAL), 4) AS dotFortran,
  ROUND(100 * dotCall_self / CAST(TotalRuntime AS REAL), 4) AS dotCall,
  ROUND(100 * dotExternal_self / CAST(TotalRuntime AS REAL), 4) AS dotExternal,
  ROUND(100 * (dotC_self + dotFortran_self + dotCall_self + dotExternal_self) /
    CAST(TotalRuntime AS REAL), 4)
    AS ExternalCode,
  ROUND(100 * gcinternal_self / CAST(TotalRuntime AS REAL), 4) AS garbagecoll,
  ROUND(100 * install_self / CAST(TotalRuntime AS REAL), 4) AS install,
  ROUND(100 * allocVector_self / CAST(TotalRuntime AS REAL), 4) AS allocVector,
  ROUND(100 * allocList_self / CAST(TotalRuntime AS REAL), 4) AS allocList,
  ROUND(100 * cons_self / CAST(TotalRuntime AS REAL), 4) AS cons,
  ROUND(100 * (dosubset_self + dosubset2_self + dosubset3_self) /
    CAST(TotalRuntime AS REAL), 4) AS Subsets_selfsum,
  ROUND(100 * (doarith_self + domatprod_self + dologic_self + dologic2_self +
    dorelop_self) / CAST(TotalRuntime AS REAL), 4) AS arith_selfsum,
  ROUND(100 * (builtinsum_self + specialsum_self +
    dotSpecial2_self) /
    CAST(TotalRuntime AS REAL), 4)
      AS otherbuiltinspecial_selfsum,
  ROUND(100 * startup_total / CAST(TotalRuntime AS REAL), 4) AS startup_total,
  ROUND(100 * startup_self / CAST(TotalRuntime AS REAL), 4) AS startup_self
  
FROM time_summary
left join traces
where traces.id=time_summary.id
order by time_summary.id;

--
-- test query
--
DROP VIEW IF EXISTS running_time_test;
CREATE VIEW running_time_test AS
  SELECT
    name,
    FunLookup       +
    SymLookup       +
    FindVarInFrame3other +
    EvalList        +
    Match           +
    Duplicate_self  + -- do not add total!
    ExternalCode    +
    garbagecoll     +
    install         +
    allocVector     +
    allocList       +
    cons            +
    Subsets_selfsum +
    arith_selfsum   +
    otherbuiltinspecial_selfsum +
    startup_self
    AS Summe
  FROM running_time_details_pct;

--- Note: this result is not really readeable thus a script is provided to draw
--- this chart, it'll be presented a later
--- Note: this chart is bit deprecated since all columns have changed, I 'll
--- fix it soon

--    Memory used

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

    -- R rundet auf, bytes_roundedup ist die reale Speicheranforderung
    -- und bytes_needed nur die Anzahl die eigentlich gebraucht würde
    -- (hoffe ich)
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
-- memory usage in percent of total bytes used
--
DROP VIEW IF EXISTS memory_used_pct;
CREATE VIEW IF NOT EXISTS memory_used_pct AS
  -- note: Includes SEXP headers for everything
  SELECT
    name,
    ROUND(100 * RUsageMemory_bytes / CAST(total_bytes AS REAL), 4)
      AS RUsage_pct,
    ROUND(100 * cons_bytes / CAST(total_bytes AS REAL), 4)
      AS cons_pct,
    ROUND(100 * promises_bytes / CAST(total_bytes AS REAL), 4)
      AS promises_pct,
    ROUND(100 * env_bytes / CAST(total_bytes AS REAL), 4)
      AS env_pct,
    ROUND(100 * external_bytes / CAST(total_bytes AS REAL), 4)
      AS external_pct,
    ROUND(100 * sexp_bytes / CAST(total_bytes AS REAL), 4)
      AS sexp_pct,
    ROUND(100 * lists_bytes / CAST(total_bytes AS REAL), 4)
      AS lists_pct,
    ROUND(100 * (largevec_bytes + 56 * largevec_count) / CAST(total_bytes AS REAL), 4)
      AS largevec_pct,
    ROUND(100 * (smallvec_bytes + 56 * smallvec_count) / CAST(total_bytes AS REAL), 4)
      AS smallvec_pct,
    ROUND(100 * (onevec_bytes + 56 * onevec_count) / CAST(total_bytes AS REAL), 4)
      AS onevec_pct,
    ROUND(100 * (nullvec_bytes + 56 * nullvec_count) / CAST(total_bytes AS REAL), 4)
      AS nullvec_pct,
    ROUND(100 * stringbuffer_bytes_needed / CAST(total_bytes AS REAL), 4)
      AS stringbuffer_needed_pct
  FROM memory_used;

DROP VIEW IF EXISTS memtest;
CREATE VIEW memtest AS
SELECT name, cons_pct + promises_pct + env_pct + external_pct + sexp_pct + lists_pct + largevec_pct + smallvec_pct + onevec_pct + nullvec_pct + stringbuffer_needed_pct as summe from memory_used_pct;

--
-- vector detail view
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
      AS one_byte_percentage_of_allvectors,
    100 * allocatedsmallvectors_size / CAST(allocatedvectors_size AS REAL)
      AS small_byte_percentage_of_allvectors,
    100 * allocatedlargevectors_size / CAST(allocatedvectors_size AS REAL)
      AS large_byte_percentage_of_allvectors,
    -- FIXME: Ist hier auch als Prozentwert des Gesamtspeichers
    -- inkl. SEXP-Headern gewünscht?

    ROUND(100 * allocatednullvectors_tl / CAST(allocatedvectors_tl AS REAL), 4)
      AS null_count_percentage_of_allvectors,
    ROUND(100 * allocatedonevectors_tl / CAST(allocatedvectors_tl AS REAL), 4)
      AS one_count_percentage_of_allvectors,
    ROUND(100 * allocatedsmallvectors_tl / CAST(allocatedvectors_tl AS REAL), 4)
      AS small_count_percentage_of_allvectors,
    ROUND(100 * allocatedlargevectors_tl / CAST(allocatedvectors_tl AS REAL), 4)
      AS large_count_percentage_of_allvectors

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


--- Note: According to our experiments user NEVER EVER use cons !!! The
--- standard library has only one call to is.pairlist and no cons call. This
--- is also the case in the MLOC that we've anaalyzed. Thus we can mostly
--- safely assert that cons are allocated byt the evaluator for it's own needs
--- (i.e. mostly function calls)

--- Note: String buffers are mostly inexistant
--- Note: Since a commit has been lost the dead of the ecoop submission (i.e.
--- `rm` BEFORE commit but AFTER generating fig 8) I have to check all these counters

--- Slight variations of this query may give more categories. However S3
--- objects CAN'T be counted (as fas I know) and S4 objects are not accurate
--- (Since the object bit may be switched `on` or `off`)

--    Number of arguments

-- There are two different way to obtain this information. One of those contains a bias thus it will not be discussed here. The
-- other one is mostly accurate with the following restriction: All numbers are bounded by 255 (this is done on purpose to
-- avoid trace size explosion, i.e. they are store on a byte).

DROP VIEW IF EXISTS number_of_args;
CREATE VIEW IF NOT EXISTS number_of_args AS

SELECT
value,
sum(param) as param,
sum(position) as position,
sum(keywords) as kw, sum(rest) as rest,
count(value)
FROM calls_frequency
WHERE not (position == 0 and keywords == 0 and rest == 0)  or value=0
GROUP by value
order by 1;

-- Static information will be extracted in the next section (currently not written)

--    Number of promises created vs. evaluated

DROP VIEW IF EXISTS number_of_promises;
CREATE VIEW IF NOT EXISTS number_of_promises AS

SELECT sum(sure) as sure, sum(eval) as eval, sum(same_level) as same_level
FROM promises left join vignettes on trace_id=id
GROUP by trace_id order by trace_id;

-- It is also possible to know which functions are not evaluating their arguments.

DROP VIEW IF EXISTS unevaluated_args;
CREATE VIEW IF NOT EXISTS unevaluated_args AS

SELECT sum(unevaled)/sum(calls.nb), promises.location_id, files.name, line, col, locations.name
FROM promises left join calls left join locations left join files
WHERE promises.location_id=calls.location_id and promises.trace_id=calls.trace_id and locations.id=promises.location_id and files.id=file
GROUP by promises.location_id order by 1 desc limit 10;

--    Number of assignment vs definition

--CREATE VIEW IF NOT EXISTS assignment_vs_definition AS

--SELECT  (definevar_local+definevar_other) as intro, (applydefine_other + applydefine_local + setvar_local + setvar_other) as assign
--FROM summary ORDER by id;

DROP VIEW IF EXISTS assignment_vs_definition;
CREATE VIEW IF NOT EXISTS assignment_vs_definition AS

SELECT
(definevar_local+definevar_other) as intro, 
(applydefine_local + setvar_local) as assign_local,
(applydefine_other + setvar_other) as assign_super

FROM summary ORDER by id;


--- Note: local are <- other are <<-

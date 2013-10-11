DROP VIEW IF EXISTS alloc_summary;
DROP VIEW IF EXISTS alloc_summary_pct;

-- allocation percentages for alloc_summary_pivot
-- display allocation values as percentage relative to
-- the sum of all vector allocations that have been traced
-- Note: The size is relative to the sum of all vector elements,
--       not the entire R interpreter memory use!
--       This is because the size values only track the size of
--       the vector elements, ignoring the SEXPREC header for each of them.
DROP VIEW IF EXISTS alloc_summary_pct_noheader;
CREATE VIEW IF NOT EXISTS alloc_summary_pct_noheader AS

SELECT
   summary_id,
   label as label,
   ROUND(100 * total/CAST(sumtl AS REAL), 4) as tl_pct, -- number of allocations in this class
   ROUND(100 * elements/CAST(sumelts AS REAL), 4) as elts_pct, -- number of elements allocated
   ROUND(100 * size/CAST(sumsize AS REAL), 4) as size_pct  -- byte size of the allocations
FROM alloc_summary_pivot
JOIN (
  SELECT
    summary_id as joinid,
    sum(total) as sumtl,
    sum(elements) as sumelts,
    sum(size) as sumsize
  FROM alloc_summary_pivot GROUP BY summary_id
) ON summary_id = joinid
-- ORDER BY size_pct desc
-- ORDER BY alloc_elts_pct desc -- to use elts column for sorting
ORDER BY summary_id, label
;

-- Same as above, but tries to include the SEXPREC header
-- sizes for everything. Partially fails for list and cons
-- because allocList() calls cons() multiple times and
-- does not adjust the allocated_cons value afterwards.
-- FIXME: Assumes a size of 56 bytes for the SEXPREC header
DROP VIEW IF EXISTS alloc_summary_pct_withheader;
CREATE VIEW IF NOT EXISTS alloc_summary_pct_withheader AS

SELECT
   summary_id,
   label as label,
   ROUND(100 * total/CAST(sumtl AS REAL), 4) as tl_pct, -- number of allocations in this class
   ROUND(100 * elements/CAST(sumelts AS REAL), 4) as elts_pct, -- number of elements allocated
   ROUND(100 * (size + 56 * total)/CAST(sumsize AS REAL), 4) as size_pct  -- byte size of the allocations
FROM alloc_summary_pivot
JOIN (
   SELECT summary_id as joinid,
          sum(total) as sumtl,
          sum(elements) as sumelts,
          sum(size) + 56 * sum(total) as sumsize
   FROM alloc_summary_pivot GROUP BY summary_id
) ON summary_id = joinid
-- ORDER BY size_pct desc
-- ORDER BY alloc_elts_pct desc -- to use elts column for sorting
ORDER BY summary_id, label
;


-- time percentages for time_summary_pivot
-- display time as percentage relative to the sum of all times that have been measured
-- FIXME

DROP VIEW IF EXISTS time_summary_pct;
-- CREATE VIEW IF NOT EXISTS time_summary_pct AS

-- SELECT
--    summary_id,
--    label as label, -- name of the timer
--    100 * self/CAST(total_interp_runtime AS REAL) as self_pct,   -- time in function (selftime?)
--    -- FIXME: Not sure if the total percentages are sensible
--    -- especially because the total for Funtab/userfns are missing
--    100 * total/CAST(sumtotal AS REAL) as total_pct -- time in function (entry to exit)
-- FROM time_summary_pivot
-- JOIN (
--   SELECT
--     summary_id as joinid,
--     sum(time_summary_pivot.total) as sumtotal,
--     time_summary.totalruntime as total_interp_runtime
--   FROM time_summary_pivot, time_summary
--   GROUP BY summary_id
-- ) ON summary_id = joinid
-- --ORDER BY self_pct DESC
-- ORDER BY summary_id, label

-- ;


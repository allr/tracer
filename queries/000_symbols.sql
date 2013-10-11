-- calls + declaring file + symbolname
-- this maps the entries in the calls table to the symbol and filenames given in the locations and files tables
-- so the names of the given call locations can be resolved
DROP VIEW IF EXISTS calls_symbols;
CREATE VIEW IF NOT EXISTS calls_symbols AS

SELECT
c.name as filename,
b.name as symname,
a.*
FROM calls AS a
LEFT JOIN locations AS b
  ON (a.location_id = b.id)
LEFT JOIN files AS c
  ON (b.file = c.id)
order by a.nb desc

;

-- foreigns + call name (.C or .Call)
-- name mapping for foreign function calls
DROP VIEW IF EXISTS foreigns_symbols;
CREATE VIEW IF NOT EXISTS foreigns_symbols AS


SELECT 
b.name, -- symbol name
a.* -- all data from foreigns, TODO this will be changed to only the meaningful values after some more analyses.
FROM foreigns AS a
LEFT JOIN locations AS b
  ON (a.location_id = b.id)
;

-- locations and their file position
-- locations are mapped to their file positions to show which functions were detected by traceR and how they were identified.
DROP VIEW IF EXISTS symbols_files;
CREATE VIEW IF NOT EXISTS symbols_files AS

SELECT
b.id as symid, -- location unique id
b.name as symname, -- symbol name
c.name as filename, -- file name
b.line as line -- this is either a real line in a file (for .R files) or an increasing index for RINTERNAL, or possibly a pointer location for other
               -- locations that cannot directly be mapped to files
FROM locations AS b
LEFT JOIN files AS c
  ON (b.file = c.id)
order by filename, line asc

;

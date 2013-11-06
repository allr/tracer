This directory contains a few SQL files that create sample views which
summarize the information gathered in the database. They are
automatically applied by the tracer.sh script after each run. Some of
the views are visualized by the demonstration plot script "plotall.sh"
in the scripts subdirectory of traceR.

All views assume that R is running on a 64-bit system where the size
of an SEXPREC data structure is 56 bytes.

Views defined in pivots.sql
---------------------------
pivots.sql defines two pivot views, i.e. they contain the same data as
an already existing table, but rotated from a values-in-columns to a
values-in-rows format. This can be useful for queries that want to
summarize data over all test runs in the database.

- alloc_summary_pivot
    This view holds data about the memory allocations in R, organized
    as a row per allocation type with columns for the total number of
    allocations (total), the number of elements for this allocation
    (elements), the size in bytes (size) and the actual size in bytes
    (asize). For some of these entries no corresponding value in the
    summary table exists, so the values are synthesized - please check
    the actual SQL statement for details.

- time_summary_pivot
    This view holds data about the time spent in various parts of the
    interpreter. This view will be replaced by a table with more data
    in the future as the current version of traceR is not able to read
    per-function timings from the timeR output.


Views defined in sample-views.sql
---------------------------------
sample-views.sql defines a few views that perform sample analysis
functions on the contents of the database.

- memory_used
    The memory_used view summarizes data from the "summary" table
    related to memory usage and performs a few simple calculations on
    them to normalize data, e.g. to compensate for known interference
    between values (cons_bytes), to sum seperate counters which for
    most purposes can be seen as counting the same thing (sexp_bytes)
    or to convert values to bytes for consistency (RUsageMemory_bytes,
    lists_bytes). Additionally this view provides the average number
    of elements per allocated list (lists_avg_elts) and the total
    number of bytes allocated during the interpreter execution,
    including all of the SEXP header structures needed for the
    allocations (total_bytes).

- vector_details
    The vector_details view calculates statistics about vector memory
    allocations. For every program in the database, the average number
    of elements per vector, number of bytes per vector and number of
    elements per vector are calculated for all, one-element, small and
    large vectors. Additionally the percentages of null/one/small/large
    vectors compared to all vectors are calculated for the number of
    bytes and the number of vectors. Byte sizes in this table ignore
    the SEXP header block that the R interpreter creates for each
    object in memory.

- external_vs_interpreter
    The external_vs_interpreter provides the (absolute) run time spent
    in the R interpreter and the time spent in external code (called
    via .C/.Fortran/.Call/.External).

- external_vs_interpreter_pct
    The external_vs_interpreter_pct view calculates the percentage of
    time spent in interpreter or external code based on the values
    calculated in the external_vs_interpreter view.

- runtime_details_pct
    The runtime_details_pct view provides a more coarse-grained view
    on the time measurements of interpreter internals provided by
    timeR, converted to percentages. The total execution time is split
    into eleven categories:

    - External
        Time spent in code called via .C/.Fortran/.External/.Call.
        Since the standard GNU R distribution uses these functions
        itself to call code in some of its included packages, this
        value does not directly correspond to time spent in native
        code provided by additional R packages.
    - Lookup
        Time spent in symbol lookups, e.g. for function calls or
        references to variables
    - Match
        Time spent in argument matching for function calls
    - Duplicate
        Time spent to duplicate objects
    - GC
        Time spent in garbage collection
    - MemAlloc
        Time spent in various memory allocation functions
    - Subset
        Time spent in subsetting functions
    - Eval
        Time spent evaluating arguments for R builtins
    - Arith
        Time spent in arithmetic functions (including matrix
        multiplication)
    - BuiltIn_Special
        Time spent in builtin/special functions not covered by any
        other of these categories.
    - Other
        Time spent in code not covered by any other of these
        categories

    This view also provides a row labelled " Average" which shows
    the average of these values over all programs in the database.

- memory_used_vs_alloc
    The memory_used_vs_alloc view shows the total amount of memory
    allocated by the R interpreter and the peak amount of RAM given to
    the R interpreter by the operating system, both values in
    MiBytes. This view also provides a row labelled " Average" which
    shows the average of these values over all programs in the
    database.

- memory_used_pct
    The memory_used_pct view shows the percentages of memory allocated
    for various types of objects within the R interpreter. The view
    divides the memory allocations into six categories:

    - Lists
        The Lists column summarizes cons cells which are used to
        construct pairlists.
    - Vectors
        The vectors column summarizes all four classes of vectors
        (zero-, one-element, short, long)
    - Promises
        The Promises column summarizes the memory used for creating
        promises.
    - Enviroments
        The Environments column summarizes the memory used for
        creating environments. For technical reasons only the memory
        for the environment header is considered here, not the memory
        for the data stored in the environment which will be accounted
        for in its data type instead.
    - External
        The External column summarizes a certain type of memory
        allocation in the R memory management system that is used by
        external packages. Memory that is allocated outside of the R
        memory management (e.g. malloc) or that has a type covered by
        another column in this table is not considered here.
        (technical detail: This column only counts R_alloc/S_alloc)
    - Other
        The Other column summarizes other types of memory allocations,
        specifically string buffers and "generic" SEXP allocations.

    This view also provides a row labelled " Average" which shows the
    average of these values over all programs in the database.

- vector_sizes
    The vector_sizes view shows the amount of memory allocated for the
    four classes of vectors (zero-/one-element, small, large). The
    values in this view are in bytes and do include the size of the
    object header that R needs for its internal memory management.

- vector_sizes_pct
    The vector_sizes_pct uses the data from the vector_sizes view and
    calculates percentages relative to the total amount of memory used
    by vectors from it. This view also provides a row labelled
    " Average" which shows the average of these values over all
    programs in the database. The average is calculated based on the
    average of the original byte values, not as an average of the
    percentages.

- vector_counts
    The vector_counts view selects four columns from the
    vector_details view that hold the percentage of the four vector
    classes compared to the total number of vectors. It simplifies the
    automatic generation of the demostration plots.

- total_runtimes
    The total_runtimes view shows the total runtime of each program in
    the database converted to seconds (assuming that timeR was
    compiled to use a nanosecond-based timing method which is the
    default) as well as an average runtime over all programs in the
    database.


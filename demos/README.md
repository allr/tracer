Demonstration R programs
========================

This directory contains a few simple R programs that are used to
demonstrate the analysis capabilities of the traceR system.

- matmult-small-loops.R

    Simple matrix multiplication using the "by the book" method with
    three nested loops on two "small" matrices (150x150).
    This is by far the slowest method of multiplying matrices in
    R. Running it via traceR shows that it uses most of its allocated
    memory for lists instead of vectors which are generated when the
    arithmetic functions in the inner loop are called. It also uses a
    lot more single-element vectors than the other versions as each
    calculation is done by extracting a single value from the input
    matrices.

- matmult-small-op.R

    The same matrix multiplication as matmult-small-loops.R, but done
    using the matrix multiplication operator (%*%). The total runtime
    is much smaller for this version and the detailed breakdown of
    execution times measured by traceR shows that the run time of this
    program is actually dominated by non-arithmethic builtins of R
    instead of the matrix multiplication itself.

- matmult-large.R

    Another matrix multiplication using the %*% operator, but on a
    larger matrix size (1500x1500). The runtime breakdown is now
    dominated by arithmethic functions, i.e. the matrix
    multiplication. The vector sizes are now heavily skewed towards
    large vectors, even though there is a larger number of small
    vectors.

- matmult-large-na.R

    A fourth version of matrix multiplication, again using the %*%
    operator and 1500x1500 matrices. The difference to the previous
    version is the introduction of two NA values in the two input
    matrices which force the interpreter to use a fallback "by the
    book" calculation instead of the optimized BLAS routines. As this
    fallback is implemented in the C code of the interpreter, it has
    no influence on the memory allocation pattern.

- mandelbrot.R

    Since matrix multiplications are a bit boring a simple Mandelbrot
    calculator has also been included. It uses a very stupid way of
    iterating the per-pixel function that does not use any early
    exits for escape values, but because of R's ability to use basic
    arithmetic on entire matrices of values at once this approach is
    better suited for R than standard single-pixel methods that stop
    iterating when a value is confirmed to have escaped. The memory
    allocation profile of this program shows that almost all
    allocations (by size) are used for vectors and among vectors
    almost all allocations are for large vectors, caused by the large
    number of temporary matrices generated during calculation. This
    also shows up in the time profile as a ~20% chunk of time spent in
    garbage collection.

- memwaste.R

    A simple script that does nothing useful, but has a non-flat
    memory-over-time curve.

- paralleldemo.R

    A script that runs matrix multiplication in parallel on multiple
    cores using the `parallel` package. This is the only script that
    is shown in the `parallel.pdf` plot because it is the only one
    that uses more than one process.

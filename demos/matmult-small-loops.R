# by-the-book matrix multiplication - very slow, but nice for demonstrations

randommatrix <- function (n,m) {
  matrix(rnorm(n*m, mean=1, sd=1), n, m)
}

SIZE <- 150

a <- randommatrix(SIZE, SIZE)
b <- randommatrix(SIZE, SIZE)

result <- matrix(0, SIZE, SIZE)

for (i in 1:SIZE) {
  for (j in 1:SIZE) {
    result[i,j] <- 0
    
    for (k in 1:SIZE) {
      result[i,j] <- result[i,j] + a[i,k] * b[k,j]
    }
  }
}

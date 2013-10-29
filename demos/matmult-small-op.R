# matrix multiplication using the %*% operator

randommatrix <- function (n,m) {
  matrix(rnorm(n*m, mean=1, sd=1), n, m)
}

SIZE <- 150

a <- randommatrix(SIZE, SIZE)
b <- randommatrix(SIZE, SIZE)

result <- a %*% b

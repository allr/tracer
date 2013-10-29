# matrix multiplication using the %*% operator, large matrix

randommatrix <- function (n,m) {
  matrix(rnorm(n*m, mean=1, sd=1), n, m)
}

SIZE <- 1500

a <- randommatrix(SIZE, SIZE)
b <- randommatrix(SIZE, SIZE)

result <- a %*% b

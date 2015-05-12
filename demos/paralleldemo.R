library(parallel)

CORES = 4
JOBS  = 100

parfunc <- function (matsize) {
    a <- runif(matsize * matsize)
    b <- runif(matsize * matsize)
    dim(a) <- c(matsize, matsize)
    dim(b) <- c(matsize, matsize)

    res <- a %*% b

    matsize <- as.double(matsize)
    checkval <- sum(res) / (matsize * matsize * matsize) * 4

    checkval
}

matsizes <- floor(runif(JOBS) * 1000 + 600)

results1 <- do.call(c, mclapply(matsizes, parfunc, mc.cores = CORES))

print(mean(results1))


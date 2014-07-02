# simple memory-wasting script to show a non-flat memory-over-time curve

ref <- 1:10000

a <- ref

for (i in 1:1000) {
  a <- c(a,ref)
}

gc()
rm(a)
gc()

a <- ref

for (i in 1:1000) {
  a <- c(a,ref)
}

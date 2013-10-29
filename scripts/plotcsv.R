# ugly, but works more or less - improvements welcome
#
# args: csvname plotnamev (pct)
# if anything is passed for pct, max range is limited to 100 and
# the values are stacked

args <- commandArgs(trailingOnly = TRUE)

csvname  <- args[1]
plotname <- args[2]
do_pct   <- args[3]

stopifnot(!is.na(csvname))
stopifnot(!is.na(plotname))

data <- read.csv(csvname)

# calculate a set of colors
colorcount <- dim(data)[2] - 1
if (colorcount > 6) {
  # modulate luminance to increase difference of
  # adjacent colors
  colors <- sapply(seq(0, colorcount - 1),
                   function (i) { hcl(h = 360 * i / colorcount,
                                      c = 50,
                                      l = 80 + 10 * (i %% 2)) }
                   )
} else {
  # create evenly spaced list of colors
  colors <- sapply(seq(0, colorcount - 1),
                   function (i) { hcl(h = 360 * i / colorcount, c = 50) }
                   )
}

# select plot type
if (!is.na(do_pct)) {
  xlimit   <- c(0, 100)
  beside   <- FALSE
} else {
  xlimit <- NULL
  beside <- TRUE
}

plotdata <- apply(t(as.matrix(data)[,-1]), c(1,2), as.numeric)

# plot bars
pdf(plotname, height = (dim(data)[1]/2 + dim(data)[2])/2) # FIXME: Very rough approximation
par(mar=c(4 + length(names(data))*1.2,10,1,2), xpd=TRUE)

coords <- barplot(plotdata,
                  col = colors,
                  xlim = xlimit,
                  horiz = TRUE,
                  beside = beside,
                  legend.text = names(data)[-1],
                  args.legend = list(y=-2)) # FIXME: Legend position appears rather random

# modify text coordinates if neccessary
if (is.na(do_pct)) {
  labelcount <- length(data[[1]])
  fac <- length(coords) / labelcount
  sums <- double(labelcount)
  for (i in seq(1:fac)) {
    sums <- sums + coords[seq(i, length(coords), fac)]
  }

  sums <- sums / fac

  coords <- sums
}

# plot text
text(x=-1, y=coords, labels=data[[1]], xpd=TRUE, srt=0, adj=c(1,0.5))

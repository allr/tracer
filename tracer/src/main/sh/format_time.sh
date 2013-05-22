#!/bin/sh
DIR=.
[ "$1" ] && DIR=$1 shift
if [ "$1" != noregen ] ; then
	for i in  $DIR/*.time ; do 
		grep -H . $i | awk '/MainLoop/ { m = $2 ; split($1, arr, ":") }
		/(Full|External) / { a += $2}
		/(Call|External|Code) / { c += $2 }
		/(SharedExtern) / { s += $2 }
		END {	if( ( m > s ) || ( m == c && c == a && a == ""))
					print arr[1] " " m " " c " " a " " s
				else
					print arr[1] " " m " " c " " a " " s " WARNING" > "/dev/stderr"
			}' | grep -v '^\s*$'
	done > $DIR/all.times 2> $DIR/strange.times
	echo "Good (at least seems) times : `cat $DIR/all.times | wc -l`" | tee $DIR/summary.txt
	echo "Strange times : `cat $DIR/strange.times | wc -l`" | tee -a $DIR/summary.txt
fi

R --no-save << EOF | tee $DIR/R.out | grep pdf: | tee -a $DIR/summary.txt 
myplot <- function(file, x, y, xlab="", ylab="", ...){
	pdf(file)

	plot(x, y, xlab=xlab, ylab=ylab, pch=20, ...)

	m_y <- mean(y)
	m_x <- mean(x)

	cat(file, ":x:", min(y)," ", m_y, " ", max(y),"\n", sep='')
	cat(file, ":y:", min(x)," ", m_x, " ", max(x),"\n", sep='')

	abline(h=m_y, col = "gray60")
	abline(v=m_x, col = "gray60")
	abline(b=(m_y/m_x), a=0, col="gray60")
}

morethan <- function(x, pc, unit, lab="") {
	nb <- length(x)
	total <- sum(x)
	for(i in 1:length(x))
		if((sum(x[1:i]) / sum(x)) >= pc)
			break

	cat(lab, ":more than ", pc * 100, ":", i / unit * 100, "\n", sep='')
}

myhist <- function(file, x, xlab="", breaks=100, freq=TRUE, ...){
	pdf(file)
	if(freq){
		ylab <- "#"
		ylim <- c(0,50)
	} else {
		ylab <- "%"
		ylim <- c(0,.1)
	}
	h <- hist(x, breaks=breaks, freq=freq, ylim=ylim, xlab=xlab, ylab=ylab)
	morethan(h\$counts, .8, breaks, lab=file)
	morethan(h\$counts, .9, breaks, lab=file)
	morethan(h\$counts, .5, breaks, lab=file)
}

runtime <- read.table("$DIR/all.times", header=FALSE)

r <- ( runtime\$V2  )
n <- ( runtime\$V5 / r ) * 100
myplot("$DIR/time-native.pdf", n, r/1000000, xlab="Foreign time (%)", ylab="Runtime (s)") #, log="y")
myplot("$DIR/time-native-log.pdf", n, r/1000000, xlab="Foreign time (%)", ylab="Runtime (s)", log="y")

c <- 0
rr <- array()
nn <- array()
for(i in 1:length(r))
	if(r[i] > 10000000) {
		c <- c + 1
		rr[c] = r[i]
		nn[c] = n[i] 
	}

myplot("$DIR/time-native-over-10s.pdf", nn, rr, xlab="Foreign time (over 10s) (%)", ylab="Runtime (µs)")

myhist("$DIR/time-native-hist.pdf", n, xlab='Foreign time (%)')
myhist("$DIR/time-native-hist-pc.pdf", n, xlab='Foreign time (%)', freq=FALSE)
myhist("$DIR/time-native-hist-over-10s.pdf", nn, xlab='Foreign time (%)')
myhist("$DIR/time-native-hist-over-pc-10s.pdf", nn, xlab='Foreign time (%)', freq=FALSE)

realruntime <- runtime\$V2
foreigntime <- runtime\$V4 / realruntime * 100
myplot("$DIR/time-native-old.pdf", foreigntime, realruntime, xlab="Foreign time (dups) (%)", ylab="Runtime (µs)")

myhist("$DIR/time-native-hist-old.pdf", foreigntime, xlab='Foreign time (dups) (%)')
myhist("$DIR/time-native-hist-old-pc.pdf", foreigntime, xlab='Foreign time (dups) (%)', freq=FALSE)

realruntime <- runtime\$V2
foreigntime <- runtime\$V3 / realruntime * 100
myplot("$DIR/time-native-noprologue-old.pdf", foreigntime, realruntime, xlab="Foreign time (without param copy) (%)", ylab="Runtime (µs)")

foreigntime <- runtime\$V3
realruntime <- ( runtime\$V4 - foreigntime ) / foreigntime * 100
myplot("$DIR/time-native-copy.pdf", foreigntime, realruntime, xlab="Copy param. (µs)", ylab="Copy (%)")
EOF

grep -o "^.*pdf" $DIR/summary.txt |sort -u | xargs tar cjf $DIR-graphs.tar.bz2 $DIR/summary.txt $DIR/all.times

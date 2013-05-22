#!/bin/sh
MERGE=
[ "$1" -a "$1" = "merge" ] && MERGE=true && shift
REGEN=true
[ "$1" -a "$1" = "plot" ] && REGEN= && shift
R='R --no-save --no-restore --slave'
[ "$1" -a "$1" = "noplot" ] && R="/bin/echo -n" && shift
DB=rx.sqlite
[ "$1" ] && DB=$1 && shift
SUFFIX=
[ "$1" ] && SUFFIX=$1 && shift
[ -z "$C" ] && C='"black"'
[ "$1" ] && C="$1" && shift
[ -z "$P" ] && P='20'
[ "$1" ] && P="$1" && shift
[ -z "$L" ] && L="$C"

[ "$S" ] && S=", spaces=$S"

[ "$MERGE" ]  && SQLITE="sqlite3 -csv -separator ; $DB" || SQLITE="sqlite3 -header -csv -separator ; $DB"

CAT_TRACE="names prom-eval alloc copy avoid-copy assign callfreq allocations vectors"
CAT_TIME="names match duplicate fun funeval funwevaltime timebreakdown"
[ -z "$CATEGORY" ] && CATEGORY="TRACE TIME"
CATEGORY="`echo $CATEGORY | sed \"s/TRACE/$CAT_TRACE/\" | sed \"s/TIME/$CAT_TIME/\"`"

query () {
	[ -z "$REGEN" ] && return
	if echo "$CATEGORY" | grep "$1" > /dev/null ; then
		file=$1$SUFFIX.csv
		[ "$DEBUG" ] && file=/dev/stdout
		shift
		echo sqlite3 $DB \'$1\'
		if [ "$MERGE" ] ; then
			$SQLITE "$*" >> $file
		else
			$SQLITE "$*" > $file
		fi
	else
		echo "$CATEGORY" | grep "$1" 
		false
	fi
}

MAIN_TIME='MainLoop_atime - System_time - Download_time - Sleep_time - inSockConnect_time - inSockOpen_time - inSockRead_time - inSockWrite_time - CurlPerform_time - gzFile_time as main'
EXCLUDE_EMPTY='(not main is null and not main = 0)'

query names "select name from traces, vignettes where vignette_id=vignettes.id and status=0 order by traces.id;"

query prom-eval "SELECT sum(sure) as sure, sum(eval) as eval, sum(same_level) as same_level from promises left join vignettes on trace_id=id group by trace_id order by trace_id" && $R << EOF
options(scipen=10)
mysummary <- function(data, v = NULL){
	s = summary(data)
	s["Var"] <- var(data)
	if(is.null(v)) s else rbind(v, s)
}
v <- read.csv("prom-eval$SUFFIX.csv", sep=";")
pdf("prom-eval$SUFFIX.pdf")
plot(v\$sure, v\$eval/v\$sure, log="x", main="Promises evaled", xlab='# prom', ylab='% eval', pch=$P, col=$C)
abline(h=mean(v\$eval/v\$sure), col="gray60")
n <- read.csv("names$SUFFIX.csv", sep=";")
if(length($L) > 1) legend(x="topleft", 0, legend=t(n), col=$C, pch=$P, cex=.3)
pdf("prom-eval-hist$SUFFIX.pdf")
hist(v\$eval/v\$sure * 100, main=NULL, xlab=NULL, ylab=NULL, breaks=20, freq=TRUE, cex=1.2, col="pink") #, xlim=c(79,100), col="red")
abline(v=mean(v\$eval/v\$sure * 100), col="gray60")
print("Promises evaluated")
sum = mysummary(v\$eval/v\$sure * 100)
for(cat in 1:3){
	ss = subset(v, catgory==cat)
	sum = mysummary(ss\$eval/ss\$sure * 100, sum)
}
print(sum)
# lines(density(v\$eval/v\$sure))
pdf("prom-eval-samelevel$SUFFIX.pdf")
plot(v\$eval, v\$same_level/v\$eval, log="x", xlab='prom eval', ylab='% eval on samelevel', pch=$P, col=$C)
abline(h=mean(v\$same_level/v\$eval), col="gray60")
if(length($L) > 1) legend(x="topleft", 0, legend=t(n), col=$C, pch=$P, cex=.3)
pdf("prom-eval-samelevel-hist$SUFFIX.pdf")
hist(v\$same_level/v\$eval * 100, xlab=NULL, ylab=NULL,main=NULL, breaks=20, freq=T, cex=1.4, col="pink")
abline(v=mean(v\$same_level/v\$eval * 100), col="gray60")
print("Promises evaluated on same level")
sum = mysummary(v\$same_level/v\$eval * 100)
for(cat in 1:3){
	ss = subset(v, catgory==cat)
	sum = mysummary(ss\$same_level/ss\$eval * 100, sum)
}
print(sum)
EOF

query alloc "select allocatedcons + allocatedvectors_elts + allocatedstringbuffer_elts as alloc, duplicate_elts as dup from summary order by id" && $R << EOF
options(scipen=10)
v <- read.csv("alloc$SUFFIX.csv", sep=";")
pdf("copy-alloc-elts$SUFFIX.pdf")
plot(v\$alloc, v\$dup/v\$alloc, log="x", pch=$P, xlab='obj allocated', ylab='% object copied', col=$C)
abline(h=mean(v\$dup/v\$alloc), col="gray60")
n <- read.csv("names$SUFFIX.csv", sep=";")
if(length($L) > 1) legend(x="topleft", 0, legend=t(n), col=$C, pch=$P, cex=.3)
EOF

query copy "select duplicate_tl as dup, named_elts + named_keeped as named from summary order by id" && $R << EOF
options(scipen=10)
v <- read.csv("copy$SUFFIX.csv", sep=";")
pdf("copy-named-elts$SUFFIX.pdf")
plot(v\$named, v\$dup/v\$named, log="x", pch=$P, ylim=c(0, 5), xlab='obj marked for copy', ylab='% object copied', col=$C)
abline(h=mean(v\$dup/v\$named), col="gray60")
n <- read.csv("names$SUFFIX.csv", sep=";")
if(length($L) > 1) legend(x="topleft", 0, legend=t(n), col=$C, pch=$P, cex=.3)
EOF

query avoid-copy "select avoideddup_named as named, avoideddup_duplicate as dup from summary left join vignettes on summary.id=vignettes.id order by summary.id" && $R << EOF
options(scipen=10)
v <- read.csv("avoid-copy$SUFFIX.csv", sep=";")
pdf("avoid-copy$SUFFIX.pdf")
tot <- (v\$named+v\$dup)
plot(v\$dup, v\$dup/tot, log="x", pch=$P, xlab='need duplicate', ylab='% actually dup', col=$C)
abline(h=mean(v\$dup/tot), col="gray60")
cat("min: ",min(v\$dup/tot), "max: ", max(v\$dup/tot), "\\n")
n <- read.csv("names$SUFFIX.csv", sep=";")
if(length($L) > 1) legend(x="topleft", 0, legend=t(n), col=$C, pch=$P, cex=.3)
pdf("avoid-copy-hist$SUFFIX.pdf")
hist(v\$dup/tot * 100, xlab=NULL, main=NULL, breaks=20, freq=TRUE, ylab=NULL, cex=1.4, col="pink")
abline(v=mean(v\$dup/tot * 100), col="gray60")
mysummary <- function(data, v = NULL){
	s = summary(data)
	s["Var"] <- var(data)
	if(is.null(v)) s else rbind(v, s)
}
print("Object duplicated")
sum = mysummary(v\$dup/(v\$named+v\$dup) * 100)
for(cat in 1:3){
	ss = v  # subset(v, catgory==cat)  # XXX
	sum = mysummary(ss\$dup/(ss\$named+ss\$dup)* 100, sum)
}
print(sum)
EOF

# FIXME
query vectors "select scalarvector_total, nullvector_total, truevector_total from summary order by id;"

query allocations "select allocatedcons, allocatedlist_tl, allocatedlist_elts, allocatedvectors_tl, allocatedvectors_elts, allocatedvectors_size, allocatedsmallvectors_tl, allocatedsmallvectors_elts, allocatedsmallvectors_size, allocatedallvectors_tl, allocatedallvectors_elts, allocatedallvectors_size, allocatedstringbuffer_tl, allocatedstringbuffer_elts, allocatedstringbuffer_size, allocatedobjects_tl, allocatedobjects_elts, allocatedobjects_size, duplicate_tl, duplicate_elts, duplicate_elts1 from summary order by id;"

query assign "SELECT  (definevar_local+definevar_other) as intro, (applydefine_other + applydefine_local + setvar_local + setvar_other) as assign FROM summary order by id" && $R << EOF
options(scipen=10)
v <- read.csv("assign$SUFFIX.csv", sep=";")
pdf("assign-ratio$SUFFIX.pdf")
plot(v\$intro, v\$assign/v\$intro, log="xy", pch=$P, xlab='Definition', ylab='Side effects/definitions', col=$C)
abline(h=mean(v\$assign/v\$intro), col="gray60")
n <- read.csv("names$SUFFIX.csv", sep=";")
if(length($L) > 1) legend(x="topleft", 0, legend=t(n), col=$C, pch=$P, cex=.3)
EOF

query match "SELECT  Match_time as match, $MAIN_TIME FROM time_summary where $EXCLUDE_EMPTY order by id" && $R << EOF
options(scipen=10)
v <- read.csv("match$SUFFIX.csv", sep=";")
pdf("match$SUFFIX.pdf")
hist(v\$match/v\$main, main="Matching args" , xlab='% Time', ylab='# vign', breaks=20, freq=TRUE)
lines(density(v\$match/v\$main))
abline(v=mean(v\$match/v\$main), col="gray60")
pdf("matchtime$SUFFIX.pdf")
plot(v\$main, v\$match/v\$main, log="x", pch=$P, xlab='Time', ylab='% Match', col=$C)
abline(h=mean(v\$match/v\$main), col="gray60")
EOF

query duplicate "SELECT  Duplicate_time as dups, $MAIN_TIME FROM time_summary where $EXCLUDE_EMPTY order by id" && $R << EOF
options(scipen=10)
v <- read.csv("duplicate$SUFFIX.csv", sep=";")
pdf("dups$SUFFIX.pdf")
hist(v\$dups/v\$main * 100, main="Duplicate time" , xlab='% Time', ylab='# vign', breaks=20, freq=TRUE)
#lines(density(v\$dups/v\$main))
abline(v=mean(v\$dups/v\$main*100), col="gray60")
pdf("dupstime$SUFFIX.pdf")
plot(v\$main, v\$dups/v\$main, log="x", pch=$P, xlab='Time', ylab='% Duplicate', col=$C)
abline(h=mean(v\$dups/v\$main), col="gray60")
EOF

query symbol "SELECT  SymLookup_time as lkup, $MAIN_TIME FROM time_summary where $EXCLUDE_EMPTY order by id" && $R << EOF
options(scipen=10)
v <- read.csv("symbol$SUFFIX.csv", sep=";")
pdf("symbol$SUFFIX.pdf")
hist(v\$lkup/v\$main, main="Sym Lookup time" , xlab='% Time', ylab='# vign', breaks=20, freq=TRUE)
lines(density(v\$lkup/v\$main))
abline(v=mean(v\$lkup/v\$main), col="gray60")
pdf("symboltime$SUFFIX.pdf")
plot(v\$main, v\$lkup/v\$main, log="x", pch=$P, xlab='Time', ylab='% Sym Lookup', col=$C)
abline(h=mean(v\$lkup/v\$main), col="gray60")
EOF

query fun "SELECT  FunLookup_time as lkup, $MAIN_TIME FROM time_summary where $EXCLUDE_EMPTY order by id" && $R << EOF
options(scipen=10)
v <- read.csv("fun$SUFFIX.csv", sep=";")
pdf("fun$SUFFIX.pdf")
hist(v\$lkup/v\$main, main="Fun Lookup time" , xlab='% Time', ylab='# vign', breaks=20, freq=TRUE)
lines(density(v\$lkup/v\$main))
abline(v=mean(v\$lkup/v\$main), col="gray60")
pdf("funtime$SUFFIX.pdf")
plot(v\$main, v\$lkup/v\$main, log="x", pch=$P, xlab='Time', ylab='% Fun Lookup', col=$C)
abline(h=mean(v\$lkup/v\$main), col="gray60")
EOF

query funeval "SELECT  FunLookupEval_time as lkup, $MAIN_TIME FROM time_summary where $EXCLUDE_EMPTY order by id" && $R << EOF
options(scipen=10)
v <- read.csv("funeval$SUFFIX.csv", sep=";")
pdf("funeval$SUFFIX.pdf")
hist(v\$lkup/v\$main, main="Fun Lookup only eval time" , xlab='% Time', ylab='# vign', breaks=20, freq=TRUE)
lines(density(v\$lkup/v\$main))
abline(v=mean(v\$lkup/v\$main), col="gray60")
pdf("funevaltime$SUFFIX.pdf")
plot(v\$main, v\$lkup/v\$main, log="x", pch=$P, xlab='Time', ylab='% Fun Lookup only eval', col=$C)
abline(h=mean(v\$lkup/v\$main), col="gray60")
EOF

query funweval "SELECT  (FunLookup_time - FunLookupEval_time) as lkup, $MAIN_TIME FROM time_summary where $EXCLUDE_EMPTY order by id" && $R << EOF
options(scipen=10)
v <- read.csv("funweval$SUFFIX.csv", sep=";")
pdf("funweval$SUFFIX.pdf")
hist(v\$lkup/v\$main, main="Fun Lookup without eval time" , xlab='% Time', ylab='# vign', breaks=20, freq=TRUE)
lines(density(v\$lkup/v\$main))
abline(v=mean(v\$lkup/v\$main), col="gray60")
pdf("funwevaltime$SUFFIX.pdf")
plot(v\$main, v\$lkup/v\$main, log="x", pch=$P, xlab='Time', ylab='% Fun Lookup without eval', col=$C)
abline(h=mean(v\$lkup/v\$main), col="gray60")
EOF

query timebreakdown "SELECT name,
						(FunLookup_time ) as flkup,
						FunLookupEval_time as flkupeval,
						SymLookup_time as slkup,
						Duplicate_time as dups,
						Match_time as match,
						Match_atime as amatch,
						dotCall_time as dotcall,
						dotCode_time as dotcode,
						dotExternal_time as dotexternal,
						(MainLoop_atime - Repl_atime) as boot,
						$MAIN_TIME,
						MainLoop_atime as rmain,
						allocVector_time as allocvector,
						(allocList_time + allocS4_time) as alloclist,
						GCInternal_time as gc,
						onExits_time as onexit,
						#FindContext_time as findcontext,
						cons_time as cons,
						Protect_time as protect,
						UnprotectPtr_time as unprot_ptr,
						CheckStack_time as chkstack,
						doLogic_time as doLogic,
						doArith_time as doArith,
						doSubset_time as doSubset,
						doSubset2_time as doSubset2,
						Install_time as Install,
						dotBuiltIn_time  + dotBuiltIn2_time as builtin,
						dotSpecial2_time as special,

						System_time,
						Download_time,
						Sleep_time as sleep,
						inSockConnect_time  + inSockOpen_time + inSockRead_time + inSockWrite_time as net,
						doUnzip_time + zipRead_time as zip

						FROM time_summary left join vignettes where main > 0  and vignettes.id=time_summary.id order by time_summary.id" && $R << EOF

shuffle_palette <- function(colors) {
	p <- 0
	tempcol <- rainbow(colors)
	nb_color <- 3
	len <- length(tempcol) / nb_color
	for(i in 1:len){
		#p[[i*nb_color - 3]] = tempcol[[i + 3*len]]
		p[[i*nb_color - 2]] = tempcol[[i + 2*len]]
		p[[i*nb_color - 1]] = tempcol[[i + 1*len]]
		p[[i*nb_color]] = tempcol[[i + 0*len]]
	}
	return(p)
}

options(scipen=10)
v <- read.csv("timebreakdown$SUFFIX.csv", sep=";")
benchnames <- sub("fasta.out","", gsub("_.*$", "", sub("shootout-", "", v\$name,fixed =TRUE)), fixed=TRUE)

p <- c("plum", "steelblue3", "yellow2", "seagreen3", "purple", "sienna2", "wheat", "skyblue", "red")
palette(p)

breakdown <- function(vv, benchnames){
	names <- colnames(vv)
	par(las=3, xpd=T, mar=par()\$mar + # c(2,0,0,4))
				c(0,0,0,4))
	barplot(t(vv),  col=p, axis.lty=.001, cex.names=0.3, # names.arg=benchnames,
		yaxp=c(0,1,10), border=NA,space=0)
	legend("right", names, fill=p, bty='n', inset=-.23)
}

vv <- cbind(
	mm = (v\$gc+v\$protect+v\$unprot_ptr)/v\$main,
	alloc.cons=(v\$cons ) /v\$main,
	alloc.list=(v\$alloclist) /v\$main,
	alloc.vector=(v\$allocvector) /v\$main,
	duplicate = v\$dups/v\$main,
	lookup = (v\$flkupeval + v\$flkup + v\$slkup)/v\$main,
	match = v\$match/v\$main,
	external=(v\$dotcall+v\$dotcode+v\$dotexternal)/v\$main,
	builtin=(v\$builtin+v\$doSubset+v\$doSubset2)/v\$main,
	arith=(v\$doArith + v\$doLogic)/v\$main,
	special = v\$special/v\$main
	)

pdf("timebreakdown$SUFFIX.pdf")

breakdown(vv, benchnames)

print(length(vv)/length(names))
print(summary(v\$main/1000))
print(summary(vv))
vals = cbind(
	mm = (v\$gc+v\$protect+v\$unprot_ptr + v\$cons + v\$alloclist+ v\$allocvector) /v\$main,
	mm.dup = (v\$gc+v\$protect+v\$unprot_ptr + v\$cons + v\$alloclist+ v\$allocvector + v\$dups) /v\$main,
	cons = (v\$alloclist+ v\$cons) / v\$main,
	match = v\$match/v\$main,
	amatch = v\$amatch/v\$main
	)

print(summary(vals))

pdf("timebreakdown-time$SUFFIX.pdf")
vv <- cbind(
	mm = (v\$gc+v\$protect+v\$unprot_ptr)/v\$main,
	alloc.cons=(v\$cons ) /v\$main,
	alloc.list=(v\$alloclist) /v\$main,
	alloc.vector=(v\$allocvector) /v\$main,
	duplicate = v\$dups/v\$main
	)

breakdown(vv, benchnames)
EOF

query callfreq "select value, sum(param) as param, sum(position) as position, sum(keywords) as kw, sum(rest) as rest, count(value) from calls_frequency where not (position == 0 and keywords == 0 and rest == 0)  or value=0 group by value order by 1" && $R << EOF
options(scipen=10)
v <- read.csv("callfreq$SUFFIX.csv", sep=";")

pdf("callfreq$SUFFIX.pdf")
vals <- cbind(v\$position, v\$kw, v\$rest)
vals[1,1] = v\$param[1]
#vals[1,2] <- 0
#vals[1,3] <- 0
#vals <- vals/1000000
#vals[vals == 0] = NA

print(vals)
total <- colSums(vals)
print(dim(vals))
print(total/sum(total))
n  <- c('Position', 'Keyword', 'Variadic')

barplot(t(vals), names.arg=v\$value, cex.names=0.5, las=3, col=rainbow(3), ylim=c(0,250))
legend(x="top", 0, legend=n,  fill=rainbow(3))

group_callfreq <- function(fs, cols) rowSums(fs[, cols])

postscript("callfreq-log$SUFFIX.ps", horizontal=T)
arg_counts <- v\$value
hs <- matrix(0, 3, max(arg_counts) + 1)
print(length(arg_counts))
print(dim(hs))
print(dim(t(vals)))
hs[, arg_counts + 1] <- t(vals)

hs_max_one = 19
df <- data.frame(hs[, 0:hs_max_one + 1])
print(df)

for (arg_cnt in 0:hs_max_one) {
  old_name <- paste('X', arg_cnt + 1, sep='')
  new_name <- as.character(arg_cnt)
  df[new_name] <- df[old_name]
  df[old_name] <- NULL
}
group_size = 20
for (arg_cnt_from in seq(hs_max_one + 1, ncol(hs), group_size)) {
  arg_cnt_to <- min(255, arg_cnt_from + group_size - 1)
  c_from <- arg_cnt_from + 1
  c_to <- arg_cnt_to + 1
  cat(c_from, c_to, '\n')
  name <- if (arg_cnt_to >= 255) paste(c(arg_cnt_from, '+'), sep='', collapse='') else paste(c(arg_cnt_from, arg_cnt_to), collapse='-')
  print(name)
  print(rowSums(hs[, c_from:c_to]))
  df <- replace(df, name, rowSums(hs[, c_from:c_to]))
}
print('df = ')
print(df)
print('df_names = ')
print(names(df))
print('hs = ')
hs <- data.matrix(df)
print(hs)
hs_names = c(v\$value)
barplot(hs, cex.names=0.5, las=3, col=rainbow(3), log="y", ylim=c(1, max(hs)), offset=0.001)
legend(x="top", 0, legend=n,  fill=rainbow(3))

library("gplots")
#vals[,1] <- vals[,1] + 1
pdf("callfreq-pos$SUFFIX.pdf")
maxcol <- 19
barplot2(t(vals[1:maxcol,1]), main=NULL , xlab=NULL, ylab=NULL, names.arg=v\$value[1:maxcol], cex.names=.6, las=3, col=rainbow(3), log="y")
#legend(x="topright", 0, legend=n,  fill=rainbow(3))
pdf("callfreq-other$SUFFIX.pdf")
n  <- c('Variadic', 'Keyword')
barplot2(t(cbind(vals[,3], vals[,2])), main=NULL , xlab=NULL, ylab=NULL, names.arg=v\$value, cex.names=.6, las=3, col=c("blue","green"), log="y")
legend(x="topright", 0, legend=n,  fill=c("blue","green"))

pdf("callfreq-param$SUFFIX.pdf")
barplot(v\$param, main="Distribution of arguments #" , xlab='number of arguments', ylab='# vign', names.arg=v\$value, cex.names=0.5, las=3)
abline(v=mean(v\$param), col="gray60")

pdf("callfreq-position$SUFFIX.pdf")
barplot(v\$position, main="Distribution of arguments # by position" , xlab='number of arguments', ylab='# calls', names.arg=v\$value, cex.names=0.5, las=3)
abline(v=mean(v\$position), col="gray60")

pdf("callfreq-keyword$SUFFIX.pdf")
barplot(v\$kw, main="Distribution of arguments # by keywords" , xlab='number of arguments', ylab='# calls', names.arg=v\$value, cex.names=0.5, las=3)
abline(v=mean(v\$kw), col="gray60")

pdf("callfreq-ldots$SUFFIX.pdf")
barplot(v\$rest, main="Distribution of arguments # by ..." , xlab='number of arguments', ylab='# calls', names.arg=v\$value, cex.names=0.5, las=3)
abline(v=mean(v\$rest), col="gray60")
EOF

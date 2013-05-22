foo <- function(bar)
	eval(bar)
	
--EOF--

HTML(as.latex("",inline=FALSE,count=TRUE) ,file=.HTML.file)   

HTML(as.latex("\int_{-\infty}^{1}f(x)dx",inline=FALSE,count=TRUE) ,file=.HTML.file)   

compare(ffmat <- ffmat + diag(xn),
        ssmat <- ssmat + diag.spam(xn), "add identity")

compare(ffmat <- 1:xn %d+% ffmat,
        ssmat <- 1:xn %d+% ssmat, "add identity quicker")
--EOF--
call(toto)
cond.current.level[i]

levels(var)[[cond.current.level[i]]]
#if (length(value) != dim(object@CpG.data)[1])
#	toto
#else
#	more
foo <- 2
foo <- function(a) {
	if(a)
		foo <- 1
	foo
}

test$foo
test$foo <- 2
test$foo <- 2
test@foo
foo(bar)
test$bar$baz
test$foo@bar
var >= levels(var)
levels(test=var, toto= foo)[[cond.current.level]]

levels(var)[[cond.current.level[i]]]

levels(var)[[cond.current.level[i]]][1]
(var >= levels(var)[[cond.current.level[i]]][1])
# --EOF--
((var >= levels(var)[[cond.current.level[i]]][1])
                 & (var <=
                    levels(var)[[cond.current.level[i]]][2]))
# --EOF--
selection<-selection & featureData(object)$CHR %in% names(chrs[chrs>=10])      
if(a) {
} else if(b) {
} else if(c) {
        if(length(args) >= 2)
        {c}
        else stop("-l option without value", call. = FALSE)
    } else if(d)
        lib <- substr(a, 11, 1000)
    else pkgs <- c(pkgs, a)

while(length(args)) {
    a <- args[1]
    if(a %in% c("-h", "--help")) {}
    else if(a %in% c("-v", "--version")) {
    cat("R add-on package remover: ")
    cat("R add-on package remover: ", " (r")
    cat("R add-on package remover: ", " (r", R.version)
    cat("R add-on package remover: ", " (r", R.version[["svn rev"]])
    cat("R add-on package remover: ", " (r", R.version[["svn rev"]], ")\n", sep = "")
    
        cat("R add-on package remover: ", " (r", R.version[["svn rev"]], ")\n", sep = "")
        q("no")
    }
    else if(a == "-l") {
        if(length(args) >= 2) {
        lib <- args[2]
        args <- args[-1]
        }
        else stop("-l option without value", call. = FALSE)
    } else if(substr(a, 1, 10) == "--library=")
        lib <- substr(a, 11, 1000)
    else pkgs <- c(pkgs, a)
    args <- args[-1]
}
if(!length(pkgs))
    stop("ERROR: no packages specified", call.=FALSE)
if(!nzchar(lib)) {
    lib <- .libPaths()[1]
    message("Removing from library ", sQuote(lib))
} else {
    ## lib is allowed to be a relative path.
    ## should be OK below, but be sure.
    cwd <- try(setwd(path.expand(lib)), silent = TRUE)
    if(inherits(cwd, "try-error"))
        stop("ERROR: cannot cd to directory ", sQuote(lib), call. = FALSE)
    lib <- getwd()
    setwd(cwd)
}
if(
	!
		utils::file_test("-d", lib)
		||
		file.access(lib, 2L)
		)
    stop("ERROR: no permission to remove from directory ", sQuote(lib),
         call. = FALSE)

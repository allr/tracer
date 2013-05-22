# Toto
a = 1
local({
    info <- loadingNamespaceInfo()
    ns <- .Internal(getRegisteredNamespace(as.name(info$pkgname)))
    if (is.null(ns)){
        stop("cannot find name space environment");
		a = 2
	}
	a
    barepackage <- sub("([^-]+)_.*", "\\1", info$pkgname)
    dbbase <- file.path(info$libname, info$pkgname, "R", barepackage)
    lazyLoad(dbbase, ns, filter = function(n) n != ".__NAMESPACE__.")
})

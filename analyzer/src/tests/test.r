a
b <- c <- d
c -> b
d
e <<- f
f ->> e
if ( a )
	if (b)
		c
	else
		d
		
if ( a )
	b

function(a, b) bar
foo <- function(a, b) bar
foo <- function(a, b = c) bar

while ( a ) foo

for ( foo in bar ) { a
	baz
	bat
	}

repeat foo

foo ( )
foo ( bar )
foo ( bar , baz )

# foo () # bar)

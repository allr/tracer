# resolution and iteration count constants
XRES = 1000
YRES = 1000
ITERS = 200

# "quantize" image
# (boolean decision if the point escaped or not)
quant <- function(v) {
  return(Re(v*v) < 16)
}

# calculate C value for a given pixel (coordinates between 0 and 1)
calcC <- function(imag, real) {
  return((-1 + 2 * imag) * 1i + (-2.3 + 3.0*real))
}

# calculate a single iteration step
iterStep <- function(img, pxc) {
  return(img * img + pxc)
}

calcMandel <- function() {
  # create empty image and per-pixel constants
  image  <- matrix(0+0i, ncol=XRES, nrow=YRES)
  pixelc <- outer((1:YRES)/YRES, (1:XRES)/XRES, calcC)

  # parallel iteration of all pixels (inefficient)
  for (i in 1:ITERS) {
    image <- iterStep(image, pixelc)
  }

  image
}

# plot the image
image <- calcMandel()
invisible(quant(image))


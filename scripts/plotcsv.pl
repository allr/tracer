#!/usr/bin/env perl

use Getopt::Long;
use Pod::Usage;
use warnings;
use strict;
use feature ':5.10';

# convert HSV to RGB
sub hsv2rgb {
  my $h = shift;
  my $s = shift;
  my $v = shift;

  if ($s == 0) {
    return (0,0,0);
  }

  $h /= 60.0;

  my $i = int($h);
  my $f = $h - $i;
  my $p = $v * (1.0 - $s);
  my $q = $v * (1.0 - $s * $f);
  my $t = $v * (1.0 - $s * (1.0 - $f));

  if ($i == 0) {
    return ($v, $t, $p);
  } elsif ($i == 1) {
    return ($q, $v, $p);
  } elsif ($i == 2) {
    return ($p, $v, $t);
  } elsif ($i == 3) {
    return ($p, $q, $v);
  } elsif ($i == 4) {
    return ($t, $p, $v);
  } else {
    return ($v, $p, $q);
  }
}

# convert [0:1] RGB to HTML-like color string
sub rgb2hex {
  return sprintf("#%02x%02x%02x", $_[0] * 255, $_[1] * 255, $_[2] * 255);
}


# --- main ---

my $show_help    = 0;
my $plot_stacked = 0;
my $plot_pdf     = 1;
my $min_range    = undef;
my $max_range    = undef;

# parse options
GetOptions(
    "help"       => \$show_help,
    "pdf"        => sub { $plot_pdf = 1; },
    "png"        => sub { $plot_pdf = 0; },
    "stacked"    => \$plot_stacked,
    "minrange=i" => \$min_range,
    "maxrange=i" => \$max_range
    ) or pod2usage(2);

$show_help = 1 if (scalar(@ARGV) != 2);

pod2usage(1) if $show_help;

# check if input file exists
my $input_file  = $ARGV[0];
my $output_file = $ARGV[1];

if (! -e $input_file) {
    say "ERROR: Input file does not exist";
    exit 2;
}

open IN, "<", $input_file or die "Can't open $input_file: $!";

# parse header line
my $headerline = <IN>;
chomp $headerline;
my @headers = split /[,;]/, $headerline;
@headers = map { /^\"/ ? $_ : "\"$_\"" }  @headers;

# read data lines
my @data;
while (<IN>) {
    chomp;
    my @linedata = split /[,;]/, $_;

    # remove quotes (FIXME: technically incorrect)
    foreach my $e (@linedata) {
        $e =~ s/^\"(.*)\"$/$1/;
    }

    # quote the first element (effectively escaping a leading "e")
    $linedata[0] = "\"$linedata[0]\"";

    push @data, \@linedata;
}

close IN;

my $columns = scalar(@headers) - 1;

#say "Columns: $columns";

# run gnuplot
open PLOT,"|-","gnuplot" or die "ERROR: Failed to run gnuplot: $!";
if ($plot_pdf) {
    say PLOT "set terminal pdf";
} else {
    say PLOT "set terminal png";
}

print PLOT <<EOT; # FIXME: Quoting
set output "$output_file"
set datafile separator ","
set key outside right top
set style data histogram
set style histogram cluster gap 1
set style fill solid border -1
set xtic rotate by -45 scale 0
set boxwidth 0.9
EOT

if ($plot_stacked) {
  say PLOT "set style histogram rowstacked";
  say PLOT "set key invert";
}

# create line styles with non-repeated colors
for (my $i = 0; $i < $columns; $i++) {
  my $v;

  if ($columns > 6) { # semi-arbitrary limit (11 columns were too much)
    # modulate v to increase distinctiveness of adjacent colors
    $v = 0.7 + 0.3 * ($i % 2);
  } else {
    $v = 1;
  }

  printf PLOT "set style line %d linecolor rgb \"%s\"\n", $i+1,
    rgb2hex(hsv2rgb(360.0/$columns * $i, 1, $v));
}


# build plot command
print PLOT "plot ";

if (defined($min_range) || defined($max_range)) {
  print PLOT "[] [";
  print PLOT $min_range if defined($min_range);
  print PLOT ":";
  print PLOT $max_range if defined($max_range);
  print PLOT "] ";
}

for (my $i = 0; $i < $columns; $i++) {
  if ($i == 0) {
    print PLOT "'-'";
  } else {
    print PLOT ", ''";
  }

  print PLOT " using ", $i+2, ":xtic(1) ls ", $i+1 ," title columnheader";
}

print PLOT "\n";

# repeat the data for each set (FIXME: use a temp file instead)
for (my $i = 0; $i < $columns; $i++) {
  say PLOT join(",",@headers);
  foreach my $line (@data) {
    say PLOT join(",",@{$line});
  }
  say PLOT "EOF";
}

close PLOT;


### command line help ###

=head1 SYNOPSIS

plotcsv [options] csvfile outfile

=head1 OPTIONS

=over 8

=item B<--help>

prints this help message

=item B<--stacked>

plot input data as a stacked bar graph

=item B<--pdf>

plot into a PDF file (default)

=item B<--png>

plot into a PNG file

=item B<--minrange val>

set the minimum plot range (default auto)

=item B<--maxrange val>

set the maximum plot range (default auto)

=back

=cut

#!/bin/sh
source /opt/r/tracer/env.sh
error "This script is deprecated, please use org.rx.FileName for this"
cat $1 | cut -f 3 -d\  | sort -u | sed 's/^.*$/basename \0/e' 

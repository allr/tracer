#!/bin/sh
source /opt/r/tracer/env.sh


#echo debug/data_110527_123054_GeneRfold/*.R | cat - debug/data_110527_123054_GeneRfold/deps |sort -u |xargs  java -cp /usr/share/java/stringtemplate.jar:/home/morandat/antlr-3.3-complete.jar:/home/morandat/rtools.jar:/home/morandat/analyser.jar org.rx.analyser.AnalyseR -T -q > debug/data_110527_123054_GeneRfold/static.txt


while read f ; do
	#i=$TRACEDIR/data_110527_123054_GeneRfold
	i=$TRACEDIR/$TAG/$f
	echo $i
	[ -d "$i" ] || exit 1
	java_exec org.rx.FileName "$i/source.map" > "$i"/deps
	java_exec org.rx.FileName -F -l "$i/source.map" > "$i"/libs # Mainly to debug
	java_exec org.rx.FileName -F -o "$i/source.map" > "$i"/others # Mainly to debug
	ls -1 "$i" |grep '\.R$' | sed "s:^:$i/:" |cat - "$i"/deps | sort -u > "$i"/alldeps
	cat "$i"/alldeps | xargs cat  | tracer/blanklines.sh | wc -l > "$i"/libSize
	cat "$i"/alldeps | xargs cat  | wc -l >> "$i"/libSize
done

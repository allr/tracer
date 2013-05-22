#!/bin/sh

MERGE=
[ "$1" -a "$1" = "merge" ] && MERGE=true && shift
REGEN=true
[ "$1" -a "$1" = "plot" ] && REGEN= && shift
R='R --no-save --no-restore --slave'

DB=rx.db
[ "$1" ] && DB=$1 && shift
SUFFIX=
[ "$1" ] && SUFFIX=$1 && shift

[ "$MERGE" ]  && SQLITE="sqlite3 -csv -separator ; $DB" || SQLITE="sqlite3 -header -csv -separator ; $DB"
DONE=""

query () {
	[ -z "$REGEN" ] && return
	file=$1$SUFFIX.csv
	[ "$DEBUG" ] && file=/dev/stdout
	shift
	echo sqlite3 $DB \'$1\'
	if [ "$MERGE" ] ; then
		$SQLITE "$*" >> $file
	else
		$SQLITE "$*" > $file
	fi
	v=$?
	[ $v ]  && DONE="$DONE $file"
	return $v
}

query /tmp/acalls "SELECT trace_id, sum(nb) as calls, sum(position + keywords + rest) as call_param, sum(position) as call_paramp, sum(keywords) as call_paramk, sum(rest) as callparam_r FROM calls left join locations where location_id=id group by trace_id order by trace_id;"
[ $? ] && echo OK
query /tmp/fcalls "SELECT trace_id, sum(nb) as fcall, sum(position + keywords + rest) as fcall_param, sum(position) as fcall_paramp, sum(keywords) as fcall_paramk, sum(rest) as fcall_paramr FROM calls left join locations where location_id=id and status=0 group by trace_id order by trace_id;"
[ $? ] && echo OK
query /tmp/scalls "SELECT trace_id, sum(nb) as scall, sum(position + keywords + rest) as scall_param, sum(position) as scall_paramp, sum(keywords) as scall_paramk, sum(rest) as scall_paramr FROM calls left join locations where location_id=id and status=1 group by trace_id order by trace_id;"
[ $? ] && echo OK
query /tmp/bcalls "SELECT trace_id, sum(nb) as bcall, sum(position + keywords + rest) as bcall_param, sum(position) as bcall_paramp, sum(keywords) as bcall_paramk, sum(rest) as bcall_paramr FROM calls left join locations where location_id=id and status=2 group by trace_id order by trace_id;"
[ $? ] && echo OK
query /tmp/ecalls "SELECT trace_id, sum(nb) as ecall, sum(position + keywords + rest) as ecall_param, sum(position) as ecall_paramp, sum(keywords) as ecall_paramk, sum(rest) as ecall_paramr FROM calls left join locations where location_id=id and name in ('.C', '.Fortran', '.External', '.Call') group by trace_id order by trace_id;"
[ $? ] && echo OK
query /tmp/mcalls "SELECT trace_id, sum(nb) as acall, sum(position + keywords + rest) as acall_param, sum(position) as acall_paramp, sum(keywords) as acall_paramk, sum(rest) as acall_paramr FROM calls left join locations where location_id=id and name in ('+', '-', '*', '/', '^', '%%', '%/%') group by trace_id order by trace_id;"
[ $? ] && echo OK
query /tmp/racalls "SELECT trace_id, sum(nb) as racall, sum(position + keywords + rest) as racall_param, sum(position) as racall_paramp, sum(keywords) as racall_paramk, sum(rest) as racall_paramr FROM calls left join locations where location_id=id and name in ('get', 'mget', 'assign') group by trace_id order by trace_id;"
query /tmp/rcalls "SELECT trace_id, sum(nb) as rcall, sum(position + keywords + rest) as rcall_param, sum(position) as rcall_paramp, sum(keywords) as rcall_paramk, sum(rest) as rcall_paramr FROM calls left join locations where location_id=id and name in ('eval', 'eval.with.vis', 'args', 'formal', 'body', 'environment', 'environment<-', 'ls', 'remove', 'get', 'mget', 'assign', 'exists') group by trace_id order by trace_id;"
[ $? ] && echo OK
query /tmp/allocs "SELECT id as trace_id, evalscount, dispatch_total, dispatch_failed, DefineVar_local+ DefineVar_other as defvar, SetVar_local+ SetVar_other as setvar, allocatedcons, allocatedvectors_tl, allocatedvectors_elts, allocatedvectors_size, allocatedsmallvectors_tl, allocatedsmallvectors_elts, allocatedsmallvectors_size, allocatedallvectors_tl, allocatedallvectors_elts, allocatedallvectors_size, (allocatedallvectors_size + allocatedstringbuffer_size) /1024 as allocated_vector_in_kb, allocatedcons * 56 as allocatedcons_in_kb, duplicate_tl, duplicate_elts, duplicate_elts1, avoideddup_named, avoideddup_duplicate from summary order by id;"
[ $? ] && echo OK
query /tmp/gc "SELECT id as trace_id, MainLoop_atime - System_time - Download_time - Sleep_time - inSockConnect_time - inSockOpen_time - inSockRead_time - inSockWrite_time - CurlPerform_time - gzFile_time as runningtime, gcinternal_time, gcinternal_hr as gc_tiggered, symlookup_time, symlookup_hr, funlookup_time, funlookup_hr, funlookupeval_time, funlookupeval_hr, match_time, match_hr, dotcall_time, dotcall_hr, dotcode_time, dotcode_hr, dotexternal_time, dotexternal_hr, dotbuiltin_time + dotbuiltin2_time as dotbuiltin, dotbuiltin_hr + dotbuiltin2_hr as dotbuiltin_hr, dotspecial2_time, dotspecial2_hr, dologic_time, dologic_hr, doarith_time, doarith_hr, dosubset_time, dosubset_hr, dosubset2_time, dosubset2_hr, duplicate_time, duplicate_hr, allocvector_time, allocvector_hr, allocs4_time, allocs4_hr, alloclist_time, alloclist_hr, cons_time, cons_hr, r_alloc_time, r_alloc_hr from time_summary order by id;"
[ $? ] && echo OK
query /tmp/prom "SELECT trace_id, sum(nb) as promise_nb, sum(eval) as promise_eval from promises group by trace_id order by trace_id;"
[ $? ] && echo OK

join "-t;" /tmp/acalls$SUFFIX.csv /tmp/fcalls$SUFFIX.csv | join "-t;" - /tmp/bcalls$SUFFIX.csv | join "-t;" - /tmp/scalls$SUFFIX.csv | join "-t;" - /tmp/ecalls$SUFFIX.csv | join "-t;" - /tmp/mcalls$SUFFIX.csv | join "-t;" - /tmp/racalls$SUFFIX.csv | join "-t;" - /tmp/rcalls$SUFFIX.csv | join "-t;" - /tmp/allocs$SUFFIX.csv | join "-t;" - /tmp/gc$SUFFIX.csv | join "-t;" - /tmp/prom$SUFFIX.csv > summary$SUFFIX.csv

wc -l $DONE summary$SUFFIX.csv

sqlite3 -separator ": " $1 "select name, message from errors left join traces where id=trace_id;" 

#!/bin/sh

if [ ."$2" == . ]; then
    echo Usage: $0 scriptfile basedir
    exit 1
fi

# FIXME: Needs proper escaping
sed "/^BASEDIR=/ s!=.*!=\"$2\"!" $1 > $1.temp && mv $1.temp $1

chmod +x $1

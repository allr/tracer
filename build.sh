#!/bin/sh

mvn clean install -Dproject.build.sourceEncoding=UTF-8
cp ./tracer/target/tracer-0.0.1-SNAPSHOT-jar-with-dependencies.jar .

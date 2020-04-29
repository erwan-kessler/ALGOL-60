#!/bin/sh

if [ "$#" -ne 1 ] ; then
    echo "Usage is: ./run.sh <input_file>"
    exit 1
fi
if ! [ -e "$1" ]; then
  echo "$1 not found" >&2
  exit 1
fi

jar=$(find . -name \*.jar)
java -jar "$jar" "--run" "$1"

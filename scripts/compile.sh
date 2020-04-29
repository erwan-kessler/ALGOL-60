#!/bin/sh

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
    echo "Usage is: ./compile.sh <input_file> (<output_file>)?"
    exit 1
fi
if ! [ -e "$1" ]; then
  echo "$1 not found" >&2
  exit 1
fi

jar=$(find . -name \*.jar)

java -jar "$jar" "--in-place" "$1"

if [ "$#" -eq 2 ]; then
  iupFile="$( echo "$1" | cut -d '.' -f 1)"
  mv "$iupFile"".iup" "$2"
fi


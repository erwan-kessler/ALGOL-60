#!/bin/sh

if [ $# -ne 0 ] ; then
    echo "Usage is: ./build.sh"
    exit 1
fi

cd ..
# shellcheck disable=SC2181
if [ "$?" -ne 0 ]; then
  exit
fi

./gradlew fatJar

cp build/libs/*.jar scripts

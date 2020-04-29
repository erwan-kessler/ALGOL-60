#!/bin/sh

cd ..
# shellcheck disable=SC2181
if [ "$?" -ne 0 ]; then
  exit
fi
./gradlew clean

# shellcheck disable=SC2164
cd scripts
# shellcheck disable=SC2181
if [ "$?" -ne 0 ]; then
  exit
fi

find . -type f ! \( -name '*.al' -o -name '*.a60' -o -name '*.sh' -o -name '*.md' \) -delete
find . -type d ! -name 'Examples' -delete

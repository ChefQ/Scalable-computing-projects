#!/bin/sh

url=""
if [ ! -z "$1" ] ; then
  url=$1
else
  url=$(cat /dev/stdin)
fi

#cat $(echo $url | faas-cli invoke youtubedl | faas-cli invoke audioextract 

curl "127.0.0.1:8080/function/audioextract" --data-binary $(curl "127.0.0.1:8080/function/youtubedl" -d url)


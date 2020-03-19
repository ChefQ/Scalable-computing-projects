#!/bin/sh

# Generate random name to avoid name conflicts
randomName=$(cat /dev/urandom | tr -cd 'a-f0-9' | head -c 32)
while [ -e "$randomName.mov" ]
do
    randomName=$(cat /dev/urandom | tr -cd 'a-f0-9' | head -c 32)
done

# Get url from user
url=""
if [ ! -z "$1" ] ; then
  url=$1
else
  url=$(cat /dev/stdin)
fi

# curl both of our functions to get our audio
curl -s "http://gateway:8080/function/youtubedl" -d $url > "$randomName.mp4"
curl -s "http://gateway:8080/function/audioextract" --data-binary "@$randomName.mp4" > "$randomName.mp3"

# print out the bytes of the mp3 file
cat "$randomName.mp3"

# remove temporary files
rm "$randomName.mp4"
rm "$randomName.mp3"


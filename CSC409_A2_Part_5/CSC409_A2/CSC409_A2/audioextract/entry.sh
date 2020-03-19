#!/bin/sh

randomName=$(cat /dev/urandom | tr -cd 'a-f0-9' | head -c 32)

while [ -e "$randomName.mov" ]
do
    randomName=$(cat /dev/urandom | tr -cd 'a-f0-9' | head -c 32)
done

content=""
if [ ! -z "$1" ] ; then
  $1>"$randomName.mov"
else
  cat /dev/stdin>"$randomName.mov"
fi

mplayer "$randomName.mov" -vo null -ao pcm:file="$randomName.mp3":fast > log.txt

#mplayer "$randomName.mov" -dumpaudio -dumpfile "$randomName.mp3" > log.txt

cat "$randomName.mp3"

rm "$randomName.mov"
rm "$randomName.mp3"

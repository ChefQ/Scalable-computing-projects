#!/bin/sh

# Generate random name to avoid name conflicts
randomName=$(cat /dev/urandom | tr -cd 'a-f0-9' | head -c 32)
while [ -e "$randomName.mov" ]
do
    randomName=$(cat /dev/urandom | tr -cd 'a-f0-9' | head -c 32)
done

# Get bytes of .mov file from stdin
content=""
if [ ! -z "$1" ] ; then
  $1>"$randomName.mov"
else
  cat /dev/stdin>"$randomName.mov"
fi

# Get .mp3 file and send mplayer logs to the log file
mplayer "$randomName.mov" -vo null -ao pcm:file="$randomName.mp3":fast > log.txt

# Print contents of .mp3 file to stdout
cat "$randomName.mp3"

# Remove temporary files
rm "$randomName.mov"
rm "$randomName.mp3"

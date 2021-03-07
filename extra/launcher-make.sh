#!/bin/bash

set -e
cd "`dirname "$0"`"

dimensions=(mdpi:1 hdpi:1.5 xhdpi:2 xxhdpi:3 xxxhdpi:4)
res='../src/main/res'

cp 'launcher.svg' 'launcher-foreground.svg'
inkscape --select circle --verb EditDelete --verb=FileSave --verb=FileQuit \
'launcher-foreground.svg'

for dimension in ${dimensions[@]}; do
  resource="${dimension%:*}"
  scale="${dimension#*:}"
  mkdir -p "$res/mipmap-$resource" "$res/drawable-$resource"
  size="`bc <<< "48 * $scale"`"
  inkscape 'launcher.svg' -a 15:15:93:93 -w "$size" -h "$size" \
  -e "$res/mipmap-$resource/ic_launcher.png"
  optipng "$res/mipmap-$resource/ic_launcher.png"
  size="`bc <<< "108 * $scale"`"
  inkscape 'launcher-foreground.svg' -w "$size" -h "$size" \
  -e "$res/drawable-$resource/ic_launcher_foreground.png"
  optipng "$res/drawable-$resource/ic_launcher_foreground.png"
done

rm 'launcher-foreground.svg'

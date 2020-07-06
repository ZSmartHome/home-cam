#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

LOG_LEVEL="info"
# AUDIO_CODEC="ac3" # AC3 supports 5.1, mp3 supports only stereo

while :
do
ffmpeg -hide_banner \
  -c h264 \
  -map 0 \
  -vf scale=640:-1 \
  -f ismv -movflags delay_moov \
  -listen 1 http://0.0.0.0:8080 \
  -loglevel $LOG_LEVEL \
  -i rtsp://admin:admin@zcamera/11 || true
  echo "Command will be restarted in 3 seconds..."
  sleep 1
  echo "Command will be restarted in 2 seconds..."
  sleep 1
  echo "Command will be restarted in 1 seconds..."
  sleep 1
  echo "Restarting"
done

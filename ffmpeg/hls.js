var ffmpeg = require('fluent-ffmpeg');

// make sure you set the correct path to your video file
const STREAM_URL = 'rtsp://admin:admin@192.168.88.9/11';
const proc = ffmpeg(STREAM_URL, {timeout: 432000})
  // set video bitrate
  .videoBitrate(1024)
  // set target codec
  .videoCodec('libx264')
  // set hls segments time
  .addOption('-hls_time', 10)
  // include all the segments in the list
  .addOption('-hls_list_size', 0)
  // setup event handlers
  .on('end', function () {
    console.log('file has been converted succesfully');
  })
  .on('error', function (err) {
    console.log('an error happened: ' + err.message);
  })
  // save to file
  .save('./data/test.m3u8');

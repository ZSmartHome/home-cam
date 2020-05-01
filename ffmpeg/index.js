const ffmpeg = require('fluent-ffmpeg');

const express = require('express');

const app = express();

require('./hls.js');

app.use(express.static(__dirname + '/player'));
app.use('/video', express.static(__dirname + '/data'));

app.get('/', function (req, res) {
  res.send('index.html');
});


const PORT = 4000;
app.listen(PORT, () => console.log(`Started at ${PORT}`));

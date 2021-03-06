# home-cam
Home HD IP camera

### Description:
Fisheye wireless IP Camera WiFi Full View Wide Angle 180 Degree 720P/960/1080P SONYIMX323 IP Camera P2P Onvif Security camera

### Model ID (Device Type):
C6F0SeZ3N0P5L0

### Protocols:
Network Protocol:	 TCP/IP, HTTP, TCP, ICMP, UDP, ARP, IGMP, SMTP, FTP, DHCP, DNS,  DDNS, NTP, UPNP, RTSP, ect.  
Access Protocol:	 ONVIF , i13, P2  

### Default Settings: 
Static IP: 192.168.1.88  
Login/password: admin/admin

### Sources:

#### Image:
 - HIDPI http://192.168.1.88/snap.jpg?JpegCam=[CHANNEL]
 - LowDPI http://192.168.1.88/tmpfs/auto.jpg

#### Video Streaming RTSP:  
 - First Channel (High-Res) rtsp://admin:admin@192.168.1.88/11
 - Second Channel (Low-Res) rtsp://admin:admin@192.168.1.88/12

ONVIF Discovery service:
http://192.168.1.88:8080/

#### Casting with FFmpeg
```shell script
ffmpeg -c copy -acodec ac3 -map 0 -f ismv -movflags delay_moov -hide_banner -loglevel verbose -listen 1 http://0.0.0.0:8080 -i rtsp://admin:admin@zcamera/11
```

### Links:

[ffmpeg streaming](https://trac.ffmpeg.org/wiki/StreamingGuide)

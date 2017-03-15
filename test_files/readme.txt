Simple test for USPphpTunnel (files in test_files dir)

Developped an tested using:
       MXQ ("MXQ 4K*2K 1080P Smart TV BOX XBMC/Kodi H.265 Android Quad Core WiFi 8GB Mini PC")
              http://www.ebay.it/itm/141956901542 (29.78 €)
                         CPU: Amlogic S805 Quad-Core Cortex-A5 Processor 1.5Ghz 
                         GPU: Quad-Core Mali-450@600Mhz
                         RAM: 1GB DDR3
                         ROM: 8GB NAND Flash
                         Android: KitKat 4.4, rooted 
                         WiFi: Wi-Fi 802.11n
                         LAN: Ethernet 10/100M, standard RJ45
                         Video: HDMI 1.4b CEC, up to UHD 4K
                         Audio: HDMI 1.4b CEC, 3.5mm jack, SPDIF/IEC958 
                         USB: 4 porte USB 2.0
                         memory Card:  SD/SDHC/MMC 
       ARDIINO UNO (UNO R3 Scheda Micro USB ATmega328P CH340G Board Modulo Controllore per Arduino)
              http://www.ebay.it/itm/152002551433 (5.79 €)
       and some free apps.

================================  Setup:
MXQ:
   pre-condition: MSQ connected via lan/wifi to main PC (win) and internet.
   note: to make things easy, connect screen and keyboard+mouse to MXQ (only for installation and setup:
         see img/screenshot01.png)
         I get one kit from ebay: (KIT TASTIERA SLIM MOUSE OTTICO WIRELESS SENZA FILI 2.4 GHZ MINI KEYBOARD)                            
                                   http://www.ebay.it/itm/112044834373 (7.80 €)
         To map all special chars required to write code and not presents in this mini Italian keyboard I used 
                  "External Keyboard Helper Demo"  
                     https://play.google.com/store/apps/details?id=com.apedroid.hwkeyboardhelperdemo  

   1) install Palapa Web server: https://play.google.com/store/apps/details?id=com.alfanla.android.pws
       1a) config Palapa Web server and DB server "start on boot"
       1b) keep default user/password (DB server: "root"/none)

   3) install phpMyAdmin ver. 4.1.14.1 from https://www.phpmyadmin.net/files/ in /sdcard/pws/phpmyadmin
      note: to make it easy, install FTP:   
       3.a) on MXQ "Rooted SSH/SFTP Daemon"  
                   https://play.google.com/store/apps/details?id=web.oss.sshsftpDaemon
       3.b) on Windows: "WinSCP"  https://winscp.net/                                       
   
   3) using phpmyadmin:  3a) create database "datatest"
                         3b) on "datatest" import the SQL file datatest.sql to make table "esempio"
  
   4) copy www/*  to MQX, in  /sdcard/pws/www

   5) install on MXQ the app: USBphpTunnel01_d.apk
          5a) copy it in /scard/download
          5b) using a file browser on MKQ  (e.g. totalCommander 
                  https://play.google.com/store/apps/details?id=com.ghisler.android.TotalCommander)
              then click on /scard/dowunload/USBphpTunnel01_d.apk to install.

   6) connect Arduino to USB :
          6a) choose "USB php Tunnel" and "Always" for USB device
              note: first run creates the config file
          6b) The app must find your Arduino board: keep the vendorId and productId (see img/screenshot03.png)
              note: for supported vendorId/productId, see the 
                    file \usbSerialForAndroid\src\main\java\com\hoho\android\usbserial\driver\UsbId.java
          6c) close the app USBphpTunnel.

   7) Edit the config file at /sdcard/USBphpTunnel/config.ini:
                - set baudRate=115200
                - update arduinoProductId=7523
                - update arduinoVendorId=1A86
                - if required update phpPath=http\://localhost\:8080

Arduino UNO:
    1) compile and install the Sketch arduino/testser02.ino
       (on MXQ you can use ArduinoDroid 
               https://play.google.com/store/apps/details?id=name.antonsmirnov.android.arduinodroid2,
       see img/screenshot02.png)

============================== test and use
   
    1) Turn-on or re-boot (Ctrl+Alt+Cancel on keyboard) the MXQ having Arduino unplugged.

    2) Wait 60 sec (time to start web server) then plug Arduino board USB

    3) the USBphpTunnel autostarts: if autoPortSelection > 0 (the default) it will send and receive (after 10 sec.)

    4) the LED on Arduino UNO - PIN 13 will flash (rate 4 sec). Very simple way to flash a led ! :)
   
    5) it is safe to connect-disconnect Arduino USB 

==============================  more on use
   
    On USBphpTunnel terminal you can see (img/screenshot04.png):

         data received from Arduino ("Read 49 bytes"):
               the data MUST be a relative URL, like: "/testio/add.php?primo=32&secondo=4.5&terzo=18:09" and MUST start with "/".
               [phpPath (in config) + data] makes an absolute URL, called by USBphpTunnel.
               note: Your Arduino Sketch will build relative URLs using the requiered PHP pages and actual values.

               The PHP sample code in add.php: 
                   a) adds a record to MySQL table datatest.esempio
                   b) buids the response: pure ASCII (not HTML)
               USBphpTunnel sends the response to Arduino:
         
          data sended to Arduino ("Send 11 bytes")
               the data are commands, as defined in testser02.ino:
                  1) Analog write,  sended as "A port value": A [3..11] [0..255] [0xD|0x0A] (decimal, 0-octal, 0x-esa)
                  2) Digital write, sended as "D pin  value": D [2..13] [0|1|2] [0xD|0x0A] 
                           (decimal, 0-octal, 0x-esa). value: 0 = LOW, 1 = HIGH, 2 = TOGGLE
                  3) Parameter set, sended as "P index long-value": P [0..15] [[-]0..2'147'483'647] [0xD|0x0A] 
                           (decimal, 0-octal, 0x-esa)
               note: USBphpTunnel accepts any line terminator (\n or \r or \r\n) and transforns it in single "0x0A" (\n)
               note: In your Arduino Sketch you can eliminate unused commands or add your custom commands. 
                     Commands MUST NOT start with "*" char.

          debug messages from Arduino ("** Echo: D 13 2") 
               the message MUST start with "*". It is show on terminal (not sended to php)
               note: green, color set on config.ini

          debug messages from PHP
               the message MUST start with "*". It is show on terminal (not sended to Arduino)
               note: red, color set on config.ini

    The protocol is pure text, so we can also do testing with a standard terminal application 
      (Arduino console terminal). (img/screenshot05.png)
    note: the only terminal app that worked for me is "Serial USB terminal" 
                    https://play.google.com/store/apps/details?id=de.kai_morich.serial_usb_terminal
    
    More apps used in development (see img/screenshot01.png): 
            * "Terminal IDE" (to compile java and simple apps, to enhance linux) 
                   https://play.google.com/store/apps/details?id=com.spartacusrex.spartacuside
            * "All-In-One Toolbox (pulito)" (Android management) 
                   https://play.google.com/store/apps/details?id=imoblife.toolbox.full
            * "MatLog: Material Logcat Reader" (logcat capture) 
                   https://play.google.com/store/apps/details?id=com.pluscubed.matlog
    
    ================  For Android developers
         
            Any contribution is very well accepted and solicited ( https://github.com/msillano/USBphpTunnel ) Thanks.
            Acknowledgement: developped starting from https://github.com/mik3y/usb-serial-for-android.

   ============================= CONCLUSIONS
  
    Now you can develop MySQL and web enabled Arduino applications only working on Arduino and PHP. 
    To keep ligth the Arduino Sketch, you can port all not realtime logic to PHP side.
    At the end your application will works on MXQ+Arduino also 24/7 with only 20 Watt AC power, and can also
    be controlled by smartphone via WiFi.
    What more?
    Enjoy.

          









   

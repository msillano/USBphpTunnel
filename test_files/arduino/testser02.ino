

long params[16];

void setup() {
    Serial.begin(115200);
    while (!Serial) {
        ; // wait for serial port to connect. Needed for native USB port only
     }
    pinMode(13, OUTPUT);
}

int tcount = 0;

void loop() {
char dummy[64];
int x;
int y;
long z;
  //  RX LOOP
  if (Serial.available()) {
  // COMMANDS minimal implementation, max 64 char
        x = Serial.read();
        
        if (x=='A' || x=='D' ||x=='P'){
               memset(dummy,0,64);
               Serial.readBytesUntil(0x0A,dummy, 64);
            // parsing 2 numeric parameters
               char* s = strtok(dummy, " ,\n");
               y = strtol(s, NULL, 0);
               s = strtok(NULL, " ,\n");
               z = strtol(s, NULL, 0);
            // echo debug
               char prts[64];
               sprintf(prts, "** Echo: %c %i %li \n",x,y,z);  
               Serial.write(prts); 
               delay(10);  // required after write      
           // generic commands implementation for Arduino UNO:
            
           // analog write, send as "A port value": A [3..11] [0..255][0D] (decimal, 0-octal, 0x-esa)
              if (x=='A'){
                 if(y == 3 || y == 5 || y == 6 || y == 9 || y == 10 || y == 11) {    // test for Arduino UNO
                    if (z>=0 && z<=255)
                       analogWrite(y, z);  }  } // if A
            
           // digital write, send as "D pin  value": D [2..13] [0|1|2][0x0D]  (decimal, 0-octal, 0x-esa)
              if (x=='D'){
                 if (y >1 && y < 14 ) {       // pin 0, pin 1: serial - test for Arduino UNO
                    if (z == 0 )                    // 0 -> LOW
                       digitalWrite(y, LOW);
                    if (z == 1)                     // 1 -> HIGH
                       digitalWrite(y, HIGH);
                    if (z== 2) {                    // 2 -> TOGGLE
                       if (digitalRead(y) == HIGH)
                          digitalWrite(y, LOW);
                       else
                          digitalWrite(y, HIGH);  
                       }
                    }
              } //  if D
// Long parameter set, send as "P index valueL": P [0..15] [[-]0..2'147'483'647][0D] (decimal, 0-octal, 0x-esa)
              if (x=='P'){
                  if (y >= 0 && y <= 15)
                      params[y] = z;
               } // if P
        tcount = 0; 
        } // if ADP
    }  // if available
    else
    {
    delay(10);  // loop delay
    // =========  TX LOOP
    tcount++;
    if (tcount > 180) {    // blinking period
        tcount = 0;
        Serial.write("/testio/add.php?primo=32&secondo=4.5&terzo=18:89\n");
        delay(10);  // required for 115200
        }
     }
  }
   



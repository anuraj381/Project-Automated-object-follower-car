int pinRight = 10;
int pinLeft = 11;
int incomingByte = 0;   // for incoming serial data

void setup() {
  // put your setup code here, to run once:

  Serial.begin(9600);

  pinMode(pinRight, OUTPUT);
  pinMode(pinLeft, OUTPUT);

}

void loop() {

if (Serial.available() > 0) {
   incomingByte = Serial.read();
   Serial.println(incomingByte);

   if(incomingByte == 'a'){
      analogWrite(pinRight, 150);
      analogWrite(pinLeft, 150);
   }
   else if(incomingByte == 'r'){
      analogWrite(pinRight, 0);
      analogWrite(pinLeft, 150);
   }
   else if(incomingByte == 'l'){
      analogWrite(pinRight, 150);
      analogWrite(pinLeft, 0);
   }
   else if(incomingByte == 's'){
      analogWrite(pinRight, 0);
      analogWrite(pinLeft, 0);
   }
   else{
      analogWrite(pinRight, 0);
      analogWrite(pinLeft, 0); 
   }
  }
  /*else{
      digitalWrite(pinRight, LOW);
      digitalWrite(pinLeft, LOW);   
  }*/
}

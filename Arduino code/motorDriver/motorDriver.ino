int pinRight = 12;
int pinLeft = 13;
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
      digitalWrite(pinRight, HIGH);
      digitalWrite(pinLeft, HIGH);
   }
   else if(incomingByte == 'r'){
      digitalWrite(pinRight, LOW);
      digitalWrite(pinLeft, HIGH);
   }
   else if(incomingByte == 'l'){
      digitalWrite(pinRight, HIGH);
      digitalWrite(pinLeft, LOW);
   }
   else if(incomingByte == 's'){
      digitalWrite(pinRight, LOW);
      digitalWrite(pinLeft, LOW);
   }
   else{
      digitalWrite(pinRight, LOW);
      digitalWrite(pinLeft, LOW); 
   }
  }
  /*else{
      digitalWrite(pinRight, LOW);
      digitalWrite(pinLeft, LOW);   
  }*/
}

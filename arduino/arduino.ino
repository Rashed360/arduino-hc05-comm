#include <SoftwareSerial.h>
// defines pins numbers
SoftwareSerial Bluetooth(5, 6);  // TX, RX
const int trigPin = 9;
const int echoPin = 10;
// defines variables
long duration;
int distance;
int Data;  // the data received
void setup() {
  Bluetooth.begin(9600);
  Serial.begin(9600);        // Starts the serial communication
  pinMode(trigPin, OUTPUT);  // Sets the trigPin as an Output
  pinMode(echoPin, INPUT);   // Sets the echoPin as an Input
}
void loop() {
  // Clears the trigPin
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  // Sets the trigPin on HIGH state for 10 micro seconds
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  // Reads the echoPin, returns the sound wave travel time in microseconds
  duration = pulseIn(echoPin, HIGH);
  // Calculating the distance
  distance = duration * 0.034 / 2;
  // Prints the distance in CM on the Serial Monitor
  // Serial.println(distance);

  if (distance <= 40) {
    Bluetooth.println("alert");
    delay(5000);
  }

  if (Bluetooth.available()) {  //wait for data received
    Data = Bluetooth.read();
    if (Data == '0') {
      Serial.println("Test Data Received");
      Bluetooth.println("received");
    } else {
      Serial.println(Data);
    }
  }
  delay(100);
}
#include <AFMotor.h>
#include <Servo.h>

Servo cameraServo;
AF_DCMotor motor1(2, MOTOR12_64KHZ); // tạo động cơ #2, 64KHz pwm  
AF_DCMotor motor2(3, MOTOR12_64KHZ); // tạo động cơ #2, 64KHz pwm  
AF_DCMotor motor3(1, MOTOR34_64KHZ);
AF_DCMotor motor4(4, MOTOR34_64KHZ);

bool stringComplete = false;
String inputString = "";
int times = 0;
int motorSpeed = 210;
const int stepSpeed = 15;
int degree = 90;
const int stepDegree = 1;
bool cameraStop = false;
const int ledPin1 =  50;      // the number of the LED pin
const int ledPin2 =  52;      // the number of the LED pin

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  Serial1.begin(115200);
  Serial.println("Start");
  
  pinMode(ledPin1, OUTPUT);
  pinMode(ledPin2, OUTPUT);
   
  cameraServo.attach(10);
  motor1.setSpeed(motorSpeed);     // chọn tốc độ maximum 255`/255
  motor2.setSpeed(motorSpeed);
  motor3.setSpeed(motorSpeed);    
  motor4.setSpeed(motorSpeed);

  //cameraServo.write(90);
}


void loop() {

  delay(100);

  

  //for(int i =0; i <=180; i += 1) {
  //    cameraServo.write(i);
  //    delay(100);
  //  }

  // put your main code here, to run repeatedly:
  while (Serial1.available()) {
 
    // get the new byte:
    char inChar = (char)Serial1.read(); 
    // add it to the inputString:
    
    if (inChar == '#') {
      stringComplete = true;
      Serial.println(inputString);

      if(inputString == "0") {
            Serial.println("stop");
            motor1.run(RELEASE);      //   dừng động cơ
            motor2.run(RELEASE);
            motor3.run(RELEASE);
            motor4.run(RELEASE);
      }
      else if(inputString == "1") {
            Serial.println("forward");     
            motor1.run(FORWARD);      // động cơ tiến 
            motor2.run(FORWARD);
            motor3.run(FORWARD);
            motor4.run(FORWARD);
      }
      else if(inputString == "2") {
            Serial.println("reverse");  
            motor1.run(BACKWARD);     // động cơ lùi  
            motor2.run(BACKWARD);
            motor3.run(BACKWARD);
            motor4.run(BACKWARD);
      }
      else if(inputString == "3") {
            Serial.println("left");  
            motor1.run(FORWARD);     // động cơ left  
            motor2.run(BACKWARD);
            motor3.run(FORWARD);
            motor4.run(BACKWARD);
      }
      else if(inputString == "4") {
            Serial.println("right");  
            motor1.run(BACKWARD);     // động cơ right  
            motor2.run(FORWARD); 
            motor3.run(BACKWARD);
            motor4.run(FORWARD);
      }
      else if(inputString == "5") {
            Serial.println("camera upward");
            updateCamera(true);
       
      }
      else if(inputString == "6") {
            Serial.println("camera downward");
            updateCamera(false);
           
      }
      else if(inputString == "7") {
            Serial.println("camera stop");
            cameraStop = true;    
      }
      else if(inputString == "8") {
            Serial.println("Led on");
            digitalWrite(ledPin1, HIGH);
      }
      else if(inputString == "-8") {
            Serial.println("Led off");
            digitalWrite(ledPin1, LOW);        
      }
      else if(inputString == "9") {
            Serial.println("Led on");
            digitalWrite(ledPin2, HIGH);
      }
      else if(inputString == "-9") {
            Serial.println("Led off");
            digitalWrite(ledPin2, LOW);               
      }
      else if(inputString == "-s") {
            updateSpeed(false); 
            motor1.setSpeed(motorSpeed);
            motor2.setSpeed(motorSpeed);
            motor3.setSpeed(motorSpeed);
            motor4.setSpeed(motorSpeed);
      }
      else if(inputString == "+s") {
            updateSpeed(true);
            motor1.setSpeed(motorSpeed);
            motor2.setSpeed(motorSpeed);
            motor3.setSpeed(motorSpeed);
            motor4.setSpeed(motorSpeed);
      }
      else {
          
      }         
      inputString = "";
      continue;
     }
     
    inputString += inChar;
    
  }
}


void updateCamera(bool isUp) {
     cameraStop = false;
     inputString = "";
     if(isUp) {
        for(degree; degree < 150; ++degree) {
          if(!cameraStop) {
              cameraServo.write(degree);
              loop();
              continue;
            }
          break;
         }
      }
    else {
        for(degree; degree > 30; --degree) {
            if(!cameraStop) {
              cameraServo.write(degree);
              loop();
              continue;
            }
          break;
          }
      }
     Serial.print("Update degree to ");
     Serial.println(degree);   
  }

void updateSpeed(bool isUp) {
    if(isUp) {
        if(motorSpeed > (255 - stepSpeed)) {
            Serial.println("Max Speed");
            return;
          }
        motorSpeed += stepSpeed;
      }
    else {
        if(motorSpeed < stepSpeed*2) {
            Serial.println("Min Speed");
            return;
          }
        motorSpeed -= stepSpeed;
      }
     Serial.print("Update speed to ");
     Serial.println(motorSpeed);
     Serial1.println(motorSpeed);       
  }

// Test
import lejos.nxt.*;
import lejos.robotics.objectdetection.*;
import lejos.util.*;


public class Harvest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// Spin until ball is found
		// Go to ball until reached or hit line
		// If hit line then
			// Reverse for half a second (?)
			// Search for next ball
		// If reached ball
			// If ball is dark pick up
			// Else let it be, reverse for half a second then search for next ball
		// If the robot spins multiple times without detecting a ball then
		// move in one direction a bit and search again
		// if still no balls are present after three movements
		// then move to edge of circle and end program
		
		// Ports:
		// S1: Ultrasonic
		// S2: IR Down 
		// S3: IR Ball
		// S4:
		// MA: Left Wheel
		// MB: Forklift
		// MC: Right Wheel		
		
		/*
		Feature hit = null;
		for(int i = 0; i < 10; i++){
			hit = scanOnce();
			if(hit != null){
				LCD.drawInt((int) hit.getRangeReading().getRange(), 0, 0);
			}
			Delay.msDelay(1000);
		}
		*/
		/*/pickUp();
		Motor.B.forward();
		Delay.msDelay(500);
		Motor.B.backward();
		Delay.msDelay(500);
		Motor.B.stop();
		*/
		lineFollow();
		/*
		while(!Button.ESCAPE.isDown()){
			LCD.drawInt(downLight.getLightValue(), 0, 0);
		}
		*/
	}
	
	private static UltrasonicSensor ultra = new UltrasonicSensor(SensorPort.S2);
	private static FeatureDetector feature = new RangeFeatureDetector(ultra, 128, 10);
	/*feature.addListener(new FeatureListener() {
		public void featureDetected(Feature feature, FeatureDetector detector) {
			int range = (int) feature.getRangeReading().getRange();
			LCD.drawInt(range, 0, 0);
		}
	});*/
	
	private static LightSensor downLight = new LightSensor(SensorPort.S1);
	private static LightSensor ballLight = new LightSensor(SensorPort.S3);
	
	private static Timer stopTimer = new Timer(10000, new TimerListener() {
		public void timedOut() {
			Motor.A.stop();
			Motor.C.stop();
		}
	});
	
	private static Timer armTimer = new Timer(500, new TimerListener() {
		public void timedOut() {
			Motor.B.stop();
			armTimer.stop();
		}
	});
	
	private static Feature scanOnce() {
		Feature hit = null;
		Motor.A.setSpeed(90);
		Motor.C.setSpeed(90);
		boolean spin = true;
		stopTimer.start();
		Motor.A.forward();
		Motor.C.backward();
		while(spin && Motor.A.isMoving() && Motor.C.isMoving()){
			hit = feature.scan();
			if(hit != null){
				spin = false;
			}
		}
		Motor.A.stop();
		Motor.C.stop();
		stopTimer.stop();
		return hit;
	}
	
	private static void pickUp() {
		Motor.B.forward();
		armTimer.start();
	}
	
	private static int lineFollow() {
		// 0 = end of line?, 1 = yellow block, 2 = blue block
		int ret = 0;
		boolean follow = true, left = true;
		Motor.A.setSpeed(200);
		Motor.C.setSpeed(200);
		Motor.C.stop();
		Motor.A.forward();
		while(!Button.ESCAPE.isDown()){
			if(left && downLight.getLightValue() > 50){
				left = false;
				Motor.C.stop();
				Motor.A.forward();
			}else if(!left && downLight.getLightValue() < 50){
				left = true;
				Motor.A.stop();
				Motor.C.forward();
			}
		}
		return ret;
	}

}
import java.io.IOException;

import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTCommConnector;
import lejos.nxt.remote.RemoteNXT;
import lejos.robotics.objectdetection.Feature;
import lejos.robotics.objectdetection.FeatureDetector;
import lejos.robotics.objectdetection.RangeFeatureDetector;
import lejos.util.TimerListener;
import lejos.util.Timer;

public class HarvestB {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Spin until ball is found
		// Go to ball until reached or hit line
		// If hit line then
		// Reverse for half a second (?)
		// Search for next ball
		// If reached ball
<<<<<<< HEAD
		// If ball is dark pick up
		// Else let it be, reverse for half a second then search for next ball
		// If the robot spins multiple times without detecting a ball then
		// move in one direction a bit and search again
		// if still no balls are present after three movements
		// then move to edge of circle and end program

=======
			// If ball is light pick up
			// Else let it be, reverse for half a second then search for next ball
		// If the robot spins multiple times without detecting a ball then
		// Move in one direction a bit and search again
		// If collected all balls of that colour
			// Proceed to the location required to dump balls
		// If there are more balls
			// Then repeat the program for the different coloured balls
		// If still no balls are present after three movements
			// Then move to edge of circle and end program
				
>>>>>>> 34593fd7d9ea6ef8bc254007a3aab1e8465d8e89
		// Ports:
		// NXTF:
		// MB: Tray
		// NXTB:
		// S1: IR Down
		// S2: Ultrasonic
		// S3: IR Ball
		// MA: Left Wheel
		// MB: Forklift
		// MC: Right Wheel

		LCD.drawString("Connecting...", 0, 0);
		if (BTConnect()) {
			LCD.clear();
			LCD.drawString("Connected.", 0, 0);
		} else {
			LCD.clear();
			LCD.drawString("Connect fail.", 0, 0);
		}
		Button.waitForAnyPress();
		System.exit(1);
	}

	UltrasonicSensor ultra = new UltrasonicSensor(SensorPort.S2);
	FeatureDetector feature = new RangeFeatureDetector(ultra, 128, 10);
	
	feature.addListener(new FeatureListener() { 
		public void featureDetected(Feature feature, FeatureDetector detector) {
			int range = (int) feature.getRangeReading().getRange();
		LCD.drawInt(range, 0, 0); 
		}
	 });
	 

	private LightSensor downLight = new LightSensor(SensorPort.S1);
	private LightSensor ballLight = new LightSensor(SensorPort.S3);

	private static NXTCommConnector connector;
	private static RemoteNXT NXTF;

	private Timer stopTimer = new Timer(10000, new TimerListener() {
		public void timedOut() {
			Motor.A.stop();
			Motor.C.stop();
		}
	});

	private Timer armTimer = new Timer(500, new TimerListener() {
		public void timedOut() {
			Motor.B.stop();
			armTimer.stop();
		}
	});

	private static Timer trayTimer = new Timer(200, new TimerListener() {
		public void timedOut() {
			NXTF.B.stop();
			trayTimer.stop();
		}
	});

	private Feature scanOnce() {
		Feature hit = null;
		Motor.A.setSpeed(90);
		Motor.C.setSpeed(90);
		boolean spin = true;
		stopTimer.start();
		Motor.A.forward();
		Motor.C.backward();
		while (spin && Motor.A.isMoving() && Motor.C.isMoving()) {
			hit = feature.scan();
			if (hit != null) {
				spin = false;
			}
		}
		Motor.A.stop();
		Motor.C.stop();
		stopTimer.stop();
		return hit;
	}

	private void pickUp() {
		Motor.B.forward();
		armTimer.start();
	}

	private int lineFollow() {
		// 0 = end of line?, 1 = yellow block, 2 = blue block
		int ret = 0;
		boolean follow = true, left = true;
		Motor.A.setSpeed(200);
		Motor.C.setSpeed(200);
		Motor.C.stop();
		Motor.A.forward();
		while (!Button.ESCAPE.isDown()) {
			if (left && downLight.getLightValue() > 50) {
				left = false;
				Motor.C.stop();
				Motor.A.forward();
			} else if (!left && downLight.getLightValue() < 50) {
				left = true;
				Motor.A.stop();
				Motor.C.forward();
			}
		}
		return ret;
	}

	private static void dumpTray() {
		NXTF.B.forward();
		trayTimer.start();
	}

	private static boolean BTConnect() {
		try {
			connector = Bluetooth.getConnector();
			NXTF = new RemoteNXT("NXTCHEES", connector);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
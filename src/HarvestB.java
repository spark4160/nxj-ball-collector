import java.io.IOException;

import lejos.nxt.Button;
import lejos.nxt.ColorSensor;
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
import lejos.robotics.objectdetection.FeatureListener;
import lejos.util.TimerListener;
import lejos.util.Timer;

public class HarvestB {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
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
		// NXTF:
		// MB: Tray
		// NXTB:
		// S1: Colour Down
		// S2: Ultrasonic
		// S3: IR Ball
		// MA: Left Wheel
		// MB: Forklift
		// MC: Right Wheel

		feature.addListener(new FeatureListener() {
			public void featureDetected(Feature feature,
					FeatureDetector detector) {
				lastFeature = feature;
			}
		});
		feature.enableDetection(false);
		armTimer.stop();
		trayTimer.stop();
		stopTimer.stop();
		
		Motor.B.setSpeed(90);
		liftArm();

		LCD.drawInt((int) scanOnce().getRangeReading().getRange(), 0, 0);
		dropArm();
		Motor.A.forward();
		Motor.C.forward();
		while (ballLight.getLightValue() < 50)
			continue;
		liftArm();
		Motor.A.stop();
		Motor.C.stop();
		Button.waitForAnyPress();
		System.exit(0);

		// LCD.drawString("Connecting...", 0, 0);
		// if (BTConnect()) {
		// LCD.clear();
		// LCD.drawString("Connected.", 0, 0);
		// } else {
		// LCD.clear();
		// LCD.drawString("Connect fail.", 0, 0);
		// }
		// Button.waitForAnyPress();
		// System.exit(1);
	}

	private static UltrasonicSensor ultra = new UltrasonicSensor(SensorPort.S2);
	private static FeatureDetector feature = new RangeFeatureDetector(ultra,
			60, 10);
	private static Feature lastFeature;

	private static ColorSensor downColour = new ColorSensor(SensorPort.S1);
	private static LightSensor ballLight = new LightSensor(SensorPort.S3);

	private static NXTCommConnector connector;
	private static RemoteNXT NXTF;

	private static boolean stopTimerDing;
	private static Timer stopTimer = new Timer(10000, new TimerListener() {
		public void timedOut() {
			stopTimerDing = true;
			stopTimer.stop();
		}
	});

	private static boolean armTimerDing;
	private static Timer armTimer = new Timer(1500, new TimerListener() {
		public void timedOut() {
			armTimerDing = true;
			armTimer.stop();
		}
	});

	private static boolean trayTimerDing;
	private static Timer trayTimer = new Timer(200, new TimerListener() {
		public void timedOut() {
			trayTimerDing = true;
			trayTimer.stop();
		}
	});

	private static Feature scanOnce() {
		lastFeature = null;
		Motor.A.setSpeed(90);
		Motor.C.setSpeed(90);
		feature.enableDetection(true);
		stopTimerDing = false;
		stopTimer.start();
		Motor.A.forward();
		Motor.C.backward();
		while (lastFeature == null && !stopTimerDing)
			continue;
		Motor.A.stop();
		Motor.C.stop();
		feature.enableDetection(false);
		stopTimer.stop();
		return lastFeature;
	}

	private void pickUp() {
		Motor.B.forward();
		armTimer.start();
	}

	private int lineFollow() {
		// 0 = end of line?, 1 = yellow block, 2 = blue block
		final int ret = 0;
		final boolean follow = true;
		boolean left = true;
		Motor.A.setSpeed(200);
		Motor.C.setSpeed(200);
		Motor.C.stop();
		Motor.A.forward();
		while (!Button.ESCAPE.isDown()) {
			if (left && downColour.getLightValue() > 50) {
				left = false;
				Motor.C.stop();
				Motor.A.forward();
			} else if (!left && downColour.getLightValue() < 50) {
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
		} catch (final IOException e) {
			return false;
		}
	}
	
	private static void dropArm(){
		Motor.B.backward();
		armTimerDing = false;
		armTimer.start();
		while(!armTimerDing)
			continue;
		Motor.B.stop();
	}
	
	private static void liftArm(){
		Motor.B.forward();
		armTimerDing = false;
		armTimer.start();
		while(!armTimerDing)
			continue;
		Motor.B.stop();
	}
}
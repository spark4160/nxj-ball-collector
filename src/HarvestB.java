import java.io.IOException;

import lejos.nxt.Button;
import lejos.nxt.ButtonListener;
import lejos.nxt.ColorSensor;
import lejos.nxt.LCD;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTCommConnector;
import lejos.nxt.remote.RemoteNXT;
import lejos.robotics.Color;
import lejos.robotics.objectdetection.Feature;
import lejos.robotics.objectdetection.FeatureDetector;
import lejos.robotics.objectdetection.RangeFeatureDetector;
import lejos.robotics.objectdetection.FeatureListener;
import lejos.util.Delay;
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
		// Move in one direction a bit and search again
		// If four balls of one colour have been collected
		// Move to line
		// Follow line until it reaches the zone corresponding to the balls it collected
		// Dump the balls
		// Repeat for the other balls
		// End program

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

		// Nothing: 30
		// Silver: 57
		// Blue: 36

		Button.ESCAPE.addButtonListener(new ButtonListener() {
			public void buttonPressed(Button b) {
				// Do nothing
			}

			public void buttonReleased(Button b) {
				System.exit(1);
			}
		});
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
		Motor.B.setSpeed(180);

		// testBallLight();
		// testDownColour();
		// lineFollow(2);
		// System.exit(2);

		// Bluetooth connect
		LCD.drawString("Connecting...", 0, 0);
		if (BTConnect()) {
			LCD.clear();
			LCD.drawString("Connected.", 0, 0);
		} else {
			LCD.clear();
			LCD.drawString("Connect fail.", 0, 0);
		}

		NXTF.B.setSpeed(90);
		NXTF.B.stop();

		// reset tray

		NXTF.B.forward();
		trayTimerDing = false;
		trayTimer.start();
		while (!trayTimerDing)
			continue;
		NXTF.B.stop();

		// Run program
		// 2: silver, 3: blue
		int searchBall = 2;
		int collected = 0;
		liftArm();
		for (searchBall = 2; searchBall < 4; searchBall++) {
			collected = 0;
			while (collected < 4) {
				if (scanOnce() == null) {
					// Move a little
					Motor.A.forward();
					Motor.C.backward();
					Delay.msDelay(4000);
					if (moveForward(5000) == 1) {
						recoil();
					}
				} else {
					int found = moveToBall(searchBall);
					if (found == searchBall) {
						collected++;
					}
					Motor.A.forward();
					Motor.C.backward();
					Delay.msDelay(4000);
					if (moveForward(5000) == 1) {
						recoil();
					}
				}
			}
			// Move to the line
			moveForward(100000);
			lineFollow(searchBall);
			dumpTray();
			// Dump
		}

		System.exit(0);
	}

	private static UltrasonicSensor ultra = new UltrasonicSensor(SensorPort.S2);
	private static FeatureDetector feature = new RangeFeatureDetector(ultra,
			100, 10);
	private static Feature lastFeature;

	private static ColorSensor downColour = new ColorSensor(SensorPort.S1);
	private static LightSensor ballLight = new LightSensor(SensorPort.S3);

	private static NXTCommConnector connector;
	private static RemoteNXT NXTF;

	private static boolean stopTimerDing;
	private static Timer stopTimer = new Timer(20000, new TimerListener() {
		public void timedOut() {
			stopTimerDing = true;
			stopTimer.stop();
		}
	});

	private static boolean armTimerDing;
	private static Timer armTimer = new Timer(750, new TimerListener() {
		public void timedOut() {
			armTimerDing = true;
			armTimer.stop();
		}
	});

	private static boolean trayTimerDing;
	private static Timer trayTimer = new Timer(800, new TimerListener() {
		public void timedOut() {
			trayTimerDing = true;
			trayTimer.stop();
		}
	});

	private static Feature scanOnce() {
		//scans the area around the robot for a ball
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
		Delay.msDelay(200);
		Motor.A.stop();
		Motor.C.stop();
		feature.enableDetection(false);
		stopTimer.stop();
		return lastFeature;
	}

	private static void lineFollow(int zone) {
		// 2 = green block (silver), 3 = blue block (blue)
		boolean left = true;
		boolean follow = true;
		Motor.A.setSpeed(360);
		Motor.C.setSpeed(360);
		Motor.C.stop();
		Motor.A.forward();
		while (follow) {
			switch (downColour.getColorID()) {
			case Color.BLUE:
				if (zone == 3)
					follow = false;
				else if (left) {
					left = false;
					Motor.C.stop();
					Motor.A.forward();
				}
				break;
			case Color.GREEN:
				if (zone == 2)
					follow = false;
				else if (left) {
					left = false;
					Motor.C.stop();
					Motor.A.forward();
				}
				break;
			case Color.WHITE:
				if (left) {
					left = false;
					Motor.A.stop();
					Motor.C.forward();
				}
				break;
			default:
				if (!left) {
					left = true;
					Motor.C.stop();
					Motor.A.forward();
				}
				break;
			}
		}
		//turn 90 degrees from the line so that it can dump the balls
		Motor.A.forward();
		Motor.B.backward();
		Delay.msDelay(500);
		switch (downColour.getColorID()) {
		case Color.GREEN:
			if (zone == 2) {
			Motor.A.backward();
			Motor.C.forward();
			Delay.msDelay(1000);
			}
		case Color.BLUE:
			if (zone == 3) {
			Motor.A.backward();
			Motor.C.forward();
			Delay.msDelay(1000);
			}
		case Color.BLACK:
			Motor.A.backward();
			Motor.C.forward();
			Delay.msDelay(1000);
		default:
			continue;
		Delay.msDelay(2000);
		Motor.A.stop();
		Motor.C.stop();
		}
	}

	private static void dumpTray() {
		//dumps the balls out of the tray
		NXTF.B.backward();
		trayTimerDing = false;
		trayTimer.start();
		while (!trayTimerDing)
			continue;
		NXTF.B.stop();
		Motor.A.setSpeed(1080);
		Motor.C.setSpeed(1080);
		Motor.A.backward();
		Motor.C.backward();
		Delay.msDelay(1000);
		Motor.A.forward();
		Motor.C.forward();
		Delay.msDelay(1000);
		Motor.A.stop();
		Motor.C.stop();
		NXTF.B.forward();
		trayTimerDing = false;
		trayTimer.start();
		while (!trayTimerDing)
			continue;
		NXTF.B.stop();
	}

	private static boolean BTConnect() {
		//connects the two NXTs together using bluetooth
		try {
			connector = Bluetooth.getConnector();
			NXTF = new RemoteNXT("NXTCHEES", connector);
			return true;
		} catch (final IOException e) {
			return false;
		}
	}

	private static void dropArm() {
		//drops the forklift
		Motor.B.backward();
		armTimerDing = false;
		armTimer.start();
		while (!armTimerDing)
			continue;
		Motor.B.stop();
	}

	private static void liftArm() {
		//lifts the forklift
		Motor.B.forward();
		armTimerDing = false;
		armTimer.start();
		while (!armTimerDing)
			continue;
		Motor.B.stop();
	}

	private static int moveToBall(int ballType) {
		// 0: wrong ball, 1: hit line, 2: silver ball, 3: blue ball
		int ret = 0;
		int control = ballLight.getLightValue();
		dropArm();
		Motor.A.forward();
		Motor.C.forward();
		boolean move = true;
		while (move) {
			if (ballLight.getLightValue() > control + 5) {
				move = false;
			}
			if (downColour.getColorID() == Color.BLACK) {
				// stop
				move = false;
				ret = 1;
			}
		}
		Delay.msDelay(400);
		if (ret == 0 && ballType == 2
				&& ballLight.getLightValue() > control + 15) {
			liftArm();
			ret = 2;
		} else if (ret == 0 && ballType == 3
				&& ballLight.getLightValue() <= control + 15) {
			liftArm();
			ret = 3;
		} else {
			Motor.A.backward();
			Motor.C.backward();
			Delay.msDelay(2000);
			liftArm();
			Motor.A.forward();
			Motor.C.backward();
			Delay.msDelay(2000);
		}
		Motor.A.stop();
		Motor.C.stop();
		return ret;
	}

	private static void testBallLight() {
		//dispays the light value of the object in front of the light sensor
		while (Button.ESCAPE.isUp()) {
			LCD.drawInt(ballLight.getLightValue(), 0, 0);
		}
		System.exit(0);
	}

	private static void testDownColour() {
		//dispays which colour is underneath the colour sensor on the screen of the NXT
		while (Button.ESCAPE.isUp()) {
			switch (downColour.getColorID()) {
			case Color.BLACK:
				LCD.drawString("Black", 0, 0);
				break;
			case Color.BLUE:
				LCD.drawString("Blue", 0, 0);
				break;
			case Color.WHITE:
				LCD.drawString("White", 0, 0);
				break;
			case Color.GREEN:
				LCD.drawString("Green", 0, 0);
				break;
			default:
				LCD.drawString("Unknown", 0, 0);
				break;
			}
		}
		System.exit(0);
	}

	private static int moveForward(int time) {
		//moves the robot forward until the timer ends or the robot passes over a black line
		stopTimer.setDelay(time);
		stopTimer.stop();
		Motor.A.setSpeed(520);
		Motor.C.setSpeed(520);
		Motor.A.forward();
		Motor.C.forward();
		stopTimerDing = false;
		stopTimer.start();
		while (stopTimerDing) {
			if (downColour.getColorID() == Color.BLACK) {
				stopTimer.stop();
				Motor.A.stop();
				Motor.C.stop();
				return 1;
			}
		}
		Motor.A.stop();
		Motor.C.stop();
		return 0;
	}

	private static void recoil() {
		//moves backwards followed by turning a small amount
		Motor.A.setSpeed(520);
		Motor.C.setSpeed(520);
		Motor.A.backward();
		Motor.C.backward();
		Delay.msDelay(5000);
		Motor.A.backward();
		Motor.C.forward();
		Delay.msDelay(5000);
		Motor.A.stop();
		Motor.C.stop();
	}
}
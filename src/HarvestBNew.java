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
import lejos.robotics.objectdetection.FeatureListener;
import lejos.robotics.objectdetection.RangeFeatureDetector;
import lejos.util.Delay;
import lejos.util.Timer;
import lejos.util.TimerListener;

public class HarvestBNew {
	// Components:
	// Main
	// Set up
	// Turn left
	// Turn right
	// Move forward
	// Move backward
	// Scan for ball
	// Move forward
	// Follow line
	// Dump

	// Objects
	// BT
	private static NXTCommConnector NXTConnector;
	private static RemoteNXT NXTCHEES;
	// Sensors
	// Ultrasonic
	private static UltrasonicSensor ultrasonicSensor;
	private static FeatureDetector featureDetector;
	private static Feature lastSeenFeature;
	// Other
	private static ColorSensor downColourSensor;
	private static LightSensor ballLightSensor;
	// Timer
	private static Timer timer;
	private static boolean timing;

	// Main
	public static void main() {
		if (!SetUp())
			System.exit(1);
		// Look for silver
		int targetColour = Color.GREEN;
		// While running
		boolean running = true;
		while (running) {
			int collected = 0;
			while (collected < 4) {
				// Still balls to find, find them
				boolean timeOut = true;
				int time = 5000;
				if (Scan()) {
					timeOut = false;
				}
				// Lower the forklift
				ForkDrop();
				int result = ProbeForward(true, targetColour, timeOut, time);
				if (result == targetColour) {
					// A good ball was collected
					collected++;
				} else if (result == 1) {
					// The line was hit
					MoveBackward(720, 2000);
					TurnRight(720, 1000);
				}
			}
			// 4 of a ball have been collected
			// Move to the line
			ProbeForward(false, 0, false, 0);
			// Move a bit forward
			MoveForward(720, 250);
			// Follow the line to the right zone
			FollowLine(targetColour);
			// Dump the balls
			Dump();
			// Move into the field
			MoveForward(720, 5000);

			// If it has just found the blue balls, then it's time to go
			if (targetColour == Color.BLUE) {
				running = false;
			} else {
				// Find the blue balls
				targetColour = Color.GREEN;
			}
		}
		System.exit(0);
	}

	// Set up
	private static boolean SetUp() {
		// Return true if successful, false if unsuccessful
		// Create exit listener
		Button.ESCAPE.addButtonListener(new ButtonListener() {
			public void buttonPressed(Button b) {
				// Do nothing
			}

			public void buttonReleased(Button b) {
				// Exit
				System.exit(1);
			}
		});

		// Connect to bluetooth
		try {
			LCD.drawString("Connecting...", 0, 0);
			NXTConnector = Bluetooth.getConnector();
			NXTCHEES = new RemoteNXT("NXTCHEES", NXTConnector);
		} catch (final IOException e) {
			return false;
		}

		LCD.drawString("Connected.", 0, 0);

		// Set up sensors
		ultrasonicSensor = new UltrasonicSensor(SensorPort.S2);
		downColourSensor = new ColorSensor(SensorPort.S1);
		ballLightSensor = new LightSensor(SensorPort.S3);
		// Feature detector
		featureDetector = new RangeFeatureDetector(ultrasonicSensor, 100, 1);
		lastSeenFeature = null;
		featureDetector.addListener(new FeatureListener() {
			public void featureDetected(Feature feature,
					FeatureDetector detector) {
				lastSeenFeature = feature;
			}
		});
		featureDetector.enableDetection(false);

		// Set up timer
		timing = false;
		timer = new Timer(0, new TimerListener() {
			public void timedOut() {
				timing = false;
				timer.stop();
			}
		});
		timer.stop();

		// Set up motors
		// Arm
		Motor.B.setSpeed(180);
		ForkLift();
		// Cage
		NXTCHEES.B.setSpeed(90);
		CageLift();
		// Wheels
		Motor.A.setSpeed(180);
		Motor.C.setSpeed(180);
		Motor.A.stop();
		Motor.C.stop();

		return true;
	}

	// Turn left
	private static void TurnLeft(int speed, int time) {
		// Set speed
		Motor.A.setSpeed(speed);
		Motor.C.setSpeed(speed);
		// Start moving
		Motor.A.backward();
		Motor.C.forward();
		// Wait
		Delay.msDelay(time);
		// Stop
		Motor.A.stop();
		Motor.C.stop();
	}

	// Turn right
	private static void TurnRight(int speed, int time) {
		// Set speed
		Motor.A.setSpeed(speed);
		Motor.C.setSpeed(speed);
		// Start moving
		Motor.A.forward();
		Motor.C.backward();
		// Wait
		Delay.msDelay(time);
		// Stop
		Motor.A.stop();
		Motor.C.stop();
	}

	// Move forward
	private static void MoveForward(int speed, int time) {
		// Set speed
		Motor.A.setSpeed(speed);
		Motor.C.setSpeed(speed);
		// Start moving
		Motor.A.forward();
		Motor.C.forward();
		// Wait
		Delay.msDelay(time);
		// Stop
		Motor.A.stop();
		Motor.C.stop();
	}

	// Move backward
	private static void MoveBackward(int speed, int time) {
		// Set speed
		Motor.A.setSpeed(speed);
		Motor.C.setSpeed(speed);
		// Start moving
		Motor.A.backward();
		Motor.C.backward();
		// Wait
		Delay.msDelay(time);
		// Stop
		Motor.A.stop();
		Motor.C.stop();
	}

	// Fork lift
	private static void ForkLift() {
		Motor.B.forward();
		Delay.msDelay(750);
		Motor.B.stop();
	}

	// Fork drop
	private static void ForkDrop() {
		Motor.B.backward();
		Delay.msDelay(750);
		Motor.B.stop();
	}

	// Cage lift
	private static void CageLift() {
		NXTCHEES.B.forward();
		Delay.msDelay(800);
		NXTCHEES.B.stop();
	}

	// Cage drop
	private static void CageDrop() {
		NXTCHEES.B.backward();
		Delay.msDelay(800);
		NXTCHEES.B.stop();
	}

	// Scan for a ball
	private static boolean Scan() {
		// Return true if a ball is seen, false if timed out
		// Set the motor speeds
		Motor.A.setSpeed(180);
		Motor.C.setSpeed(180);
		// Start to spin
		Motor.A.forward();
		Motor.C.backward();
		// Start the timer and enable detection
		timer.setDelay(10000);
		timing = true;
		lastSeenFeature = null;
		timer.start();
		featureDetector.enableDetection(true);
		// Wait until something has been detected or the scanning has timed out
		while (timing && lastSeenFeature == null)
			continue;
		// Stop the timer and detection
		timer.stop();
		featureDetector.enableDetection(false);
		// Stop moving
		Motor.A.stop();
		Motor.C.stop();
		// If lastSeenFeature is no longer null, then something has been seen,
		// otherwise it has timed out
		if (lastSeenFeature == null)
			return false;
		return true;
	}

	// Move forward
	private static int ProbeForward(boolean pickUp, int targetBall,
			boolean timed, int milliSeconds) {
		// Return 0 if it just moved forward, 1 if it hit a line, Color.GREEN if
		// it hit a silver ball and Color.BLUE if it hit a blue ball
		int returnInt = 0;
		// Get a control value for the light sensor so that balls can be
		// detected
		int control = ballLightSensor.getLightValue();
		// Set the motor speeds
		Motor.A.setSpeed(720);
		Motor.C.setSpeed(720);
		// Start to move
		Motor.A.forward();
		Motor.C.forward();
		// If timed, then start the timer
		if (timed) {
			timer.setDelay(milliSeconds);
			timing = true;
			timer.start();
		}
		// Move until a ball is in the forklift or a black line is hit
		boolean moveForward = true;
		while (moveForward) {
			if (timed && !timing) {
				// Timed out
				moveForward = false;
			} else if (downColourSensor.getColorID() == Color.BLACK) {
				// Hit black line
				returnInt = 1;
				moveForward = false;
			} else if (pickUp) {
				if (ballLightSensor.getLightValue() > control + 15) {
					// Found a silver ball
					returnInt = Color.GREEN;
					moveForward = false;
				} else if (ballLightSensor.getLightValue() > control + 5) {
					// Found a blue ball
					returnInt = Color.BLUE;
					moveForward = false;
				}
			}
		}
		// Stop the timer
		timer.stop();
		// If a ball has been found, then check if it is the right ball or not
		// and if it is pick it up, otherwise stop and move backwards and spin a
		// bit
		if (pickUp) {
			if (returnInt == targetBall) {
				// Found a good ball
				// Pick it up
				ForkLift();
			} else if (returnInt != targetBall) {
				// Found a bad ball
				// Move away
				MoveBackward(720, 3000);
				TurnRight(720, 1000);
			}
		}
		// Stop moving
		Motor.A.stop();
		Motor.C.stop();
		return returnInt;
	}

	// Follow line
	private static void FollowLine(int targetColour) {
		// Color.GREEN = green block (silver), Color.BLUE = blue block (blue)
		// Keep on the left side of the line, turn right when on white, to the
		// left when on black
		// Set the variables
		boolean turningLeft = true;
		boolean following = true;
		// Set the motor speeds
		Motor.A.setSpeed(360);
		Motor.C.setSpeed(360);
		// Start to turn left
		Motor.A.backward();
		Motor.C.forward();
		// Enter loop
		while (following) {
			switch (downColourSensor.getColorID()) {
			case Color.GREEN:
				// On top of green
				if (targetColour == Color.GREEN) {
					// Destination reached
					following = false;
				} else {
					// If this isn't the target, then treat it as black
					// If turning right, then turn left
					if (!turningLeft) {
						turningLeft = true;
						// Start to turn left
						Motor.A.backward();
						Motor.C.forward();
					}
				}
				break;
			case Color.BLUE:
				// On top of blue
				if (targetColour == Color.BLUE) {
					// Destination reached
					following = false;
				} else {
					// If this isn't the target, then treat it as black
					// If turning right, then turn left
					if (!turningLeft) {
						turningLeft = true;
						// Start to turn left
						Motor.A.backward();
						Motor.C.forward();
					}
				}
				break;
			case Color.BLACK:
				// On top of black
				// If turning right, then turn left
				if (!turningLeft) {
					turningLeft = true;
					// Start to turn left
					Motor.A.backward();
					Motor.C.forward();
				}
				break;
			default:
				// On top of any other colour (white)
				// If turning left, then turn right
				if (turningLeft) {
					turningLeft = false;
					// Start to turn right
					Motor.A.forward();
					Motor.C.backward();
				}
				break;
			}
		}
		// Stopped following
		// Stop motors
		Motor.A.stop();
		Motor.C.stop();
	}

	// Dump cage
	private static void Dump() {
		TurnRight(720, 1000);
		CageDrop();
		MoveBackward(1080, 1000);
		MoveForward(1080, 1000);
		CageLift();
	}
}

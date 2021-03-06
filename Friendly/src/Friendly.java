import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.*;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;
import lejos.utility.Delay;

import org.opencv.core.*;

/**
 * Friendly is a robot who tries to look at you and keep your face in front of the camera
 * He likes seeing people.
 * 
 * @author mcrosbie
 *
 */
public class Friendly {

	static FaceDetector fd;	
	static EV3LargeRegulatedMotor left, right;
	
	public static void main(String[] args) {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		LCD.clear();
		LCD.drawString("Friendly", 0, 0);
		
		try {
			left = new EV3LargeRegulatedMotor(MotorPort.B);
			right = new EV3LargeRegulatedMotor(MotorPort.C);
			
			left.setSpeed(400);
			right.setSpeed(400);

			fd = new FaceDetector();
			fd.setDaemon(true);
			fd.start();
		
			Delay.msDelay(500);
			System.out.println("Running");
			while(Button.ENTER.isUp()) {
				
				if(fd.faceDetected()) {
					Point p = fd.getFaceCenter();
					System.out.println("Face center: " + p.x + ","+p.y);
					if(p.x < 70) {
						LCD.drawString("LEFT  ", 0, 2);
						right.rotate(-360, true);
					} else if(p.x > 90) {
						LCD.drawString("RIGHT ", 0, 2);
						left.rotate(-360, true);
					} else {
						LCD.drawString("CENTER", 0, 2);
						left.stop();
						right.stop();
					}				
				}
			}
		} catch(Exception e) {
			Sound.buzz();
			System.err.println("Exception: " + e.getMessage());
			System.exit(0);
		}
		
		fd.stopRunning();
		
		left.stop();
		right.stop();
	}

}


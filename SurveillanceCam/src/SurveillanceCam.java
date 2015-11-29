import java.io.IOException;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.utility.Delay;

import com.dropbox.core.DbxException;

/**
 * A simple webcam surveillance program that captures an image periodically
 * overlays the date and time and current battery voltage and then saves that
 * to Dropbox.
 * 
 * Needs to be on wifi to talk to Dropbox, and have a USB webcam attached to the
 * host port of the EV3
 * 
 * @date 7 July 2015
 * @author mcrosbie http://thinkbricks.net
 *
 */
public class SurveillanceCam {

	private static ImageCapture imgCapture;

	private static final String AUTHFILENAME = "/home/lejos/programs/access-token.json";
	private static final String IMAGEFILENAME = "snapshots/wanderer_";

	public static void main(String args[]) throws IOException, DbxException {
	   int ret = _main(args);
	   System.exit(ret);
	}

   public static int _main(String[] args) {

	   // Default to access token file if not on cmd line
	   String argAuthFile;
	   if (args.length != 1) {
		   argAuthFile = AUTHFILENAME;
	   } else {
		   argAuthFile = args[0];
	   }
	   
	   System.out.println(">> SURVEILLANCE STARTING...");
	   
	   try {
		   imgCapture = new ImageCapture(argAuthFile, IMAGEFILENAME);
	   } catch (Exception ex) {
		   System.err.println("Failed to initialise camera and Dropbox: " + ex.getMessage());
	   }
	   
	   imgCapture.setDaemon(true);
	   imgCapture.start();
	   
	   Sound.beepSequenceUp();
	   while(Button.ENTER.isUp()) {
		   Delay.msDelay(100);
	   }
	   
	   imgCapture.stopRunning();
	   
	   System.out.println("Done");
	   return 0;
	}

}

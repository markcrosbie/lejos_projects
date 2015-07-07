import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Locale;

import javax.imageio.ImageIO;

import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.lcd.LCD;
import lejos.hardware.video.Video;
import lejos.hardware.video.YUYVImage;
import lejos.utility.Delay;

import com.dropbox.core.*;
import com.dropbox.core.json.JsonReader;
import com.dropbox.core.util.IOUtil;

/**
 * An autonomous robot that drives around and takes photos using a webcam and
 * saves them to Dropbox.
 * 
 * @author mcrosbie
 *
 */
public class Wand3r3r {

	private static Navigator nav;
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
	   
	   System.out.println(">> WANDERER STARTING...");
	   
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

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
 * Use a cheap webcam to take a snapshot and send the image to Dropbox.
 * Requires a Dropbox authorisation token to be created
 * EV3 needs to be connected to the wifi and the webcam connected too.
 * Requires leJOS 0.9.0 release and some setup. 
 * See http://thinkbricks.net/set-up-dropbox-in-lejos-on-the-ev3 for Dropbox-leJOS setup
 * 
 * Press ENTER button to take a photo. You should see it appear in your Dropbox folder
 * 
 * @author mcrosbie
 * http://thinkbricks.net
 * April 2015
 */

public class Snapshot {
   private static final int WIDTH = 160;
   private static final int HEIGHT = 120;
   private static int NUM_PIXELS = WIDTH * HEIGHT;
   private static int FRAME_SIZE = NUM_PIXELS * 2;
   private static final String AUTHFILENAME = "./access-token.json";
   private static final String FILENAME = "snapshots/snapshot_";

   public static void main(String args[]) throws IOException, DbxException {
	   int ret = _main(args);
	   System.exit(ret);
   }
   
   public static int _main(String[] args) throws IOException, DbxException  {
       int frames = 0;
       int threshold;
              
       // Default to access token file if not on cmd line
       String argAuthFile;
	   if (args.length != 1) {
		   argAuthFile = AUTHFILENAME;
	   } else {
		   argAuthFile = args[0];
	   }

       System.out.println("Reading auth info file " + argAuthFile);
       // Read auth info file.
       DbxAuthInfo authInfo;
       try {
           authInfo = DbxAuthInfo.Reader.readFromFile(argAuthFile);
       } catch (JsonReader.FileLoadException ex) {
         System.err.println("Error loading <auth-file>: " + ex.getMessage());
         return 1;
       }
       
       System.out.println("Creating Dropbox client...");

       // Create a DbxClient to make API calls.
       String userLocale = Locale.getDefault().toString();
       DbxRequestConfig requestConfig = new DbxRequestConfig("Snapshot", userLocale);
       DbxClient dbxClient = new DbxClient(requestConfig, authInfo.accessToken, authInfo.host);
 
       System.out.println("Open video camera...");
       // Get the video device and open the stream
       Video video = BrickFinder.getDefault().getVideo();
       video.open(WIDTH, HEIGHT);
     
       // Create the frame buffer to hold the image in
       byte[] frame = video.createFrame();
       BufferedImage img = new BufferedImage(WIDTH, HEIGHT,BufferedImage.TYPE_INT_RGB);
       YUYVImage yuyvImg = new YUYVImage(frame, video.getWidth(), video.getHeight());

       GraphicsLCD g = BrickFinder.getDefault().getGraphicsLCD();

       // Keep displaying an image. 
       // Save a file to storage when enter is pressed
       // Escape to exit
       g.clear();
       System.out.println("Starting main capture loop...");
       while (Button.ENTER.isUp()) {
    	   try {
               video.grabFrame(frame);
               
               // Display on the EV3 screen
               //threshold = yuyvImg.getMeanY();
               //yuyvImg.display(g, 0, 0, threshold);
               
             // if (Button.ENTER.isUp()) {
            	  Sound.playTone(500, 100);
                  for(int i=0;i<FRAME_SIZE;i+=4) {
                      int y1 = frame[i] & 0xFF;
                      int y2 = frame[i+2] & 0xFF;
                      int u = frame[i+1] & 0xFF;
                      int v = frame[i+3] & 0xFF;
                      int rgb1 = convertYUVtoARGB(y1,u,v);
                      int rgb2 = convertYUVtoARGB(y2,u,v);
                      img.setRGB((i % (WIDTH * 2)) / 2, i / (WIDTH * 2), rgb1);
                      img.setRGB((i % (WIDTH * 2)) / 2 + 1, i / (WIDTH * 2), rgb2);
                  }
                  
                  // Convert the RGB image into a jpg format
                  ByteArrayOutputStream baos = new ByteArrayOutputStream();
                  ImageIO.write(img, "jpg", baos);
                  byte[] jpgImageBytes = baos.toByteArray();

                  String filename = FILENAME+frames+".jpg";
                  System.out.println("Saving image to " + filename);
                  saveImageToFile(baos.toByteArray(), filename);
                  
                  System.out.println("Saving to Dropbox");
                  writeImageToDropbox(dbxClient, baos.toByteArray(), "/"+filename);
                  System.out.println("Done");
                  Sound.beepSequenceUp();
                  
            	  frames++;
              //}
              
              Delay.msDelay(2000);
              
    	   } catch (IOException ioe) {
	           ioe.printStackTrace();
	           System.out.println("Driver exception: " + ioe.getMessage());
	           Sound.buzz();
	           return 1;
	      }
       }
      video.close();
      g.clear();
      return 0;
   }
   
    /**
    * Save an image file from the webcam to disk
    * 
    * @param img - image byte buffer
    * @param filename - file to store to
    */
   public static void saveImageToFile(byte[] img, String filename) {
	   try {
		   FileOutputStream fos = new FileOutputStream(filename);
		   fos.write(img);
		   fos.close();
	   } catch(IOException ioe) {
           ioe.printStackTrace();
           System.out.println("Driver exception: " + ioe.getMessage());		   
	   }
   }
   
   private static void writeImageToDropbox(DbxClient dbxClient, byte[] img, String dbFilename) 
		   throws DbxException, IOException {
       // Make the API call to upload the file.
       DbxEntry.File metadata;
       try {
    	   metadata = dbxClient.uploadFile(dbFilename, DbxWriteMode.add(), img.length, 
    			   new ByteArrayInputStream(img));
    	   System.out.println("Metadata: " + metadata.toStringMultiline());
       } catch (DbxException ex) {
               System.out.println("Error uploading to Dropbox: " + ex.getMessage());
       }
   }
    
   private static int convertYUVtoARGB(int y, int u, int v) {
       int c = y - 16;
       int d = u - 128;
       int e = v - 128;
       int r = (298*c+409*e+128)/256;
       int g = (298*c-100*d-208*e+128)/256;
       int b = (298*c+516*d+128)/256;
       r = r>255? 255 : r<0 ? 0 : r;
       g = g>255? 255 : g<0 ? 0 : g;
       b = b>255? 255 : b<0 ? 0 : b;
       return 0xff000000 | (r<<16) | (g<<8) | b;
   }

 }
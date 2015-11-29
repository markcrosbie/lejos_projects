import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import javax.imageio.ImageIO;

import lejos.hardware.Battery;
import lejos.hardware.BrickFinder;
import lejos.hardware.Sound;
import lejos.hardware.video.Video;
import lejos.hardware.video.YUYVImage;
import lejos.utility.Delay;

import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWriteMode;
import com.dropbox.core.json.JsonReader;

/**
 * ImageCapture thread
 * Handles periodicly capturing images from the webcam, saving them to disk
 * and uploading to Dropbox.
 * 
 * @author mcrosbie
 *
 */
public class ImageCapture extends Thread {

	private final int WIDTH = 320;
	private final int HEIGHT = 240;
	private int NUM_PIXELS = WIDTH * HEIGHT;
	private int FRAME_SIZE = NUM_PIXELS * 2;
	private final int CAPTURE_FREQUENCY = 10000;
	
	private DbxClient dbxClient;
	private Video video;
	
	private boolean running = true;
	private String base;
	
    // Timing buffers
	private long t1[], t2[], t3[], t4[];
	private final int SAMPLES = 10;
	private int iter;
	
	public ImageCapture(String argAuthFile, String imageBaseFilename) throws IOException {

		initialiseDropbox(argAuthFile);
		openVideoCamera();
		base = imageBaseFilename;		
		t1 = new long[SAMPLES];
		t2 = new long[SAMPLES];
		t3 = new long[SAMPLES];
		t4 = new long[SAMPLES];
	}
		
	public void stopRunning() {
		running = false;
	}
	
	/**
	 * Initialise the Dropbox client API
	 */
	public int initialiseDropbox(String argAuthFile) {
	   // Read auth info file.
	   DbxAuthInfo authInfo;
	   try {
	       authInfo = DbxAuthInfo.Reader.readFromFile(argAuthFile);
	   } catch (JsonReader.FileLoadException ex) {
	     System.err.println("Error loading <auth-file>: " + ex.getMessage());
	     return 1;
	   }
	   	
	   // Create a DbxClient to make API calls.
	   String userLocale = Locale.getDefault().toString();
	   DbxRequestConfig requestConfig = new DbxRequestConfig("Wanderer", userLocale);
	   dbxClient = new DbxClient(requestConfig, authInfo.accessToken, authInfo.host);

	   return 0;
	}
	
	/**
	 * Open the video camera
	 * 
	 */
	public void openVideoCamera() throws IOException {
	   // Get the video device and open the stream
	   video = BrickFinder.getDefault().getVideo();
	   video.open(WIDTH, HEIGHT);	 
	}
	
   /**
    * Save an image file from the webcam to disk
    * 
    * @param img - image byte buffer
    * @param filename - file to store to
    */
   public void saveImageToFile(byte[] img, String filename) {
	   try {
		   FileOutputStream fos = new FileOutputStream(filename);
		   fos.write(img);
		   fos.close();
	   } catch(IOException ioe) {
           ioe.printStackTrace();
           System.out.println("Driver exception: " + ioe.getMessage());		   
	   }
   }
   
   private void writeImageToDropbox(DbxClient dbxClient, byte[] img, String dbFilename) 
		   throws DbxException, IOException {
       // Make the API call to upload the file.
       DbxEntry.File metadata;
       try {
    	   metadata = dbxClient.uploadFile(dbFilename, DbxWriteMode.add(), img.length, 
    			   new ByteArrayInputStream(img));
       } catch (DbxException ex) {
               System.out.println("Error uploading to Dropbox: " + ex.getMessage());
       }
   }
    
   private int convertYUVtoARGB(int y, int u, int v) {
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
   
   public void run() {
	   
	   int i=0;
	   
	   // Create the frame buffer to hold the image in
	   byte[] frame = video.createFrame();
	   BufferedImage img = new BufferedImage(WIDTH, HEIGHT,BufferedImage.TYPE_INT_RGB);
	   
	   long lastCapture = 0;
	 
	   while(running) {
		   
		   long t = System.currentTimeMillis();
		   
		   // Only start a capture if we're due...
		   if(t > (lastCapture + CAPTURE_FREQUENCY)) {
			   lastCapture = t;
			   
			   System.out.println("Starting capture...");
			   
			   // Grab a video frame
			   try {
				   video.grabFrame(frame);
			   } catch (IOException ex) {
	               System.out.println("Error grabbing video: " + ex.getMessage());
			   }
			   			   
			   // Convert the YUV format from the camera into RGB format
			   // leJOS libraries only return YUV format from cameras.
			   try {
				   for(i=0;i<FRAME_SIZE;i+=4) {
		        	   int y1 = frame[i] & 0xFF;
		        	   int y2 = frame[i+2] & 0xFF;
		        	   int u = frame[i+1] & 0xFF;
		        	   int v = frame[i+3] & 0xFF;
		        	   int rgb1 = convertYUVtoARGB(y1,u,v);
		        	   int rgb2 = convertYUVtoARGB(y2,u,v);
		        	   img.setRGB((i % (WIDTH * 2)) / 2, i / (WIDTH * 2), rgb1);
		        	   img.setRGB((i % (WIDTH * 2)) / 2 + 1, i / (WIDTH * 2), rgb2);
				   }
			   } catch (Exception ex) {
				   System.err.println("Error converting frame: i="+ i);
				   System.err.println(ex.getMessage());
			   }
			   
			   img = addTextOverlay(img);
	
		      // Convert the RGB image into a jpg format
		      ByteArrayOutputStream baos = new ByteArrayOutputStream();
		      try {
			      ImageIO.write(img, "jpg", baos);	    	  
		      } catch (IOException ex) {
	              System.out.println("Error converting to jpg: " + ex.getMessage());  
		      }
		      
		      // Save the JPG image to Dropbox
		      try {
			      Date date = new Date();
			      String filename = base+date.toString()+".jpg";
		    	  writeImageToDropbox(dbxClient, baos.toByteArray(), "/"+filename);
		      } catch (Exception ex) {
	              System.out.println("Error saving to Dropbox: " + ex.getMessage());
		      }
		      
		      System.out.println("Save to Dropbox complete");
		   }	   
	   }

	   try {
		   video.close();		   
	   } catch (IOException ex) {
           System.out.println("Error closing video: " + ex.getMessage());		   
	   }	   
   }
   
   private BufferedImage addTextOverlay(BufferedImage old) {
       int w = old.getWidth();
       int h = old.getHeight();
       BufferedImage img = new BufferedImage(
               w, h, BufferedImage.TYPE_INT_RGB);
       Graphics2D g2d = img.createGraphics();
       g2d.drawImage(old, 0, 0, null);
       g2d.setPaint(Color.red);
       g2d.setFont(new Font("Serif", Font.BOLD, 15));
       Date d = new Date(System.currentTimeMillis());
       String s = "" + d.toString();
       FontMetrics fm = g2d.getFontMetrics();
       int x = img.getWidth() - fm.stringWidth(s) - 5;
       int y = fm.getHeight();
       g2d.drawString(s, x, y);

       s = "B: " + Battery.getVoltageMilliVolt() + " mV";
       x = img.getWidth() - fm.stringWidth(s) - 5;
       g2d.drawString(s, x, h - y - 5);
       
       g2d.dispose();
       return img;
   }

   
   private void printTiming() {
	   long total=0;
	   int i;
	   for(i=0; i < SAMPLES; i++) {
		   total += t1[i];
	   }
	   System.out.println(">> Average video frame grab time:\t" + total/SAMPLES + " ms");

	   total=0;
	   for(i=0; i < SAMPLES; i++) {
		   total += t2[i];
	   }
	   System.out.println(">> Average YUV to RGB time:\t" + total/SAMPLES + " ms");

	   total=0;
	   for(i=0; i < SAMPLES; i++) {
		   total += t3[i];
	   }
	   System.out.println(">> Average RGB to JPG time:\t" + total/SAMPLES + " ms");

	   total=0;
	   for(i=0; i < SAMPLES; i++) {
		   total += t4[i];
	   }
	   System.out.println(">> Average time to save to Dropbox:\t" + total/SAMPLES + " ms");
   }
}

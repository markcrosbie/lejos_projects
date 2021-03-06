import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

/**
 * Detect a face in the video stream from the camera and highlight the face in the image
 * streamed to the web-browser
 * 
 * To use: either run directly and open your browser at the IP address for the brick :8080
 * e.g. 192.168.0.123:8080 to see a 160x120 live streamed video with a green square around the 
 * faces detected.
 * 
 * Very sensitive to lighting of course!
 * 
 * This code heavily borrows code written by Lawrie Griffiths available at
 * https://lejosnews.wordpress.com/2015/09/26/opencv-web-streaming/
 * 
 * As a convenience you can run this from the command line to play around with the accuracy
 * parameters:
 * jrun -cp HTTPFaceDetect.jar HTTPFaceDetect <minNeighbours> <scale> <l/h>
 * l = LBP feature classifier
 * h = Haar feeature classifier (slower but more accurate)
 * 
 * @author mcrosbie http://thinkbricks.net
 * @date 29/11/2015
 *
 */
public class HTTPFaceDetect {
	
	public static void main(String[] args) throws Exception {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		DetectFaceDemo d;
		
		if(args.length > 0) {
			int min = Integer.parseInt(args[0]);
			double scale = Float.parseFloat(args[1]);
			String features;
			if(args[2].equals("h")) {
				features = "/haarcascade_frontalface_alt.xml";
			} else {
				features = "lbpcascade_frontalface.xml"; 
			}
			
			d = new DetectFaceDemo(0, scale, min, features);
			
		} else {
			d = new DetectFaceDemo();
		}
	    d.run();
	}
}

class DetectFaceDemo {

	private int frameCount = 0;
	private CascadeClassifier faceDetector;
	
	private int flags = 0;
	private double scaleFactor = 1.8;
	private int minNeighbours = 2;
    private Size minSize = new Size(20,20);
    private Size maxSize = new Size(160,120);
    private final String HaarFeatures = "/haarcascade_frontalface_alt.xml";  //classifiers to detect eyes and face.
    private final String LbpFeatures = "/lbpcascade_frontalface.xml";
    private String features;
    
    public DetectFaceDemo() {
    	features = LbpFeatures;
    }
    
    public DetectFaceDemo(int _flags, double _scale, int _minNeighbours, String _features) {
    	flags = _flags;
    	scaleFactor = _scale;
    	minNeighbours = _minNeighbours;
    	features = _features;
    }
    
	public void run() throws Exception {
		
	    Mat frame = new Mat();
	    
        VideoCapture vid = new VideoCapture(0);
        vid.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 160);
        vid.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 120);
        vid.open(0);
        System.out.println("Camera open");   
        
        faceDetector = new CascadeClassifier(getClass().getResource(features).getPath());

        if(faceDetector.empty()) {
        	System.err.println("Failed to load classifier ");
        	Sound.buzz();
        	System.exit(1);
        } else {
        	System.out.println("Loaded classifier");
        }

        ServerSocket ss = new ServerSocket(8080);
        Socket sock = ss.accept();
        System.out.println("Socket connected");
        String boundary = "Thats it folks!";
        writeHeader(sock.getOutputStream(), boundary);
        System.out.println("Written header");
          
        /**
         * Capture images and stream to the web client
         */
        while (Button.ESCAPE.isUp()) {
            
        	vid.read(frame);     	
        	
        	Point midPoint1 = new Point((double)(frame.width()/2), 0.0);
        	Point midPoint2 = new Point((double)(frame.width()/2), (double)frame.height());
        	
            if (!frame.empty()) {
            	           	
                MatOfRect faces = new MatOfRect();
                Mat mRgba=new Mat();  
                Mat mGrey=new Mat();  
                frame.copyTo(mRgba);  
                frame.copyTo(mGrey);  
                Imgproc.cvtColor( mRgba, mGrey, Imgproc.COLOR_BGR2GRAY);  
                Imgproc.equalizeHist( mGrey, mGrey );                  
                
                // detect faces
                long startTime = System.nanoTime();  
                faceDetector.detectMultiScale(
                		mGrey, 
                		faces,
                		scaleFactor,
                		minNeighbours,
                		flags,
                		minSize,
                		maxSize);
                long endTime = System.nanoTime();  

                //System.out.println(String.format("detectMultiScale done: took %.2f ms", 
                //		(float)(endTime - startTime)/1000000));  

               	// draw a line down the middle of the frame so we can tell left from right
            	Core.line(frame, midPoint1, midPoint2, new Scalar(255, 0, 0, 255), 1);

                int numFaces = faces.toArray().length;
                if(numFaces > 0) {
                	System.out.println(String.format("+++ Detected %s faces", numFaces));

		            // each rectangle in faces is a face
		            Rect[] facesArray = faces.toArray();
		            for (int i = 0; i < facesArray.length; i++) {
		            	Rect rect = facesArray[i];
		                Point center= new Point(rect.x + rect.width*0.5, rect.y + rect.height*0.5 );  
		                if(center.x < (frame.width()/2)) {
		                	System.out.println("<<<<<<<<<< Face to the LEFT");
		                } else {
		                	System.out.println("Face to the RIGHT >>>>>>>>>>>>>");
		                }
		 
		            	Core.rectangle(frame, rect.tl(), rect.br(), new Scalar(0, 255, 0, 255), 2);
		            }
                } else {
                	LCD.clear(5);
                }

                writeJpg(sock.getOutputStream(), frame, boundary);
             } else {
            	 System.out.println("No picture");
             }
        }
        sock.close();
        ss.close();    
	}
	
	/**
	 * Write the HTTP header to the output stream
	 * 
	 * @param stream OutputStream connected to the connect
	 * @param boundary Text to insert between messages
	 * @throws IOException
	 */
    private void writeHeader(OutputStream stream, String boundary) throws IOException {
        stream.write(("HTTP/1.0 200 OK\r\n" +
                "Connection: close\r\n" +
                "Max-Age: 0\r\n" +
                "Expires: 0\r\n" +
                "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                "Pragma: no-cache\r\n" + 
                "Content-Type: multipart/x-mixed-replace; " +
                "boundary=" + boundary + "\r\n" +
                "\r\n" +
                "--" + boundary + "\r\n").getBytes());
    }

    /**
     * Convert an OpenCV matrix into JPG format and write it to the HTTP output stream
     * 
     * @param stream
     * @param img
     * @param boundary
     * @throws IOException
     */
    private void writeJpg(OutputStream stream, Mat img, String boundary) throws IOException {
        MatOfByte buf = new MatOfByte();
        Highgui.imencode(".jpg", img, buf);
        byte[] imageBytes = buf.toArray();
        stream.write(("Content-type: image/jpeg\r\n" +
                "Content-Length: " + imageBytes.length + "\r\n" +
                "\r\n").getBytes());
        stream.write(imageBytes);
        stream.write(("\r\n--" + boundary + "\r\n").getBytes());
    }
}

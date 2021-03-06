import lejos.hardware.Sound;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

/**
 * A class to detect and track faces streamed from a webcam
 * Keeps track of the x,y of the faces and can be queried
 * 
 * @author mcrosbie
 *
 */
class FaceDetector extends Thread {

	private CascadeClassifier faceDetector;
	
	private int flags = 0;
	private double scaleFactor = 1.8;
	private int minNeighbours = 2;
    private Size minSize = new Size(10,10);
    private Size maxSize = new Size(320,240);
    private final String HaarFeaturesFile = "/haarcascade_frontalface_alt.xml";  //classifiers to detect eyes and face.
    private final String LbpFeaturesFile = "/lbpcascade_frontalface.xml";
    private String features;
    
    private final int DEFAULT_FRAME_WIDTH = 160;
    private final int DEFAULT_FRAME_HEIGHT = 120;
    private int frameWidth, frameHeight;
    
    private int numFacesDetected = 0;
    private Point faceCenter;
    private Mat frame;
    private VideoCapture vid;
    
    private boolean running = true;
    
    /**
     * Default constructor uses the LBP feature detector - faster but less accurate
     * and sets scaleFactor and minNeighbours
     */
    public FaceDetector() {
    	this(0, 1.1, 1, "/lbpcascade_frontalface.xml", 160, 120);
    }
    
    public FaceDetector(int _frameWidth, int _frameHeight) {
    	this(0, 1.5, 1, "/lbpcascade_frontalface.xml", _frameWidth, _frameHeight);  	
    }
    
    /**
     * 
     * @param _flags Flags value
     * @param _scale scaleFactor
     * @param _minNeighbours
     * @param _features which feature file to use
     * @param _frameWidth Width of capture frame from camera
     * @param _frameHeight Height of capture frame from camera
     */
    public FaceDetector(int _flags, double _scale, int _minNeighbours, String _features, int _frameWidth, int _frameHeight) {
    	flags = _flags;
    	scaleFactor = _scale;
    	minNeighbours = _minNeighbours;
    	features = _features;
    	frameWidth = _frameWidth;
    	frameHeight = _frameHeight;
    	
	    frame = new Mat();
	    
        vid = new VideoCapture(0);
        vid.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, frameWidth);
        vid.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, frameHeight);
        vid.open(0);
        
        faceDetector = new CascadeClassifier(getClass().getResource(features).getPath());

        System.out.println("Classifier " + features + " loaded");
        
        if(faceDetector.empty()) {
        	System.err.println("Failed to load classifier ");
        	return;
        } 

    }
    
    public boolean faceDetected() {
    	return numFacesDetected > 0;
    }
    
    public int numFacesDetected() {
    	return numFacesDetected;
    }
    
    public Point getFaceCenter() {
    	if(numFacesDetected > 0) {
    		return new Point(faceCenter.x, faceCenter.y);
    	} else {
    		return null;
    	}
    }
    
    public void stopRunning() {
    	running = false;
    }
    
	public void run() {
		
		System.out.println("FaceDetector starting");
          
        /**
         * Capture images and detect face centers
         */
        while (running) {
            
        	try {
	        	vid.read(frame);         	
	       	
	            if (!frame.empty()) {
	            	           	
	                MatOfRect faces = new MatOfRect();
	                Mat mRgba=new Mat();  
	                Mat mGrey=new Mat();  
	                frame.copyTo(mRgba);  
	                frame.copyTo(mGrey);  
	                Imgproc.cvtColor( mRgba, mGrey, Imgproc.COLOR_BGR2GRAY);  
	                Imgproc.equalizeHist( mGrey, mGrey );                  
	                
	                // detect faces
	                faceDetector.detectMultiScale(
	                		mGrey, 
	                		faces,
	                		scaleFactor,
	                		minNeighbours,
	                		flags,
	                		minSize,
	                		maxSize);
	
	                // we only want the first face found
	                numFacesDetected = faces.toArray().length;
	                if(numFacesDetected > 0) {
	                	System.out.println("Face detected " + numFacesDetected);
			            // each rectangle in faces is a face
			            Rect rect = faces.toArray()[0];
			            faceCenter = new Point(rect.x + rect.width/2, rect.y + rect.height/2 );  
	                } 
	             }
        	} catch(Exception e) {
        		System.err.println("FaceDetector Exception: " + e.getMessage());
        		Sound.buzz();
        	}
        }
	}
}
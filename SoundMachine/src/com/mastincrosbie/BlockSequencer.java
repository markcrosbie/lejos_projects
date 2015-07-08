package com.mastincrosbie;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import javax.bluetooth.LocalDevice;

import lejos.nxt.*;
import lejos.util.*;
import lejos.robotics.Color;
import lejos.nxt.comm.*;

/**
 * BlockSequencer
 * Scan blocks on a 32x32 plate that encode a MIDI instrument sequence.
 * Blocks are arranged on 4 parallel tracks. Block placement encodes the placement of an instrument into
 * a MIDI clip.
 * 
 * @author mcrosbie
 *
 */
public class BlockSequencer {

	private static final int TURNTABLE_SPEED = 180;	// degrees per second	
	private static int scanningSpeed = TURNTABLE_SPEED;
	
	private static final int numNotes = 8;			// number of beat slots per 32x32 plate
	private static final int numTracks = 4;			// how many tracks on the plate

	private static boolean exit = false;		// true if we have to exit the program loop
	private static boolean debug = false;		// true if console output is enabled
	
	private static boolean sendEnabled = true;	// true if you want to send data over USB. False for debug output only
	
	private static boolean useHSV = false;		// if true then HSV colour mapping used, else default RGB model
	
	private static ColorSensor cs[];
  
	private static String colorNames[] = {"Red", "Green", "Blue", "Yellow",
           "Magenta", "Orange", "White", "Black", "Pink",
           "Grey", "Light Grey", "Dark Grey", "Cyan", "None"};

	public static int numColours;
	
	public static final int RED = 0;
	public static final int GREEN = 1;
	public static final int BLUE = 2;
	public static final int YELLOW = 3;
	public static final int MAGENTA = 4;
	public static final int ORANGE = 5;
	public static final int WHITE = 6;
    public static final int BLACK = 7;
    public static final int PINK = 8;
    public static final int GRAY = 9;
    public static final int LIGHT_GRAY = 10;
    public static final int DARK_GRAY = 11;
    public static final int CYAN = 12;
    public static final int NONE = -1;

	private static 		ColorSensor.Color vals;    

	private static int programState;
	private static final int WAITFOREDGE = 1;		// waiting for the edge of the plate to be detected
	private static final int SCANNING = 2;			// scanning blocks
	private static final int IDLE = 3;				// idle, awaiting a command
	private static final int SENDING = 4;			// sending MIDI data to the host to play
	
	private static final int whiteScanInterval = 10000;	// ms to wait to find white
	private static final int scanningInterval = 30000;		// ms to allow a scan to complete
	private static final int USBTIMEOUT = 5000;		// how long to wait for a connection on USB to send notes
		
	private static int colourMap[][];				// stores the colour recorded at each slot
		
	private static String MYNAME = "SOUND3";

	/**
	 * waitForEdge
	 * Wait until the light sensor detects a non-black colour so that we know that the
	 * sensor is on the start edge of the board. If the timeout is hit, or the ENTER
	 * button is pressed then return false.
	 * 
	 * @param timeout How long to wait for white before giving up in milliseconds
	 * @return True if white detected within timeout, otherwise false
	 */
	public static boolean waitForEdge(int timeout) {
		int thisColour;
		
		RConsole.println("waitForWhite, waiting for " + timeout + " ms");
		
		long endTicks = System.currentTimeMillis() + timeout;
		
    	thisColour = sampleSensor(0);
    	boolean found = false;
    	while( (System.currentTimeMillis() < endTicks) && Button.ESCAPE.isUp()) {
    		if(sampleSensor(0) != Color.BLACK) {
    			found = true;
    			break;
    		}
    	}
    	
    	if(found) 
    		RConsole.println("waitForEdge: Detected colour = " + getColourName(thisColour));
    	else 
    		RConsole.println("waitForEdge: timeout waiting for white");
    	
    	return found;
	}
	
	/**
	 * startTurntable
	 * Start the turntable rotating
	 * @param speed - the speed for the rotation
	 */
	public static void startTurntable(int speed) {
		Motor.A.setSpeed(speed);
	    Motor.A.backward();	
	}

	/**
	 * reverseTurntable
	 * Start the turntable rotating in the reverse direction for a fixed time
	 * @param speed - the speed for the rotation
	 * @param rotationCount - how many rotations to turn for in degrees
	 */
	public static void reverseTurntable(int speed, int rotationCount) {
		Motor.A.setSpeed(speed);
	    Motor.A.rotate(rotationCount);
		    
	    stopTurntable();
	}

	public static void stopTurntable() {
		Motor.A.stop();
	}
	
    public static void displayColor(String name, int raw, int calibrated, int line)
    {
        LCD.drawString(name, 0, line);
        LCD.drawInt(raw, 5, 6, line);
        LCD.drawInt(calibrated, 5, 11, line);
    }

    /**
     * Convert a colour value from the NXT colour sensor to a printable string
     * @param c Colour value
     * @return String name of the colour
     */
    public static String getColourName(int c) {
    	
    	if( (c < 0) || (c > colorNames.length) )
    		return "???";
    
    	return colorNames[c];
    }
    
    public static int sampleSensor(int sensor) {
    	
		int[] thisColour = new int[3];
		int finalColour;
		
		// RGB model
		if(!useHSV) {
			vals = cs[sensor].getColor();
			finalColour = vals.getColor();
/**
			thisColour[0] = vals.getColor();
			
			// Test
			//RConsole.println("Sensor " + sensor + " sees (r,g,b) ("+vals.getRed() + ","+vals.getGreen()+"," + vals.getBlue()+")");
			
			Delay.msDelay(10);
			vals = cs[sensor].getColor();
			thisColour[1] = vals.getColor();
			Delay.msDelay(10);
			vals = cs[sensor].getColor();
			thisColour[2] = vals.getColor();
	
			// see which colour occurred most often
			if(thisColour[0] == thisColour[1]) {
				finalColour = thisColour[0];
			} else if(thisColour[0] == thisColour[2]) {
				finalColour = thisColour[0];
			} else if(thisColour[1] == thisColour[2]) {
				finalColour = thisColour[1];
			} else {
				finalColour = thisColour[0];
			}
**/
		} else {
			// HSV model
 			vals = cs[sensor].getColor();
 	 		HSV hsv = new HSV(vals.getRed(), vals.getGreen(), vals.getBlue());
 	 		double hue = hsv.hue(); 	 		
 	 		finalColour = hsv.colourID();
 	 		
 	 		RConsole.print("Sensor " + sensor + " Hue = " + hue);
 	 		RConsole.println("  colorID = " + getColourName(finalColour));
		}
		
		return finalColour;
    }

    public static void initialiseColourSensors() {
        // Use the colour sensor white WHITE light
 		cs = new ColorSensor[4];
 		
 		cs[0] = new ColorSensor(SensorPort.S1);
 		cs[0].setFloodlight(true);
  		cs[0].setFloodlight(Color.WHITE);

 		cs[1] = new ColorSensor(SensorPort.S2);
 		cs[1].setFloodlight(true);
  		cs[1].setFloodlight(Color.WHITE);

  		cs[2] = new ColorSensor(SensorPort.S3);
 		cs[2].setFloodlight(true);
  		cs[2].setFloodlight(Color.WHITE);
 
  		cs[3] = new ColorSensor(SensorPort.S4);
 		cs[3].setFloodlight(true);
  		cs[3].setFloodlight(Color.WHITE); 		
    }
    
    
    /**
     * Scan the colours on the plate. Read the colours from each sensor until the end of the plate
     * is reached. Store the colours in the colour[] array. If no end  marker
     * is found within timeout milliseconds then abort
     * 
     */
    public static boolean scanColours(int timeout) {
 		boolean done = false;
		
		RConsole.println("scanColours, waiting for " + timeout + " ms");
		
		long endTicks = System.currentTimeMillis() + timeout;
		
    	startTurntable(scanningSpeed);
    	
    	int numSamples = 400;
    	int colours[][] = new int[numTracks][numSamples];
    	int iters = 0;
    	int blackCounter = 0;
    	HSV hsv;
    	
    	clearNotes();
    	
 		while( (System.currentTimeMillis() < endTicks) && Button.ESCAPE.isUp() && !done) { 
 			
 			if(useHSV) {
 				vals = cs[0].getColor();
 	 	 		hsv = new HSV(vals.getRed(), vals.getGreen(), vals.getBlue());
 	 	 		colours[0][iters] = hsv.colourID();

 				vals = cs[1].getColor();
 	 	 		hsv = new HSV(vals.getRed(), vals.getGreen(), vals.getBlue());
 	 	 		colours[1][iters] = hsv.colourID();
 
 				vals = cs[2].getColor();
 	 	 		hsv = new HSV(vals.getRed(), vals.getGreen(), vals.getBlue());
 	 	 		colours[2][iters] = hsv.colourID();
 
 				vals = cs[3].getColor();
 	 	 		hsv = new HSV(vals.getRed(), vals.getGreen(), vals.getBlue());
 	 	 		colours[3][iters] = hsv.colourID();
 
 			} else {
	 			colours[0][iters] = cs[0].getColor().getColor();
				colours[1][iters] = cs[1].getColor().getColor();
				colours[2][iters] = cs[2].getColor().getColor();
				colours[3][iters] = cs[3].getColor().getColor();
 			}
 			
			if(colours[0][iters] == Color.BLACK) {
				blackCounter++;
				if(blackCounter >= 15)
					done = true;
			} else {
				blackCounter = 0;
			}
			
			iters++;
			if(iters >= numSamples)
				iters = numSamples - 1;
       	}
 		
 		stopTurntable();
 		
 		// Verbose debug code
 		if(!sendEnabled) {
	 		for(int i=0; i < iters; i++) {
				RConsole.println(i + ":  "+ getColourName(colours[0][i])+
							"\t" + getColourName(colours[1][i]) + 
							"\t" + getColourName(colours[2][i]) + 
							"\t" + getColourName(colours[3][i]));
	 		}
	 		
	 		RConsole.println("Calculating subsequences");
 		}
 		
 		for(int track=0; track < numTracks; track++) {
 			RConsole.println("Processing Track " + track);
 			
	 		// collapse the colours sampled into meaningful groups
			int currentColour = colours[track][0];
			int sequenceLength = 0;
			int note = 0;
	 		for(int i=1; i < iters; i++) {
	 			if(colours[track][i] == currentColour)
	 				sequenceLength++;
	 			else {
	 				// we only care about sequences of length > 3
	 				if(sequenceLength > 3) {
	 					if(!sendEnabled)
	 						RConsole.println("("+sequenceLength+","+getColourName(currentColour)+")  ");
	 					if(currentColour == Color.BLACK)
	 						currentColour = Color.BLUE;		// blue is often mis-read as Black
	 					if(currentColour != Color.WHITE)	// store notes not white in the array
	 						colourMap[track][note] = currentColour;
	 					if(currentColour == Color.WHITE)
	 						note++;
	 					if(note >= numNotes)
	 						note = numNotes-1;
	 				}
	 				currentColour = colours[track][i];
	 				sequenceLength = 0;
	 			}
	 		}
	 		RConsole.println("----------------");
 		} 		
      	return done;

    }
    
    /**
     * Clear the colourMap to delete notes from a previous scan
     * All of the notes are set to BLUE as this indicates no note in the slot
     */
    public static void clearNotes() {
    	for(int i=0; i < numTracks; i++) {
    		for(int j=0; j < numNotes; j++) {
    			colourMap[i][j] = Color.BLUE;
    		}
    	}
    }
    
    /**
     * Send the colour data to the host
     */
    public static void sendResults() {
    	// for now we just pretty print the results to the console
    	int i, j;
    	
    	if(!sendEnabled) {
    	
	    	RConsole.println("======= SCAN RESULTS ========");
	    	for(i=0; i < numTracks; i++) {
	    		for(j=0; j < numNotes; j++) {
	    			if(colourMap[i][j] != -1)
	    				RConsole.print(getColourName(colourMap[i][j]) + "  ");
	    			else
	    				RConsole.print("--  ");
	    		}
	    		RConsole.println("");
	    	}
	    	RConsole.println("=============================");
	    	
	    	// alert to let me know that send is not enabled
	    	Sound.buzz();
	    	Sound.buzz();
    	} else {
   
	    	// close the console in case there is a collision with the USB comms
	    	RConsole.println("Closing console to send data over USB");
	    	RConsole.close();
	
	    	LCD.clear();
	    	
			USBConnection conn = USB.waitForConnection(USBTIMEOUT, 0);
			if(conn == null) {
				LCD.drawString("No connection", 0, 1);
				Sound.buzz();
				return;
			}
			
			LCD.drawString("Connected  ", 0, 1);
			
			Sound.beep();
			
			Delay.msDelay(500);
			
			DataOutputStream dos = conn.openDataOutputStream();
			DataInputStream dis = conn.openDataInputStream();
			
			try {
			   	for(i=0; i < numTracks; i++) {
		    		for(j=0; j < numNotes; j++) {
		    				dos.writeInt(colourMap[i][j]);
		    				dos.flush();
		    		}
		    	}
			   	
			   	LCD.drawString("Done", 0, 2);
			   	dos.close();
			   	dis.close();
			   	conn.close();
			   	Sound.beepSequenceUp();
			} catch(Exception e) {
				LCD.clear();
				Sound.buzz();
				LCD.drawString("EXCEPTION", 0, 0);
				LCD.drawString(e.getMessage(), 0, 1);
			}
	    }
    }
    
    /**
     * Print the colours
     */
    public static void showColours(int line) {

    	for(int i=0; i < 4; i++) {
	    	int thisColour = sampleSensor(i);
			LCD.clear(line);
			LCD.drawString(thisColour + "  " + getColourName(thisColour), 0, line++);
    	}
    }
    
    /**
     * Draw the notes array contents on the screen, with a hollow block if no note is there and a
     * solid coloured block if there is a note there. No colour needed
     */
    public static void drawNotes() {

    	for(int i=0; i < numTracks; i++) {
    		for(int j=0; j < numNotes; j++) {
    			if(colourMap[i][j] != Color.BLUE) {
    				// draw a note square
    			} else {
    				// draw an empty square
    			}
    		}
    	}
    }
    
    /**
     * Print a hello message
     */
    public static void sayHello() {
    	try {
    		LCD.drawString(LocalDevice.getLocalDevice().getFriendlyName(), 0, 0);
    	} catch(Exception e) {
    		LCD.drawString("NO NAME SET!", 0, 0);
    	}
    	
    	if(useHSV)
    		LCD.drawString("HSV model", 0, 1);
    	else
    		LCD.drawString("RGB model", 0, 1);
    	if(sendEnabled)
    		LCD.drawString("Send enabled", 0, 2);
    	else
    		LCD.drawString("Send disabled", 0, 2);
    	LCD.drawString("Num notes= " + numNotes, 0, 3);
    	LCD.drawString("Num tracks= " + numTracks, 0, 4);
       	if(debug)
    		LCD.drawString("Debug on", 0, 5);
    	else
    		LCD.drawString("Debug off", 0, 5);
    	
       	LCD.drawString("Motor Port A", 0, 6);
       	
       	Delay.msDelay(3000);
    }
    /**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(debug)
			RConsole.openUSB(3000);
		
		RConsole.println("BlockSequencer Starting");
		
		initialiseColourSensors();
		
    	//USB.setName(MYNAME);	    	

		sayHello();
		
		programState = IDLE;
		
 		LCD.clear();
 		LCD.drawString("Sequencer", 0, 0);
 		 				 		
 		exit = false; 		
 		numColours = colorNames.length;
 		
 		// Allocate the array to hold what we scan in from the plate
 		colourMap = new int[numTracks][numNotes];
 		int i, j;
 		for(i = 0; i < numTracks; i++)
 			for(j=0; j < numNotes; j++)
 				colourMap[i][j] = -1;
 		
 		RConsole.println("Starting run loop");
 		 		
 		try {
	 		// Keep running unless the ESCAPE button is pressed or the EXIT command is received
	 		while (!exit) {  

	 			LCD.clear(0);
	 		   	try {
	 	    		LCD.drawString(LocalDevice.getLocalDevice().getFriendlyName(), 0, 0);
	 	    	} catch(Exception e) {
	 	    		LCD.drawString("SoundMachine", 0, 0);
	 	    	}
	 	 	 			
	 			LCD.clear(1);
	 			LCD.drawInt(Battery.getVoltageMilliVolt(), 0, 7);
	 			
	 			// display what the sensors are currently reading
	 			showColours(2);
	 			
	 			drawNotes();
	 			
	 			// state machine time!
	 			switch(programState) {
	 			
	 			// IDLE state. ENTER starts the scanning
	 			// ESCAPE exits the program
	 			case IDLE:
	 				LCD.drawString("IDLE", 0, 1);
	 				//RConsole.println("State: IDLE");
	 				if(Button.ENTER.isDown()) {
	 					while(Button.ENTER.isDown());	// debounce
	 					startTurntable(scanningSpeed);
	 					Sound.beep();
	 					programState = WAITFOREDGE;
	 				}
	 				
	 				if(Button.ESCAPE.isDown()) {
	 					exit = true;
	 				}
	 				
	 				if(Button.LEFT.isDown()) {
	 					Sound.beep();
	 					sendResults();
	 				} 
	 				
	 				if(Button.RIGHT.isDown()) {
	 					RConsole.openUSB(3000);
	 					Sound.beep();
	 					RConsole.println("Console reopened");
	 				}
	 				
	 				// print what the sensors see
	 				showColours(2);
	 				break;
	 				
	 			case WAITFOREDGE:
	 				LCD.drawString("WAITFOREDGE", 0, 1);
	 				//RConsole.println("State: WAITFORWHITE");
		 		    if(!waitForEdge(whiteScanInterval)) {
		 		    	programState = IDLE;
		 		    	stopTurntable();
		 		    	Sound.buzz();
		 		    } else {
		 		    	Sound.beep();
		 		    	stopTurntable();
		 		    	reverseTurntable(scanningSpeed, 45);
		 		    	programState = SCANNING;
		 		    }
	 				break;
	 				
	 			case SCANNING:
	 				LCD.drawString("SCANNING", 0, 1);
	 				//RConsole.println("State: SCANNING");

	 				if(scanColours(scanningInterval)) {
	 					Sound.beep();
	 					programState = SENDING;
		 				stopTurntable();
	 				} else {
	 					stopTurntable();
	 					Sound.buzz();
	 					programState = IDLE;
	 				}
	 				break;
	 				
	 			case SENDING:
	 				LCD.drawString("SENDING", 0, 1);
	 				//RConsole.println("State: SENDING");

	 				sendResults();
	 				programState = IDLE;
	 				break;
	 			}
	 				
	 			//Delay.msDelay(50);
	        }
	
	 		// All done, time to clean up
	 		stopTurntable();
			RConsole.close();
	 		System.exit(0);
	 		
 		} catch(Exception e) {
 			RConsole.println("EXCEPTION: " + e);
 			LCD.clear();
 			LCD.drawString(e.getMessage(), 0, 0);
 			Sound.buzz();
 		}
	}
	
}

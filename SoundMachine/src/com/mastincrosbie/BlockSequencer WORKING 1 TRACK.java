package com.mastincrosbie;

import java.util.ArrayList;

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
	
	private static final int lengthOfPlate = 590;	// magic number - total tacho count for an entire plate
	private static final int numNotes = 8;			// number of beat slots per 32x32 plate
	private static final int numTracks = 4;			// how many tracks on the plate
	private static final int tachoPerSlot = lengthOfPlate / numNotes;

	private static boolean exit = false;		// true if we have to exit the program loop

	private static ColorSensor cs[];
  
	private static String colorNames[] = {"Red", "Green", "Blue", "Yellow",
           "Magenta", "Orange", "White", "Black", "Pink",
           "Grey", "Light Grey", "Dark Grey", "Cyan", "None"};

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
		
	private static int colourMap[][];				// stores the colour recorded at each slot
		

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

		vals = cs[sensor].getColor();
		thisColour[0] = vals.getColor();
		Delay.msDelay(10);
		vals = cs[sensor].getColor();
		thisColour[1] = vals.getColor();
		Delay.msDelay(10);
		vals = cs[sensor].getColor();
		thisColour[2] = vals.getColor();

		int finalColour;
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
     * Scan the colours for a given slot and take the most frequent colour read as the colour reading for the 
     * slot.
     * @param slot The slot we are scanning for
     */
    public static void scanSlot(int slot) {
    	
		//sanity check
		if(slot < 0)
			slot = 0;
		if(slot > numNotes-1)
			slot = numNotes-1;

		RConsole.println("scanSlot: starting to scan slot " + slot);
		
		int thisColour = sampleSensor(0);
		int r, g, b;
		int sampleCount = 0;
		r = g = b = 0;
		ColorSensor.Color v;
		try {
			// Algorithm is as follows:
			// If a non-white colour is seen then gather RGB values and add to the running average
			// If a white colour is seen then the slot is finished, so compute the average of RGB and from that
			// convert to a colour map
			while(thisColour != Color.WHITE && Button.ESCAPE.isUp()) {
				v = cs[0].getColor();
				r = r + v.getRed();
				g = g + v.getGreen();
				b = b + v.getBlue();
				sampleCount++;
				thisColour = cs[0].getColor().getColor();
				Delay.msDelay(25);
			}
			
			r = r / sampleCount;
			g = g / sampleCount;
			b = b / sampleCount;
			
			RConsole.println("scanSlot: sampleCount = " + sampleCount);
			RConsole.println("scanSlot: r=" + r + " g=" + g + " b="+b);
		} catch(Exception e) {
			RConsole.println("EXCEPTION scanning slot " + slot);
			RConsole.println("Message: " + e.getMessage());
			RConsole.println("sampleCount = " + sampleCount);
		}
/**
 * This code scans assuming we know the fixed size of each slot, which isn't realistic
 */
		/**
		Motor.A.rotate(-tachoPerSlot);
		
		for(int i=0; i < numTracks; i++) {
			colourMap[i][slot] = sampleSensor(i);
		}
		**/

		/**
		int colourCount[][];
    	
    	// allocate a counting table to count how many of each colour type are read per "slot" scan
    	// this then allows us to take the most frequently occurring colour seen for a given slots as
    	// the colour of that slot. It eliminates outliers and spurious readings.
    	int numberOfColours = colorNames.length;
    	colourCount = new int[numTracks][numberOfColours];
    	
    	boolean whiteFound = false;
    	startTurntable(scanningSpeed);
    	
  
		while(!whiteFound && Button.ESCAPE.isUp()) {	
			for(int i=0; i < numTracks; i++) {
				int thisColour = sampleSensor(i);  
				if(thisColour == Color.WHITE) {
					whiteFound = true;
				} else {
					colourCount[i][thisColour]++;
	    			RConsole.println("colourCount["+i+"]["+thisColour+"]="+colourCount[i][thisColour]);
				}
			}
		}
		
		RConsole.println("scanSlot: sampling finished, finding most popular colours");
		
		// now find the most frequent colour in this slot on each track, and assign that to the colour map
		for(int i=0; i < numTracks; i++) {
			int max=0, maxColour=0;
			for(int j=0; j < numberOfColours; j++) {
				if(colourCount[i][j] > max)
					maxColour = j; // track the most frequent colour for this track
			}
			colourMap[i][slot] = maxColour;
			RConsole.println("Assigned colour " + maxColour + " to " + i + "," + slot);
		}	
**/
    	
    }
    
    /**
     * Scan the colours on the plate. Read the colours from each sensor until the end of the plate
     * is reached. Store the colours in the colour[] array. If no end  marker
     * is found within timeout milliseconds then abort
     * 
     */
    public static boolean scanColours(int timeout) {
 		int thisColour, prevColour;	// what we've seen now and the previous colour we saw
 		boolean done = false;
		
		RConsole.println("scanColours, waiting for " + timeout + " ms");
		
		long endTicks = System.currentTimeMillis() + timeout;
		
    	startTurntable(scanningSpeed);
    	
 		prevColour = -255;
 		int blackCount=0;
 		int whiteCount = 0;
 		int redCount = 0;
 		int noteCount = 0;
 		int blueCount = 0;
 		int yellowCount = 0;
 		int greenCount = 0;
 		
 		while( (System.currentTimeMillis() < endTicks) && Button.ESCAPE.isUp() && !done) {    		

			thisColour = sampleSensor(0);
			
			switch(thisColour) {
			
				case Color.BLACK:
					if(prevColour == Color.BLACK)
						blackCount++;
					else
						blackCount=1;
					if(blackCount > 10) {
						// we've seen ten BLACK samples, so exit
						//RConsole.println("scanColours: ten BLACK samples, done");
						done=true;
					}
					prevColour = Color.BLACK;
					break;
					
				case Color.WHITE:
					if(prevColour == Color.WHITE)
						whiteCount++;
					else
						whiteCount=1;
					if(whiteCount > 2) {
						// we've seen enough WHITE samples, so exit
						noteCount++;
						//RConsole.println("scanColours: white detected, noteCount=" + noteCount);
						whiteCount=0;
					}
					if(noteCount >= numNotes) {
						noteCount = numNotes-1;
					}
					prevColour = Color.WHITE;
					break;
					
				case Color.RED:
					if(prevColour == Color.RED)
						redCount++;
					else
						redCount=1;
					if(redCount > 2) {
						// we've seen enough RED samples, so record a note
						//RConsole.println("scanColours: RED detected at noteCount=" + noteCount);
						colourMap[0][noteCount] = Color.RED;
						redCount=0;
					}
					prevColour = Color.RED;
					break;

				case Color.BLUE:
					if(prevColour == Color.BLUE)
						blueCount++;
					else
						blueCount=1;
					if(blueCount > 2) {
						// we've seen enough BLUE samples
						//RConsole.println("scanColours: BLUE detected at noteCount=" + noteCount);
						colourMap[0][noteCount] = Color.BLUE;
						blueCount=0;
					}
					prevColour = Color.BLUE;
					break;

				case Color.YELLOW:
					if(prevColour == Color.YELLOW)
						yellowCount++;
					else
						yellowCount=1;
					if(yellowCount > 2) {
						// we've seen enough YELLOW samples
						//RConsole.println("scanColours: YELLOW detected at noteCount=" + noteCount);
						colourMap[0][noteCount] = Color.YELLOW;
						yellowCount=0;
					}
					prevColour = Color.YELLOW;
					break;

				case Color.GREEN:
					if(prevColour == Color.GREEN)
						greenCount++;
					else
						greenCount=1;
					if(greenCount > 2) {
						// we've seen enough GREEN samples
						//RConsole.println("scanColours: GREEN detected at noteCount=" + noteCount);
						colourMap[0][noteCount] = Color.GREEN;
						greenCount=0;
					}
					prevColour = Color.GREEN;
					break;

				default:
					prevColour = thisColour;
					break;
			}
					
			Delay.msDelay(5);
       	}

      	RConsole.println("scanColours: scan completed. Found " + noteCount + " notes in time=" + System.currentTimeMillis());
      	
      	return done;

    }
    
    /**
     * Send the colour data to the host
     */
    public static void sendResults() {
    	// for now we just pretty print the results to the console
    	int i, j;
    	
    	RConsole.println("======= SCAN RESULTS ========");
    	for(i=0; i < numTracks; i++) {
    		for(j=0; j <  numNotes; j++) {
    			if(colourMap[i][i] != -1)
    				RConsole.print(getColourName(colourMap[i][j]) + "  ");
    			else
    				RConsole.print("--  ");
    		}
    		RConsole.println("");
    	}
    	RConsole.println("=============================");
 
    }
    
    /**
     * Print the colours
     */
    public static void showColours(int line) {

    	for(int i=0; i < 4; i++) {
	    	int thisColour = sampleSensor(i);
			LCD.clear(line);
			LCD.drawString("Col " + i +"=" + getColourName(thisColour), 0, line++);
    	}
    }
    
    /**
	 * @param args
	 */
	public static void main(String[] args) {
		RConsole.openUSB(3000);
		
		RConsole.println("BlockSequencer Starting");
		
		initialiseColourSensors();
		
		programState = IDLE;
		
 		LCD.clear();
 		LCD.drawString("Sequencer", 0, 0);
 		 				 		
 		exit = false; 		
 		int iters = 0;
 		 		
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

	 			LCD.clear(1);
	 			LCD.drawInt(iters++, 0, 7);
	 			
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
	 					Motor.A.rotate(-10);
	 				}

	 				if(Button.RIGHT.isDown()) {
	 					Motor.A.rotate(10);
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
	 				
	 			Delay.msDelay(50);
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

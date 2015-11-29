/**
 * SoundMachine
 * Controller class for the NXT to run on a SoundMachine unit
 * 
 * Mark Crosbie mark@mastincrosbie.com
 * April 2012
 * 
 */

package com.mastincrosbie;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

import lejos.nxt.*;
import lejos.util.*;
import lejos.robotics.Color;
import lejos.nxt.addon.*;
import lejos.nxt.comm.RConsole;
import lejos.nxt.comm.USB;
import lejos.nxt.comm.USBConnection;

public class SoundMachine extends Thread {

	private static byte myAddress = 1;
	
	private static ColorSensor cs;
	private static NXTBee nb;

	private static InputStream is;
	private static OutputStream os;
	
	// ASSUME 120 BPM
	//
	// My disk has 40 beats on it, at 120 bpm = 3 revolutions of the disk per minute.
	// Motor is geared at 8:56 = 1:7 gearing ratio on the turntable. 
	// 7 revolutions of the motor = 1 revolution of the turntable.
	// Thus for 3 revolutions per minute the motor should turn 21 times per minute
	// 21 times per 60 seconds
	// 21*360 degrees = 7560 degrees per 60 seconds = 126 degrees per second speed on the motor.
	private static int BPM = 120;	// beats per minute

	private static final int TURNTABLE_SPEED = 126;	// degrees per second

	private static final int sampleInterval = 5;
	
	private static int turntableSpeed = TURNTABLE_SPEED;
	
	private static boolean debug = false;
	
	private static boolean running = false;		// true if the disk is rotating
	private static boolean exit = false;		// true if we have to exit the program loop
	
	///////////////////////////////////////////////////
	// Set to true if you want to run the simulator
	private static boolean simulator = false;
	
    private static String colorNames[] = {"Red", "Green", "Blue", "Yellow",
                            "Magenta", "Orange", "White", "Black", "Pink",
                            "Grey", "Light Grey", "Dark Grey", "Cyan", "None"};

    private static 		ColorSensor.Color vals;    
        
    // Command codes to send to the SoundMachines
    private static final byte CMD_NOP = 0;		// NOP
    private static final byte CMD_STOP = 1;		// STOP operation
    private static final byte CMD_TIMECODE = 2;	// Send the global timecode
    private static final byte CMD_START = 3;		// Start operation
    private static final byte CMD_SPEEDUP = 4;	// Rotate the disk faster
    private static final byte CMD_SLOWDOWN = 5;	// Rotate the disk slower
    private static final byte CMD_EXIT = 6;		// Stop and exit

	/**
	 * printWelcomeScreen
	 * Print a nice welcome screen with instructions on how to use
	 */
	public static void printWelcomeScreen() {
        LCD.drawString("Sound Machine", 0, 0);
        if(simulator) {
        	LCD.drawString("SIMULATOR", 0, 2);
        	Delay.msDelay(2000);
        }
	}
	
	/**
	 * setMyAddress
	 * Choose the address for this NXT to use when talking to the host
	 * @return address chosen by the user (1..3)
	 */
	public static void setMyAddress() {
        String addr[] = {"1", "2", "3"};
        TextMenu modeMenu = new TextMenu(addr, 1, "My Address");

        myAddress = (byte)(modeMenu.select() & 0xFF);

        if(debug) RConsole.println("Set my address to " + myAddress);
        
	}
	
	/**
	 * waitForWhite
	 * Wait until the light sensor detects the colour white so that we know that the
	 * sensor is on a white region to start from.
	 */
	public static void waitForWhite() {
		int thisColour;
		
		if(debug) RConsole.println("waitForWhite");
		
	  	ColorSensor.Color vals = cs.getColor();
    	thisColour = vals.getColor();

    	while(thisColour != Color.WHITE) {
    	  	vals = cs.getColor();
        	thisColour = vals.getColor();    		
    	}
    	
    	if(debug) RConsole.println("thisColour = " + getColourName(thisColour));
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
     * Send a data block to the Mac
     */
    public static void sendSignal(int colour) {
    	    	
    	byte[] b = new byte[6];
    	
    	try {

	    	b[0] = CMD_TIMECODE;
	    	b[1] = (byte)(colour & 0xFF);
/**
	    	b[1] = (byte)(timecode >>> 24);
	    	b[2] = (byte)(timecode >>> 16);
	    	b[3] = (byte)(timecode >>> 8);
	    	b[4] = (byte)(timecode);
	    	b[5] = (byte)(colour & 0xFF);
**/ 	
	    	os.write(b, 0, 2);	    	
    	} catch(Exception e) {
    		RConsole.println("Error sending message to controller: " + e);
    	}
     }
      
    public static void readColourSensor() {  	
    	while(true) {
    		if(running) {
    			vals = cs.getColor();		
    		}
    	}
    }
    
    /**
     * Convert a colour value from the NXT colour sensor to a printable string
     * @param c Colour value
     * @return String name of the colour
     */
    public static String getColourName(int c) {
    	
    	if( (c < 0) || (c > colorNames.length) )
    		return "Unknown";
    
    	return colorNames[c];
    }
    
    /**
     * Check to see if any keys are pressed on the NXT keypad
     */
    public static void checkKeys() {
	   	// Slow down the turntable
    	if(Button.LEFT.isDown()) {
    		while(Button.LEFT.isDown());
    		turntableSpeed -= 10;
    		if(turntableSpeed < 0)
    			turntableSpeed = 0;
    	}

 	   // Speed up the turntable
        if(Button.RIGHT.isDown()) {
     	   while(Button.RIGHT.isDown());
         	turntableSpeed += 10;
         	if(turntableSpeed > 720)
         		turntableSpeed = 720;
         }
         
        // Reset to the default speed
        if(Button.ENTER.isDown()) {
     	   while(Button.ENTER.isDown());
     	   turntableSpeed = TURNTABLE_SPEED;
        }
 		
        // Reflect any speed changes
        Motor.A.setSpeed(turntableSpeed);
    }
    
    /**
     * Execute a command received from the controller
     */
    public static void executeCommand(int cmd) {
		    	
    	// what type of message is this?
    	try {
			switch(cmd) {
			case CMD_TIMECODE:
				RConsole.println("CMD_TIMECODE");
				break;
			
			case CMD_EXIT:
				RConsole.println("CMD_EXIT");
				exit = true;
				break;
				
			case CMD_SPEEDUP:
				RConsole.println("CMD_SPEEDUP");
	        	turntableSpeed += 10;
	         	if(turntableSpeed > 720)
	         		turntableSpeed = 720;
	         	break;
	         	
			case CMD_SLOWDOWN:
				RConsole.println("CMD_SLOWDOWN");
				turntableSpeed -= 10;
				if(turntableSpeed < 0)
					turntableSpeed = 0;
				break;
				
			case CMD_STOP:
				RConsole.println("CMD_STOP");
				stopTurntable();
				running = false;
				break;
				
			case CMD_START:
				RConsole.println("CMD_START");
				turntableSpeed = TURNTABLE_SPEED;
				startTurntable(turntableSpeed);
				running = true;
				break;
				
			default: break;
			}
    	} catch(Exception e) {
    		RConsole.println("***** Exception: " + e);
    	}
    	
        // Reflect any speed changes
        Motor.A.setSpeed(turntableSpeed);

    }
    
    /**
     * Run the thread to read commands from the Processing controller
     */
    public void run() {
    	
    	int cmd;
    	
    	RConsole.println("run method started...");
    	while(true) {
	 	   	// Read commands from the controller
	  	   	try {
	  	   		cmd = is.read();
	      	   	executeCommand(cmd);
	      	} catch(Exception e) {
				RConsole.println("***** COMMAND EXCEPTION " + e);
	      	}
	  	   		  	   	
	  	   	Delay.msDelay(100);
    	}
    }
    
    /**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int[] thisColour = new int[3];
		byte[] buf = new byte[100];
		
		try  {
		if(debug) RConsole.openUSB(3000);
		
		if(debug) RConsole.println("SoundMachine Starting");
		
		printWelcomeScreen();
		
		nb = new NXTBee(115200);		
		Thread t = new Thread(nb);
		t.start();
		
		is = nb.getInputStream();
		os = nb.getOutputStream();

        // Use the colour sensor white WHITE light
		cs = new ColorSensor(SensorPort.S1);
		cs.setFloodlight(true);
 		cs.setFloodlight(Color.WHITE);
             
 		LCD.clear();
 		LCD.drawString("SoundMachine", 0, 0);
 		 				 		
 		exit = false;
 		
 		myAddress = 1;
 		 		
 		if(debug) RConsole.println("Starting run loop");
 		
 		SoundMachine sm = new SoundMachine();
 		sm.start();
 		
 		int iters = 0;
 		Random rand = new Random();
 		
 		// Keep running unless the ESCAPE button is pressed or the EXIT command is received
 		while (Button.ESCAPE.isUp() && !exit) {            
 						
 			// If the disk is currently rotating...
 			if(running) {
 				LCD.drawString("RUNNING", 0, 1);
	 			
 				vals = cs.getColor();
	 			thisColour[0] = vals.getColor();
	 			Delay.msDelay(5);
	 			thisColour[1] = vals.getColor();
	 			Delay.msDelay(5);
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
	 			
	 			LCD.clear(2);
	 			
	      	   	LCD.drawString("Colour: " + getColourName(finalColour) + "  ", 0, 3);       
	      	   	LCD.drawString("Speed " + turntableSpeed + "   ", 0, 4);
/**
	      	   	LCD.drawString("TEST!!", 0, 6);
	      	   	iters++;
	      	   	if((iters % 2) == 1)
	      	   		finalColour=6;
	      	   	else
	      	   		finalColour=0;
**/
	      	   	sendSignal(finalColour);
	      	   		      	   	
	      	   	Delay.msDelay(sampleInterval);     	   		      	   	
 			} else {
 				LCD.drawString("STOPPED", 0, 1);
 			}
 			       	   	
        }

 		// All done, time to clean up
 		stopTurntable();
 		
 		// Stop the NXTBee reader thread
 		nb.stop();
 		
       
 		if(debug) RConsole.close();
 		
 		System.exit(0);
	} catch(Exception e) {
		if(debug) RConsole.println("EXCEPTION: " + e);
		LCD.clear();
		LCD.drawString(e.getMessage(), 0, 0);
		Sound.buzz();
	}
	}
}
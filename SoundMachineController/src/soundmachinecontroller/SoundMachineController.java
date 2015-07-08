package soundmachinecontroller;

import ddf.minim.AudioSample;
import ddf.minim.Minim;
import processing.core.PApplet;
import processing.serial.Serial;

import java.io.*;
import java.nio.*;

import lejos.pc.comm.NXTConnector;
import oscP5.*;

//SoundMachine v1
//
// Mark Crosbie
//
// Generate the sound from a bank of SoundMachine units which broadcast
// when they read a coloured bar.
//
// Uses the NXTBee as an interface
//
//Version 1: 
//Sends a simple timecode to all NXTs
//Generates a bass drum and snare drum sound based on the data from the NXT
//
//Key Commands:
//SPACE: Start/stop all sound machines
//x: Exit all sound machines
//a: Play bass drum
//b: Play snare drum
//+: Speed up SoundMachine units
//-: Slow down SoundMachine units

public class SoundMachineController extends PApplet {
	
	AudioSample bassdrum, snare;
	Minim minim;
	SerialController serial;
	ReaderThread reader;
	PlayerThread player;
	
	InputStream in;
	OutputStream out;
		
	// Address for the Processing unit
	byte ME = 1;
	
	// true if we are running
	boolean running = false;

	String colorNames[] = {"Red", "Green", "Blue", "Yellow",
           "Magenta", "Orange", "White", "Black", "Pink",
           "Grey", "Light Grey", "Dark Grey", "Cyan", "None"};

	// Command codes to send to the SoundMachines
	byte CMD_NOP = 0;		// NOP
	byte CMD_STOP = 1;		// STOP operation
	byte CMD_TIMECODE = 2;	// Send the global timecode
	byte CMD_START = 3;		// Start operation
	byte CMD_SPEEDUP = 4;	// Rotate the disk faster
	byte CMD_SLOWDOWN = 5;	// Rotate the disk slower
	byte CMD_EXIT = 6;		// Exit all SM units

	byte BROADCAST = 0;
	int myAddress = BROADCAST;
	
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

	// ASSUME 120 bpm = 2 beats per second. Sampling speed is 1/bpm, or 60/BPM * 1000 milliseconds apart
	private static final int sampleInterval = (60*1000)/BPM;

	public void setup() {
		// initialize the screen
	  	size(300, 200);
	  	smooth();
									
		println("Initialising the XBee serial controller...");
		// Create the XBee serial controller
		serial = new SerialController(this, 0, 115200);
		serial.listPorts();
		
		// Get streams to talk to the Xbee
		in = serial.getInputStream();
		out = serial.getOutputStream();
		
		// Start the thread to read data from the XBee
		println("Starting the reader thread...");
		reader = new ReaderThread(50, in);		// poll Xbee every 50 ms
		reader.start();
		
		// Start the thread to play notes at the correct interval
		println("Starting the player thread...");
		player = new PlayerThread(this, reader, sampleInterval);
		player.start();
		
	}
	
	public void draw() {
		if(running) {
			background(0,255,0);
		} else {
	   		background(255,0,0);
		}
		
		// Draw the current drum state
		drawDrums();
	}
	
	// Shut down processing elements
	// REQUIRED so that Minim can close up shop
	public void stop() {
		
		println("stop cleaning up...");
		
		reader.quit();
		
		exitAll();		// stop the SoundMachines by transmitting a EXIT command
		
		super.stop();
	}
		
	// Send a STOP command to all of the SoundMachine units
	public void sendStop() {
	    try {
	    	out.write(CMD_STOP);
			println("Sent STOP");
	   	} catch(Exception e) {
	   		println("Error sending STOP " + e);
	   	}
	}

	// Send a START command to the SoundMachines to tell them to start operation
	// Also sends the initial timecode value
	public void sendStart() {

	    try {
	    	out.write(CMD_START);

			println("Sent START");
	   	} catch(Exception e) {
	   		println("Error sending START " + e);
	   	}
	}
	
	// Send a SLOWDOWN command to the SoundMachines to tell them to slow down
	public void slowDown() {
		try {
	    	out.write(CMD_SLOWDOWN);

			println("Sent SLOWDOWN");
	   	} catch(Exception e) {
	   		println("Buffer out of space " + e);
	   	}
	}
	
	// Send a SPEEDUP command to the SoundMachines to tell them to start operation
	public void speedUp() {
	    try {
	    	out.write(CMD_SPEEDUP);

			println("Sent SPEEDUP");
	   	} catch(Exception e) {
	   		println("Buffer out of space " + e);
	   	}
	}
	
	// Send a EXIT command to the SoundMachines to tell them to exit
	public void exitAll() {
	    try {
	    	out.write(CMD_EXIT);

			println("Sent EXIT");
	   	} catch(Exception e) {
	   		println("Buffer out of space " + e);
	   	}
	}	
	
	public void keyPressed() {
		if(key == 'x' || key == 'X') {
			exitAll();
			running = false;
		}
		else if(key == ' ') {
			if(running) {
				sendStop();
				player.pause();
				running = false;
			} else {
				sendStart();
				player.restart();
				running = true;
			}
		}
		else if(key == '-') {
			slowDown();
		}
		else if(key == '+') {
			speedUp();
		}
	}

	// Draw the drums
	public void drawDrums() {
		// draw the drums: if a draw has just been struck
		// then fill it with color as visual feedback for the user

		// drum 1
		if (player.drum1struck == true) {
			fill(0);
			player.drum1struck = false;
		} else {
			fill(255);
		}
		ellipse(50, 55, 100, 100);
		
		// drum 2
		if (player.drum2struck == true) {
			fill(0);
			player.drum2struck = false;
		} else {
			fill(255);
		}
		ellipse(160, 55, 100, 100);
	}	

}

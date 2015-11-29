package soundmachinecontroller;

//
// MIDIController is a sketch that receives messages from a LEGO NXT via a Dexter NXTBee and converts
// them into OSC commands to send to Ableton Live.

import processing.core.PApplet;
import processing.serial.Serial;
import java.io.*;
import java.nio.*;

import netP5.NetAddress;
import lejos.pc.comm.*;
import oscP5.*;

public class MIDIController extends PApplet {

	SerialController serial;
	
	OscP5 oscP5;
	NetAddress myRemoteLocation;
	String MIDI_DEVICE = "localhost";	// defines the location of the remote MIDI device
										// replace with an IP address/hostname of a remote host
	
	boolean USB = true;					// If true then uses USB for communication to NXT, otherwise uses XBee
	boolean XBEE = false;
	boolean BT = false;
	
	DataInputStream in;
	DataOutputStream out;
		
	// Address for the Processing unit
	byte ME = 1;
	
	// true if we are running
	boolean running = false;


	byte BROADCAST = 0;
	int myAddress = BROADCAST;
	

	public void setup() {
		// initialize the screen
	  	size(300, 200);
	  	smooth();
	  	
	  	if(USB) {
	  		println("Opening USB serial port...");
			NXTConnector nxtComm1 = new NXTConnector();
			boolean connected = nxtComm1.connectTo("usb://NXT");
  			if (!connected) {
  				System.err.println("Failed to connect to any NXT");
  				System.exit(1);
  			}

			println("Connected to NXT");
			in = new DataInputStream(nxtComm1.getInputStream());
			out = new DataOutputStream(nxtComm1.getOutputStream());
	  	} 
	  	
	  	if(XBEE) {									
			println("Initialising the XBee serial controller...");
			// Create the XBee serial controller
			serial = new SerialController(this, 0, 115200);
			serial.listPorts();
			
			// Get streams to talk to the Xbee
			in = new DataInputStream(serial.getInputStream());
			out = new DataOutputStream(serial.getOutputStream());
	  	}
	  	
	  	if(BT) {
	  		try {
		  		println("Opening Bluetooth serial port...");
				NXTConnector nxtComm1 = new NXTConnector();
	
	  			// Connect to any NXT over Bluetooth
	  			boolean connected = nxtComm1.connectTo("btspp://");  		
	  			
	  			if (!connected) {
	  				System.err.println("Failed to connect to any NXT");
	  				System.exit(1);
	  			}
				println("Connected to NXT");
				in = new DataInputStream(nxtComm1.getInputStream());
				out = new DataOutputStream(nxtComm1.getOutputStream());

	  		} catch (Exception e) {
	  			println("EXCEPTION: " + e.getMessage());
	  			System.exit(1);
	  		}
	  	}
	  	
	  	println("Creating OSC connection to " + MIDI_DEVICE);
	  	
		// Create the connection to the MIDI enabled device via OSC
		oscP5 = new OscP5(this,9001);
		myRemoteLocation = new NetAddress(MIDI_DEVICE,9000);
	}
	
	void oscEvent(OscMessage theOscMessage) {
		  //println("received an osc message. ");
		  //println("addrpattern: "+theOscMessage.addrPattern());
	}

	public void draw() {

		if(running) {
			background(0,255,0);
		} else {
	   		background(255,0,0);
		}
		
		// Draw the current drum state
		drawDrums();

		try {
			System.out.println("Received " + in.readInt());
		} catch (IOException ioe) {
			System.out.println("IO Exception reading bytes:");
			System.out.println(ioe.getMessage());
		}

	}

	public void reader() {
		OscMessage myMessage;
		
		try {
			while(in.available() > 0) {
				// if data is available then read it from the NXTBee
				int data = in.readInt();					
				println("Received: " + data);
				
				int track = data % 16;
				int clip = data / 16;
			    myMessage = new OscMessage("/live/play/clip");
			    myMessage.add(track);
			    myMessage.add(clip);
			    oscP5.send(myMessage, myRemoteLocation); 
			    println("Sent /live/play/clip " + track +","+ clip);
			}
		} catch(Exception e) {
			println("EXCEPTION in reader: " + e.getMessage());
		}
	}

	// Shut down processing elements
	public void stop() {
		println("stop cleaning up...");				
		super.stop();
	}
			

	public void keyPressed() {
		if(key == 'x') {
			System.exit(1);
		}
	}

	// Draw the drums
	public void drawDrums() {
		// draw the drums: if a draw has just been struck
		// then fill it with color as visual feedback for the user

	}	


}

package com.mastincrosbie;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;

import lejos.nxt.*;
import lejos.util.*;
import lejos.nxt.addon.*;
import lejos.nxt.comm.*;
import lejos.nxt.LCD;
import lejos.nxt.SensorPort;
import lejos.nxt.TouchSensor;

/**
 * A MIDI control surface for Ableton Live or other MIDI devices built in Lego Mindstorms.
 * 
 * Read button presses and transmit the messages to the Processing code via a NXTBee
 * 
 * @author mcrosbie
 *
 */
public class MIDISurface {

	private static byte myAddress = 1;

	private static NXTBee nb;
	private static NXTConnection BTconn;
	
	private static DataInputStream is;
	private static DataOutputStream os;

	private static final boolean debug = false;
	
	private static boolean running = false;		// true if the disk is rotating
	private static boolean exit = false;		// true if we have to exit the program loop

	private static TouchSensor touch[];
	
	private static int bank = 0;				// instrument bank
	private static final int buttonCount = 4;	// number of buttons attached to the NXT
	
	private static final boolean useUSB = true;			// if true then use USB for comms, else use NXTBee
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int iter = 0;
		
		LCD.clear();
				
		try  {
			if(!useUSB) {
				RConsole.openUSB(3000);
				
				RConsole.println("MIDISurface Starting");
				
				nb = new NXTBee(115200);		
				Thread t = new Thread(nb);
				t.start();
				
				RConsole.println("NXTBee thread started");
				
				is = new DataInputStream(nb.getInputStream());
				os = new DataOutputStream(nb.getOutputStream());
			} else {
				LCD.drawString("MIDI Surface", 0, 0);
				LCD.drawString("Waiting for BT", 0, 1);
				BTconn = Bluetooth.waitForConnection();
				Sound.beepSequenceUp();
				
				is = BTconn.openDataInputStream();
				os = BTconn.openDataOutputStream();
			}
						
			LCD.clear();
			LCD.drawString("MIDISurface", 0, 0);

			touch = new TouchSensor[buttonCount];
			
			touch[0] = new TouchSensor(SensorPort.S1);
			touch[1] = new TouchSensor(SensorPort.S2);
			touch[2] = new TouchSensor(SensorPort.S3);
			touch[3] = new TouchSensor(SensorPort.S4);
				 
	          LCD.drawString("MIDI Surface", 0, 0);
	          
			// Keep running unless the ESCAPE button is pressed
	 		while (Button.ESCAPE.isUp() ) {      
	 			
	 			LCD.drawString("Iter " + iter++, 0, 1);
/**	 			
	 			int instrument = -1;
	 			boolean touched = false;
	 			for(int i=0; i < buttonCount; i++) {
	 				if(touch[i].isPressed()) {
	 					instrument = i;
	 					touched = true;
	 					// wait for button to be released
	 					while(touch[i].isPressed());
	 					break;
	 				}
	 			}
	 			
	 			// if a button was touched, send a message to the remote side
	 			if(touched) {
	 				//if(!useUSB) RConsole.println("Sending " + (bank + instrument));
	 				LCD.drawString("Sending " + (instrument) + "  ", 0, 2);
	 				
	 				os.writeInt(instrument);
	 				os.flush();
	 				
	 				LCD.drawString("Done", 0, 3);
	 				Delay.msDelay(100);
	 				LCD.clear(3);
	 			}
**/
	 			if(Button.LEFT.isDown()) {
	 				LCD.drawString("Left", 0, 5);
	 				os.writeInt(123);
	 				os.flush();
	 				Sound.beep();
	 				while(Button.LEFT.isDown());
	 				LCD.clear(5);
	 			}
	 			
	 			if(Button.RIGHT.isDown()) {
	 				LCD.drawString("Right", 0, 5);
	 				os.writeInt(456);
	 				os.flush();
	 				Sound.beep();
	 				while(Button.RIGHT.isDown());
	 				LCD.clear(5);
	 			}

	 			Delay.msDelay(100);
	 		}
	 	 		
	 		LCD.drawString("Closing...", 0, 7);
	 		
	 		os.close();
	 		is.close();
	 		
	 		if(!useUSB) {
	 			nb.stop();
	 			RConsole.close();
	 		} else {
	 			BTconn.close();
	 		}
		} catch(Exception e) {
			RConsole.println("EXCEPTION: " + e);
			LCD.clear();
			LCD.drawString(e.getMessage(), 0, 0);
			Sound.buzz();
		}

	}

}

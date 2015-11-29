package com.mastincrosbie;
/**
 * Timecode implementation for SoundMachine
 * 
 * @author mcrosbie
 *
 */

import com.mastincrosbie.*;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import lejos.nxt.*;
import lejos.util.Delay;
import lejos.util.TextMenu;
import lejos.robotics.Color;
import lejos.nxt.addon.*;
import lejos.nxt.comm.RConsole;

public class Timecode implements Runnable {

	private long timecode;
	private boolean running = false;
	private int period = 100; 		// one beat every 100ms = 10 bps
	
	Timecode(NXTBee nb) {
		timecode = 0;
	}
	
	public void start() {
		running = true;
	}
	
	public void stop() {
		running = false;
	}
	
	/**
	 * Constantly update the timecode based on messages from the NXTBee
	 */
	public void run() {
		
	}
}

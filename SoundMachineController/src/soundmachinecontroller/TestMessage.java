package soundmachinecontroller;

import processing.core.PApplet;
import processing.serial.Serial;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.*;

public class TestMessage extends PApplet {

	MessageOLD m;
	InputStream in;
	OutputStream out;
	
	String fileNameIn = "foo.txt";
	
	public void setup() {
		// initialize the screen
	  	size(210, 120);
	  	smooth();
	  	try {
	  		m = new MessageOLD(in);
	  	} catch(Exception e) {
	  		println("EXCEPTION: " + e);
	  	}
	}
	
	public void draw() {
		
		m.setType((byte)1);
		m.setAddress((byte)2);
		m.addByte((byte)100);
		
		
	}
}

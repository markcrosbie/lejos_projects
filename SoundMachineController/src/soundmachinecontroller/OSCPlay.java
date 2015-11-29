package soundmachinecontroller;

import oscP5.*;
import netP5.*;
import processing.core.PApplet;

public class OSCPlay extends PApplet {
	
	OscP5 oscP5;
	NetAddress myRemoteLocation;
	int running;

	public void setup() {
	  size(400,400);
	  oscP5 = new OscP5(this,9001);
	  myRemoteLocation = new NetAddress("localhost",8000);
	  running = 0;

	}
	
	public void draw() {
	  background(0);
	}
	
	void oscEvent(OscMessage theOscMessage) {
	  println("received an osc message. ");
	  println("addrpattern: "+theOscMessage.addrPattern());
	}

	public void mousePressed() {
	  OscMessage myMessage;
	  if(running == 0) {
	    /* in the following different ways of creating osc messages are shown by example */
	    myMessage = new OscMessage("/live/play/clip");
	    myMessage.add(1);
	    myMessage.add(0);
	    running++;
	    println("Running clip 1");
	  } else if(running == 1) {
	    myMessage = new OscMessage("/live/play/clip");
	    myMessage.add(2);
	    myMessage.add(0);
	    running++;
	    println("Running clip 2");
	  } else if(running == 2) {
	    myMessage = new OscMessage("/live/stop/clip");
	    myMessage.add(1);
	    myMessage.add(0);
	    running++;
	    println("Stopped clip 1...");
	  } else {
	    myMessage = new OscMessage("/live/stop/clip");
	    myMessage.add(2);
	    myMessage.add(0);
	    running = 0;
	    println("Stopped clip 2...");
	  }
	 
	  println("I sent a message.");
	  /* send the message */    
	  oscP5.send(myMessage, myRemoteLocation); 
	}
}

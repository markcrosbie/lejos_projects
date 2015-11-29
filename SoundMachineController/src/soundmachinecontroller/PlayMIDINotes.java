package soundmachinecontroller;

import oscP5.*;
import netP5.*;
import processing.core.PApplet;

public class PlayMIDINotes extends PApplet {

		OscP5 oscP5;
		NetAddress myRemoteLocation;
		int note;

		public void setup() {
		  size(400,400);
		  
		  text("Play MIDI Notes over OSC", 0, 0);
		  oscP5 = new OscP5(this,9001);
		  myRemoteLocation = new NetAddress("localhost",8000);
		}
		
		public void draw() {
		  background(255);
		}
		
		void oscEvent(OscMessage theOscMessage) {
		  println("received an osc message. ");
		  println("addrpattern: "+theOscMessage.addrPattern());
		}

		public void mousePressed() {
			OscMessage myMessage;
			myMessage = new OscMessage("/live/play/note");
		    myMessage.add(note);
		  
		    println("I sent a message /live/play/note/"+note);
		    note++;
		  /* send the message */    
		  oscP5.send(myMessage, myRemoteLocation); 
		}
	}

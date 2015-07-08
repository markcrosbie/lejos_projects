package soundmachinecontroller;

/**
  * This sketch is a more involved use of AudioSamples to create a simple drum machine. Click on the buttons to 
  * toggle them on and off. The buttons that are on will trigger samples when the beat marker passes over their 
  * column. You can change the tempo by clicking in the BPM box and dragging the mouse up and down.
  */

import controlP5.*;
import processing.core.PApplet;

public class DrumMachine extends PApplet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		
	private ControlP5 gui;
	boolean[] hatRow = new boolean[16];
	boolean[] snrRow = new boolean[16];
	boolean[] kikRow = new boolean[16];
	
	
	public int bpm;
	int tempo; // how long a sixteenth note is in milliseconds
	int clock; // the timer for moving from note to note
	int beat; // which beat we're on
	boolean beatTriggered; // only trigger each beat once
	
	public void setup()
	{
	  size(395, 200);
	  	  
	  gui = new ControlP5(this);
	  gui.setColorForeground(color(128, 200));
	  gui.setColorActive(color(255, 0, 0, 200));
	  
	  println("GUI initialised ok");
	  
	  Toggle h;
	  Toggle s;
	  Toggle k;
	  for (int i = 0; i < 16; i++)
	  {
	    h = gui.addToggle("hat" + i, false, 10+i*24, 50, 14, 30);
	    h.setId(i);
	    h.setLabel("hat");
	    s = gui.addToggle("snr" + i, false, 10+i*24, 100, 14, 30);
	    s.setId(i);
	    s.setLabel("snr");
	    k = gui.addToggle("kik" + i, false, 10+i*24, 150, 14, 30);
	    k.setId(i);
	    k.setLabel("kik");
	  }
	  gui.addNumberbox("bpm", 120, 10, 5, 20, 15);
	  bpm = 120;
	  tempo = 125;
	  clock = millis();
	  beat = 0;
	  beatTriggered = false;
	  
	  textFont(createFont("Arial", 16));
	}
	
	public void draw()
	{
	  background(0);
	  fill(255);
	  //text(frameRate, width - 60, 20);
	  
	  if ( millis() - clock >= tempo )
	  {
	    clock = millis();
	    beat = (beat+1) % 16;
	    beatTriggered = false;
	  }
	  
	  if ( !beatTriggered )
	  {
	    if ( hatRow[beat] ) println("HAT");
	    if ( snrRow[beat] ) println("SNARE");
	    if ( kikRow[beat] ) println("KICK");
	    beatTriggered = true;
	  }
	  
	  stroke(128);
	  if ( beat % 4 == 0 )
	  {
	    fill(200, 0, 0);
	  }
	  else
	  {
	    fill(0, 200, 0);
	  }
	    
	  
	  // beat marker    
	  rect(10+beat*24, 35, 14, 9);
	  	  
	  gui.draw();
	}
	
	public void controlEvent(ControlEvent e)
	{
	  //println(e.controller().label() + ": " + e.controller().value());
	  if ( e.controller().label() == "hat" )
	  {
	    hatRow[ e.controller().id() ] = e.controller().value() == 0.0 ? false : true;
	  }
	  else if ( e.controller().label() == "snr" )
	  {
	    snrRow[ e.controller().id() ] = e.controller().value() == 0.0 ? false : true;
	  }
	  else if ( e.controller().label() == "kik" )
	  {
	    kikRow[ e.controller().id() ] = e.controller().value() == 0.0 ? false : true;
	  }
	  else if ( e.controller().name() == "bpm" )
	  {
	    float bps = (float)bpm/60.0f;
	    tempo = (int)(1000 / (bps * 4)); 
	  }
	}
	
	public void stop()
	{
	  // close the AudioSamples before we exit
	  super.stop();
	}
}
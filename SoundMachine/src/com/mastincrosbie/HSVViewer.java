package com.mastincrosbie;

import lejos.nxt.Button;
import lejos.nxt.ColorSensor;
import lejos.nxt.LCD;
import lejos.nxt.SensorPort;
import lejos.nxt.comm.RConsole;
import lejos.robotics.Color;
import lejos.util.Datalogger;
import lejos.util.Delay;

/**
 * Display the colour under the colour sensor attached to port 1 using the HSV
 * colour model.
 * 
 * @author mcrosbie
 *
 */
public class HSVViewer {
	private static String colorNames[] = {"Red", "Green", "Blue", "Yellow",
        "Magenta", "Orange", "White", "Black", "Pink",
        "Grey", "Light Grey", "Dark Grey", "Cyan", "None"};

	public static int numColours;
	
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
 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Datalogger d;
		
		HSV hsv[];
		ColorSensor.Color vals[];    
		ColorSensor cs[];

		//d = new Datalogger();
		
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

  		vals = new ColorSensor.Color[4];
  		hsv = new HSV[4];
  		  		
  		while(Button.ESCAPE.isUp()) {
  			
  			for(int i=0; i < 4; i++) {
  				vals[i] = cs[i].getColor();
  				hsv[i] = new HSV(vals[i]);
		 		
	  	 		int colour = hsv[i].colourID();
	  	 		int sat = (int)hsv[i].saturation();
	  	 		int luma = (int)hsv[i].value();
	  	 		
	  	 		LCD.drawString(colour + " " + sat + " " + luma, 0, i);
	 		//d.writeLog((float)hsv[0].hue(), (float)hsv[1].hue(), (float)hsv[2].hue(), (float)hsv[3].hue());	 		
	  	 		Delay.msDelay(10);
  			}	  		
  		//d.transmit();
  		}
	}
}

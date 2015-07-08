package com.mastincrosbie;

import lejos.nxt.Button;
import lejos.nxt.ColorSensor;
import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.robotics.Color;

public class ColourReader {

	   private static String colorNames[] = {"Red", "Green", "Blue", "Yellow",
           "Megenta", "Orange", "White", "Black", "Pink",
           "Grey", "Light Grey", "Dark Grey", "Cyan", "None"};

	   private static 		ColorSensor.Color vals;

	   private static ColorSensor cs;
	   private static int turntableSpeed = 180;

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
	 * @param args
	 */
	public static void main(String[] args) {

		int thisColour;
		
		// Use the colour sensor white WHITE light
		cs = new ColorSensor(SensorPort.S1);
		cs.setFloodlight(true);
		cs.setFloodlight(Color.WHITE);         

		startTurntable(turntableSpeed);
		
		// Keep running unless the ESCAPE button is pressed or the EXIT command is received
 		while (Button.ESCAPE.isUp() ) {            
 			
			LCD.drawString("Colours", 0, 1);
 			
			vals = cs.getColor();

 			thisColour = vals.getColor();
 			int red = vals.getRed();
 			int green = vals.getGreen();
 			int blue = vals.getBlue();
 			int background = vals.getBackground();
 				 			
      	   	LCD.drawString("Colour: " + getColourName(thisColour) + "  ", 0, 3);       

      	   	LCD.drawString("Red  Green  Blue", 0, 5);
      	   	LCD.clear(6);
      	   	LCD.drawInt(red, 0, 6);
      	   	LCD.drawInt(green, 5, 6);
      	   	LCD.drawInt(blue, 12, 6);
      	   	LCD.drawString("Backg: " + background + "  ", 0, 7); 
      	   	
      	   	if(thisColour == Color.BLACK) {
      	   		Sound.beep();
      	   	}
 		}
 		
 		stopTurntable();
	}
}
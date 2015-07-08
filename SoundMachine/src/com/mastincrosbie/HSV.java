package com.mastincrosbie;

import lejos.nxt.ColorSensor;


public class HSV {
	
	private double hue;
	private double sat;
	private double value;
	
	public static final int RED = 5;
	public static final int GREEN = 7;
	public static final int BLUE = 9;
	public static final int YELLOW = 6;
	public static final int MAGENTA = 4;
	public static final int ORANGE = 5;
	public static final int WHITE = 6;
    public static final int BLACK = 7;
    public static final int PINK = 10;
    public static final int GRAY = 9;
    public static final int LIGHT_GRAY = 10;
    public static final int DARK_GRAY = 11;
    public static final int CYAN = 12;
    public static final int NONE = -1;

	private static String colorNames[] = {"Red", "Green", "Blue", "Yellow",
        "Magenta", "Orange", "White", "Black", "Pink",
        "Grey", "Light Grey", "Dark Grey", "Cyan", "None"};


	public HSV() {
		hue = sat = value = NONE;
	}
	
	/**
	 * Create a HSV triple from a ColorSensor Color value
	 */
	public HSV(ColorSensor.Color col) {
		RGBtoHSV(col.getRed(), col.getGreen(), col.getBlue());
	}
	
	/**
	 * Create a HSV triple given a Red,Green,Blue triple
	 * @param r Red
	 * @param g Green
	 * @param b Blue
	 */
	public HSV(double r, double g, double b) {
		RGBtoHSV(r, g, b);
	}

	public double hue() {
		return hue;
	}
	
	public double saturation() {
		return sat;
	}
	
	public double value() {
		return value;
	}
	
	 /**
	  * Convert a colour value from the NXT colour sensor to a printable string
	  * @param c Colour value
	  * @return String name of the colour
	  */
	 private static String getColourName(int c) {
	 	
	 	if( (c < 0) || (c > colorNames.length) )
	 		return "???";
	 
	 	return colorNames[c];
	 }
	 
	/**
	 * Convert RGB colors to HSV
	 * @param red the red input value
	 * @param green the green input value
	 * @param blue the blue input value
	 * @return void
	 */
	private void RGBtoHSV(double red, double green, double blue) {
		hue = 0;
		sat = 0;
		value = 0;
		
	  //   Value
	  double rgb_max = max3(red, green, blue);
	  double rgb_min;
	  value = rgb_max / 2.56;
	  if (value == 0){
	    hue = -1;
	    sat = -1;
	    return;
	  }

	  //   Saturation
	  red /= rgb_max;
	  green /= rgb_max;
	  blue /= rgb_max;

	  rgb_max = max3(red, green, blue);
	  rgb_min = min3(red, green, blue);
	  sat = (rgb_max - rgb_min) * 100;
	  if (sat == 0){
	    hue = -1;
	    return;
	  }

	  //   Hue
	  red = (red - rgb_min) / (rgb_max - rgb_min);
	  green = (green - rgb_min) / (rgb_max - rgb_min);
	  blue = (blue - rgb_min) / (rgb_max - rgb_min);

	  rgb_max = max3(red, green,blue);
	  rgb_min = min3(red, green,blue);

	  if (rgb_max == red){
	    hue = 0.0 + 60.0*(green-blue);
	    if (hue < 0.0){
	      hue += 360.0;
	    }
	  } else if (rgb_max == green){
	    hue = 120.0 + 60.0 * (blue-red);
	  } else {
	    hue = 240.0 + 60.0 * (red-green);
	  }
	}

	public boolean isValid() { return this.sat != -1; }

	
	/**
	 * Returns the LEGO colour ID for the given HSV value
	 * @return Colour ID, or NONE if no colour is known for these values
	 */
	public int colourID() {

		// quantifies the shade into a number for easy comparison
		// with lots of magic numbers!
		
		/**
		// 1 = invalid, 2 = black, 3 = grey, 4 = white
		if (!isValid()) return NONE;
		if (value < 25) return BLACK;	// black
		if (sat < 25) {
			if (value > 45) return WHITE;  // white
			return GRAY; // gray
		}
		**/
		
		// it is a colour
		// give us a number representing one of 6 hues
		// 5 = red, 6 = yellow, 7 = green, 8 = cyan, 9 = blue, 10 = magenta
		
		double c = hue / 45.0f;
		
		// center the result on the colour
		c += 0.5f;
		if (c >= 8.0f) c -= 8.0f;
		
		return (int)(c);

		/**
		if( (hue >= 325) )
			return BLACK;
		
		if( (hue >= 228) && (sat >= 66) && (value >= 47))
			return BLUE;
		
		if( (hue >= 47) && (sat >= 5) && (value >= 85))
			return WHITE;
		
		if( (hue >= 330) && (sat >= 50) && (value >= 90))
			return PINK;
		
		if( (hue >= 122) && (sat >= 44) && (value >= 56))
			return GREEN;
		
		if( (hue >= 7) && (sat >= 64) && (value >= 101))
			return ORANGE;		
		return NONE;
		**/

	}

	/**
	 * This define returns the smallest of the three numbers
	 */
	private double min3(double a, double b, double c)  {
		return (a < b) ? ((a < c) ? a : c) : ((b < c) ? b : c);
	}

	/**
	 * This function returns the biggest of the three numbers
	 */
	private double max3(double a, double b, double c) {
		return (a > b) ? ((a > c) ? a : c) : ((b > c) ? b : c);
	}

}


public class HSV {
	
	private double hue;
	private double sat;
	private double value;
	
	public HSV() {
		hue = sat = value = -1.0;
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
	 * Convert RGB colors to HSV
	 * @param red the red input value
	 * @param green the green input value
	 * @param blue the blue input value
	 * @param hue the hue output value (from 0 to 365, or -1 if n/a)
	 * @param sat the saruration output value (from 0 to 100, or -1 if n/a)
	 * @param value the value output value (from 0 to 100)
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

	
	public int colorID() {
		// quantifies the shade into a number for easy comparison
		// with lots of magic numbers!
		
		// 1 = invalid, 2 = black, 3 = grey, 4 = white
		if (!isValid()) return 1;
		if (value < 25) return 2;	// black
		if (sat < 25) {
			if (value > 45) return 4;  // white
			return 3; // gray
		}
		
		// it is a colour
		// give us a number representing one of 6 hues
		// 5 = red, 6 = yellow, 7 = green, 8 = cyan, 9 = blue, 10 = magenta
		
		double c = hue / 60.0f;
		// center the result on the colour
		c += 0.5f;
		if (c >= 6.0f) c -= 6.0f;
		
		return (int)(c) + 5;
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
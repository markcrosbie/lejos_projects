import lejos.nxt.Battery;
import lejos.nxt.Button;
import lejos.nxt.ColorSensor;
import lejos.nxt.Sound;
import lejos.nxt.ColorSensor.Color;
import lejos.nxt.LCD;
import lejos.nxt.SensorPort;
import lejos.nxt.comm.RConsole;
import lejos.util.Delay;

public class HSVtester {

	private static ColorSensor cs[];
	  
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

	private static 		ColorSensor.Color vals;    

	public static void initialiseColourSensors() {
        // Use the colour sensor white WHITE light
 		cs = new ColorSensor[4];
 		
 		cs[0] = new ColorSensor(SensorPort.S1);
 		//cs[0].setFloodlight(true);
  		//cs[0].setFloodlight(Color.WHITE);

 		cs[1] = new ColorSensor(SensorPort.S2);
 		//cs[1].setFloodlight(true);
  		//cs[1].setFloodlight(Color.WHITE);

  		cs[2] = new ColorSensor(SensorPort.S3);
 		//cs[2].setFloodlight(true);
  		//cs[2].setFloodlight(Color.WHITE);
 
  		cs[3] = new ColorSensor(SensorPort.S4);
 		//cs[3].setFloodlight(true);
  		//cs[3].setFloodlight(Color.WHITE); 		
    }


    
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

		RConsole.openUSB(3000);
		
		HSV hsv;		
		
		initialiseColourSensors();
				
 		LCD.clear();
 		LCD.drawString("HSVtester", 0, 0);
 		 				 		
 		numColours = colorNames.length;
 		 		
 		boolean done = false;
 		while(!done) {
	 		Color vals;
	 		LCD.drawString("Position colour", 0, 1);
	 		LCD.drawString("Press ENTER", 0, 2);
	 		
	 		while(Button.ENTER.isUp());
	 		
	 		int numSamples = 500;
	 		
	 		double h[] = new double[numSamples];
	 		double s[] = new double[numSamples];
	 		double v[] = new double[numSamples];
	 		 		
	 		LCD.drawString("Sampling...", 0, 3);
	 		
	 		for(int i=0; i < numSamples; i++) {
	 			vals = cs[0].getColor();
	 	 		hsv = new HSV(vals.getRed(), vals.getGreen(), vals.getBlue());
	 	 		
	 	 		//LCD.drawString("Hue: " + hsv.hue() + "   ", 0, 2);
	 	 		//LCD.drawString("Sat: " + hsv.saturation() + "   ", 0, 3);
	 	 		//LCD.drawString("Val: " + hsv.value() + "   ", 0, 4);
	 	 		//LCD.drawString(getColourName(vals.getColor()) + "   ", 0, 5);
	 			LCD.drawString("Sample " + i, 0, 6);
	 			
	 	 		h[i] = hsv.hue();
	 	 		s[i] = hsv.saturation();
	 	 		v[i] = hsv.value();
	 	 		
	 	 		Delay.msDelay(10);
	 		}
	 		
	 		double avgHue, avgSat, avgValue;
	 		avgHue = avgSat = avgValue = 0.0;
	 		
	 		for(int i=0; i < numSamples; i++) {
	 			avgHue += h[i];
	 			avgSat += s[i];
	 			avgValue += v[i];
	 		}
	 		
	 		avgHue /= numSamples;
	 		avgSat /= numSamples;
	 		avgValue /= numSamples;
	 		
	 		LCD.clear();
	 		LCD.drawString("Avg Hue " + avgHue, 0, 0);
	 		LCD.drawString("Avg Sat " + avgSat, 0, 1);
	 		LCD.drawString("Avg Val " + avgValue, 0, 2);
	 			 		
	 		LCD.drawString("ENTER to resample", 0, 4);
	 		LCD.drawString("ESC to exit", 0, 5);
	 		
	 		while(Button.ENTER.isUp() && Button.ESCAPE.isUp());
	 		if(Button.ESCAPE.isDown())
	 			done = true;
	 		LCD.clear();
 		}
 		
 		RConsole.println("Goodbye");
 		RConsole.close();
	}

}

import lejos.hardware.motor.Motor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.AbstractFilter;
import lejos.utility.Delay;

/**
 * Tracks movement of the joystick class
 * 
 * @author mcrosbie
 *
 */

public class Joystick extends Thread {

	private Port port;
	private EV3TouchSensor t;
	private SimpleTouch fireButton;
	private RegulatedMotor m;
	private int low, high, mid;
	private int current;
	private int sampleDelay = 50;
	private boolean running = true;
	
	public Joystick(Port fireButtonPort, RegulatedMotor _m) {
		port = fireButtonPort;
		
		t = new EV3TouchSensor(port);
		fireButton = new SimpleTouch(t);
		
		m = _m;
		m.resetTachoCount();
	}
	
	public int getPosition() {
		return current;
	}
	
	public void calibrate() {
		running = false;
		
	}
	
	public boolean firePressed() {
		return fireButton.isPressed();
	}
	
	public void run() {
		while(running) {
			current = m.getTachoCount();
			Delay.msDelay(sampleDelay);
		}
	}
	
	protected class SimpleTouch extends AbstractFilter {
		  private float[] sample;

		  public SimpleTouch(SampleProvider source) {
		    super(source);
		    sample = new float[sampleSize];
		  }

		  public boolean isPressed() {
		    super.fetchSample(sample, 0);
		    if (sample[0] == 0)
		      return false;
		    return true;
		  }
	}
}

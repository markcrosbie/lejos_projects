
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.GraphicsLCD;import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.AbstractFilter;
import lejos.robotics.geometry.Rectangle;
import lejos.utility.Delay;

/**
 * Represents the ship on the screen
 * 
 * @author mcrosbie
 *
 */
public class Ship extends Thread {

	private GraphicsLCD g;
	public int x, y;
	private Sprite ship, explosion;
	private boolean running = true;
	private EV3TouchSensor t;
	private SimpleTouch fireButton;
	private RegulatedMotor m;


	/**
	 * Create a ship object to represent the players ship
	 * 
	 * @param _g The graphics context we're drawing in
	 * @param shipFilename Name of the file containing the ship sprite
	 * @param xpos Starting x position of the ship
	 * @param ypos Starting y position of the ship
	 */
	public Ship(GraphicsLCD _g, String shipFilename, int xpos, int ypos, Port fireButtonPort, RegulatedMotor _m) {
		x = xpos;
		y = ypos;
		g = _g;		
		ship = new Sprite(g, shipFilename, xpos, ypos);
		explosion = new Sprite(g, "explosion.lni", xpos, ypos);
		
		t = new EV3TouchSensor(fireButtonPort);
		fireButton = new SimpleTouch(t);	
		m = _m;
		m.resetTachoCount();
	}
	
	/**
	 * Draw the ship on the screen 
	 */
	public void render() {
		ship.render();
	}
	
	/**
	 * Move the ship to a specified position. It can only move left/right
	 * 
	 * @param xpos New position to move to
	 */
	public void moveTo(int xpos) {
		if( (xpos > 0) && (xpos < g.getWidth())) {
			x = xpos;
		}
	}

	public void stopRunning() {
		running = false;
	}
	
	/**
	 * Return true if the x1,y1 position is within the boundary of the ship
	 * @param x1
	 * @param y1
	 * @return true if x1,y1 intersects with the ship
	 */
	public boolean intersect(int x1, int y1) {
		Rectangle r = new Rectangle(x, y, ship.getWidth(), ship.getHeight());
		return r.intersects(x1, y1, 2, 2);	
	}

	public void run() {
		
		int SW = g.getWidth();
				
		while(running) {
						
			// check to see if we have been hit by a missile
			if(SpaceInvaders.missiles.checkMissileHitShip(this)) {
				// we've been hit
				SpaceInvaders.score.loseLive(1);
				ship.erase();
				explosion.moveTo(x, y, false);
				explosion.render();
				Sound.beepSequence();
				Delay.msDelay(1000);
				explosion.erase();
				ship.render();
				continue;
			}
			
			// draw the ship on the screen
			render();

			// if we're using key control then the keypad on the front of the EV3 controls the ship movement
			if(SpaceInvaders.keyControl) {
				if(Button.LEFT.isDown() && x > 0) {
		    		x = x - 5;
		    		ship.erase();
			    	ship.moveTo(x, y, false);
		    		Delay.msDelay(10);
		    	}
		    	
		    	if(Button.RIGHT.isDown() && x < (SW-15)) {
		    		x = x + 5;
		    		ship.erase();
			    	ship.moveTo(x, y, false);
		    		Delay.msDelay(10);
		    	}
		    	
		    	if(Button.ENTER.isDown()) {
		    		// do we have ammo left?
		    		if(SpaceInvaders.score.getAmmo() > 0) {
		    			// debounce
			    		Delay.msDelay(100);
			    		SpaceInvaders.missiles.fireMissile(x, y - (ship.getHeight() + 3), true);
			    		SpaceInvaders.score.shootAmmo(1);
			    	}	
		    	}
			} else {

				// we're using joystick control so get the input from the motor tacho
				int pos = m.getTachoCount();			
				int newx = pos + 15;
				if (newx < 0) {
					newx = 0;
				}
				if(newx > 30) {
					newx = 30;
				}
				
				int scale = SW / 30;
				newx = newx * scale;
	
				if(Math.abs(x - newx) >= 2) {
					ship.erase();
					x = newx;
			    	ship.moveTo(x, y, false);
				}
				
		    	if(fireButton.isPressed()) {
		    		// do we have ammo left?
		    		if(SpaceInvaders.score.getAmmo() > 0) {
		    			// debounce
			    		Delay.msDelay(100);
			    		SpaceInvaders.missiles.fireMissile(x, y - (ship.getHeight() + 3), true);
			    		SpaceInvaders.score.shootAmmo(1);
			    	}	
		    	}	    	
			}
		    Delay.msDelay(50);
		}  
		t.close();
		ship.erase();
	}
	
	protected class SimpleTouch extends AbstractFilter {
		  private float[] sample;
		  int sampleSize = 1;
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

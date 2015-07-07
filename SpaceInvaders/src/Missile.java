import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.lcd.Image;
import lejos.robotics.geometry.Rectangle;
import lejos.utility.Delay;

/**
 * A missile on the screen can travel with up or down depending on who shot it
 * 
 * @author mcrosbie
 *
 */
public class Missile extends Thread {
	private GraphicsLCD g;
	public int x, y;
	private int direction;
	private final int SW;
	private final int SH;
	private final int speed = 100;
	private final int missileWidth = 3;
	private final int missileHeight = 3;
	public boolean movingUp, running;
	
	/**
	 * Create a missile object to represent a missile on the screen
	 * 
	 * @param _g The graphics context we're drawing in
	 * @param xpos Starting x position of the missile
	 * @param ypos Starting y position of the missile
	 * @param up true if the missile is moving up the screen, otherwise false for downwards
	 */
	public Missile(GraphicsLCD _g, int xpos, int ypos, boolean up) {
		x = xpos;
		y = ypos;
		g = _g;
		SW = g.getWidth();
		SH = g.getHeight();
		
		movingUp = up;
		if(movingUp) {
			direction = -1;
		} else {
			direction = 1;
		}
	}

	public void run() {
		int top = 10;
		int bottom = SH;
		
		// animate the missile depending on the direction of travel
		running = true;
		if(movingUp) {
			for(; y > top && running; y-=3) {
				g.setColor(g.BLACK);
				g.fillRect(x, y, missileWidth, missileHeight);
				Delay.msDelay(speed);
				g.setColor(g.WHITE);
				g.fillRect(x, y, missileWidth, missileHeight);				
			}
		} else {
			for(; y < bottom && running; y+=3) {
				g.setColor(g.BLACK);
				g.fillRect(x, y, missileWidth, missileHeight);
				Delay.msDelay(speed);
				g.setColor(g.WHITE);
				g.fillRect(x, y, missileHeight, missileHeight);				
			}	
		}		
	}

	/**
	 * Does this missile intersect with the given x,y location?
	 * 
	 * @param x1
	 * @param y1
	 * @return true if the missile overlaps the x1,y1 position
	 */
	public boolean intersect(int x1, int y1) {
		Rectangle r = new Rectangle(x, y, missileWidth, missileHeight);
		return r.intersects(x1, y1, missileWidth, missileHeight);	
	}

	public void stopRunning() {
		running = false;
		g.setColor(g.WHITE);
		g.drawRect(x, y, 2, 2);				
	}
	
}

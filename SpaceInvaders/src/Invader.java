import lejos.hardware.Sound;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.lcd.Image;
import lejos.robotics.geometry.Rectangle;

/**
 * Controller logic for one space invader
 * 
 * @author mcrosbie
 *
 */
public class Invader {

	private int x, y;
	private Sprite me1, me2;
	private int direction = 1;
	private final int SW;
	private final int SH;
	private int width, height;
	private boolean whichToDraw = true;
	private int lowerEdgeOfPlay;
	private GraphicsLCD g;
	
	public Invader(GraphicsLCD _g, Image img1, Image img2, int xpos, int ypos) {

		x = xpos;
		y = ypos;
		g = _g;
		me1 = new Sprite(g, img1, x, y);
		me2 = new Sprite(g, img2, x, y);
		SW = g.getWidth();
		SH = g.getHeight();
		width = img1.getWidth();
		height = img1.getHeight();

		lowerEdgeOfPlay = SH - 30;
	}
	
	public void render() {
		if(whichToDraw) {
			me2.erase();
			me1.moveTo(x, y, true);
			me1.render();
		} else {
			me1.erase();
			me2.moveTo(x, y, true);
			me2.render();
		}
		whichToDraw = !whichToDraw;
	}
	
	public void erase() {
		if(whichToDraw) {
			me1.erase();
		} else {
			me2.erase();
		}
		
	}
	
	public boolean intersect(int x1, int y1) {
		Rectangle r = new Rectangle(x, y, width, height);
		return r.intersects(x1, y1, 2, 2);	
	}
	
	public void switchDirection() {
		direction = -direction;
	}
	
	/**
	 * Move the invader sideways across the screen in the direction of travel
	 * If the edge of the screen is reached then change the direction of travel and
	 * move back in the other direction.
	 * Animates between two invader images at it moves
	 * 
	 * @param xstep Number of pixels to move the invader
	 * @return True if the invader has hit the left/right edge of the screen
	 */
	public boolean moveSideways(int xstep) {
		x += direction*xstep;
		// two conditions; we come within an invader width of the left or right edge of the screen
		if( (x >= (SW-width)) || (x <= 0)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Move the invader down the screen
	 * 
	 * @param ystep Number of pixels to move down
	 * @returns true if the invader is at the bottom of the screen, otherwise false
	 */
	public boolean moveDown(int ystep) {
		// Gameover - Have we hit the bottom of the screen?
		y += ystep;
		if(y >= lowerEdgeOfPlay) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Drop a bomb from the invader downwards
	 * The bomb is a missile and runs in its own thread
	 */
	public void dropBomb() {		
		SpaceInvaders.missiles.fireMissile(x, y, false);
	}
}

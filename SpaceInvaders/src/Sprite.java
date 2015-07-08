import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.lcd.Image;
import lejos.robotics.geometry.Rectangle;

/**
 * Represents a sprite or drawable object on the screen
 * 
 * @author mcrosbie
 *
 */
public class Sprite {

	private int x, y;
	private GraphicsLCD g;
	private Image image;
	private int width, height;
	private Rectangle rect;

	/**
	 * Create a sprite that can be displayed on the screen
	 * @param _g The current graphics LCD context we are drawing in
	 * @param sprite A previously opened image for the sprite
	 * @param x_pos It's initial X position on the screen
	 * @param y_pos The initial Y position on the screen

	 */
	public Sprite(GraphicsLCD _g, Image sprite, int x_pos, int y_pos) {
		x = x_pos;
		y = y_pos;
		g = _g;
		image = sprite;
		width = image.getWidth();
		height = image.getHeight();
		rect = new Rectangle(x, y, width, height);
	}
	
	/**
	 * Create a sprite that can be displayed on the screen
	 * @param _g The current graphics LCD context we are drawing in
	 * @param spriteFilename The filename to load the sprite from
	 * @param x_pos It's initial X position on the screen
	 * @param y_pos The initial Y position on the screen
	 */
	public Sprite(GraphicsLCD _g, String spriteFilename, int x_pos, int y_pos) {
		x = x_pos;
		y = y_pos;
		g = _g;
		image = loadImageFrom(spriteFilename);
		width = image.getWidth();
		height = image.getHeight();		 
		rect = new Rectangle(x, y, width, height);
	}
	
	/**
	 * Load a sprite image from a file in FLASH storage.
	 * @param filename File to load
	 * @return An image for the file loaded
	 * @throws Exception if the file is not found
	 */
	private Image loadImageFrom(String filename) {	
		try {
			return Image.createImage(new FileInputStream(new File(filename)));    		
	    } catch (IOException e) {
	    	System.err.println("Exception opening image " + filename + " : " + e.getMessage());
	    }
		return null;
	}

	/**
	 * Move the sprite to the specified x,y position
	 * @param x_pos X position to move to
	 * @param y_pos Y position to move to
	 * @param erase if true then erase before moving
	 */
	public void moveTo(int x_pos, int y_pos, boolean erase) {
		if(erase) 
			erase();
		x = x_pos;
		y = y_pos;
	}

	/**
	 * Render the sprite onto the screen at its current x,y position
	 */
	public void render() {
		g.setColor(g.BLACK);
		g.drawRegion(image, 0, 0, width, height, 
	    	GraphicsLCD.TRANS_NONE, 
	    	x, y, 
	    	GraphicsLCD.TOP | GraphicsLCD.TOP);	
	}
	
	/**
	 * Erase the sprite from the screen at its current x,y position.
	 * This draws a rectangle in white at the current x,y position, so it erases anything
	 * else in that region.
	 */
	public void erase() {
		g.setColor(g.WHITE);
		g.fillRect(x, y, width, height);
		g.setColor(g.BLACK);
	}
	
	/**
	 * Return true if the x,y position passed intersects with the sprite
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean intersect(int x1, int y1) {
		int SW = g.getWidth();
		int SH = g.getHeight();

		// sanity check
	   	if( (x1 < 0) || (x1 > SW))
    		return false;
    	if( (y1 < 0) || (y1 > SH))
    		return false; 	

    	return rect.contains(x1, y1);		
	}
	
	public int getWidth() {
		return image.getWidth();
	}
	
	public int getHeight() {
		return image.getHeight();
	}
}

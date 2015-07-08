import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import lejos.hardware.Sound;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.lcd.Image;
import lejos.robotics.Color;
import lejos.utility.Delay;

/**
 * Represents the invaders as a whole and moves them on the screen in a thread
 * 
 * @author mcrosbie
 *
 */
public class Invaders extends Thread {

	private ArrayList<Invader> enemies;	// The list of enemies on the screen
	
	private int gridWidth;
	private int gridHeight;
	private GraphicsLCD g;
	private int gridXWidth, gridYHeight;
	
    private int delay = 1000;
    private final int delayStep = 100;
    private int iters = 0;
    private final int xstep = 5;
    private final int ystep = 5;
    private boolean running = true;
    private int gridx, gridy;
    
	/**
	 * Create the grid of invaders
	 * 
	 * @param _g The graphics context we're drawing in
	 * @param width The number of invaders in the grid wide
	 * @param height The number of lines of invaders
	 * @param x The initial X position of the grid
	 * @param y The initial Y position of the grid
	 * @param xgap The gap between each invader in the X direction
	 * @param ygap The gap between the rows of invaders
	 * @param invader1Filename Filename for the invader sprite in position 1
	 * @param invader2Filename Filename for the invader sprite in position 2
	 */
	public Invaders(GraphicsLCD _g, int width, int height, 
					int x, int y, 
					int xgap, int ygap, 
					String invader1Filename,
					String invader2Filename) {
		gridWidth = width;
		gridHeight = height;
		gridx = x;
		gridy = y;
		
		g = _g;
		
		Image invader1 = loadImageFrom(invader1Filename);
		Image invader2 = loadImageFrom(invader2Filename);
		
		xgap += invader1.getWidth();
		ygap += invader1.getHeight();
		
		// Create the rows and columns of invaders by placing them across the screen
		enemies = new ArrayList<Invader>();
		
		for(int i=0; i < height; i++) {
			for(int j=0; j < width; j++) {
				enemies.add(new Invader(g, invader1, invader2, x + (j*xgap), y + (i*ygap)));
			}
		}
		
		gridXWidth =  (width * xgap);
		gridYHeight =  (height * ygap);
	}

	/**
	 * Draw the current invader grid on the screen. 
	 */
	public void render() { 
		
		g.setColor(Color.WHITE);
		g.fillRect(gridx, gridy, gridXWidth, gridYHeight);
		g.setColor(Color.BLACK);
		
   		Iterator<Invader> l = enemies.iterator();
   		while(l.hasNext()) {
   			l.next().render();
   		}
      	// play the sound of the invaders moving
    	if((iters++ % 2) == 0) {
			Sound.playTone(300, 25, 30);
    	} else {
			Sound.playTone(100, 25, 30);
    	}

	}
	
	/**
	 * Perform one iteration of the invader grid moving right/left and down
	 * Moves all sprites one step to the right. If they reach the edge of the screen
	 * changes direction to move to the left and moves down one row.
	 * Takes into account that the grid of invaders may be shrinking as they are shot
	 * by the player
	 * 
	 * @param xstep The amount of X direction change left/right
	 * @param ystep The amount of Y direction changes downwards
	 * @returns false if the invaders have space to keep moving, true if the invaders have 
	 * reached the bottom of the screen (i.e. gameover)
	 */
	public boolean iterate(int xstep, int ystep) {
		boolean edgeHit = false;
		boolean gameOver = false;
		
		Delay.msDelay(delay);
		// move all of the invaders sideways
   		Iterator<Invader> l = enemies.iterator();
	   	while(l.hasNext()) {
	   		edgeHit |= l.next().moveSideways(xstep);
	   	}
    		
   		// did any invader hit an edge? switch direction of movement
   		if(edgeHit) {
   	   		// iterate over the list and tell them all to switch direction of movement
   	   		// and move down the screen
   	   		l = enemies.iterator();
	   		while(l.hasNext()) {
	   			Invader inv = l.next();
	   			inv.switchDirection();
	   			gameOver |= inv.moveDown(ystep);
	   		}		
   			delay -= delayStep;
   		}
   		
		return gameOver;
	}
	
	/**
	 * Check if any of the invaders have collided with the sprite (usually the ship)
	 * 
	 * @param s
	 * @return
	 */
	public boolean checkCollision(Ship s) {
   		Iterator<Invader> l = enemies.iterator();
   		while(l.hasNext()) {
   			if(l.next().intersect(s.x, s.y)) {
   				return true;
   			}
   		}		
		return false;		
	}
	
	
	/**
	 * Are there any space invaders left to shoot?
	 * 
	 * @return true if there are any invaders left on the grid
	 */
	public boolean invadersLeft() {
		if(enemies.size() > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	// main space invader loop just iterates keeping the invaders moving and checking to see
	// if any missiles had hit them.
	public void run() {
		boolean bombDropped = false;
		
		while(invadersLeft() && running) {
			
 			Iterator<Invader> l = enemies.iterator();
			
 			// iterate through each invader left flying and see if it has been hit
 			// by a missile. If so remove it from the invaders list and add one
 			// to the player's score
			while(l.hasNext()) {
				Invader i = l.next();
				if(SpaceInvaders.missiles.checkMissiles(i)) {
					i.erase();
					l.remove();
					SpaceInvaders.score.addToScore(1);
				}
				
				// drop the bomb
				if(!bombDropped && Math.random() <= 0.1) {
					i.dropBomb();
					bombDropped = true;
				}
			}	
			
			// now make all the invaders take one step sideways in the direction of travel
        	if(iterate(xstep, ystep)) {
        		SpaceInvaders.aliensAtBottom = true;
        		break;
        	}       	
        	render();
        	bombDropped = false;
		}
		
  		Iterator<Invader> l = enemies.iterator();
   		while(l.hasNext()) {
   			l.next().erase();
   		}

	}
	
	public void stopRunning() {
		running = false;
	}

	/**
	 * Load a sprite image from a file in FLASH storage.
	 * @param filename File to load
	 * @return
	 */
	private Image loadImageFrom(String filename) {	
		try {
			return Image.createImage(new FileInputStream(new File(filename)));    		
	    } catch (IOException e) {
	    	System.err.println("Exception opening image " + filename + " : " + e.getMessage());
	    }
		return null;
	}

}

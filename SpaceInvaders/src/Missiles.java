import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import lejos.hardware.lcd.GraphicsLCD;

/**
 * Represents all the missiles flying on the screen
 * 
 * @author mcrosbie
 *
 */
public class Missiles {

    private ArrayList<Missile> missiles;
    private GraphicsLCD g;
    Semaphore s;
    
    public Missiles(GraphicsLCD _g) {
    	g = _g;
	   	missiles = new ArrayList<Missile>();
	   	s = new Semaphore(1);
    }
    
    /**
     * Start a missile flying in the given direction from the start position
     * 
     * @param x
     * @param y
     * @param direction if true then flying up other false for flying down
     */
    public void fireMissile(int x, int y, boolean direction) {
		try {
    		s.acquire();
	   		Missile m = new Missile(g, x+10, y+20, direction);
			missiles.add(m);
			m.setDaemon(true);
			m.start();
			s.release();
		} catch (Exception e) {
			System.out.println("fireMissile Exception: " + e.getMessage());
		}
    }
    
	/**
	 * Check if any player missiles have hit an invader, and if so, return true
	 * Only checks the first invader hit. Missile must be moving up to have been 
	 * fired by a player, don't let invaders shoot each other
	 * Deletes the missiles that hit the invader
	 * 
	 * @param invader a space invader to check
	 * @returns true if the missile hit an invader, else false
	 */
	public boolean checkMissiles(Invader invader) {		
   		try {
   			s.acquire();
   	   		Iterator<Missile> mIter = missiles.iterator();
	   		while(mIter.hasNext()) {
	   			Missile m = mIter.next();
	   	  		if(m.movingUp && invader.intersect(m.x, m.y)) {
	   	   			m.stopRunning();
	   	   			missiles.remove(m);
	   	   			s.release();
	   	   			return true;
	   	   		}
	  		}
	   		s.release();
   		} catch(Exception e) {
   			// not much we can do with an exception at this point
   			System.out.println("checkMissiles Exception: " + e.getMessage());
   		}
   		return false;
	}

	/**
	 * Check if any invader missiles have hit the ship, and if so, return true
	 * Deletes the missile from the missiles list
	 * 
	 * @param x x position of the ship
	 * @param y y position of the ship
	 * @returns true if the missile hit an invader, else false
	 */
	public boolean checkMissileHitShip(Ship ship) {		
   		try {
   			s.acquire();
   	   		Iterator<Missile> mIter = missiles.iterator();
	   		while(mIter.hasNext()) {
	   			Missile m = mIter.next();
	   			//System.out.println("checkMissileHitShip: m.x,m.y=" + m.x + "," + m.y);
	   	  		if(ship.intersect(m.x, m.y)) {
	   	  			//System.out.println("Missile intersects the ship");
	   	   			m.stopRunning();
	   	   			missiles.remove(m);
	   	   			s.release();
	   	   			return true;
	   	   		}
	  		}
	   		s.release();
   		} catch(Exception e) {
   			// not much we can do with an exception at this point
   			System.out.println("checkMissileHitShip Exception: " + e.getMessage());
   		}
   		return false;
	}

}

import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.robotics.Color;
import lejos.utility.Delay;

/**
 * Represent the score for the game
 * @author mcrosbie
 *
 */
public class Score extends Thread {

	private int score;
	private int livesLeft;
	private int ammo;
	private final int x, y;
	private GraphicsLCD g;
	private int SW;
	private boolean running = false;
	
	/**
	 * Initialise the score and lives left
	 * 
	 * @param _g The current graphics context to draw in
	 * @param initialScore What score to start from
	 * @param lives Initial number of lives
	 * @param _ammo Initial amount of ammo
	 * @param _x x position of where the score is drawn
	 * @param _y y position of where the score is drawn
	 */
	public Score(GraphicsLCD _g, int initialScore, int lives, int _ammo, int _x, int _y) {
		g = _g;
		score = initialScore;
		livesLeft = lives;
		ammo = _ammo;
		x = _x;
		y = _y;
		SW = g.getWidth();
		running = true;
	}
	
	public void run() {
		while(running) {
			render();
			Delay.msDelay(100);
		}
		
		erase();
	}
	
	
	public void stopThread() {
		running = false;
	}
	
	/**
	 * Draw the score on the screen
	 */
	public void render() {
		g.setColor(GraphicsLCD.BLACK);
        g.setFont(Font.getDefaultFont());
		//g.setFont(Font.getSmallFont());
		g.drawString("Sc:" + score + "  Li:" + livesLeft + "  Am:" + ammo,
				x, y, GraphicsLCD.TOP | GraphicsLCD.TOP);
	}

	private void erase() {
		// only draw if the display is changed
		g.setColor(GraphicsLCD.WHITE);
        g.setFont(Font.getDefaultFont());
		//g.setFont(Font.getSmallFont());
		g.drawString("Sc:" + score + "  Li:" + livesLeft + "  Am:" + ammo,
				x, y, GraphicsLCD.TOP | GraphicsLCD.TOP);		
	}
	
	public void setScore(int _score) {
		erase();
		score = _score;
	}
	
	public void addToScore(int _score) {
		erase();
		score += _score;
	}
	
	public void deductFromScore(int _score) {
		erase();
		score -= _score;
	}
	
	public int currentScore() {
		return score;
	}
	
	public void setLives(int _lives) {
		erase();
		livesLeft = _lives;
	}
	
	public void addLive(int _lives) {
		erase();
		livesLeft += _lives;
	}
	
	public void loseLive(int _lives) {
		erase();
		livesLeft -= _lives;
	}
	
	public void shootAmmo(int _ammo) {
		erase();
		ammo -= _ammo;
	}
	
	public int getLives() {
		return livesLeft;
	}
	
	public int getAmmo() {
		return ammo;
	}
}

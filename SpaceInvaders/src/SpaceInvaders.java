
import java.io.File;

import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.motor.Motor;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.port.TachoMotorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MeanFilter;
import lejos.utility.Delay;

/**
 * A Space Invaders game for the LEGO Mindstorms EV3.
 * The classic Space Invaders game from the arcade. Shoot down the invading swarm of aliens before
 * they shoot you!
 * 
 * Motors:
 * Motor Port A: Large EV3 servo motor for joystick
 * Motor Port B: Small EV3 motor for the coin slot
 * Sensor Port 1: Touch sensor as the Joystick fire button
 * Sensor Port 2: EV3 colour sensor for the coin slot to detect a coin
 * 
 * Written by: Mark Crosbie mark@mastincrosbie.com
 * http://thinkbricks.net
 * Copyright 2014 Mark Crosbie
 */

public class SpaceInvaders extends Thread {

    static GraphicsLCD g = LocalEV3.get().getGraphicsLCD();
    final static int SW = g.getWidth();
    final static int SH = g.getHeight();
    final static int DELAY = 2000;
    final static int LOOPDELAY = 400;
    final static int TITLE_DELAY = 1000;
    final static int GRID_WIDTH = 5;
    final static int GRID_HEIGHT = 3;
    final static int XGAP = 10;
    final static int YGAP = 5;
    final static int GRID_STARTX = 0;
    final static int GRID_STARTY = 20;
    final static String THEMETUNE = "Theme2.wav";
    static boolean which;
    static boolean gameover = false;
    static boolean aliensAtBottom = false;
    static boolean musicPlaying = true;
    public static boolean keyControl = false;	// set to false for joystick control
    
    static Brick brick;
    static EV3ColorSensor sensor;
    static RegulatedMotor joystickMotor = Motor.A;

    static Ship ship;
    static Invaders invaders;
    static Score score;
    static Missiles missiles;
    
    public void run() {
        try {
        	File f = new File(THEMETUNE);
        	while(musicPlaying) {
        		Sound.playSample(f, 100);
        	}
        } catch (Exception e) {
        	Sound.buzz();
        }
    }
    
    static void splash() {
        g.clear();   
         
        g.setFont(Font.getLargeFont());
        g.drawString("Spac3", SW/2, SH/2, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
        Button.waitForAnyPress(TITLE_DELAY);
        g.clear();
        g.drawString("Invad3rs", SW/2, SH/2, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
        Button.waitForAnyPress(TITLE_DELAY);
        g.clear();
        Button.waitForAnyPress(TITLE_DELAY*2);
        g.setFont(Font.getDefaultFont());
        g.clear();
    }

    /**
     * Display a nice Game Over message... loser!
     */
    static void gameOver() {
        g.clear();
        g.setFont(Font.getLargeFont());
        g.drawString("Game Over", SW/2, SH/2, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
        g.drawString("Score: " + score.currentScore(), SW/2, SH/2 + 30, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
        Sound.buzz();
        Button.waitForAnyPress(TITLE_DELAY*4);
    }
    
    static void playAgain() {
        g.clear();
        g.setFont(Font.getLargeFont());
        g.drawString("Play Again?", SW/2, SH/2, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
        g.drawString("Press Fire", SW/2, SH/2 + 30, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
        Sound.buzz();
        Button.waitForAnyPress(TITLE_DELAY*4);    	
    }

    /**
     * Display a You Win message - winner!
     */
    static void youWin() {
        g.clear();
        g.setFont(Font.getLargeFont());
        g.drawString("You Win!", SW/2, SH/2, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
        g.drawString("Score: " + score.currentScore(), SW/2, SH/2 + 30, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
        Sound.beepSequenceUp();
        Button.waitForAnyPress(TITLE_DELAY*4);
    }

    private static void initialise() {
    	        
    	ship = new Ship(g, "ship.lni", SW/2, SH-20, SensorPort.S1, joystickMotor);
    	score = new Score(g, 0, 3, 50, 0, 0);
    	invaders = new Invaders(g, GRID_WIDTH, GRID_HEIGHT, 
    							GRID_STARTX, GRID_STARTY, 
    							XGAP, YGAP, 
    							"invader1.lni",
    							"invader2.lni");    
    	missiles = new Missiles(g);    	
    }
        
	
    private static void waitForFire() {
        g.setFont(Font.getLargeFont());
        g.drawString("Press", SW/2, SH/2, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
        g.drawString("Fire", SW/2, SH/2+50, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
        while(true) {
        	
        }
    }
    
    private static void waitForCoin() {
        g.setFont(Font.getLargeFont());
        g.drawString("Insert", SW/2, SH/2, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
        g.drawString("Coin", SW/2, SH/2+50, GraphicsLCD.BASELINE|GraphicsLCD.HCENTER);
 
	    Motor.B.setSpeed(300);

	    SampleProvider ambient = sensor.getRedMode();
	   	
	    // get the average of the last 5 samples
	    SampleProvider average = new MeanFilter(ambient, 5);
	   
	    // allocate space for the samples
	    float samples[] = new float[average.sampleSize()];

        boolean noCoin = true;
	    while(noCoin && brick.getKey("Escape").isUp()) {
	    	average.fetchSample(samples, 0);
	    	if(samples[0] >= 0.3) {
	    		Sound.playTone(500, 50);
	    		Delay.msDelay(500);
	    		Motor.B.rotate(300);
	    		Delay.msDelay(500);
	    		Motor.B.rotate(-300);
	    		noCoin = false;
	    	} else {
		    	Delay.msDelay(50);
	    	}
	    }
	    
	    sensor.close();
    }
    
    public static void main(String[] options) throws Exception {
    	
		// Steps to initialize a sensor - done once so do it here
	    brick = BrickFinder.getDefault();
	    Port s4 = brick.getPort("S4");
	    sensor = new EV3ColorSensor(s4);

        LCD.clear();       
        initialise(); 
        
        SpaceInvaders game = new SpaceInvaders();
        
        musicPlaying = true;
        game.start();   
        game.splash();    
        game.waitForCoin();
        musicPlaying = false;
        LCD.clear();
  
	    g.setAutoRefresh(false);

        invaders.render();
 
        score.setDaemon(true);
        score.start();
        
        ship.setDaemon(true);
        ship.start();
        
        invaders.setDaemon(true);
        invaders.start();
        
        boolean exitPressed = false;

        // main loop
        while(!aliensAtBottom && !exitPressed && invaders.invadersLeft() && score.getLives() > 0) {        	
        	 
        	g.refresh();
        	if(Button.ESCAPE.isDown()) {
        		exitPressed = true;
        	}  
        	Delay.msDelay(100);
        }
       
        // did the player press the ESCAPE button?
        if(exitPressed) {
        	System.exit(0);
        }
        
        score.stopThread();
        ship.stopRunning();
        invaders.stopRunning();
        
        Delay.msDelay(500);
        g.clear();
        
        // did we win or lose?
        if(score.getLives() == 0 || aliensAtBottom) {
        	gameOver();
        } else {
        	youWin();
        }
        System.exit(0);
    }
}


package soundmachinecontroller;

import netP5.NetAddress;
import ddf.minim.AudioSample;
import ddf.minim.Minim;
import processing.core.*;
import oscP5.*;

public class PlayerThread extends Thread {

	// Instrument codes that can be received from a SoundMachine unit
	private int BASSDRUM = 6; 	// WHITE
	private int SNARE = 0; 		// RED
	private int NOSIGNAL = 7;	// BLACK

	private AudioSample bassdrum, snare;
	private Minim minim;
	private PApplet myParent;
	private ReaderThread reader;
	private Timer timer;
	
	// If using the Minim built-in library
	private final String BASSDRUM_SOUND = "26888__vexst__kick-4.wav";
	//final String BASSDRUM_SOUND = "hardbassdrum002.wav";
	private final String SNARE_SOUND = "26900__vexst__snare-1.wav";

	// If using the OSC connection to Ableton Live
	private NetAddress myRemoteLocation;
	private final String MIDI_DEVICE = "localhost";	// defines the location of the remote MIDI device
										// replace with an IP address/hostname of a remote host

	private OscP5 oscP5;

	private boolean running = false;
	private int wait;
	
	// track when a drum has been struck for the keyboard input
	public boolean drum1struck;
	public boolean drum2struck;

	private final boolean useMinim = false;
	private final boolean useOSC = true;
	
	/**
	 * Initialise an instance of the PlayerThread responsible for playing the instruments
	 * @param parent The PApplet instance of the parent
	 * @param r An instance of a ReaderThread that has been initialised.
	 * @param playInterval How frequently a note should be played - in ms
	 */
	public PlayerThread(PApplet parent, ReaderThread r, int playInterval) {
		
		myParent = parent;
		reader = r;
		wait = playInterval;
		running = false;

		// A timer will run so we know when to play the beat of an instrument
		timer = new Timer(playInterval);

		// Load the sound files as samples so that they can be triggered
		// using Minim and can be re-started if already playing
		if(useMinim) {
			try {
				minim = new Minim(parent);
				bassdrum = minim.loadSample(BASSDRUM_SOUND, 1024); 
				snare = minim.loadSample(SNARE_SOUND, 1024);
			} catch(Exception e) {
				System.out.println("EXCEPTION initialising minim: " + e);
				System.out.println("NOTE: Have you coped the sound files into the data folder?");
				System.out.println("Required: " + BASSDRUM_SOUND);
				System.out.println("Required: " + SNARE_SOUND);
				System.exit(1);
			}	
		} else {
			try {
				System.out.println("PlayerThread: Creating OSC connection to " + MIDI_DEVICE);
		  	
				// Create the connection to the MIDI enabled device via OSC
				oscP5 = new OscP5(this,9001);
				myRemoteLocation = new NetAddress(MIDI_DEVICE,9000);
			} catch(Exception e) {
				System.out.println("EXCEPTION initialising OSC: " + e);
				System.exit(1);				
			}
		}
	}
	
	  /**
	   * Start the Player thread running
	   */
	  public void start() {
	    // Set running equal to true
	    running = true;

	    System.out.println("Starting player thread (will execute every " + wait + " milliseconds.)"); 
	    
	    timer.start();
	    
	    // Do whatever start does in Thread, don't forget this!
	    super.start();
	  }
	  
	  /**
	   * Pause the current player thread. The thread does not die, it just will not play any instruments
	   */
	  public void pause() {
		  running = false;
	  }
	  
	  /**
	   * Resume the current player thread. Starts playing instruments again
	   */
	  public void restart() {
		  System.out.println("Restarting player thread (will execute every " + wait + " milliseconds.)"); 
		  timer.start(wait);
		  running = true;	  
	  }

	  /**
	   * Shut down processing elements
	   * REQUIRED so that Minim can close up shop
	   **/
	public void quit() {
		
		System.out.println("PlayerThread stop cleaning up...");
	
		running = false;
		
		// always close Minim audio classes when you are done with them
		bassdrum.close();
		snare.close();
		minim.stop();	
		
		// In case the thread is waiting
		interrupt();
	}

	
	/**
	 * Check to see if the timer has expired, and if so, see if there is a sample available
	 * for us to play.
	 */
	public void run() {
		
		while(true) {
			if(running && timer.isFinished()) {
				if(reader.available()) {
					int instrument = reader.getData();
					
					if(instrument == BASSDRUM) {
						System.out.println("PlayerThread triggering BASSDRUM");
						if(useMinim) {
							bassdrum.trigger();
						} else {
							OscMessage myMessage;
						    myMessage = new OscMessage("/live/play/clip");
						    myMessage.add(0); // track
						    myMessage.add(0); // clip
						    oscP5.send(myMessage, myRemoteLocation); 
						    System.out.println("Sent /live/play/clip ");
						}
						drum1struck = true;
					} 
					
					if(instrument == SNARE) {
						System.out.println("PlayerThread triggering SNARE");
						if(useMinim) {
							snare.trigger();
						} else {
							OscMessage myMessage;
						    myMessage = new OscMessage("/live/play/clip");
						    myMessage.add(1); // track
						    myMessage.add(0); // clip
						    oscP5.send(myMessage, myRemoteLocation); 
						    System.out.println("Sent /live/play/clip ");
						}
						drum2struck = true;
					}
				}
				
				// re-start a new timer
				timer.start();
			}
		}
	}
}

package soundmachinecontroller;

import processing.core.PApplet;
import java.io.*;

import controlP5.*;
import lejos.pc.comm.*;
import arb.soundcipher.*;


/**
 * Controller for the BlockSequencer project
 * 
 * @author mcrosbie
 * 24 August 2012
 */

public class BlockSequencerController extends PApplet {

	private static final long serialVersionUID = 1L;
	
	// Processing Packages
	NXTConnector conn;
	DataOutputStream dos;
	DataInputStream dis;	
	ControlP5 gui;
	SoundCipher sc;

	// Global constants
	int numTracks = 4;			// Number of tracks on the plate
	int numNotes = 8;			// How many notes per track, either 8 or 16
	boolean running = false;

	// GUI placement constants
	int border = 60;
	int noteWidth = 28;
	int noteHeight = 30;
	int startofTracks = 50;

	// USB names of SoundMachine NXTs
	String NAMEOFSM1 = "SOUND1";
	String NAMEOFSM2 = "SOUND2";
	String NAMEOFSM3 = "SOUND3";
	String NAMEOFSM4 = "SOUND4";
	
	// Timing constants
	public int bpm;
	int tempo; // how long am eigth note is in milliseconds
	int clock; // the timer for moving from note to note
	int beat; // which beat we're on
	boolean beatTriggered; // only trigger each beat once
	boolean playing;	// are we playing or paused
	
	// Represents the notes encoded in each step in the sequence, numTracks * numNotes IN SIZE
	// This is what is read in from the LEGO sensors
	int notes[][];
	
	// Map each LEGO colour note onto a MIDI note
	// We map per track as each track maps a colour onto a different instrument for maximum
	// flexibility
	int midiMap[][] = {	
			{36, 35, 0, 38, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			{38, 48, 0, 37, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			{36, 37, 0, 40, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			{36, 37, 0, 38, 0, 0, 0, 0, 0, 0, 0, 0, 0}	// DRUM
			};
	
	// Human readable form of the LEGO colours
	private static String colorNames[] = {"Red", "Green", "Blue", "Yellow",
        "Magenta", "Orange", "White", "Black", "Pink",
        "Grey", "Light Grey", "Dark Grey", "Cyan", "None"};
	int numColours;	// how many colours we will recognise using the LEGO colour sensor

	// LEGO Colour values returned from the colour sensor
	public static final int RED = 0;
	public static final int GREEN = 1;
	public static final int BLUE = 2;
	public static final int YELLOW = 3;
	public static final int MAGENTA = 4;
	public static final int ORANGE = 5;
	public static final int WHITE = 6;
	public static final int BLACK = 7;
	public static final int PINK = 8;
	public static final int GRAY = 9;
	public static final int LIGHT_GRAY = 10;
	public static final int DARK_GRAY = 11;
	public static final int CYAN = 12;
	public static final int NONE = -1;
 		
	int lastNotePlayed;
	
	// record the number of the last SoundMachine read from, or 0 if none has been read from yet
	int lastSMRead;
	
	/**
     * Convert a colour value from the NXT colour sensor to a printable string
     * @param c Colour value
     * @return String name of the colour
     */
    public String getColourName(int c) {
    	
    	if( (c < 0) || (c > colorNames.length) )
    		return "???";
    
    	return colorNames[c];
    }

    /**
     * Draw the play/paused icon depending on the state
     */
    public void drawPlayPaused() {
    	
    	float rw = textWidth("PAUSE");
		float lineSize = g.textSize+textDescent();
		fill(0);
		noStroke();
		rect(width-5-rw, 5, rw, lineSize);
	    fill(255, 255, 0);
		textAlign(RIGHT, TOP);
   	
		if(playing) {
     		text("PLAY", width-5, 5);

    	} else {
    		text("PAUSE", width-5, 5);
    		
    	}
    }
    
    public void showNXTs() {
    	
    	NXTInfo[] n = conn.search(null, null, NXTCommFactory.ALL_PROTOCOLS);
    	
    	println("Found " + n.length + " NXTs");
    	for(int i=0; i < n.length; i++) {
    		println("NXT " + n[i].deviceAddress + "  " + n[i].name);
    	}
    }
    
   
	public void setup() {
		println("Block Sequencer Controller using USB to NXT...");
		size(400, 300);
			
		// SoundCipher provides the MIDI interface bridge
		sc = new SoundCipher(this);
		SoundCipher.getMidiDeviceInfo();        
		sc.setMidiDeviceOutput(1);

		// NXTConnector reads data from the USB interface
		conn = new NXTConnector();		

		showNXTs();
		
		// Holds the notes read in from the scanning NXT
		notes = new int[numTracks][numNotes];

		// Create the GUI
		gui = new ControlP5(this);
		gui.setColorForeground(color(128, 200));
		gui.setColorActive(color(255, 0, 0, 200));

		gui.addTextlabel("track1")
                 .setText("Track 1")
                 .setPosition(10,startofTracks + noteHeight/2)
                 ;

		gui.addTextlabel("track2")
                .setText("Track 2")
                .setPosition(10,startofTracks*2 + noteHeight/2)
                ;

		gui.addTextlabel("track3")
                .setText("Track 3")
                .setPosition(10,startofTracks*3 + noteHeight/2)
                ;

		gui.addTextlabel("track4")
        .setText("Track 4")
        .setPosition(10,startofTracks*4 + noteHeight/2)
        ;
				
		bpm = 120; 
		tempo = 250;  
		clock = millis();  
		beat = 0; 
		beatTriggered = false; 
		playing = true;
		
		textFont(createFont("Arial", 16));
		println("Setup complete");
		
		clearNotes();
		
		numColours = colorNames.length;
		
		displayStatusMessage("Ready");
	}

	/** 
	 * Clear out the notes
	 * 
	 */
	public void clearNotes() {
		// Set all notes to empty
		for(int i=0 ;i < numTracks; i++) {
			for(int j = 0; j < numNotes; j++) {
				notes[i][j]= NONE;
			}
		}
		
		lastSMRead = 0;
	}
	
	/**
	 * Read a new set of notes from the NXT and updates the notes array
	 * @param soundUnit  Name of the SoundMachine unit to connect to over USB
	 * @return True if the notes were read, else false
	 */
	public boolean readNewNotes(String soundUnit) {
		int x = 0;
		boolean ret;

		println("Connecting to "+soundUnit+" over USB...");
		displayStatusMessage("Connect to "+ soundUnit);
		
		// Connect to any NXT over USB
		boolean connected = conn.connectTo("usb://"+soundUnit);
	
		if (!connected) {
			System.err.println("Failed to connect to "+soundUnit);
			displayStatusMessage("Failed to connect");
			return false;
		}
		
		println("Connected successfully to "+soundUnit);
		displayStatusMessage("Connect OK");
		
		dos = new DataOutputStream(conn.getOutputStream());
		dis = new DataInputStream(conn.getInputStream());
	
		try {
			for(int i=0;i < numTracks;i++) {
				for(int j=0; j < numNotes; j++) {
		        	x = dis.readInt();
		 	        print(x + "," + getColourName(x) + "\t");
		 	        notes[i][j] = x;
				}
				println();
			}
			beat = 0; // start playing from the beginning again
			
			// record these notes in a log file for historical use
			saveNotesToFile("notedata_"+System.currentTimeMillis()+".dat");
	    } catch (IOException ioe) {
	    	println("IO Exception reading reply");
	    	ret = false;
	    }            
				
		try {
			dis.close();
			dos.close();
			println("Closed data streams");
		} catch (IOException ioe) {
			println("IO Exception Closing connection");
		}
		
		try {
			conn.close();
			println("Closed connection");
		} catch (IOException ioe) {
			println("IO Exception Closing connection");
		}
		
		beat = 0;
		clearStatusMessage();
		ret = true;
		return ret;
	}
	
	/**
	 * Save the notes currently in the notes array to be later reloaded
	 */
	public void saveNotes() {
		
		println("saveNotes");
		
		playing = false;
		selectOutput("Save notes to...", "saveNotesToFile");
	}

	/**
	 * Write notes out to a file
	 */
	public void saveNotesToFile(String filename) {
		if(filename != null) {
			println("Saving note data to " + filename);
			PrintWriter output = createWriter(filename);
			output.print(numTracks);
			output.print(numNotes);
			for(int i=0; i < numTracks; i++) {
				for(int j=0; j < numNotes; j++) {
					output.print(notes[i][j]);
				}
			}
			output.flush();
			output.close();
		}
	}
	
	/**
	 * Load notes previously saved so we can replay them
	 */
	public void loadNotes() {	
		playing = false;
		selectInput("Load notes from...", "loadNotesFromFile");
	}
	
	public void loadNotesFromFile(String filename) {
		if(filename != null) {
			try {
				BufferedReader input = createReader(filename);
				if(input != null) {
					int tracks = input.read() - 48;
					int n = input.read() - 48;
					
					if( (tracks != numTracks) || (n != numNotes)) {
						println("Mismatch - cannot read data from file " + filename);
					} else {
						for(int i=0; i < numTracks; i++) {
							for(int j=0; j < numNotes; j++) {
								notes[i][j] = input.read() - 48;
							}
						}
					}
					input.close();
				}
			} catch(Exception e) {
				println("EXCEPTION reading: " + e.getMessage());
				clearNotes();
			}
		}
	}
	
	public void keyPressed() {	
		
		switch(key) {
		
		case '1':	if(readNewNotes(NAMEOFSM1))
						lastSMRead = 1;
					else
						lastSMRead = 0;
					break;

		case '2':	if(readNewNotes(NAMEOFSM2))
						lastSMRead = 2;
					else	
						lastSMRead = 0;
					break;

		case '3':	if(readNewNotes(NAMEOFSM3))
						lastSMRead = 3;
					else
						lastSMRead = 0;
					break;

		case '4':	if(readNewNotes(NAMEOFSM4))
						lastSMRead = 4;
					else
						lastSMRead = 0;
					break;

		case '+':	tempo *= 2; bpm /= 2;
					break;
					
		case '-':	tempo /= 2; bpm *= 2;
					break;
					
		case 'r':	tempo = 250; bpm = 120;
					break;
					
		case ' ':	playing = !playing;
					break;
					
		case 'x':	System.exit(1);
		
		case 's':	saveNotes();
					break;
					
		case 'l':	loadNotes();
					break;
		
		case 'c':	clearNotes();
					break;
		}
	}
	
	/**
	 * Convert a note value to a RGB colour fill for the note display
	 * 
	 * @param note
	 */
	public void noteToColourFill(int note) {
		
		switch(note) {
		
		case RED: fill(200, 0, 0); break;
		case GREEN: fill(0, 200, 0); break;
		case BLUE: fill(0, 0, 200); break;
		case YELLOW: fill(255, 237, 28); break;
		case ORANGE: fill(255, 112, 28); break;
		
		default: fill(127); break;
		}
	}
	
	/**
	 * Print a status message in the bottom line of the GUI for a pre-determined amount of time
	 */
	public void displayStatusMessage(String statusMessage) {
		
		// display the message
    	float rw = textWidth(statusMessage);
		float lineSize = g.textSize+textDescent();
		
		fill(0);
		noStroke();
		rect(5, height-5-lineSize, rw, lineSize);
	    
		fill(255, 255, 0);
		//textAlign(RIGHT, TOP);
 		text(statusMessage, 5, height-5-lineSize);
	}
	
	/**
	 * Set the status message to display
	 * 
	 */
	public void clearStatusMessage() {
	
		fill(0);
		noStroke();
		float lineSize = g.textSize+textDescent();
		rect(5, height-5-lineSize, width-5, lineSize);		
	}

	/**
	 * Update the note display of what was read in.
	 */
	public void draw() {
  
		background(0);
		fill(255);

		stroke(127);
		for(int i = 0; i < numTracks; i++) {
			for(int j=0; j < numNotes; j++) {
				noteToColourFill(notes[i][j]);
				rect(border+j*38, 50+i*50, 28, 30);
			}
		}
		
		displayStatusMessage("Ready");

		drawPlayPaused();
		
		fill(0);
		noStroke();
   		float rw = textWidth("BPM: "+bpm);
		float lineSize = g.textSize+textDescent();
		rect(10, 5, rw, lineSize);
	    fill(255, 255, 0);
		textAlign(LEFT, TOP);
		text("BPM: " + bpm, 10, 5);
		
		if(lastSMRead != 0) {
			fill(0);
			noStroke();
	   		rw = textWidth("SM: "+ lastSMRead);
			lineSize = g.textSize+textDescent();
			rect(10 + rw*4, 5, rw, lineSize);
		    fill(255, 255, 0);
			textAlign(LEFT, TOP);
			text("SM: " + lastSMRead, 10 + rw * 4, 5);			
		}
		
		if(!playing) {
			return;
		}
		
		if ( millis() - clock >= tempo ) {
			clock = millis();
			beat = (beat+1) % numNotes;
			beatTriggered = false;
		}
  
		if ( !beatTriggered ) {
			for(int track=0; track < numTracks; track++) {
				if(notes[track][beat] != NONE) {
					//println("Triggering track " + track + " beat " + beat);
					int note = notes[track][beat];
				    sc.sendMidi(sc.NOTE_ON, track, midiMap[track][note] , 100);
				    //println("NOTE ON " + midiMap[track][note]);
				    sc.sendMidi(sc.NOTE_OFF, track, midiMap[track][note], 0);
				    //println("NOTE OFF " + midiMap[track][note]);
				    
				}
			}
		    beatTriggered = true;
		}
  
		stroke(128);
		if ( beat % 4 == 0 ) {
			fill(200, 0, 0);
		} else {
			fill(0, 200, 0);
		}
  
		// beat marker    
		rect(border+beat*38, 35, 28, 9); 
  
		gui.draw();	
	}
	
	// to enable export to an application
	  public static void main(String args[])
	    {
	      PApplet.main(new String[] { soundmachinecontroller.BlockSequencerController.class.getName() });
	    }

}

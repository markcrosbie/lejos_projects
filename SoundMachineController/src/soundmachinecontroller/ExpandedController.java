package soundmachinecontroller;

import processing.core.PApplet;
import java.io.*;

import controlP5.*;
import lejos.pc.comm.*;
import arb.soundcipher.*;


/**
 * Expanded Controller for the SoundMachine project. Uses the same note layout as the basic controller, but adds the concept
 * of quadrants that are playing. Notes can be read into one of four quadrants
 * Arranged in a 2x2 square
 * 
 * @author mcrosbie
 * 20 Sept 2012
 */

public class ExpandedController extends PApplet {

	private static final long serialVersionUID = 1L;
	
	// Processing Packages
	NXTConnector conn;
	DataOutputStream dos;
	DataInputStream dis;	
	ControlP5 gui;
	SoundCipher sc;

	// Global constants
	int bankXcount = 2;		// how many banks of 8 notes are in the X direction
	int bankYcount = 2;		// how many banks of 8 notes are in the Y direction
	
	// Each bank consists of 4 tracks of 8 notes each
	int plateTracks = 4;			// Number of tracks on the plate
	int plateNotes = 8;			// How many notes per track, either 8 or 16
	
	int numTracks = plateTracks*bankYcount;		// Total number of tracks in the GUI
	int numNotes = plateNotes*bankXcount;		// Total number of notes in the GUI
	boolean running = false;

	// GUI placement constants
	int border = 60;
	int noteWidth = 38;
	int noteHeight = 30;
	int startofTracks = 50;

	int startOfHelp = height - 100;
	
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
	
	// Represents the hold time for each note. The hold time is encoded in the MIDI code and
	// is in 100s of ms. 
	int hold[][];
	
	// Map each LEGO colour note onto a MIDI note
	// We map per track as each track maps a colour onto a different instrument for maximum
	// flexibility. The midiMap corresponds to the track layout in the quadrants
	int midiMap[][] = {	
			{36, 35, 0, 38, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			{38, 48, 0, 37, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			{36, 42, 0, 40, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			{36, 45, 0, 38, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			
			{848, 852, 0, 855, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			{860, 864, 0, 867, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			{836, 840, 0, 843, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			{848, 852, 0, 855, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			
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
	
	int bankToReadInto;		// which bank (0..3) will the next notes be read into
	
	int COMMAND = 157;		// keyCode for Cmd key on Mac
	
	boolean commandPressed;
	boolean ctrlPressed;
	boolean altPressed;
	
	boolean saveNotesRequested;
	
	NXTInfo n[];
	
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
    	    	
    	try {
			NXTComm nxtComm = NXTCommFactory.createNXTComm(NXTCommFactory.USB);
			n = nxtComm.search(null);
    	} catch(Exception e) {
    		System.err.println("Exception: Failed to search for NXTs " + e.getMessage());
    		return;
    	}
    	//NXTInfo[] n = conn.search(null, null, NXTCommFactory.ALL_PROTOCOLS);
    	
    	println("Found " + n.length + " NXTs");
    	for(int i=0; i < n.length; i++) {
    		println("NXT " + n[i].deviceAddress + "  " + n[i].name);
    	}
    }
   
	public void setup() {
		println("Block Sequencer Controller using USB to NXT...");
		
		size(800, 600);
			
		// SoundCipher provides the MIDI interface bridge
		sc = new SoundCipher(this);
		SoundCipher.getMidiDeviceInfo();        
		sc.setMidiDeviceOutput(1);

		// NXTConnector reads data from the USB interface
		conn = new NXTConnector();		

		// Display NXTs attached
		showNXTs();
		
		// Holds the notes read in from the scanning NXT
		notes = new int[numTracks][numNotes];

		// Hold time for each notes
		hold = new int[numTracks][numNotes];
		
		// Create the GUI
		gui = new ControlP5(this);
		gui.setColorForeground(color(128, 200));
		gui.setColorActive(color(255, 0, 0, 200));

		int line=0;
		for(int i= 0; i < numTracks; i++) {
			gui.addTextlabel("track"+i)
            .setText("Track "+ (i+1))
            .setPosition(10,startofTracks + startofTracks*line + noteHeight/2)
            ;			
			line++;
		}
				
		bpm = 120; 
		tempo = 250;  
		clock = millis();  
		beat = 0; 
		beatTriggered = false; 
		playing = true;
		
		textFont(createFont("Arial", 16));
		println("Setup complete");
		
		clearAllNotes();
		
		numColours = colorNames.length;
		
		displayStatusMessage("Ready");
	}

	/** 
	 * Clear out all of the notes
	 * 
	 */
	public void clearAllNotes() {
		// Set all notes to empty
		for(int i=0 ;i < numTracks; i++) {
			for(int j = 0; j < numNotes; j++) {
				notes[i][j]= NONE;
			}
		}
	}
	
	/**
	 * Clear a specific bank of notes
	 * @param bank The bank to clear 0..3
	 */
	public void clearBank(int bank) {
				
		// Read notes into the bank selected
		int noteStart = (bank % bankXcount) * plateNotes;
		int trackStart = (bank / bankYcount) * plateTracks;
				
		for(int i=0; i < plateTracks;i++) {
			for(int j=0; j < plateNotes; j++) {
	 	        notes[trackStart + i][noteStart + j] = NONE;
			}
		}		
	}
	
	/**
	 * Read a new set of notes from the NXT and updates the notes array
	 * @param soundUnit  Name of the SoundMachine unit to connect to over USB
	 */
	public void readNewNotes(String soundUnit) {
		int x = 0;

		println("Connecting to "+soundUnit+" over USB...");
		displayStatusMessage("Connect to "+ soundUnit);
		
		// Connect to any NXT over USB
		boolean connected = conn.connectTo("usb://"+soundUnit);

		if (!connected) {
			System.err.println("Failed to connect to "+soundUnit);
			displayStatusMessage("Failed to connect");
			return;
		}
			
		println("Connected successfully to "+soundUnit);
		displayStatusMessage("Connect OK");
			
		dos = new DataOutputStream(conn.getOutputStream());
		dis = new DataInputStream(conn.getInputStream());
		
		// Read notes into the bank selected
		int noteStart = (bankToReadInto % bankXcount) * plateNotes;
		int trackStart = (bankToReadInto / bankYcount) * plateTracks;
		
		println("noteStart = " + noteStart + " trackStart = " + trackStart);
		
		try {
			for(int i=0; i < plateTracks;i++) {
				for(int j=0; j < plateNotes; j++) {
		        	x = dis.readInt();
		 	        print(x + "," + getColourName(x) + "\t");
		 	        notes[trackStart + i][noteStart + j] = x;
				}
				println();
			}
			beat = 0; // start playing from the beginning again
			
			// record these notes in a log file for historical use
			saveNotesToFile(new File("notedata_"+System.currentTimeMillis()+".dat"));
	    } catch (IOException ioe) {
	    	println("IO Exception reading reply");
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
	}
	
	/**
	 * Save the notes currently in the notes array to be later reloaded
	 */
	public void saveNotes() {
		
		println("saveNotes");
		
		playing = false;
		
		try {
			 selectOutput("Save notes to...", "saveNotesToFile");
		} catch (Exception e) {
			System.err.println("Error choosing filename: " + e.getMessage());
		}
		
	}

	/**
	 * Write notes out to a file
	 */
	public void saveNotesToFile(File f) {
		if(f != null) {
			
			String filename = f.getAbsolutePath();
			println("Saving note data to " + filename);
	
			String[] lines = new String[numTracks + 2]; // one line for each track, plus one each for track/note count
			lines[0] = numTracks+"";
			lines[1] = numNotes+"";
			for (int i = 0; i < numTracks; i++) {
				lines[i+2] = "";
				for(int j=0; j < numNotes; j++) {
					lines[i+2] = lines[i+2] + notes[i][j] + "\t";
				}
			}
			saveStrings(filename, lines);
		} else {
			println("No filename chosen, cancelling");
		}
	}
	
	/**
	 * Load notes previously saved so we can replay them
	 */
	public void loadNotes() {
				
		playing = false;
		selectInput("Load notes from...", "loadNotesFromFile");
	}
	
	public void loadNotesFromFile(File f) {
		if(f != null) {
			String filename = f.getAbsolutePath();
			try {		
				String lines[] = loadStrings(filename);
				if(lines.length < (numTracks+2)) {
					println("Error: not enough lines in input file: " + lines.length);
					return;
				}
				int tracks = Integer.parseInt(lines[0]);
				int n = Integer.parseInt(lines[1]);
				
				println("Parsing " + tracks + " tracks and " + n + " notes from " +filename);
				
				for(int i=0; i < tracks; i++) {
					String pieces[] = split(lines[2+i], '\t'); // tab separated file
					if(pieces.length != (n+1)) { // one extra tab at end of the line
						println("Error: line " + i + " length mismatch: " + pieces.length);
						return;
					}
					for(int j=0; j < n; j++) {
						notes[i][j] = Integer.parseInt(pieces[j]);
					}
				}				
			} catch(Exception e) {
				println("EXCEPTION reading: " + e.getMessage());
				clearAllNotes();
			}
		}
	}
	
	/**
	 * Randomize the notes in the array
	 */
	public void randomizeNotes() {
	
		for(int i=0; i < numTracks; i++) {
			for(int j=0; j < numNotes; j++) {
				notes[i][j] = (int)random(-1,5);
			}
		}
	}
	
	public void keyPressed() {	
				
		if(key == CODED) {
			// Detect Cmd-keypresses
			if(keyCode == COMMAND) {
				commandPressed = true;
			}
			
			if(keyCode == ALT) {
				altPressed = true;
			}
			
			if(keyCode == CONTROL) {
				ctrlPressed= true;
			}
			return;
		} 
		
		if(commandPressed) {
			
			int x = key - '1';			
			if(x < 0) x = 0;
			if(x >= bankXcount * bankYcount) {
				println("Unknown bank " + x);
				commandPressed = false;
				return;
			}
			
			bankToReadInto = x;
		
			// draw a square around the bank we've select to read into
			println("Read notes into bank " + bankToReadInto);
			commandPressed = false;
			
			return;
		}
		
		if(ctrlPressed) {
			int x = key - '1';	
			if(x < 0) x = 0;
			if(x >= bankXcount * bankYcount) {
				println("Unknown bank " + x);
				ctrlPressed = false;
				return;						
			}
			
			println("Clearing notes from bank " + x);
			clearBank(x);

			ctrlPressed = false;
			
			return;
			
		}
		
		switch(key) {
		
		case '1':	readNewNotes(NAMEOFSM1);
					break;

		case '2':	readNewNotes(NAMEOFSM2);
					break;
					
		case '3':	readNewNotes(NAMEOFSM3);
					break;

		case '4':	readNewNotes(NAMEOFSM4);
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
		
		case 's':	saveNotesRequested = true;
					break;
					
		case 'l':	loadNotes();
					break;
		
		case 'c':	clearAllNotes();
					midiOff();
					break;
							
		case 'n':	randomizeNotes();
					break;
					
		case 'm':	midiOff();
					break;
		}
	}
	
	/**
	 * Send MIDI OFF to all channels
	 */
	void midiOff() {
		println("Sending MIDI OFF to all tracks");
		for(int i=0; i < numTracks; i++) {
			for(int j=0; j < numNotes; j++) {
				int note = notes[i][j];
				if(note != NONE) {
					int midiMessage = midiMap[i][note];
					sc.sendMidi(sc.NOTE_OFF, i, midiMessage, 0);
				}
			}
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
		case PINK: fill(244, 39, 255); break;
		case GRAY: fill(127); break;
		
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
	 * Draw a help message in the bottom of the screen with guidance on what keys to press
	 */
	public void drawHelp() {
		
		String helpmsg1 = "Cmd-1..4=Select bank   x=Exit   c=Clear   s=Save Notes   l=Load Notes   1,2,3=Read from SM";
		String helpmsg2 = "Ctrl-1..4=Clear bank   -=Speed Up   +=Slow Down   r=Reset tempo   n=Random Notes";
		
		noStroke();
   		float rw = textWidth(helpmsg1);
		float lineSize = g.textSize+textDescent();
	    fill(255, 255, 0);
		textAlign(LEFT, TOP);
		text(helpmsg1, 5, height-100);
		text(helpmsg2, 5, height+lineSize-100);
	}
	
	/**
	 * Update the note display of what was read in.
	 */
	public void draw() {
  
		background(0);

		// Draw lightly coloured squares under each bank of notes
		stroke(255,0,0);
		int q = 0;
		for(int i=0; i < bankYcount; i++) {
			for(int j=0; j < bankXcount; j++) {
				if(q == bankToReadInto) {
					fill(255,255,0,75);
					stroke(255);
				} else {
					fill(0);
					stroke(255,0,0);
				}
				rect(border-5 + j*(plateNotes*38), startofTracks-10 + i*(plateTracks*50), 
						plateNotes*38,  plateTracks*50 );
				q++;
			}
		}

		stroke(127);
		int line=0;
		int col=0;
		for(int i = 0; i < numTracks; i++) {
			col=0;
			for(int j=0; j < numNotes; j++) {
				noteToColourFill(notes[i][j]);
				rect(border+col*38, startofTracks+line*50, 28, 30);
				col++;
			}
			line++;
		}
		
		displayStatusMessage("Ready");

		drawPlayPaused();
		
		drawHelp();
		
		fill(0);
		noStroke();
   		float rw = textWidth("BPM: "+bpm);
		float lineSize = g.textSize+textDescent();
		rect(10, 5, rw, lineSize);
	    fill(255, 255, 0);
		textAlign(LEFT, TOP);
		text("BPM: " + bpm, 10, 5);
		
		if(saveNotesRequested) {
			saveNotes();
			saveNotesRequested = false;
		}
		
		if(!playing) {
			return;
		}
		
		if ( millis() - clock >= tempo ) {
			clock = millis();
			beat = (beat+1) % numNotes;
			beatTriggered = false;
		}
  
		boolean sendMIDIon, sendMIDIoff;
		int midiMessage;
				
		if ( !beatTriggered ) {
			// first process the holds for all tracks and beats
			for(int track = 0; track < numTracks; track++) {
				for(int beat = 0; beat < numNotes; beat++) {
					// handle the hold counters
					// see if we've reached the end point for holding this note
					if(hold[track][beat] > 1) {
						hold[track][beat]--; // decrement the beat counter
						//println("Decrementing hold["+track+"]["+beat+"] = " + hold[track][beat]);
					} else if(hold[track][beat] == 1) {
						int note = notes[track][beat];
						if(note != NONE) {
							midiMessage = midiMap[track][note];
							if(midiMessage != NONE) {
								if(midiMessage > 100) {
									midiMessage = midiMessage % 100;
								}
	
								//println("Time: " + millis() + " Stopping note "+midiMessage+" at ["+track+"]["+beat+"]");
								sc.sendMidi(sc.NOTE_OFF, track, midiMessage, 0);
								hold[track][beat] = 0;
							}
						}
					} 	
				}
			}
				
			// now process the notes
			for(int track=0; track < numTracks; track++) {
				int note = notes[track][beat];
				int beatsToHold = 0;
				
				if(note != NONE) {
					midiMessage = midiMap[track][note];
					// a MIDI code > 100 means a hold time is encoded in the note so set
					// the corresponding entry in the hold array if it is not
					// already set. This allows us to "hold" a synth key down for a given
					// time and then stop it
					if(midiMessage > 100) {
						beatsToHold = midiMessage / 100; // how many beats to hold for
						midiMessage = midiMessage % 100;
					}
					
					// by default we send a MIDI on followed by off message to trigger
					// this works well for drum machine beats, but not good for synths that
					// need a hold time. Logic for synth delays is below
					sendMIDIon = sendMIDIoff = true;
					
					if(beatsToHold > 0) {
						// check if we are already holding a note for this slot
						// if no note is being held in this slot then enter a hold
						// effectively enter the time is ms the note should end
						// if a note is being held already then we have to wait until
						// it expires before we can hold a new note
						if(hold[track][beat] == 0) {
							hold[track][beat] = beatsToHold;
							//println("Time: " + millis() + " Holding note " + midiMessage + " at ["+track+"]["+beat+"] for " + hold[track][beat] + " beats");
							sendMIDIon = true;
							sendMIDIoff = false;
						} 
					} 
					
					if(sendMIDIon) {
						sc.sendMidi(sc.NOTE_ON, track, midiMessage , 100);
					}

					if(sendMIDIoff) {
						sc.sendMidi(sc.NOTE_OFF, track, midiMessage, 0);
					}
				}
			}
			
			// flag that we've triggered a beat
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
  
		//gui.draw();	
	}
	
	// to enable export to an application
	  public static void main(String args[])
	    {
	      PApplet.main(new String[] { soundmachinecontroller.BlockSequencerController.class.getName() });
	    }

}

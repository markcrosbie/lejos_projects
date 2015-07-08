package soundmachinecontroller;

import java.io.*;

import processing.core.PApplet;

public class ReaderThread extends Thread {
	private boolean running;           // Is the thread running?  Yes or no?
	private int wait;                  // How many milliseconds should we wait in between executions?
	private InputStream in;
	private int count;                 // counter
	private boolean available;		// is data available?
	private byte CMD_TIMECODE = 2;	// Send the global timecode
	private int data;
	
	/**
	 * Initialise a reader thread that reads data from the given InputStream every w milliseconds
	 * @param w Delay between read read of the InputStream
	 * @param i InputStream to read - must be open already!
	 */
	public ReaderThread(int w, InputStream i) {
	  wait = w;
	  running = false;
	  in = i;
	  count = 0;
	}
	  
	  /**
	   * Start running the thread
	   */
	  public void start() {
	    // Set running equal to true
	    running = true;

	    System.out.println("Starting reader thread (will execute every " + wait + " milliseconds.)"); 
	    
	    // Do whatever start does in Thread, don't forget this!
	    super.start();
	  }
	
		private int readByte0() throws IOException
		{
			int ch = in.read();
			if (ch < 0)
				throw new EOFException();
			//actually, InputStream.read() should always return values from 0 to 255
			return ch & 0xFF;
		}
		
		public final int readInt() throws IOException 
		{
			int x = readByte0();
			x = (x << 8) | readByte0();
			x = (x << 8) | readByte0();
			x = (x << 8) | readByte0();
			return x;
		}
		
		public final byte readByte() throws IOException
		{
			return (byte)readByte0();
		}

	  /**
	   * 
	   */
		public void run() {
			System.out.println("Reader thread is starting");
			while (running) {
				count++;
				try {
					int msgSize = in.available();

					if(msgSize > 0) {
						if(readByte() == CMD_TIMECODE) {	
							data = readByte();
							available = true;
							//System.out.println("Reader thread: data available " + data);
						}
					}
	        
					sleep((long)(wait));
				} catch (Exception e) {
				}
			}
			System.out.println("Reader thread is done!");  // The thread is done when we get to the end of run()
	  }
	  
	  public boolean available() {
        return available;
	  }

	  public int getCount() {
		  return count;
	  }
	  
	  public int getData() {
		  available = false;
		  return data;
	  }

	  /**
	   * Our method that quits the thread
	   */
	  public void quit() {
	    System.out.println("Quitting."); 
	    running = false;  // Setting running to false ends the loop in run()

	    // In case the thread is waiting. . .
	    interrupt();
	  }

}

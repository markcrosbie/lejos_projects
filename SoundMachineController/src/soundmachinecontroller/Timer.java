package soundmachinecontroller;

import processing.core.*;

class Timer {
	  long savedTime;  // When Timer started
	  long totalTime;  // How long Timer should last
	  boolean running; // Is the timer running?
	  
	  /**
	   * Create an instance of a timer to run for the given number of milliseconds
	   * @param pTotalTime Time to run the timer for
	   */
	  Timer(long pTotalTime)  {
	    totalTime = pTotalTime;
	    running = false;
	  }

	  /**
	   * Start the timer
	   */
	  void start() {
	    savedTime = System.currentTimeMillis(); // When the Timer starts it stores the current time in seconds
	    running = true;
	  }
	  
	  /**
	   * Starting the timer by changing its total time
	   * @param pTotalTime Length of time to run the timer for
	   */
	  void start(long pTotalTime)  {
	    totalTime = pTotalTime;
	    start();
	  }
	  
	  /**
	   * Return true if the timer has elapsed
	   * @return True if the timer has elapsed at lest the specified time, else false
	   */
	  boolean isFinished() {
		  if(running) {
			//  Check how much time has passed
			long passedTime = System.currentTimeMillis() - savedTime;
			if(passedTime > totalTime) {
				running = false;
				//System.out.println("Timer: FINISHED passedTime = " + passedTime);
				return true;
			} else {
				return false;
			}
		  } else {
			  return false;
		  }
	  }
	  
	  /**
	   * Return the length of time the timer is set for
	   * @return The amount of time in milliseconds set in the timer.
	   */
	  long getDelay() {
	     return totalTime;
	  }
	}

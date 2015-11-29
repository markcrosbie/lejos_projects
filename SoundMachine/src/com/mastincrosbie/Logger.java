package com.mastincrosbie;

import java.util.ArrayList;
import lejos.nxt.comm.RConsole;

public class Logger {
	
	private class LogEntry {
		int r;
		int g;
		int b;
		int colour;
		long t;
		
		public LogEntry(long ts, int c, int red, int green, int blue) {
			r = red;
			g = green;
			b = blue;
			colour = c;
			t = ts;
		}
	}
	
	private LogEntry a[];
	private int count = 0;
	
	public Logger() {
		a = new LogEntry[1000];
		count = 0;
	}
	
	public void addEntry(long ts, int c, int red, int green, int blue) {
		LogEntry l = new LogEntry(ts, c, red, green, blue);
		a[count++] = l;
	}
	
	public int size() {
		return count;
	}
	
	public void printLog() {
		int i;
		
		RConsole.println("---------------");
		RConsole.println("Array contains " + count + " elements");
		for(i=0; i < count; i++) {
			LogEntry l = a[i];
			RConsole.println(l.t + "\t" + l.colour + "\t" + l.r + "\t" + l.g + "\t" + l.b);
		}
		RConsole.println("---------------");
	}
}


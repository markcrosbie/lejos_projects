package soundmachinecontroller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import processing.core.*;
import lejos.pc.comm.*;
import lejos.util.Delay;

public class TestUSBSend extends PApplet {

	DataInputStream in1, in2;
	DataOutputStream out1, out2;
	NXTInfo[] nxtInfo;
	NXTComm nxtComm1, nxtComm2;
	
	public void setup() {
		
		try {
/**			
			nxtComm1 = NXTCommFactory.createNXTComm(NXTCommFactory.USB);
		
			// Search for the first NXT
			nxtInfo = nxtComm1.search(null);

			println("Found " + nxtInfo.length + " NXTs");
			
			if(nxtInfo.length <= 0) {
				println("No NXTs found, exiting");
				System.exit(1);
			}

			if( nxtComm1.open(nxtInfo[0])) {
				println("Connected to " + nxtInfo[0].name);
			} else {
				println("Failed to connect to NXT " + nxtInfo[0].name);
				System.exit(1);
			}
**/
			NXTConnector nxtComm1 = new NXTConnector();
			if(!nxtComm1.connectTo("usb://WHITE")) {
				println("Cannot connect to WHITE");
				System.exit(1);
			}
			println("Connected to WHITE");
			in1 = new DataInputStream(nxtComm1.getInputStream());
			out1 = new DataOutputStream(nxtComm1.getOutputStream());
			
			NXTConnector nxtComm2 = new NXTConnector();
			if(!nxtComm2.connectTo("usb://NXT")) {
				println("Cannot connect to NXT");
				System.exit(1);
			}
			println("Connected to NXT");
			in2 = new DataInputStream(nxtComm2.getInputStream());
			out2 = new DataOutputStream(nxtComm2.getOutputStream());
			
/**
			// Now connect to the second NXT
			NXTComm nxtComm2 = NXTCommFactory.createNXTComm(NXTCommFactory.USB);
			
			// Search for the first NXT
			nxtInfo = nxtComm2.search("NXT");

			println("Found " + nxtInfo.length + " NXTs");
			
			if(nxtInfo.length <= 0) {
				println("No NXTs found, exiting");
				System.exit(1);
			}

			if( nxtComm2.open(nxtInfo[0])) {
				println("Connected to " + nxtInfo[0].name);
			} else {
				println("Failed to connect to NXT " + nxtInfo[0].name);
				System.exit(1);
			}
			
			in2 = new DataInputStream(nxtComm2.getInputStream());
			out2 = new DataOutputStream(nxtComm2.getOutputStream());
**/
			
		} catch (Exception e) {
			println("EXCEPTION " + e);
			System.exit(1);
		}
			
	}
	
	public void draw() {
		int x = 0;
		for(int i=0;i<10;i++) 
		{
			try {
			   out1.writeInt(i);
			   out1.flush();
			   
			   println("USBSend: just sent "+ i +" to WHITE");
	
			} catch (IOException ioe) {
				println("IO Exception writing bytes");
			}
			
			Delay.msDelay(500);
	        
			try {
	        	 x = in1.readInt();
	        } catch (IOException ioe) {
	           println("IO Exception reading reply");
	        }            
	        println("Sent " +i + " Received " + x +" from WHITE");
	        
	        Delay.msDelay(500);
	      
	        // Send it on to the other NXT
			try {
				   out2.writeInt(x);
				   out2.flush();
				   
				   println("USBSend: just sent "+ x +" to NXT");
		
				} catch (IOException ioe) {
					println("IO Exception writing bytes");
				}
				
				Delay.msDelay(500);
		        
				try {
		        	 x = in2.readInt();
		        } catch (IOException ioe) {
		           println("IO Exception reading reply");
		        }            
		        println("Received " + x +" from NXT");        
		}
		
		try {
			in1.close();
			out1.close();
			in2.close();
			out2.close();
			println("Closed data streams");
		} catch (IOException ioe) {
			println("IO Exception Closing connection");
		}
		
		try {
			nxtComm1.close();
			nxtComm2.close();
			println("Closed connection");
		} catch (IOException ioe) {
			println("IO Exception Closing connection");
		}
	}
}

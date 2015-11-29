package soundmachinecontroller;

import java.io.*;
import java.util.*;
import processing.core.*;

/**
 * Implements a message that is sent/received to/from the Controller
 * @author mcrosbie
 *
 */
public class Message extends PApplet {
	
	private DataInputStream dis;
	private DataOutputStream dos;
	
	private int msgType;
	private int address;
	private int msgSize;

    private final int MAX_MSG_SIZE = 64;
	private final int START_BYTE = 0xff;
	
	/**
	 * Create a new message that will be sent on the given OutputStream
	 * @param o The previously opened OutputStream to write the message onto
	 */
	Message(OutputStream os) {
		dos = new DataOutputStream(os);
	}
	
	/**
	 * Create a new message from an InputStream for reading
	 * @param in A previously opened InputStream that can be read for bytes 
	 */
	Message(InputStream is) {
		dis = new DataInputStream(is);
	}
	
	/**
	 * Read and parse a message from the InputStream
	 * @return Number of bytes in the message body, excluding the header. Returns 0 if no bytes were read
	 * as the message body. The message still consists of a header field, with a type.
	 * @throws IOException if a poorly formed message was read, or one with a bad checksum
	 */
	public synchronized int readHeader() throws IOException {
		int ba;
		
 	   	// Read data from the input stream
  	   	if( (ba = dis.available()) > 0) {
  	   		
  	   		println("Message read(): " + ba + " bytes available");
  	   		
  	   		int startByte = dis.read();
  	   		println("Message read(): startByte = " + startByte);
  	   		if(startByte != START_BYTE) {
	  	   		println("Message read() No magic value");
	  	   		throw new IOException("No magic value");
	  	   	}	  	   	
  	   		  	   		  	   	
  	   		// Read the type of the message next
  	   		msgType = dis.read();
  	   		println("Message read(): msgType = " + msgType);

  	   		if(msgType < 0) {
  	   			println("MEssage read() bad msgType: " + msgType);
  	   			throw new IOException("Bad message type");
  	   		}
  	   		
  	   		// Read the address value
  	   		address = dis.read();

  	   		println("Message read() address="+address);
  	   		
  	   		// Read the byte count in the message
	  	   	msgSize = dis.read();

	  	   	println("Message read() msgSize= " + msgSize + " bytes");
	  	   	
	  	   	// A little sanity checking is good for you!
	  	   	if( msgSize == 0) {
	  	   		println("Message read(), 0 length message body, returning");
	  	   		return 0;	// empty message, so exit ok
	  	   	}
	  	   	
	  	   	if(msgSize < 0 || msgSize > MAX_MSG_SIZE) {
	  	   		println("Message read() invalid message size " + msgSize);
	  	   		throw new IOException("Invalid message size " + msgSize);
	  	   	}
	  	   	return msgSize;
	 	} else {
	 		return 0; // no bytes available
	 	}
	}
		
	public synchronized void setType(byte t) {
		msgType = t;
	}
	
	/**
	 * Returns the message type of this message as an integer from 0-255
	 * @return The message type
	 */
	public synchronized int getType() {
		return (int)msgType;
	}
	
	/**
	 * Set the destination address to send this message to
	 * @param a Address to send. BROADCAST sends to all receivers
	 */
	public synchronized void setAddress(byte a) {
		address = a;
	}
	
	/**
	 * Return the address encoded in the message
	 * @return The address from 0-255 as an integer
	 */
	public synchronized int getAddress() {
		return (int)address;
	}
		
	/** 
	 * Initiates sending a message by writing the header to the OutputStream.
	 * @param msgSize Number of bytes in the final message after the header
	 * @throws IOException If an error occurs writing to the output stream
	 */
	public synchronized void sendHeader(int msgSize) throws IOException {	   		
	   		// Send the start byte
	   		dos.write(START_BYTE);
	   		dos.flush();

	   		// Send the msg type
	   		dos.write(msgType);
	   		dos.flush();
	   		
	   		// Send the destination address
	   		dos.write(address);
	   		dos.flush();
	   		
	   		// Send the message length
	   		dos.write(msgSize);		
	   		dos.flush();
   	}
	
	/**
	 * Send an integer to the end of the message
	 * @param val Integer value to add
	 * @throws IOException 
	 */
	public synchronized void sendInt(int val) throws IOException {
		dos.writeInt(val);
   		dos.flush();
	}
	
	/**
	 * Send an long value to the end of the message
	 * @param val Long value to add
	 * @throws IOException 
	 */
	public synchronized void sendLong(long val) throws IOException {
		dos.writeLong(val);
   		dos.flush();
	}

	
	/**
	 * Send a byte to the end of the message
	 * @param val The byte value to send 0-255 encoded as an integer. Low-order byte is sent.
	 * @throws IOException
	 */
	public synchronized void sendByte(int val) throws IOException {
		dos.write(val);
   		dos.flush();
	}
	
	/**
	 * Send an array of bytes to the end of the message
	 * @param val Bytes to send
	 * @throws IOException
	 */
	public synchronized void sendBytes(byte[] val) throws IOException {
		this.sendBytes(val, 0, val.length);
	}
	
	/**
	 * Send an array of bytes to the output stream. Send val[off] to val[off+len] 
	 * @param val Array to add to the message
	 * @param off Offset indicating the first element in val to send
	 * @param len Number of elements from val to send.
	 * @throws IOException 
	 */
	public synchronized void sendBytes(byte[] val, int off, int len) throws IOException {
		if( (len - off) > MAX_MSG_SIZE ) {
			throw new IOException("Message too large");
		}
		
		if(len > val.length) {
			throw new IOException("Invalie length");
		}
	
		dos.write(val, off, len);
   		dos.flush();
	}
	
	/**
	 * Return the number of bytes encoded in the message
	 * @return Number of bytes encoded
	 */
	public synchronized int available() {
		return msgSize;
	}

	/**
	 * Get a byte from the next message position
	 * @return The next byte in the message from 0-255 is returned as an integer
	 * @throws IOException if no more bytes are available
	 */
	public synchronized byte readByte() throws IOException {
		return dis.readByte();
	}

	/**
	 * Read an integer from the message
	 * @return The next integer value in the message
	 * @throws IOException is thrown if no more bytes are available
	 */
	public synchronized int readInt() throws IOException {
		return dis.readInt();
	}
	
	/**
	 * Read a long from the message
	 * @return The next long value in the message
	 * @throws IOException is thrown if no more bytes are available
	 */
	public synchronized long readLong() throws IOException {
		return dis.readLong();
	}

}

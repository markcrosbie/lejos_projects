package com.mastincrosbie;

import java.io.*;
import java.util.Arrays;

import lejos.nxt.*;
import lejos.util.*;
import lejos.nxt.comm.RConsole;
import java.util.*;

/**
 * Implements a message that is sent/received to/from the Controller
 * @author mcrosbie
 *
 */
public class Message {
	
	private InputStream dis;
	private OutputStream dos;
	private ByteArrayOutputStream baos;
	private ByteArrayInputStream bais;
	private byte[] b;
	
	private int msgType;
	private int address;
	private int msgSize;		// how many bytes are stored in the message buffer

    private final int MAX_MSG_SIZE = 90;
	private final int START_BYTE = 0xff;
	
	/**
	 * Create a new message that will be sent on the given OutputStream
	 * @param o The previously opened OutputStream to write the message onto
	 */
	Message(OutputStream os) {
		dos = os;
		baos = new ByteArrayOutputStream(MAX_MSG_SIZE);
		msgSize = 0;
	}
	
	/**
	 * Create a new message from an InputStream for reading
	 * @param in A previously opened InputStream that can be read for bytes 
	 */
	Message(InputStream is) {
		dis = is;
		b = new byte[MAX_MSG_SIZE];
		bais = new ByteArrayInputStream(b);
		msgSize = 0;
	}
	
	/**
	 * Extract the message message header from the Message. Must be called after a previous call to readMessage()
	 * @return Number of bytes in the message body, excluding the header. Returns 0 if no bytes were read
	 * as the message body. The message still consists of a header field, with a type.
	 * @throws IOException if a poorly formed message was read, or one with a bad checksum
	 */
	public synchronized int extractHeader() throws IOException {

		// Has any data been read from the input stream?
  	   	if( msgSize > 0) {
  	   		
  	   		int startByte = bais.read();
  	   		if(startByte != START_BYTE) {
	  	   		RConsole.println("Message read() No magic value");
	  	   		throw new IOException("No magic value");
	  	   	}	  	   	
  	   		  	   		  	   	
  	   		// Read the type of the message next
  	   		msgType = bais.read();
  	   		if(msgType < 0) {
  	   			RConsole.println("MEssage read() bad msgType: " + msgType);
  	   			throw new IOException("Bad message type");
  	   		}
  	   		
  	   		// Read the address value
  	   		address = bais.read();

  	   		//RConsole.println("Message read() address="+address);
  	   		
  	   		// Read the byte count in the message
	  	   	msgSize = bais.read();

	  	   	RConsole.println("Message read() msgSize= " + msgSize + " bytes");
	  	   	
	  	   	// A little sanity checking is good for you!
	  	   	if( msgSize == 0) {
	  	   		RConsole.println("Message read(), 0 length message body, returning");
	  	   		return 0;	// empty message, so exit ok
	  	   	}
	  	   	
	  	   	if(msgSize < 0 || msgSize > MAX_MSG_SIZE) {
	  	   		RConsole.println("Message read() invalid message size " + msgSize);
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
	
	/** Create a header in the message buffer prior to writing any data in it.
	 * Must be called before any of the write* methods are called.
	 */
	public void buildHeader() {
		
	}
	
	/**
	 * Initiates sending a message by writing the header to the OutputStream, followed by
	 * the message bytes
	 * @throws IOException If an error occurs writing to the output stream
	 */
	public synchronized void sendMessage() throws IOException {
		
   		// now send the whole byte buffer to the remote side
   		baos.writeTo(dos);
	}
	
	/**
	 * Read a message from the underlying InputStream that was passed in to the constructor. Will block
	 * if the underlying InputStream read() method blocks.
	 * @throws IOException If an error occurred reading the input stream
	 */
	public synchronized void readMessage() throws IOException {
		
		msgSize = dis.read(b);
	}
	
	/**
	 * Store a double value into the message
	 * @param v Value to store
	 * @throws IOException
	 */
	public final void writeDouble(double v) throws IOException
	{
		writeLong(Double.doubleToLongBits(v));
	}
	
	/**
	 * Store a float value into the message
	 * @param v Value to store
	 * @throws IOException
	 */
	public final void writeFloat(float v) throws IOException
	{
		writeInt(Float.floatToIntBits(v));
	}

	/**
	 * Store a 4 byte integer signed value into the message
	 * @param v
	 * @throws IOException
	 */
	public final void writeInt(int v) throws IOException
	{
		baos.write(v >>> 24);
		baos.write(v >>> 16);
		baos.write(v >>>  8);
		baos.write(v);
	}

	/**
	 * Store a 8 byte long into the message
	 * @param v
	 * @throws IOException
	 */
	public final void writeLong(long v) throws IOException
	{
		int tmp = (int)(v >>> 32);
		baos.write(tmp >>> 24);
		baos.write(tmp >>> 16);
		baos.write(tmp >>> 8);
		baos.write(tmp);
		tmp = (int)v;
		baos.write(tmp >>> 24);
		baos.write(tmp >>> 16);
		baos.write(tmp >>> 8);
		baos.write(tmp);
	}

	/**
	 * Store a 2 byte short into the message
	 * @param v Value to store - only the low-order 16 bits are stored
	 * @throws IOException
	 */
	public final void writeShort(int v) throws IOException
	{
		baos.write(v >>> 8);
		baos.write(v);
	}

	/**
	 * Send a byte to the end of the message
	 * @param val The byte value to send 0-255 encoded as an integer. Low-order byte is sent.
	 * @throws IOException
	 */
	public synchronized void writeByte(int val) throws IOException {
		baos.write(val);
		msgSize++;
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
			throw new IOException("Invalid length");
		}
	
		baos.write(val, off, len);
	}
	
	/**
	 * Return the number of bytes encoded in the message just read, or written thus far
	 * @return Number of bytes encoded
	 */
	public synchronized int size() {
		return msgSize;
	}

	/**
	 * Get a byte from the next message position
	 * @return The next byte in the message from 0-255 is returned as an integer
	 * @throws IOException if no more bytes are available
	 */
	public synchronized byte readByte() throws IOException {
		return (byte)(bais.read() & 0xFF);
	}

	/**
	 * Read an integer from the message
	 * @return The next integer value in the message
	 * @throws IOException is thrown if no more bytes are available
	 */
	public synchronized int readInt() throws IOException {
		return bais.read();
	}

	/**
	 * Read a long from the message
	 * @return The next long value in the message
	 * @throws IOException is thrown if no more bytes are available
	 */
	public synchronized long readLong() throws IOException {
		return dis.read();
	}
}

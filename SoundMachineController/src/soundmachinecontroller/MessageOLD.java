package soundmachinecontroller;

import processing.core.*;
import java.io.*;
import java.util.*;

/**
 * Implements a message that is sent/received to/from the Controller
 * @author mcrosbie
 *
 */
public class MessageOLD extends PApplet {

	private OutputStream os;
	private InputStream is;
	
	private int index;
	private byte[] b;
	private static EndianTools en = new EndianTools();

    //The maximum message size. 7 bytes for header, 90 for body
    // The XBee protocol limits the message buffer to 100 bytes, so
    // anything bigger than that gets fragmented. Avoid that.
    private final int MAX_MSG_SIZE = 32;

	 // field definitions for the header
	private final int MAGIC = 0;
	private final int MSG_TYPE = MAGIC+2;		// 1 byte for the message type
	private final int BYTE_COUNT = MSG_TYPE+1;	// 1 byte for the byte count
	private final int SRC_ADDRESS = BYTE_COUNT+1;	// 1 byte for the address
	private final int CKSUM = SRC_ADDRESS+1;		// 4 bytes for the checksum
	private final int HEADER_SIZE = CKSUM+4;
	
	private final byte MAGICVALUE1 = (byte)0xde;
	private final byte MAGICVALUE2 = (byte)0xad;
	
	private final int START_OF_MSG = HEADER_SIZE;

	/**
	 * Create a new message that will be sent on the given OutputStream
	 * @param o The previously opened OutputStream to write the message onto
	 */
	MessageOLD(OutputStream o) {
		os = o;
		b = new byte[MAX_MSG_SIZE];
		index = START_OF_MSG;
	}
	
	/**
	 * Create a new message from an InputStream for reading
	 * @param in A previously opened InputStream that can be read for bytes 
	 */
	MessageOLD(InputStream in) {
		is = in;
		b = new byte[MAX_MSG_SIZE];
		index = 0;	
	}
	
	/**
	 * Read and parse a message from the InputStream
	 * @return Number of bytes in the message body, excluding the header. Returns 0 if no bytes were read
	 * @throws IOException if a poorly formed message was read, or one with a bad checksum
	 */
	public int read() throws IOException {
		
		int bytesRead;
		
 	   	// Read data from the input stream
  	   	if( (bytesRead = is.available()) > 0) {
  	   		is.read(b);
  	   		  	   	
  	   		println("Message read() got " + bytesRead + " bytes");
  	   		
  	   		for(int i=0; i < bytesRead; i++)
  	   			println(b[i] + " ");
  	   		println("---------");
  	   		
	  	   	// Did we read enough bytes?
	  	   	if(bytesRead < HEADER_SIZE) {
	  	   		// not a good start!
	  	   		throw new IOException("No header");
	  	   	}
	  	   		 
	  	   	// check for magic value
	  	   	boolean ok = (b[MAGIC] == MAGICVALUE1) && (b[MAGIC+1] == MAGICVALUE2);
	  	   	if(!ok) {
	  	   		throw new IOException("No magic value");
	  	   	}
	  	  
	  	   	// Did we read at least as many bytes as are in the message length field?
	  	   	int msgSize = b[BYTE_COUNT];
	  	   	if(bytesRead < msgSize) {
	  	   		throw new IOException("Short message");
	  	   	}

	  	   	println("Message read() msgSize= " + msgSize + " bytes");
	  	   	
	  	   	// validate the checksum across the valid message size
	  	   	if(validateChecksum(msgSize)) {
	  	   		println("Message read: valid checksum");
	  	   		return b[BYTE_COUNT];
	  	   	} else {
	  	   		println("Message read: bad checksum!");
	  	   		throw new IOException("Bad checksum");
	  	   	}
	 	} else {
	 		return 0;
	 	}
	}
	
	/**
	 * Compute the checksum across the message before sending
	 */
	private void computeChecksum(int len) {
		Xor8 x = new Xor8();
		
		// Compute the checksum across the header plus message bytes
		x.update(b, START_OF_MSG, len);
		
		// Store the checksum value into the header
		en.encodeIntBE(x.getValue(), b, CKSUM);
	}
	
	/**
	 * Compute and validate the checksum of a message that has been received
	 * @param len Number of bytes in teh total message to compute checksum on
	 * @return True if the checksum matches, else false
	 */
	private boolean validateChecksum(int len) {
		println("validateChecksum; len=" + len);
		
		if(len <=0) {
			return true;
		}
		
		Xor8 x = new Xor8();

		// Extract the checksum value from the header
		int checksum = en.decodeIntLE(b, CKSUM);
		
		println("validateChecksum: extracted checksum=" + checksum);
		
		// Compute the checksum across the message bytes
		x.update(b, START_OF_MSG, len);
		
		println("validateChecksum: computed checksum as " + x.getValue());
		
		return x.getValue() == checksum;		
	}
	
	public void setType(byte t) {
		b[MSG_TYPE] = t;
	}
	
	public byte getType() {
		return b[MSG_TYPE];
	}
	
	public void setAddress(byte a) {
		b[SRC_ADDRESS] = a;
	}
	
	public byte getAddress() {
		return b[SRC_ADDRESS];
	}
	
	/**
	 * Send a message by writing it to the OutputStream
	 */
	public void send() {
		
		b[MAGIC] = MAGICVALUE1;
		b[MAGIC+1] = MAGICVALUE2;
		
		b[BYTE_COUNT] = (byte)(index - START_OF_MSG);	// bytes in the message body after the header
		
		computeChecksum(b[BYTE_COUNT]);

		int len = HEADER_SIZE + index;
		println("Message send");
	   	for(int i=0; i < len; i++)
  	   		print(b[i] + " ");
  	   	println("---------");

	   	try {
     		os.write(b, 0, index);
    	} catch(Exception e) {
    		println("*** EXCEPTION " + e);
    	}
	}
	
	/**
	 * Add an integer to the end of the message
	 * @param val Integer value to add
	 * @throws ArrayStoreException if no space left
	 */
	public void addInt(int val) throws ArrayStoreException {
		
		if( (index + 4) > MAX_MSG_SIZE ) {
			throw new ArrayStoreException();
		}
    	en.encodeIntBE(val, b, index);
    	index += 4;
	}
	
	/**
	 * Add a byte to the end of the message
	 * @param val The byte value to add
	 * @throws ArrayStoreException if no space left
	 */
	public void addByte(byte val) throws ArrayStoreException {
		if( (index + 1) > MAX_MSG_SIZE ) {
			throw new ArrayStoreException();
		}
		b[index]  = val;
		index++;
	}
	
	/**
	 * Add an array of bytes to the end of the message
	 * @param val Bytes to add
	 * @throws ArrayStoreException if no space left
	 */
	public void addBytes(byte[] val) throws ArrayStoreException {
		this.addBytes(val, 0, val.length);
	}
	
	/**
	 * Add an array of bytes to the end of the message. Stores val[off] to val[off+len] in message
	 * @param val Array to add to the message
	 * @param off Offset indicating the first element in val to add
	 * @param len Number of elements from val to add.
	 * @throws ArrayStoreException if no space
	 */
	public void addBytes(byte[] val, int off, int len) throws ArrayStoreException {
		if( (index + (len - off)) > MAX_MSG_SIZE ) {
			throw new ArrayStoreException();
		}
		
		if(len > val.length) {
			throw new ArrayStoreException();
		}
	
		for(int i = off; i < (off+len); i++) {
			b[index++] = val[i];
		}
	}
	
	/**
	 * Return the number of bytes encoded in the message
	 * @return Number of bytes encoded
	 */
	public int available() {
		return index - START_OF_MSG;
	}

	/**
	 * Get a byte from the next message position
	 * @return The next byte in the message queue
	 * @throws EmptyMessageException if no more bytes are available
	 */
	public byte nextByte() throws EmptyMessageException {
		if(index >= MAX_MSG_SIZE) {
			throw new EmptyMessageException("No elements");
		}
		return b[index++];
	}

	public int nextInt() throws EmptyMessageException {
		if( (index +4) > MAX_MSG_SIZE) {
			throw new EmptyMessageException("No elements");
		}
		int ret = en.decodeIntBE(b, index);
		index += 4;
		
		return ret;
	}
}

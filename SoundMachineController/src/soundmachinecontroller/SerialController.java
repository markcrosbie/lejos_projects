package soundmachinecontroller;

import processing.serial.Serial;
import processing.core.PApplet;
import java.io.*;
 
/**
 * Provide a wrapper to control the serial interface in Processing, returning an InputStream and an
 * OutputStream for simplicity.
 * 
 * @author mcrosbie
 * @version 1.0
 */

public class SerialController {

	private Serial myPort;
	private PApplet p;
	private InputStream in;
	private OutputStream out;
	private boolean open = false;
	
	protected class SerialInputStream extends InputStream {
		/**
		 * Returns the number of bytes that can be read (or skipped over) from this
		 * input stream without blocking by the next caller of a method for this input
		 * stream. The next caller might be the same thread or or another thread.
		 *
		 * @return the number of bytes that can be read from this input stream without blocking.
		 * @throws IOException if the stream is closed.
		 *
		 */
		public int available() throws IOException {
			synchronized (SerialController.this){
				if (!open) throw new IOException("InputStream has been closed, it is not ready.");
				return (myPort.available());
			}
		}

		/**
		 * Close the stream. Once a stream has been closed, further read(), available(),
		 * mark(), or reset() invocations will throw an IOException. Closing a
		 * previously-closed stream, however, has no effect.
		 *
		 * @throws IOException never.
		 *
		 */
		public void close() throws IOException {
			synchronized (SerialController.this){
				open = false;
				myPort.stop();
			}
		}

		/**
		 * Not implemented - read ahead is not supported
		 */
		public void mark(int readAheadLimit) {
		}

		/**
		 * Tell whether this stream supports the mark() operation.
		 *
		 * @return false, mark is NOT supported.
		 */
		public boolean markSupported() {
			return false;
		}

		/**
		 * Read a single byte.
		 * This method will block until a byte is available, an I/O error occurs,
		 * or the end of the stream is reached.
		 *
		 * @return The byte read, as an integer in the range 0 to 255 (0x00-0xff),
		 *     or -1 if the end of the stream has been reached
		 * @throws IOException if the stream is closed.
		 *
		 */
		public int read() throws IOException {
			if(open)
				return myPort.read();
			else
				throw new IOException("Serial port closed");
		}
		
		/**
		 * Read bytes into an array.
		 * This method will block until some input is available,
		 * an I/O error occurs, or the end of the stream is reached.
		 *
		 * @param cbuf Destination buffer.
		 * @return The number of bytes read, or -1 if the end of
		 *   the stream has been reached
		 * @throws IOException if the stream is closed.
		 *
		 */
		public int read(byte[] cbuf) throws IOException {
			if(open) 
				return myPort.readBytes(cbuf);
			else
				throw new IOException("Serial port closed");				
		}

		/**
		 * Read bytes into a portion of an array.
		 * This method will block until some input is available,
		 * an I/O error occurs, or the end of the stream is reached.
		 *
		 * @param cbuf Destination buffer.
		 * @param off Offset at which to start storing bytes.
		 * @param len Maximum number of bytes to read.
		 * @return The number of bytes read, or -1 if the end of
		 *   the stream has been reached
		 * @throws IOException if the stream is closed.
		 *
		 */
		public int read(byte[] cbuf, int off, int len) throws IOException {
			synchronized (SerialController.this){
				if(open)
					return myPort.readBytes(cbuf);
				else
					throw new IOException("Serial port closed");
			}
		}
	
		/**
		 * Reset the stream. Not implemented.
		 * @throws IOException if the stream is closed.
		 *
		 */
		public void reset() throws IOException {

		}

		/**
		 * Skip bytes. Not implemented
		 *
		 * @param n The number of bytes to skip
		 * @return 0
		 */
		public long skip(long n) {
			return 0;
		}
	}
	
	protected class SerialOutputStream extends OutputStream {
		/**
		 * Close the stream.
		 *
		 * @throws IOException never.
		 *
		 */
		public void close() throws IOException {
			synchronized (SerialController.this){
				myPort.stop();
				open = false;
			}
		}
		
		/**
		 * Flush the stream.Not implemented.
		 *
		 */
		public void flush() {
				// this method needs to do nothing
		}

		/**
		 * Write an array of bytes.
		 * If the buffer allows blocking writes, this method will block until
		 * all the data has been written rather than throw an IOException.
		 *
		 * @param cbuf Array of bytes to be written
		 * @throws IOException if the stream is closed, or the write is interrupted.
		 *
		 */
		public void write(byte[] cbuf) throws IOException {
			p.println("write: cbuf len"+cbuf.length);
			
			synchronized (SerialController.this){
				if(open)
					myPort.write(cbuf);
				else
					throw new IOException("Serial port closed");
			}
		}

		/**
		 * Write a portion of an array of bytes.
		 *
		 * @param cbuf Array of bytes
		 * @param off Offset from which to start writing bytes
		 * @param len - Number of bytes to write
		 * @throws IOException if the stream is closed, or the write is interrupted.
		 *
		 */
		public void write(byte[] cbuf, int off, int len) throws IOException {
			synchronized (SerialController.this){
				this.write(cbuf);
			}
		}
			
		/**
		 * Write a single byte.
		 * The byte to be written is contained in the 8 low-order bits of the
		 * given integer value; the 24 high-order bits are ignored.
		 * If the buffer allows blocking writes, this method will block until
		 * all the data has been written rather than throw an IOException.
		 *
		 * @param c number of bytes to be written
		 * @throws BufferOverflowException if buffer does not allow blocking writes
		 *   and the buffer is full.
		 * @throws IOException if the stream is closed, or the write is interrupted.
		 *
		 */
		public void write(int c) throws IOException {
			synchronized (SerialController.this){
				if(open)
					myPort.write(c);
				else
					throw new IOException("Serial port closed");
			}
		}		

	}
	
	/**
	 * Constructor for the serial controller gets things initialised. Connects to the first serial port found at 115200 baud
	 * @param pa The PApplet for the parent
	 */
	public SerialController(PApplet pa) {
	  	this(pa, 0, 115200);
	}
	
	/**
	 * Serial controller class. Connects to the n'th serial port in the list, at the specified baud rate
	 * @param pa The PApplet for the parent of this class
	 * @param baud The baudrate to connect at
	 * @param n The serial port to connect to; n=0 connects to the first serial port returned by Serial.list()
	 */
	public SerialController(PApplet pa, int n, int baud) {
		p = pa;	
	  	myPort = new Serial(pa, Serial.list()[n], baud);
	  	open = true;
	  	in = new SerialInputStream();
	  	out = new SerialOutputStream();
	  	
	  	p.println("SerialController init");
	}
	
	/**
	 * Print the current serial port list on the console
	 */
	public void listPorts() {
	  	// List all the available serial ports:
	  	p.println(Serial.list());
	}
	
	/**
	 * Returns an input stream for the serial port, or null if the stream is closed
	 * @return InputStream or null
	 */
	public InputStream getInputStream() {
		if(open)
			return in;
		else
			return null;
	}
	
	/**
	 * Returns an output stream for the serial port, or null if the stream is closed
	 * @return OutputStream or null
	 */
	public OutputStream getOutputStream() {
		if(open)
			return out;
		else
			return null;
	}
	
	/**
	 * Close the serial port and stop communicating
	 */
	public void stop() {
		open = false;
		myPort.stop();
	}
}

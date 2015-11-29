package soundmachinecontroller;

/**
 * Implements a simple XOR checksum
 * 
 * @author mcrosbie
 *
 */
public class Xor8 {

	private int value;
	
    public Xor8() {
        value = 0;
    }

    public void update(byte b) {
        value ^= b & 0xFF;
    }

    public void update(int b) {
        update((byte)(b & 0xFF));
    }

    /**
     * Updates the current checksum with the specified array of bytes.
     * @param bytes the byte array to update the checksum with
     * @param offset the start offset of the data
     * @param length the number of bytes to use for the update
     */
    // from the Checksum interface
    public void update(byte[] bytes, int offset, int length) {
        for (int i = offset; i < length + offset; i++) {
            update(bytes[i]);
        }
    }

    /**
     * Updates the current checksum with the specified array of bytes.
     */
    public void update(byte[] bytes) {
        update(bytes, 0, bytes.length);
    }

    /**
     * Returns the value of the checksum.
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Reset the checksum
     */
    public void reset() {
    	value = 0;
    }
}
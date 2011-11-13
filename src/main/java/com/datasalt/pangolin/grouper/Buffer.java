package com.datasalt.pangolin.grouper;

public class Buffer {
	private int size;
	private byte[] bytes;

	public Buffer(){
		this(0);
	}
	
	public Buffer(int initialCapacity){
		this.bytes = new byte[initialCapacity];
	}
	
	
	public byte[] getBytes(){
		return bytes;
	}
	
	/**
	 * Get the current size of the buffer.
	 */
	public int getLength() {
		return size;
	}

	/**
	 * Change the size of the buffer. The values in the old range are preserved
	 * and any new values are undefined. The capacity is changed if it is
	 * necessary.
	 * 
	 * @param size
	 *          The new number of bytes
	 */
	public void setSize(int size) {
		if (size > getCapacity()) {
			setCapacity(size * 3 / 2);
		}
		this.size = size;
	}

	/**
	 * Get the capacity, which is the maximum size that could handled without
	 * resizing the backing storage.
	 * 
	 * @return The number of bytes
	 */
	public int getCapacity() {
		return bytes.length;
	}

	/**
	 * Change the capacity of the backing storage. The data is preserved.
	 * 
	 * @param new_cap
	 *          The new capacity in bytes.
	 */
	public void setCapacity(int new_cap) {
		if (new_cap != getCapacity()) {
			byte[] new_data = new byte[new_cap];
			if (new_cap < size) {
				size = new_cap;
			}
			if (size != 0) {
				System.arraycopy(bytes, 0, new_data, 0, size);
			}
			bytes = new_data;
		}
	}

}
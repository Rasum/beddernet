package itu.beddernet.router.message.multi;

import itu.beddernet.router.Message;

/**
 * This is an abstract multicast message
 * 
 * 
 */
public abstract class MultiMessage extends Message {

	/**
	 * A list of to addresses
	 */
	protected long[] toNetworkAddresses;
	protected byte numberOfAddresses;

	
	/**
	 * Indentifies what application this message is intended for
	 */
	protected long serviceHash;
	protected byte[] appMessage;
	/**
	 * 
	 * 
	 * @return the list of receivers
	 */
	public long[] getToAddresses() {
		return toNetworkAddresses;
	}

	/**
	 * 
	 * @return the number of receivers
	 */
	public byte getNumberOfAddresses() {
		return numberOfAddresses;
	}

	public byte[] getAppMessage() {
		return appMessage;
	}

	public long getServiceHash() {
		return serviceHash;
	}

}

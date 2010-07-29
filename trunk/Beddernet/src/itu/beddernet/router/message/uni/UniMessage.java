package itu.beddernet.router.message.uni;

import itu.beddernet.router.Message;

/**
 * This is an abstract unicast message
 * 
 * 
 */
public abstract class UniMessage extends Message {
	protected long toNetworkAddress;

	/**
	 * 
	 * @return the network address as long
	 */
	public long getToNetworkAddress() {
		return toNetworkAddress;
	}

}

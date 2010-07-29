package itu.beddernet.router;

public abstract class Message {

	/**
	 * This is an abstract base message class
	 */
	protected byte type;
	protected long fromNetworkAddress;
	protected byte[] datalinkPacket;

	protected String TAG = itu.beddernet.common.BeddernetInfo.TAG;

	public static final byte DSDV_FULLDUMP_MSG_CODE = 1;
	public static final byte UNICAST_APPLICATION_MSG = 2;
	public static final byte MULTICAST_APPLICATION_MSG = 14;
	public static final byte ROUTE_BROADCAST_MSG = 15;
	public static final byte MULTICAST_APPLICATION_MESSAGE_UAIH = 21;
	public static final byte UNICAST_APPLICATION_MESSAGE_UAIH = 22;

	/**
	 * Get the type of this message
	 * @return type as byte
	 */
	public byte getType() {
		return type;
	}

	/**
	 * Get the senders address
	 * @return the senders address
	 */
	public long getFromNetworkAddress() {
		return fromNetworkAddress;
	}

	/**
	 * Serializes the message into a byte[]
	 * @return serialize object as byte[]
	 */
	public abstract byte[] serialize();

}

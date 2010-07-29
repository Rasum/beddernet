
package itu.beddernet.router.dsdv.msg;



/**
 * This Class represents a base DSDV message
 */
public abstract class DSDVPacket {
	public int type;
	public byte[] datalinkPacket;

	// Header info of an AODV message
	public long toNetAddr;
	public long fromNetAddr;

	public static final byte DSDV_FULLDUMP_MSG_CODE = 1;
	public static final byte APPLICATION_MESSAGE_NORMAL = 2;
	// public static final byte BROADCAST_MSG = 3;

	public static final byte SERVICE_REQUEST = 3;
	public static final byte SERVICE_REQUEST_PRIVATE = 4;
	public static final byte SERVICE_REPLY_POSITIVE = 5;
	public static final byte SERVICE_REPLY_NEGATIVE = 6;
	public static final byte SERVICE_REQUEST_FORCE_UPDATE = 7;
	public static final byte SERVICE_REQUEST_ALL = 8;
	public static final byte MULTIDEST_SERVICE_REQUEST = 9;
	public static final byte MULTIDEST_SERVICE_REPLY_POSITIVE = 10;
	public static final byte MULTIDEST_SERVICE_REPLY_NEGATIVE = 11;
	public static final byte MULTIDEST_SERVICE_REQUEST_FORCE_UPDATE = 12;
	public static final byte MULTIDEST_SERVICE_REQUEST_ALL = 13;
	public static final byte MULTIDEST_NORMAL = 14;

	/**
	 * Class constructor
	 * @param up Create a DSDVpacket from the byte array
	 */
	public DSDVPacket(byte[] up) {
		datalinkPacket = up;
	}

	/**
	 * Class constructor
	 * @param sendto the receivers address
	 * @param from the recipients address
	 */
	public DSDVPacket(long sendto, long from) {
		toNetAddr = sendto;
		fromNetAddr = from;
	}

	/**
	 * 
	 * @return serialized object of the DSDVpacket
	 */
	abstract public byte[] serialize();

	/**
	 * Method to return the values in header of DSDV messages
	 * 
	 * @return String - DSDV Message header contents as a string
	 */
	public String toString() {
		return "Destination Address : " + toNetAddr + ", "
				+ "Source Address : " + fromNetAddr;
	}

}

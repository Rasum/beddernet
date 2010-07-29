package itu.beddernet.router;

import itu.beddernet.router.message.multi.MultiMessage;
import itu.beddernet.router.message.uni.UniMessage;

/**
 * 
 * interface class for the router
 */

public interface RouterInterface {

	static final byte NORMAL = 0;
	static final byte SERVICE_REQUEST = 1;
	static final byte SERVICE_REQUEST_PRIVATE = 2;
	static final byte SERVICE_REPLY_POSITIVE = 3;
	static final byte SERVICE_REPLY_NEGATIVE = 4;
	static final byte SERVICE_REQUEST_FORCE_UPDATE = 5;
	static final byte SERVICE_REQUEST_ALL = 6;
	static final byte MULTIDEST_SERVICE_REQUEST = 7;
	static final byte MULTIDEST_SERVICE_REPLY_POSITIVE = 8;
	static final byte MULTIDEST_SERVICE_REPLY_NEGATIVE = 9;
	static final byte MULTIDEST_SERVICE_REQUEST_FORCE_UPDATE = 10;
	static final byte MULTIDEST_SERVICE_REQUEST_ALL = 11;
	static final byte MULTIDEST_NORMAL = 12;

	/**
	 * Starts the services
	 */
	public void setup();

	// public void exitApplication();

	/**
	 * Method to start the protocol handler. This is done by calling route
	 * manager. This method is request by the GUI.
	 */
	public void startApplication();

	/**
	 * Method to stop the protocol handler. Requested by the GUI. Calls the
	 * route manager to stop the protocol handler. TODO: Only a request to close
	 * it, will not close if other apps are using it. Will need a "Force close"
	 * option but not a part of the spec
	 */
	public void stopApplication();

	// TODO: Send message will now need a port number, probably handled in the
	// Network
	// address class
	// public void sendMessage(NetworkAddress dest, byte[] appMsg);
	// TODO: Does this method work everywhere?

	/**
	 * Method for sending unicast messages
	 * 
	 * @param msg
	 *            the unicast message to send
	 */
	public void sendMessage(UniMessage msg);

	/**
	 * Method for sending multicast messages
	 * 
	 * @param msg
	 *            the multicast message to send
	 */
	public void sendMessage(MultiMessage msg);

	// TODO: Should we return all devices on scatternet (yes, probably)
	// of only the ones using the app?
	/**
	 * Returns an array with addresses of all devices in the scatternet
	 * 
	 * @return addresses
	 */
	public long[] getAvailableDevices();

	/**
	 * @return your network address as long
	 */
	public long getNetworkAddress();

	/**
	 * Initiates a device discovery
	 */
	public void searchNewNeighbours();

	/**
	 * Broadcasts a message
	 * 
	 * @param appMsg
	 *            the message to broadcast
	 */
	public void broadcastMessage(byte[] appMsg);

	/**
	 * Swap roles with a device
	 * 
	 * @param dest
	 *            the address of the device to swap roles with
	 */
	public void swap(long dest);

	/**
	 * Break a connect with a device
	 * 
	 * @param dest
	 *            the address to break connection to
	 */
	public void breakConn(long dest);

	/**
	 * Get your Role in the connection with the device (S)lave, (M)aster, (X)
	 * indirectly connected
	 * 
	 * @param dest
	 *            address
	 * @return Role
	 */
	public String getStatus(long dest);

	/**
	 * Prints the routing table
	 */
	public void printRouteTable();

	/**
	 * Manually connect with a device
	 * 
	 * @param dest
	 *            The address
	 */
	public void connect(long dest);

	/**
	 * Connect using a string address
	 * 
	 * @param remoteAddress
	 *            the address
	 */
	public void manualConnect(String remoteAddress);

	/**
	 * Get a list of neighbors
	 * 
	 * @return list of neighbors
	 */
	public long[] getNeighbours();

	/**
	 * Manually disconnect from an address
	 * 
	 * @param remoteAddress
	 *            the addres to disconnect from
	 */
	public void manualDisconnect(String remoteAddress);

	/**
	 * Adds an application identifier to own entry in routing table
	 * 
	 * @param UAIH
	 */
	public void addUAIH(long UAIH);

	/**
	 * Removes an application identifier from own entry in routing table
	 * 
	 * @param UAIH
	 */
	public void removeUAIH(long UAIH);

	public long[] getDevicesSupportingUAIH(long beddernetHash);

	public long[] getAllUAIHOnDevice(long UAIH);

	/**
	 * Check whether a particular devices supports a service
	 * 
	 * @param deviceAddress
	 *            the network address as long
	 * @param UAIS
	 *            The Unique Application Identifier String
	 * @return true or false
	 */
	boolean getApplicationSupport(long deviceAddress, long UAIH);

}

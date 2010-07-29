package itu.beddernet.router.dsdv;

import itu.beddernet.approuter.BeddernetService;
import itu.beddernet.common.NetworkAddress;
import itu.beddernet.router.AppRouterInterface;
import itu.beddernet.router.RouterInterface;
import itu.beddernet.router.dsdv.net.RouteManager;
import itu.beddernet.router.message.multi.MultiMessage;
import itu.beddernet.router.message.uni.UniMessage;
import android.os.Handler;
import android.util.Log;

/**
 * Implementation of the DSDV routing algorithm
 * 
 * @author Arnar
 */
public class DsdvRouter implements RouterInterface, AppRouterInterface {
	public RouteManager rtMgr;
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	private BeddernetService bednetService;

	/**
	 * Class constructor
	 * 
	 * @param appMsgListener
	 *            handles messages
	 * @param bednetService
	 *            used to read of the status of the initialization
	 */
	public DsdvRouter(Handler appMsgListener, BeddernetService bednetService) {
		// create & initialize the route manager
		this.bednetService = bednetService;
		rtMgr = new RouteManager(appMsgListener, this);
	}

	public void setup() {
		rtMgr.setup();
	}

	/**
	 * Method to stop the protocol handler. This is done by requesting route
	 * manager to stop activities and then performs an exit with success return
	 * code. This method is called by the GUI
	 */

	/**
	 * Method to start the protocol handler. This is done by calling route
	 * manager. This method is request by the GUI.
	 */
	public void startApplication() {
		rtMgr.startApplication();
	}

	/**
	 * Method to stop the protocol handler. Requested by the GUI. Calls the
	 * route manager to stop the protocol handler.
	 */
	public void stopApplication() {
		rtMgr.stopApplication();
	}

	/**
	 * Method for sending unicast messages
	 */
	public void sendMessage(UniMessage appMsg) {
		Log.d(TAG, "Message send to DSDVRouter");
		rtMgr.sendMessage(appMsg);
	}

	/**
	 * Method for seding multicast messages
	 */
	public void sendMessage(MultiMessage appMsg) {
		rtMgr.sendMessage(appMsg);
	}

	/**
	 * @return all devices in the routing table excluding your own
	 */
	public long[] getAvailableDevices() {
		return rtMgr.getAvailableDevices();

	}

	/**
	 * @return your network address
	 */
	public long getNetworkAddress() {
		return rtMgr.getNetworkAddress();
	}

	/**
	 * Initiates a new search for devices
	 */
	public void searchNewNeighbours() {
		rtMgr.searchNewNeighbours();
	}

	/**
	 * Broadcasts a message
	 * 
	 * @param appMsg
	 *            the message to broadcast
	 */
	public void broadcastMessage(byte[] appMsg) {
		rtMgr.broadcastMessage(appMsg);
	}

	/**
	 * @param swaps
	 *            roles with the destination address
	 */
	public void swap(long dest) {
		rtMgr.swap(dest);
	}

	/**
	 * @param breaks
	 *            the connection with this address
	 */
	public void breakConn(long dest) {
		rtMgr.breakConn(dest);
	}

	/**
	 * @return your role to the destination (S)lave, (M)aster or (X) indirectly
	 *         connected
	 */
	public String getStatus(long dest) {
		return rtMgr.getStatus(dest);
	}

	/**
	 * Prints the routing table
	 */
	public void printRouteTable() {
		rtMgr.printRouteTable();
	}

	/**
	 * Connect to an address
	 * 
	 * @param address
	 *            to connect to
	 */
	public void connect(long dest) {
		rtMgr.connect(dest);
	}

	public boolean getApplicationSupport(long deviceAddress,
			long UAIH) {
		
		return rtMgr.getApplicationSupport(deviceAddress, UAIH);
		
	}

	/**
	 * Return a list of all active devices on the scattenet that support a 
	 * certain UAIH
	 * 
	 * @return long[] the list of devices that offer the servcie
	 */
	public long[] getDevicesSupportingUAIH(long UAIH) {
		return rtMgr.getDevicesSupportingUAIH(UAIH);
	}


	/**
	 * Not implemented
	 */
	public void setInvokeable(String applicationName, boolean canBeInvoked) {
		// TODO Auto-generated method stub

	}

	/**
	 * Not implemented
	 */
	public void setPublic(boolean isPublic, String serviceString) {
		// TODO Auto-generated method stub

	}

	/**
	 * Notified bednetService that the router is initialized
	 * 
	 * @param routeManagerStatus boolean to specify that the router is online
	 */
	public void routeManagerStatus(boolean routeManagerStatus) {
		if (routeManagerStatus) {
			bednetService.dsdvRouterStatus(routeManagerStatus);
		}

	}

	/**
	 * Manually connect to an address
	 * @param remoteAddress the MAC-address of the device to connect to
	 */
	public void manualConnect(String remoteAddress) {
		rtMgr.manualConnect(remoteAddress);

	}
	
/**
 * @return an array of neighbors
 */
	public long[] getNeighbours() {
		return rtMgr.getNeigbours();
	}


	/**
	 * DEBUG
	 * Manually disconnet from a device
	 * @param remoteAddress the address to disconnect from
	 */
	public void manualDisconnect(String remoteAddress) {
		// TODO Auto-generated method stub
		rtMgr.breakConn(NetworkAddress.castNetworkAddressToLong(remoteAddress));

	}

	public void addUAIH(long UAIH) {
		rtMgr.addUAIH(UAIH);		
	}

	public void removeUAIH(long UAIH) {
		rtMgr.removeUAIH(UAIH);
		
	}

	public long[] getAllUAIHOnDevice(long UAIH) {
		return rtMgr.getAllUAIHOnDevice(UAIH);
	}
	

}

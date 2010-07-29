package itu.beddernet.router.dsdv.net;

import itu.beddernet.approuter.MultiMap;
import itu.beddernet.common.BeddernetInfo;
import itu.beddernet.datalink.bluetooth.BluetoothDatalink;
import itu.beddernet.router.Message;
import itu.beddernet.router.dsdv.DsdvRouter;
import itu.beddernet.router.dsdv.info.ConfigInfo;
import itu.beddernet.router.dsdv.info.CurrentInfo;
import itu.beddernet.router.dsdv.minders.BroadcastMinder;
import itu.beddernet.router.dsdv.minders.PeriodicBroadcastMinder;
import itu.beddernet.router.dsdv.minders.RouteSender;
import itu.beddernet.router.message.RouteBroadcastMessage;
import itu.beddernet.router.message.multi.MultiAppMessage;
import itu.beddernet.router.message.uni.UniAppMessage;
import itu.beddernet.router.message.uni.UniMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import android.os.Handler;
import android.util.Log;

/**
 * Starts routing table service and manages routes
 * 
 * @author Arnar
 */
public class RouteManager {

	public BluetoothDatalink datalink;
	private Handler appMsgObserver;
	private RoutingTable routeTable;
	private PacketHandlerOut pktHandlerOut;
	private PacketHandlerIn pktHandlerIn;
	private BroadcastMinder broadcaster;
	private PeriodicBroadcastMinder periodicBroadcaster;
	/**
	 * manages a route dampening time for new or deleted routes
	 */
	private RouteSender routeSender;
	public final String TAG = BeddernetInfo.TAG;
	private long ownAddress;
	private DsdvRouter dsdvRouter;

	/**
	 * Class constructor
	 * 
	 * @param appMsgListener
	 *            appMsgObserver is the AppMessageObserver aka the applicaton
	 *            that is being notified
	 * @param dsdvRouter
	 */
	public RouteManager(Handler appMsgListener, DsdvRouter dsdvRouter) {

		this.appMsgObserver = appMsgListener;
		this.dsdvRouter = dsdvRouter;
		datalink = new BluetoothDatalink(this);

	}

	/**
	 * Initializes the datalink layer
	 */
	public void setup() {
		datalink.setup();

	}

	/**
	 * 
	 * @return whether a device discovery in ongoing
	 */
	public boolean isDiscovering() {
		return datalink.isDiscovering();
	}

	/**
	 * Init the environment, for new user, broadcast your routing table i.e. the
	 * one line table containing yourself
	 */
	private void initRoutingTable() {
		Log.i(TAG, "initializing routing");
		ownAddress = datalink.getNetworkAddress();
		long dest = ownAddress;
		long next_hop = dest;
		int hops = 0;
		RoutingTableEntry rte = new RoutingTableEntry(dest, next_hop, hops,
				CurrentInfo.incrementOwnSeqNum());
		
		routeTable.addRoutingEntry(rte);
		// Announce arrival on the network
		BroadcastRouteTableMessage();

	}
	
	/**
	 * Method to start the Protocol Handler. Starting is done by doing the
	 * following,
	 * 
	 * - init the routing environment - creates & starts the packet listener -
	 * creates & starts the AODV msg listener - creates & starts the hello
	 * minder - creates and starts the RREQ ID minder
	 */
	public synchronized void startApplication() {
		try {
			Log.d(TAG, "RouteManager: StartApplication");
			// start packet listener thread
			pktHandlerIn.start();
			pktHandlerOut.start();
			Log.d(TAG, "packethandlers started");

			datalink.connectToNetwork(pktHandlerIn, routeTable);

			// begin active routing
			initRoutingTable();
			// // start broadcasting thread
			broadcaster.start();
			// // start the periodic broadcaster
			periodicBroadcaster.start();
			// // start the route deleter
			// staleRouteDeleter.start();

		} catch (Exception ex) {
			Log.e(TAG, "Exception in RouteManager.startApp:" + ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * Method to stop the protocol handler. Does this by stopping all the
	 * threads that were started start function and cleaning up the routing
	 * environment 
	 */
	public synchronized void stopApplication() {

		datalink.disconnectNetwork();

		// stop broadcasting thread
		broadcaster.abort();

		// stop the periodic broadcaster
		periodicBroadcaster.abort();

		// Make pktHandlerIn release the block
		pktHandlerIn.abort();
		pktHandlerOut.abort();

		// start the route deleter
		// staleRouteDeleter.stop();
	}

	/**
	 * 
	 * @return a list of all neighbors
	 */
	public long[] getNeigbours() {
		return routeTable.getNeigbourArray();
	}

	/**
	 * 
	 * @return all entries in the routing table (excluding our own)
	 */
	public long[] getAvailableDevices() {
		// TODO: I hate vectors!
		Vector<RoutingTableEntry> routes = routeTable.getRoutesAsVector();
		long[] connectedDevices = new long[routes.size() - 1];
		int j = 0; // index of connectedDevices
		for (int i = 0; i < routes.size(); i++) {
			long dest = ((RoutingTableEntry) routes.elementAt(i))
					.getDestination();
			if (dest == ConfigInfo.netAddressVal)// own address
			{
			} else {
				connectedDevices[j] = dest;
				j++;
			}
		}
		return connectedDevices;
	}

	/**
	 * 
	 * @return the network address as long
	 */
	public long getNetworkAddress() {
		return datalink.getNetworkAddress();
	}

	/**
	 * This methods return the your role to the destination
	 * 
	 * @param s
	 *            the network error
	 * @return Slave (S), Master (M) or indirectly connected (X)
	 */
	public String getStatus(long s) {
		return datalink.getStatus(s);
	}

	/**
	 * Initiates a new search
	 */
	public void searchNewNeighbours() {
		datalink.searchNewConnections();
	}

	/**
	 * Manually connect to a device
	 * 
	 * @param remoteAddress
	 *            the address to connect to
	 */
	public void manualConnect(String remoteAddress) {
		datalink.manualConnect(remoteAddress);
	}

	// TODO: Is there a nicer way of doing this whitout sacrifying good
	// architecture?? Is the case run linear? Position matters
	// is instanceof faster?
	// maybe set a boolean in the parent class: isMultiCast
	/**
	 * Method for sending messages
	 * 
	 * @param appMsg
	 *            the message to send
	 */
	public void sendMessage(Message appMsg) {
		byte type = appMsg.getType();
		switch (type) {
		case Message.UNICAST_APPLICATION_MSG:
			sendUNICAST_MSG((UniAppMessage) appMsg);
			break;
		case Message.UNICAST_APPLICATION_MESSAGE_UAIH:
			sendUNICAST_MSG((UniAppMessage) appMsg);
			break;
		case Message.MULTICAST_APPLICATION_MSG:
			sendMULTICAST_APPLICATION_MSG((MultiAppMessage) appMsg);
			break;
		case Message.MULTICAST_APPLICATION_MESSAGE_UAIH:
			sendMULTICAST_APPLICATION_MSG((MultiAppMessage) appMsg);
			break;
		default:
			break;
		}
	}

	/**
	 * The route is down. Reestablish new routes
	 * 
	 * @param toNetworkAddresses
	 *            the addresses to send "rounte down" messages to
	 */
	public void BroadcastRouteDown(long[] toNetworkAddresses) {
		for (long l : toNetworkAddresses) {
			BroadcastRouteDown(l);
		}
	}

	/**
	 * Method for sending unicast messages
	 * 
	 * @param appMsg
	 *            the message to send
	 */
	private void sendUNICAST_MSG(UniMessage appMsg) {
		// first check if route is available
		Log.d(TAG, "Message is send to sendUNICAST_MSG");
		RoutingTableEntry rte = routeTable.getRoutingEntry(appMsg
				.getToNetworkAddress());
		pktHandlerOut.put(rte.getNextHop(), appMsg);
	}

	/**
	 * Method for sending multicast application messages
	 * 
	 * @param appMsg
	 *            the message to send
	 */
	private void sendMULTICAST_APPLICATION_MSG(MultiAppMessage appMsg) {
		// first check if route is available

		MultiMap mm = new MultiMap();
		long nexthop = 0;
		// Getting the information
		RoutingTableEntry rte;
		long[] destinations = appMsg.getToAddresses();
		long fromNetworkAddress = appMsg.getFromNetworkAddress();
		byte[] payload = appMsg.getAppMessage();
		long serviceHash = appMsg.getServiceHash();

		for (long d : destinations) {
			rte = routeTable.getRoutingEntry(d);
			// if the address is not in routingtable it is dropped
			if (rte != null) {
				nexthop = rte.getNextHop();
				mm.put(nexthop, d);
			}
		}

		// Get the sets from the multimap
		Set<Entry<Long, ArrayList<Long>>> s = mm.getSets();
		Iterator<Entry<Long, ArrayList<Long>>> i = s.iterator();

		// Get the destination per nexthop
		while (i.hasNext()) {
			Entry<Long, ArrayList<Long>> e = i.next();
			nexthop = e.getKey();
			ArrayList<Long> a = e.getValue();
			int aSize = a.size();

			if (aSize > 1) {
				// More than one address per nexthop
				long[] toNetworkAddresses = new long[aSize];

				for (int x = 0; x < aSize; x++) {
					toNetworkAddresses[x] = a.get(x);
				}
				// Create the message
				appMsg = new MultiAppMessage(fromNetworkAddress,
						toNetworkAddresses, serviceHash, payload);

				// send to next hop or DEST
				pktHandlerOut.put(nexthop, appMsg);

			} else {
				// No more than one toNetworkAddresses -> send unicast message
				UniAppMessage UniappMsg = new UniAppMessage(fromNetworkAddress,
						a.get(0), serviceHash, payload);

				sendUNICAST_MSG(UniappMsg);
			}
		}

	}

	/**
	 * Method for broadcasting messages
	 * 
	 * @param appMsg
	 *            the message to broadcast
	 */
	public void broadcastMessage(byte[] appMsg) {
		Vector<RoutingTableEntry> devices = routeTable.getRoutesAsVector();
		if (devices != null) {
			for (int i = 0; i < devices.size(); i++) {
				try {
					RoutingTableEntry rte = (RoutingTableEntry) devices
							.elementAt(i);
					long dest = rte.getDestination();
					long nextHop = rte.getNextHop();
					RouteBroadcastMessage appMessage = new RouteBroadcastMessage(
							dest, ConfigInfo.netAddressVal, false); // TODO:
					// routedown
					// message???
					// was set
					// to true

					pktHandlerOut.put(nextHop, appMessage);
				} catch (Exception exception) {
					Log.e(TAG, "Error in BroadCast message"
							+ exception.getMessage());
				}
			}
		}
	}

	/**
	 * Broadcasts a route broadcast packet
	 * 
	 * @param routes
	 *            The route to broadcast
	 * @param routeDown
	 *            indicates whether the route(s) are down
	 */
	private void broadcastRouteBroadcastPacket(
			Vector<RoutingTableEntry> routes, boolean routeDown) {
		long[] neighbors = routeTable.getNeigbourArray();

		if (neighbors != null) {
			for (int i = 0; i < neighbors.length; i++) {
				try {
					long dest = neighbors[i];
					RouteBroadcastMessage rbp = new RouteBroadcastMessage(dest,
							ownAddress, routeDown);

					rbp.addRoutesToMessage(routes);
					pktHandlerOut.put(dest, rbp);
				} catch (Exception exception) {
					Log.e(TAG, "Error in BroadcastRouteBroadcastPacket",
							exception);
				}
			}
		}
	}

	/**
	 * Updates the seq num of the route to an odd number and sends that route
	 * immediatly to all neighbors. Should send also an updated route concerning
	 * all routes that have dest as next hop, update those to odd num as well
	 * and send
	 * 
	 * @param dest
	 *            the destination that is down
	 */

	public void BroadcastRouteDown(long dest) {
		RoutingTableEntry rte = routeTable.getRoutingEntry(dest);
		if (rte != null) {
			// get all routes concerning dest, set as invalid
			Vector<RoutingTableEntry> route = routeTable
					.getRoutesAsVectorInvalid(dest);

			broadcastRouteBroadcastPacket(route, true);
		}
	}

	/**
	 * Sends all active routes (routes with even seq nums) to all neighbors
	 */
	public void BroadcastRouteTableMessage() {
		Vector<RoutingTableEntry> routes = routeTable.getAllRoutesAsVector();
		broadcastRouteBroadcastPacket(routes, false);
	}

	/**
	 * Sends the supplied routes to all neighbors, used to send incremental
	 * updates as well as immediate changes
	 * 
	 * @param Vector
	 *            routes The vector of RoutingTableEntry objects to be sent
	 */
	public void BroadcastRouteTableMessageInstant(
			Vector<RoutingTableEntry> routes) {
		broadcastRouteBroadcastPacket(routes, false);
	}

	/**
	 * Processes a Route Down message, this is sent when sending a packet
	 * detects a broken link/route
	 */
	public void processDSDVRouteDownBroadcast(RouteBroadcastMessage rBroad) {

		// log.out("Processing down message :"+rBroad.routes.size());
		for (int i = 0; i < rBroad.routes.size(); i++) {
			RoutingTableEntry rte = (RoutingTableEntry) rBroad.routes
					.elementAt(i);
			// check if route exists
			RoutingTableEntry existing = routeTable.getRoutingEntry(rte
					.getDestination());
			// if I use the rte then increase the hop count by one
			rte.setNumHops(rte.getNumHops() + 1);

			if (existing != null && existing.getSeqNum() < rte.getSeqNum()
					&& rBroad.getFromNetworkAddress() == existing.getNextHop()) {
				routeTable.addRoutingEntry(rte);
			}
		}
	}

	/**
	 * DSDVBroadcast is a standard broadcast message used to send routing table
	 * information Processing this message requires going through all routes
	 * supplied in the package and analyze whether they are better than the
	 * known routes.
	 * 
	 * @param rBroad
	 *            A broadcast message received and forwarded
	 */

	public void processDSDVMsgBroadcast(RouteBroadcastMessage rBroad) {
		try {
			boolean reBroadcast = false;
			// System.out.println("Processing broadcast. :"+rBroad.routes.size());
			for (int i = 0; i < rBroad.routes.size(); i++) {
				RoutingTableEntry rte = (RoutingTableEntry) rBroad.routes
						.elementAt(i);
				// check if route exists
				RoutingTableEntry existing = routeTable.getRoutingEntry(rte
						.getDestination());
				// if I use the rte then increase the hop count by one
				rte.setNumHops(rte.getNumHops() + 1);
				// System.out.println("**** Got from " +
				// rBroad.fromNetAddr.getAddressAsString()+" to "+rte.getDestination().getAddressAsString()
				// + " with sewnum "+rte.getSeqNum());
				// assume new node in the network
				if (existing == null) {// || rte.getSeqNum() == 0) {
					// System.out.println("Added a route.. not in table");
					// routeSender.reset();
					rte.setRouteChanged(true); // mark as sent now
					routeTable.addRoutingEntry(rte);
					reBroadcast = true;
				} else // compare the existing route with the new one
				{
					// System.out.println("Route exists");
					// If the dest is myself and the sequence num is higher and
					// odd then increment own seq num to one higher then the one
					// sent
					if (rte.getDestination() == ConfigInfo.netAddressVal
							&& rte.getSeqNum() % 2 == 1
							&& rte.getSeqNum() > CurrentInfo.lastSeqNum) {
						CurrentInfo.setOwnSeqNum(rte.getSeqNum() + 1);
						rte.setSeqNum(CurrentInfo.lastSeqNum);
						rte.setNextHop(ConfigInfo.netAddressVal);
						rte.setNumHops(0);
						rte.setRouteChanged(true);
						routeTable.addRoutingEntry(rte);
						reBroadcast = true;
					} else if (existing.getSeqNum() < rte.getSeqNum()) {
						// System.out.println("Got from " +
						// rBroad.fromNetAddr.getAddressAsString()+" to "+rte.getDestination().getAddressAsString()
						// +
						// " with sewnum "+rte.getSeqNum()+" existing seq is lower");
						// if there is a newer Seq num and the node I get the
						// message
						// from is the next hop for the destination then store
						// route
						if (rte.getSeqNum() % 2 == 1
								&& rBroad.getFromNetworkAddress() == existing
										.getNextHop()) {
							// System.out.println("Got from " +
							// rBroad.fromNetAddr.getAddressAsString()+" to "+rte.getDestination().getAddressAsString()
							// +
							// " with sewnum "+rte.getSeqNum()+" route is odd num");
							rte.setRouteChanged(true);
							routeTable.addRoutingEntry(rte);
							reBroadcast = true;

						} else if (rte.getSeqNum() % 2 == 0) {
							// System.out.println("Got from " +
							// rBroad.fromNetAddr.getAddressAsString()+" to "+rte.getDestination().getAddressAsString()
							// +
							// " with sewnum "+rte.getSeqNum()+" route is even num");
							// routeSender.reset();
							routeTable.addRoutingEntry(rte);
							// reBroadcast = true;
						} else {
							// System.out.println("Doing nada !! ");
						}
					} else if (existing.getSeqNum() == rte.getSeqNum()
							&& (existing.getNumHops() > rte.getNumHops())) {
						// System.out.println("Got from " +
						// rBroad.fromNetAddr.getAddressAsString()+" to "+rte.getDestination().getAddressAsString()
						// +
						// " with sewnum "+rte.getSeqNum()+" same seq lower hop");
						rte.setRouteChanged(true);
						routeTable.addRoutingEntry(rte);
					} else {
						// System.out.println("Do nothing with Route");
					}
				}
			}

			// reBroadcast message since there was a new route or a route with
			// an odd seq num or fewer hops
			if (reBroadcast) {
				routeSender = new RouteSender(this, routeTable);
				routeSender.start();
			}

		} catch (Exception exception) {
			System.out.println("Exception in process : "
					+ exception.getMessage());
		}

	}

	/**
	 * Processes an Application message after receiving it from another device.
	 * Check whether message is intended for this device, if so then process
	 * else send on to next hop
	 * 
	 * @param appMsg
	 *            the message recieved
	 */
	public void processDSDVMsgApplication(UniMessage appMsg) {
		// log.out("processing App msg");

		long to = appMsg.getToNetworkAddress();
		if (to == ConfigInfo.netAddressVal) {// message is intended for me
			// appMsgObserver.update(appMsg);
			appMsgObserver.obtainMessage(appMsg.getType(), -1, -1, appMsg)
					.sendToTarget();
		} else { // find next hop in routing table
			RoutingTableEntry destination = routeTable.getRoutingEntry(to);

			if (destination != null) {
				sendMessage(appMsg);
			} else {
				try {
					Thread.sleep(500);
					// TODO: Is this a bug or something clever?
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	/**
	 * Used to swap roles Master/Slave
	 * 
	 * @param dest
	 *            the destination address to swap with
	 */
	public void swap(long dest) {
		datalink.swap(dest);
	}

	/**
	 * Break a connection
	 * 
	 * @param dest
	 *            address to the device
	 */
	public void breakConn(long dest) {
		datalink.breakConn(dest);
	}

	/**
	 * Prints the routing table. 
	 */
	public void printRouteTable() {
		Vector<RoutingTableEntry> v = routeTable.getAllRoutesAsVector();
		Log.d(TAG, "Routing Table from route manager:");

		for (int i = 0; i < v.size(); i++) {
			RoutingTableEntry rte = v.elementAt(i);
			Log.d(TAG, rte.toString());

		}
	}

	/**
	 * Connect to an address
	 * 
	 * @param dest
	 *            the address to connect to
	 */
	public void connect(long dest) {
		datalink.connect(dest);
	}

	/**
	 * A multicast message is recieved. If it is for me it is pushed upwards,
	 * and if there are more recievers that i have in my routing table
	 */
	public void processMultiAppMessage(MultiAppMessage multiAppMessage) {
		long[] toAdresses = multiAppMessage.getToAddresses();
		long[] newAddresses = new long[toAdresses.length - 1];
		long myAddress = ConfigInfo.netAddressVal;
		int i = 0;
		boolean changed = false;

		for (long address : toAdresses) {
			if (address == myAddress) {// message is intended for me
				// appMsgObserver.obtainMessage(Message.MULTICAST_APPLICATION_MSG,
				// -1, -1, multiAppMessage)
				// .sendToTarget();
				changed = true;
				appMsgObserver.obtainMessage(Message.MULTICAST_APPLICATION_MSG,
						-1, -1, multiAppMessage).sendToTarget();
				// change message)
				// sendMessage (redone)
			} else {
				newAddresses[i] = address;
				i++;
			}

			// find next hop in routing table
		} 
		if (changed) {

			multiAppMessage.setToAddresses(newAddresses);
		}
		sendMessage(multiAppMessage);
	}

	/**
	 * When the Bluetooth adapter has been initialized, the rest of the datalink
	 * can be initialized
	 * 
	 * @param datalinkStatus
	 *            inidicates whether the bluetooth adapater has been initialized
	 */
	public void datalinkStatus(boolean datalinkStatus) {
		if (datalinkStatus) {
			new ConfigInfo(datalink);
			new CurrentInfo();

			routeTable = new RoutingTable(this);

			pktHandlerOut = new PacketHandlerOut(datalink, this);
			pktHandlerIn = new PacketHandlerIn(this);

			broadcaster = new BroadcastMinder(this, routeTable);
			periodicBroadcaster = new PeriodicBroadcastMinder(this, routeTable);
			//TODO: 
			// staleRouteDeleter = new StaleRouteMinder(routeTable);
			Log.d(TAG, "Created a routemanager");

			dsdvRouter.routeManagerStatus(datalinkStatus);
		} else {
			Log.d(TAG, "RouteManager status called with false");
		}
	}

	public void addUAIH(long UAIH) {
		RoutingTableEntry rte =	routeTable.getRoutingEntry(ownAddress);
		if(rte != null){
			rte.addService(UAIH);
			Log.d(TAG, "new service added");
			rte.setRouteChanged(true);
		}
		else
		{
			Log.d(TAG, "could not add the service entry as no destination was found");
		}
	}

	public void removeUAIH(long UAIH) {
		RoutingTableEntry rte =	routeTable.getRoutingEntry(ownAddress);
		if(rte != null){
			rte.removeService(UAIH);
			rte.setRouteChanged(true);
			Log.d(TAG, UAIH + " service removed");
		}
		else
		{
			Log.d(TAG, "could not remove the service entry as now destination was found");
		}
	}

	public boolean getApplicationSupport(long deviceAddress, long uAIH) {
		return routeTable.getRoutingEntry(deviceAddress).getServices().contains(uAIH);
	}

	public long[] getDevicesSupportingUAIH(long UAIH) {
		Vector<RoutingTableEntry> routes = routeTable.getRoutesAsVector();
		ArrayList<Long> devicesSupportingList = new ArrayList<Long>();
		for (RoutingTableEntry rte : routes) {
			if (rte.getServices().contains(UAIH)){
				devicesSupportingList.add(rte.getDestination());
			}
		}
		
		int lenght = devicesSupportingList.size();
		long[] deviceArray = new long[lenght];
		
		for(int i = 0; i<lenght; i++){
			deviceArray[i] = devicesSupportingList.get(i);
		}
		
		return deviceArray;
	}

	public long[] getAllUAIHOnDevice(long uAIH) {
		RoutingTableEntry rte = routeTable.getRoutingEntry(uAIH);
		long[] deviceArray = new long[rte.getNumServices()];
		
		ArrayList<Long> alist = rte.getServices();
		int lenght = rte.getNumServices();
		
		for(int i = 0; i<lenght; i++){
			deviceArray[i] = alist.get(i);
		}
		return deviceArray;
	}


}

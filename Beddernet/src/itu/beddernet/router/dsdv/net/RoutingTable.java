/**
 * This class is the Routing table itself, holds a collection of RouteingTableEntry objects.
 * It also manages all maintainance of the table
 */

package itu.beddernet.router.dsdv.net;

import itu.beddernet.common.Observer;
import itu.beddernet.router.dsdv.info.ConfigInfo;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import android.util.Log;

/**
 * 
 * @author Arnar
 */
public class RoutingTable implements Observer {

	private Hashtable<Long, RoutingTableEntry> rTable;
	private long[] neighbours;
	
	@SuppressWarnings("unused")
	private RouteManager rm;
	public final String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	public final String TAG2 = "RoutingTable";

	public RoutingTable(RouteManager rm) {
		Log.d(TAG, "Created a RoutingTable");
		rTable = new Hashtable<Long, RoutingTableEntry>();
		this.rm = rm;
	}

	/**
	 * Adds a routing element to the routing table. This is used both for a new
	 * element as well as an update. It removes the old element if it exists and
	 * adds the new under the Destination address of the RoutingTableEntry
	 * @param rte the routing table entry to add
	 */
	public boolean addRoutingEntry(RoutingTableEntry rte) {

		try {
			long dest = rte.getDestination();
			rte.updateRouteCreationTime();
			synchronized (rTable) {
				rTable.put(dest, rte);
			}

		} catch (Exception exception) {
			return false;
		}

		return true;
	}

	/**
	 * Does not delete routes .. marks them as invalid
	 * @param destination the destination to mark as invalid
	 */
	public boolean removeRoutingEntry(long destination) {
		Log.d(TAG, TAG2 + " Removing entry : " + destination);
		try {
			synchronized (rTable) {
				RoutingTableEntry rte = getRoutingEntry(destination);
				rte.setRouteUnvalid(); // increase seq num by 1
				rte.setRouteChanged(true);
			}
		} catch (Exception exception) {
			return false;
		}
		return true;
	}

	/**
	 * Deletes the route permanently from the routing table
	 * @param l the destination to mark as invalid
	 */
	public void deleteRoutingEntry(long l) {
		System.out.println("Deleting entry :" + l);
		try {
			synchronized (rTable) {
				rTable.remove(l);
			}
		} catch (Exception exception) {
		}
	}

	/**
	 * Gets the routing table entry from an address
	 * @param destination the address to lookuo
	 * @return the routing table entry from the address
	 */
	public RoutingTableEntry getRoutingEntry(long destination) {
		RoutingTableEntry rte = rTable.get(destination);
		if (rte != null) {
			return rte;
		} else
			Log.d(TAG, "No entry found in the routing table returning null");
			return null;
	}

	/**
	 * Get all routes that are known to be valid
	 * @return all routes that are known to be valid 
	 */
	public Vector<RoutingTableEntry> getRoutesAsVector() {
		Vector<RoutingTableEntry> v = new Vector<RoutingTableEntry>();
		synchronized (rTable) {
			for (Enumeration<RoutingTableEntry> e = rTable.elements(); e
					.hasMoreElements();) {
				RoutingTableEntry rte = e.nextElement();
				if (rte.getSeqNum() % 2 == 0)
					v.addElement(rte);
			}
		}
		return v;
	}

	/**
	 * Get all neighbors
	 * @return all neightbors
	 */
	public Vector<RoutingTableEntry> getNeigborRoutesAsVector() {
		Vector<RoutingTableEntry> v = new Vector<RoutingTableEntry>();
		synchronized (rTable) {
			for (Enumeration<RoutingTableEntry> e = rTable.elements(); e
					.hasMoreElements();) {
				RoutingTableEntry rte = e.nextElement();
				if (rte.getNumHops() == 1)
					v.addElement(rte);
			}
		}
		return v;
	}

	/**
	 * Returns all routes in the routing table as a vector
	 * @return all routes
	 */
	public Vector<RoutingTableEntry> getAllRoutesAsVector() {
		Vector<RoutingTableEntry> v = new Vector<RoutingTableEntry>();
		synchronized (rTable) {
			for (Enumeration<RoutingTableEntry> e = rTable.elements(); e
					.hasMoreElements();) {
				RoutingTableEntry rte = e.nextElement();
				v.addElement(rte);
			}
		}
		return v;
	}

	/** 
	 * Returns all routes that have changed
	 * @param sendMe adds your own address to the vector
	 * @return all routes that have changed
	 */
	public Vector<RoutingTableEntry> getRoutesAsVectorChanged(boolean sendMe) {
		Vector<RoutingTableEntry> v = new Vector<RoutingTableEntry>();
		synchronized (rTable) {
			if (sendMe)
				v.addElement(rTable.get(ConfigInfo.netAddressVal));// always
																	// send my
																	// own info
			for (Enumeration<RoutingTableEntry> e = rTable.elements(); e
					.hasMoreElements();) {
				RoutingTableEntry rte = e.nextElement();
				if (rte.isRouteChanged()) {
					v.addElement(rte);
					rte.setRouteChanged(false); // reset route changed, update
												// has been sent
				}
			}
		}
		return v;
	}

	/**
	 * Gets all routes that use dest as the next hop. This is used to invalidate
	 * all routes going through a known bad node
	 * @param dest destination
	 * @return the routes to be marked as invalid
	 */
	public Vector<RoutingTableEntry> getRoutesAsVectorInvalid(long dest) {
		Vector<RoutingTableEntry> v = new Vector<RoutingTableEntry>();
		synchronized (rTable) {
			for (Enumeration<RoutingTableEntry> e = rTable.elements(); e
					.hasMoreElements();) {
				RoutingTableEntry rte = (RoutingTableEntry) e.nextElement();
				if (rte.getNextHop() == dest)// route is using dest as next hop
				{
					rte.setRouteUnvalid();
					v.addElement(rte);
				}
			}
		}
		return v;
	}


	/**
	 * Goes through routing table, adding the entries in "neigbours" that have
	 * not been entered. Neighbours is the list of actual neighbours
	 * 
	 * @param routes
	 *            = list of entries currently marked as neighbours note that
	 *            this param is not the "new neigbours" they ares stored in the
	 *            neigbours array
	 */
	private void inputNewNeighbors(Vector<RoutingTableEntry> routes) {
		boolean exists = false;
		for (int j = 0; j < neighbours.length; j++) {
			exists = false;
			for (int i = 0; i < routes.size(); i++) {
				RoutingTableEntry rte = routes.elementAt(i);
				if (rte.getDestination() == neighbours[j]) {
					exists = true;
					if (!rte.isValid()) {
						rte.increaseSeqNum();
					}
				}
			}
			// if neighbor has left remove from routing table, but do not remove
			// own entry
			// because that is not in the neighbor table
			if (!exists) {
				long dest = neighbours[j];
				RoutingTableEntry rt = new RoutingTableEntry(dest, dest, 1, 0);
				addRoutingEntry(rt);
			}
		}
	}

	/**
	 * Goes through the new neighbor list and removes all entries from the route
	 * table if they are gone Could be rewritten for either speed or readability
	 */
	private synchronized void updateRouteTable() {
		Vector<RoutingTableEntry> routes = getNeigborRoutesAsVector();

		inputNewNeighbors(routes);
		Log.d(TAG, "Routingtable -> getNeigborRoutesAsVectore: route size = "
				+ routes.size());
		for (int i = 0; i < routes.size(); i++) {
			RoutingTableEntry rte = routes.elementAt(i);
			Log.d(TAG, "routes.elementAt(" + i + ") = " + routes.elementAt(i));
			// if not in new neighbor list then remove
			boolean exists = false; // assume neighbor has left
			for (int j = 0; j < neighbours.length; j++) {
				if (rte.getDestination() == neighbours[j]) {
					exists = true;
					Log.d(TAG, rte.getDestination()
							+ " Already found in table, not removed");
				}
			}
			// if neighbor has left remove from routing table, but do not remove
			// own entry
			// because that is not in the neighbor table
			if (!exists && rte.getNumHops() == 1) {
				removeRoutingEntry(rte.getDestination());
			}

		}
	}

	/**
	 * Updates the routing table with new information
	 */
	public synchronized void update(Object neigbourArray) {

		Log.i(TAG, " Routing Table: Update called"
				+ ((long[]) neigbourArray).length);
		// NetworkAddress[] naUpdated = neigbourArray;

		// search for new devices if no connections exist
		//
		// TODO Might make maintainer do more active discovery if new
		// neighbourArray is empty
		// if(naUpdated.length == 0){
		// new ConnectionMinder(rm,1l).start();
		// }
		exchangeArrays((long[]) neigbourArray);
		updateRouteTable();
	}

	private synchronized void exchangeArrays(Object neigbourArray) {
		neighbours = null;
		neighbours = (long[]) neigbourArray;
	}

	public synchronized long[] getNeigbourArray() {
		if (neighbours == null || neighbours.length == 0)
			return null;
		
//TODO: Use clone instead!
		long[] nArray = new long[neighbours.length];
		System.arraycopy(neighbours, 0, nArray, 0, neighbours.length);
		return nArray;
	}
}

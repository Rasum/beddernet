package itu.beddernet.router.dsdv.minders;

import itu.beddernet.router.dsdv.info.ConfigInfo;
import itu.beddernet.router.dsdv.net.RouteManager;
import itu.beddernet.router.dsdv.net.RoutingTable;
import itu.beddernet.router.dsdv.net.RoutingTableEntry;

import java.util.Vector;

/**
 * Periodically sends out messages to all neighbors containing the routing table
 * This only sends out incremental updates of the route table, i.e. routes that
 * are marked as having changed
 * @author Arnar
 */
public class PeriodicBroadcastMinder extends Thread {

	RoutingTable rt;
	RouteManager rm;
	boolean abort = false;

    /**
     * Class constructor
     * @param rm route manager for broadcasting routing tables
     * @param rt routing table for getting all changed routing entries
     */
	public PeriodicBroadcastMinder(RouteManager rm, RoutingTable rt) {
		this.rm = rm;
		this.rt = rt;
	}

	public void run() {
		while (!abort) {
			try {
				Thread.sleep(ConfigInfo.periodicRouteBroadcastIncremental);
				// send out my routing table
				Vector<RoutingTableEntry> r = rt
						.getRoutesAsVectorChanged(false);
				if (r != null && r.size() > 0) {
					rm.BroadcastRouteTableMessageInstant(r);
				}

			} catch (InterruptedException ex) {
				ex.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * Kills the thread
	 */
	public synchronized void abort() {
		abort = true;
	}
}

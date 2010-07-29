package itu.beddernet.router.dsdv.minders;

import itu.beddernet.router.dsdv.info.ConfigInfo;
import itu.beddernet.router.dsdv.net.RouteManager;
import itu.beddernet.router.dsdv.net.RoutingTable;
import itu.beddernet.router.dsdv.net.RoutingTableEntry;

import java.util.Vector;

/**
 * When activated this thread sleeps for the dampening time and then sends out the 
 * routing table.  
 * @author Arnar
 */
public class RouteSender extends Thread {

	RoutingTable rt;
	RouteManager rm;
	boolean abort = false;
	private boolean reset = false;
	private boolean isRunning = false;

	public RouteSender(RouteManager rm, RoutingTable rt) {
		this.rm = rm;
		this.rt = rt;
	}

	public void run() {
		isRunning = true;
		reset = false;
		try {
			Thread.sleep(ConfigInfo.changedRouteDampening); // sleep for
			// dampening time
			// and then send

			if (!reset) {
				// send out my routing table
				Vector<RoutingTableEntry> r = rt.getRoutesAsVectorChanged(true);
				rm.BroadcastRouteTableMessageInstant(r);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void reset() {
		this.reset = true;
	}

	public boolean isRunning() {
		return isRunning;
	}
}

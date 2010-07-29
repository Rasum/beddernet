package itu.beddernet.router.dsdv.minders;

import itu.beddernet.router.dsdv.info.ConfigInfo;
import itu.beddernet.router.dsdv.net.RoutingTable;
import itu.beddernet.router.dsdv.net.RoutingTableEntry;

import java.util.Vector;

/**
 * Go through the RouteTable and delete routes that are stale
 * 
 * @author Arnar
 */
public class StaleRouteMinder extends Thread {

	boolean abort = false;
	private RoutingTable rt;

	public StaleRouteMinder(RoutingTable rt) {
		this.rt = rt;
	}

	/**
	 * go through all routes, if older then ConfigInfo.deleteRouteStaleRoute
	 * then delete from routing table
	 */
	public void run() {
		while (!abort) {
			try {
				Thread.sleep(ConfigInfo.deleteRouteSleepVal);
				Vector<RoutingTableEntry> v = rt.getRoutesAsVector();

				for (int i = 0; i < v.size(); i++) {
					RoutingTableEntry rte = (RoutingTableEntry) v.elementAt(i);
					long timeNow = System.currentTimeMillis();
					long lastRouteRenewTime = rte.getRouteCreationTime();

					// never delete self from routing table
					if (!(rte.getDestination() == ConfigInfo.netAddressVal)
							&& timeNow - lastRouteRenewTime > ConfigInfo.deleteRouteStaleRoute) {
						rt.deleteRoutingEntry(rte.getDestination());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
}

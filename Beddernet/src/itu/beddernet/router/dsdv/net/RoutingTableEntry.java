package itu.beddernet.router.dsdv.net;

import java.util.ArrayList;

import itu.beddernet.router.dsdv.info.CurrentInfo;

/**
 * Represents one entry in the Routing table
 * 
 * @author Arnar
 */
public class RoutingTableEntry {

	private long destination, nextHop;
	private int numHops, seqNum;
	private long routeCreationTime;
	private boolean routeChanged = false;
	private ArrayList<Long> serviceList;

	/**
	 * Class constructor
	 */
	public RoutingTableEntry() {
		/**
		 * log when route is created for cleanup
		 */
		routeCreationTime = System.currentTimeMillis();
		this.serviceList= new ArrayList<Long>();
	}

	/**
	 * Class constructor
	 * 
	 * @param dest
	 *            Address of the destination device
	 * @param nextHop2
	 *            how to get there
	 * @param hops
	 *            the number of hops required to get there
	 * @param seqNum
	 *            The sequence numbers are even if a link is present; else, an
	 *            odd number is used.
	 */
	public RoutingTableEntry(long dest, long nextHop2, int hops, int seqNum) {
		destination = dest;
		nextHop = nextHop2;
		numHops = hops;
		this.seqNum = seqNum;
		this.serviceList= new ArrayList<Long>();
		/**
		 * log when route is created for cleanup
		 */
		routeCreationTime = System.currentTimeMillis();
	}

	public RoutingTableEntry(long dest, long nextHop2, int hops,
			int seqNum, long UAIH) {
			destination = dest;
			nextHop = nextHop2;
			numHops = hops;
			this.seqNum = seqNum;
			//TODO: is this the best datastructure?
			this.serviceList= new ArrayList<Long>();
			serviceList.add(UAIH);
			/**
			 * log when route is created for cleanup
			 */
			routeCreationTime = System.currentTimeMillis();
		
	}

	public void removeService(long UIAH){
		serviceList.remove(UIAH);
}
	public void addService(long UIAH){
		serviceList.add(UIAH);
	}
	public ArrayList<Long> getServices(){
		return serviceList;
	}

	public int getNumServices(){
		return serviceList.size();
	}

	/**
	 * Check whether a route has changed
	 * 
	 * @return whether a route has changed
	 */
	public boolean isRouteChanged() {
		return routeChanged;
	}

	/**
	 * 
	 * @param value
	 *            has the route changed
	 */
	public void setRouteChanged(boolean value) {
		routeChanged = value;
	}

	/**
	 * 
	 * @return the destination address
	 */
	public long getDestination() {
		return destination;
	}

	/**
	 * 
	 * @param destination
	 *            the destination address for this routing table entry
	 */
	public void setDestination(long destination) {
		this.destination = destination;
	}

	/**
	 * 
	 * @return the destinations next hop
	 */
	public long getNextHop() {
		return nextHop;
	}

	/**
	 * 
	 * @param nextHop
	 *            the destinations next hop
	 */
	public void setNextHop(long nextHop) {
		this.nextHop = nextHop;
	}

	/**
	 * 
	 * @return the number of hops required
	 */
	public int getNumHops() {
		return numHops;
	}

	/**
	 * 
	 * @param numHops
	 *            sets the number of hops
	 */
	public void setNumHops(int numHops) {
		this.numHops = numHops;
	}

	/**
	 * 
	 * @return the sequence number for this link
	 */
	public int getSeqNum() {
		return seqNum;
	}

	/**
	 * 
	 * @param seqNum
	 *            sets the sequence number for this link
	 */
	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}

	/**
	 * Increment the sequence number
	 */
	public void increaseSeqNum() {
		this.seqNum = CurrentInfo.incrementOwnSeqNum();
	}

	/**
	 * Marks a route as invalid by making it odd
	 */
	public void setRouteUnvalid() {
		if (this.seqNum % 2 == 0) // if is even num then mark as invalid else do
			// nothing
			this.seqNum++; // sets the sequence num as unvalid
	}

	/**
	 * 
	 * @return the creation time for this link
	 */
	public long getRouteCreationTime() {
		return routeCreationTime;
	}

	/**
	 * Updates the create time to NOW
	 */
	public void updateRouteCreationTime() {
		this.routeCreationTime = System.currentTimeMillis(); // log when route
		// is created
		// for cleanup;
	}

	/**
	 * 
	 * @return whether this link is invalid
	 */
	public boolean isValid() {
		return this.seqNum % 2 == 0;

	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Dest: ").append(destination).append("next: ")
				.append(nextHop);
		sb.append(" Seq:").append(seqNum).append(" hop:").append(numHops);
		return sb.toString();
	}

}

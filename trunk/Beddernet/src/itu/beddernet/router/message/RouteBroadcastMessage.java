package itu.beddernet.router.message;

import itu.beddernet.router.Message;
import itu.beddernet.router.dsdv.net.RoutingTableEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import android.util.Log;

/**
 * Represents a FULL DUMP message sent by the BroadcastMinder
 * 
 * @author Arnar
 */

public class RouteBroadcastMessage extends Message {

	public Vector<RoutingTableEntry> routes;
	
	/**
	 * states whether this is a routeDown message or not
	 */
	public boolean routeDownMsg; 
	private long toNetworkAddress;
	private long fromNetworkAddress;
	int numbServices;
	/**
	 * Class constructor
	 * @param msg create a message from the byte[]
	 */
	public RouteBroadcastMessage(byte[] msg) {
		routes = new Vector<RoutingTableEntry>();
		deserialize(msg);
	}

	/**
	 * Class constructor
	 * @param toNetworkAddress specifies the to address
	 * @param fromNetworkAddress specifies the from address
	 * @param routeDown specifies whether this router is down
	 */
	public RouteBroadcastMessage(long toNetworkAddress,
			long fromNetworkAddress, boolean routeDown) {
		this.type = Message.ROUTE_BROADCAST_MSG;

		this.fromNetworkAddress = fromNetworkAddress;
		this.toNetworkAddress = toNetworkAddress;
		routes = new Vector<RoutingTableEntry>();
		routeDownMsg = routeDown;
	}
/**
 * 
 * @param rtes the routes added to the message
 */
	public void addRoutesToMessage(Vector<RoutingTableEntry> rtes) {
		// add all routes to the route Vector
		routes = rtes;
	}

	/*
	 * Note that there is no need to send our seq num specially because it is
	 * always sent in the message since own device is always in the list
	 */

	/**
	 * This methods serializes the message into a byte[]
	 * @return The message as a byte[]
	 * 
	 */
	public byte[] serialize() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		byte n = 0;
		ArrayList<Long> serviceList;
		try {
			dos.writeByte(DSDV_FULLDUMP_MSG_CODE);
			dos.writeLong(fromNetworkAddress);
			dos.writeLong(toNetworkAddress);
			dos.writeBoolean(routeDownMsg);
			dos.writeInt(routes.size());
			for (int i = 0; i < routes.size(); i++) {
				RoutingTableEntry rte = (RoutingTableEntry) routes.elementAt(i);
				dos.writeLong(rte.getDestination());
				dos.writeInt(rte.getNumHops());
				dos.writeInt(rte.getSeqNum());
				n = (byte) rte.getNumServices();
				serviceList = rte.getServices();
				dos.writeByte(n);
				for (int j = 0; j<n; j++){
					dos.writeLong(serviceList.get(j));
				}
			}

		} catch (IOException iOException) {
			Log.e(TAG, "Error in serializing route broadcast message", iOException);
		}

		return baos.toByteArray();
	}

	/**
	 * The method deserializes byte[] into a message object
	 * 
	 * @param msg
	 *            the byte[] to create the message from
	 */
	private void deserialize(byte[] msg) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(msg);
			DataInputStream dis = new DataInputStream(bais);
			type = dis.readByte();
			fromNetworkAddress = dis.readLong();
			toNetworkAddress = dis.readLong();
			routeDownMsg = dis.readBoolean();
			int numRoutes = dis.readInt();
			for (int i = 0; i < numRoutes; i++) {
				RoutingTableEntry rte = new RoutingTableEntry();
				long addr = dis.readLong();
				rte.setDestination(addr);
				int numh = dis.readInt();
				rte.setNumHops(numh);
				int seq = dis.readInt();
				rte.setSeqNum(seq);
				rte.setNextHop(fromNetworkAddress);
				numbServices = dis.readByte();
				for (int j = 0; j<numbServices; j++){
					rte.addService(dis.readLong());
				}
				routes.addElement(rte);
			}
		} catch (Exception ex) {
			Log.e(TAG, "Error in parsing routebroadcastMessage", ex);
		}
	}

	/**
	 * 
	 * @return the recipient's address
	 */
	public long getToAddress() {
		return toNetworkAddress;
	}
}

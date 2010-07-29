package itu.beddernet.router.dsdv.net;

import itu.beddernet.common.BeddernetInfo;
import itu.beddernet.datalink.DatalinkInterface;
import itu.beddernet.router.Message;

import java.util.concurrent.LinkedBlockingQueue;
import android.util.Log;

/**
 * Sends packets on down to the DataLink Layer for sending out on BT
 * 
 * @author Arnar
 */
public class PacketHandlerOut extends Thread {

	/**
	 * Queue holding the address for the outgoing messages. This is created for
	 * performance issues so that we don't have to look up the receiver address
	 * in the message object as they are serialized
	 */
	private LinkedBlockingQueue<Long> outgoingAddressBuffer = new LinkedBlockingQueue<Long>();

	/**
	 * 
	 * Queue holding the outgoing messages
	 */
	private LinkedBlockingQueue<byte[]> outgoingMessageBuffer = new LinkedBlockingQueue<byte[]>();
	private boolean abort = false;
	DatalinkInterface dl;
	public final String TAG = BeddernetInfo.TAG;
	RouteManager rm;

	/**
	 * Class constructor
	 * 
	 * @param dl
	 *            gives access to the bluetooth layer for sending messages
	 * @param rm
	 *            if a route is down the router manager broadcasts a
	 *            "route down" message
	 */
	public PacketHandlerOut(DatalinkInterface dl, RouteManager rm) {
		this.dl = dl;
		this.rm = rm;
	}

	/**
	 * All outgoing messages are put in a queue.
	 * 
	 * @param na
	 *            The address the the message is intended for
	 * @param appMsg
	 *            the message object
	 */
	public void put(long na, Message appMsg) {

		// Better than having to create map object for every message...
		outgoingAddressBuffer.add(na);
		outgoingMessageBuffer.add(appMsg.serialize());

	}

	/**
	 * terminating the queue
	 */
	public void abort() {
		Log.d(TAG, "aborting queuehandler");
		abort = true;
		outgoingAddressBuffer.add(0L);
		outgoingMessageBuffer.add(new byte[] { 0 });
	}

	/**
	 * 
	 * This thread picks up a message from the queue and processes it
	 */
	public void run() {
		try {
			while (!abort) {
				long dest = outgoingAddressBuffer.take();
				byte[] msg = outgoingMessageBuffer.take();
				if (dest == 0) // Break signal
				{
					Log.d(TAG, "Dest is 0");
					break;
				}

				while (rm.isDiscovering()) {
					Thread.sleep(1000); // sleep while it is discovering
				}

				if (!dl.sendPacket(dest, msg)) { // Returns false if the route
					// is broken
					// FIXME: Same thread goes back and removes the destination.
					// un cool
					Log.d(TAG, "A route is down - remove the entry ");
					rm.BroadcastRouteDown(dest);
				}
			}
		} catch (InterruptedException e) {
			Log
					.e(
							TAG,
							"Error in PacketHandlerOut - no outgoing messages will be sent!",
							e);
		}
		Log.d(TAG, "PacketHandlerOut aborting");

	}

}

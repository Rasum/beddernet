package itu.beddernet.router.dsdv.net;

import itu.beddernet.common.Observer;
import itu.beddernet.router.Message;
import itu.beddernet.router.message.RouteBroadcastMessage;
import itu.beddernet.router.message.multi.MultiAppMessage;
import itu.beddernet.router.message.uni.UniAppMessage;
import itu.beddernet.router.message.uni.UniAppMessageUAIH;

import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

/**
 * Handles all incoming packages and calls on RoutingManager to either route
 * further or deliver to application
 * 
 * @author Arnar
 */

// TODO: doesn't really implement observer, remove and clean up.
public class PacketHandlerIn extends Thread implements Observer {
	/**
	 * A queue to store incoming messages The blockingqueue is ThreadSafe. Take
	 * blocks if empty and Put blocks if full
	 */
	private LinkedBlockingQueue<byte[]> incomingBuffer;

	private boolean abort = false;
	private RouteManager rm;
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;

	/**
	 * Class constructor
	 * 
	 * @param rm
	 *            routermanager handles messages when they have been identified
	 */
	public PacketHandlerIn(RouteManager rm) {
		incomingBuffer = new LinkedBlockingQueue<byte[]>();
		this.rm = rm;
	}

	/**
	 * When a message is recieve by the device it is put in the incoming queue
	 * "incomingBuffer"
	 * 
	 * @param in
	 *            the message to be put in the queue
	 */
	public void update(Object in) {
		try {
			incomingBuffer.put((byte[]) in);
		} catch (InterruptedException e) {
			Log.e(TAG, "PacketHandlerIn:" + e.getMessage());
		}
	}

	/**
	 * Terminates the queue
	 */
	public void abort() {
		byte[] b = { -1 };
		try {
			abort = true;
			incomingBuffer.put(b);
		} catch (InterruptedException e) {
			Log.e(TAG, "abort failed", e);
		}
	}

	/**
	 * This thread picks up a message from the queue and examines it's type.
	 * It then and creates a message object processes it
	 */
	public void run() {
		while (!abort) {
			try {
				byte[] packet = null;
				byte type;
				packet = incomingBuffer.take();
				type = packet[0];
				Log.d(TAG, "message is of type:" + type);
				switch (type) {
				case Message.DSDV_FULLDUMP_MSG_CODE:
					RouteBroadcastMessage rBroad = new RouteBroadcastMessage(
							packet);
					if (!rBroad.routeDownMsg) {
						rm.processDSDVMsgBroadcast(rBroad);
					} else {
						rm.processDSDVRouteDownBroadcast(rBroad);
					}
					break;
				case Message.UNICAST_APPLICATION_MSG:
					UniAppMessage applicationMessage = new UniAppMessage(packet);
					rm.processDSDVMsgApplication(applicationMessage);
					break;
				case Message.UNICAST_APPLICATION_MESSAGE_UAIH:
					UniAppMessageUAIH applicationServiceMessage = new UniAppMessageUAIH(
							packet);
					rm.processDSDVMsgApplication(applicationServiceMessage);
					break;
				case Message.MULTICAST_APPLICATION_MSG:
					rm.processMultiAppMessage(new MultiAppMessage(packet));
					break;
				default:
					Log.e(TAG, "Unknown message type in packetHandlerIn");
					break;

				}
			} catch (InterruptedException ex) {
				Log.e(TAG, "Exception in PacketHandlerIn.run(): "
						+ ex.getMessage());
			} catch (Exception e) {
				Log.e(TAG, "Exception in PacketHandlerIn: " + e.getMessage());
			}
		}
		Log.d(TAG, "PacketHandlerIn aborting");
	}
}

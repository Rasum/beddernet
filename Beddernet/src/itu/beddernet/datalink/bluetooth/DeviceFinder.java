package itu.beddernet.datalink.bluetooth;

import itu.beddernet.common.NetworkAddress;

import java.io.IOException;
import java.util.UUID;
import java.util.Vector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.util.Log;

/**
 * This class initiates a device discovery to find new devices in the vicinity
 * and runs device discoveries on those devices to see if they should be
 * connected to
 * 
 * @author GhostGob
 */
public class DeviceFinder implements Runnable {

	private static Vector<BluetoothDevice> devicesDiscovered;
	// Gathers all new connections made during one device discovery
	private Vector<BluetoothSocket> newConnections;
	private BluetoothAdapter ba;
	private DeviceManager dm;
	public boolean searchRunning = false;
	private long lastDiscoveryTime = 0;
	byte b[];

	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	public static UUID BT_NETWORK_UUID = itu.beddernet.common.BeddernetInfo.BT_NETWORK_UUID;

	static boolean discovering;

	/**
	 * Constructor sets up the class for device discovery
	 * 
	 * @param ba
	 *            A reference to this device
	 * @param dm
	 *            The deviceManager to handle new devices and connections
	 */
	public DeviceFinder(BluetoothAdapter ba, DeviceManager dm) {
		this.ba = ba;
		this.dm = dm;
		devicesDiscovered = new Vector<BluetoothDevice>();
		newConnections = new Vector<BluetoothSocket>();
	}

	/**
	 * Puts device into threadsafe vector to be handled later
	 * @param device
	 */
	public static void addDevice(BluetoothDevice device) {
		if (devicesDiscovered != null) {
			if (!devicesDiscovered.contains(device)) {
				devicesDiscovered.addElement(device);
			}
		}
	}

	/**
	 * Starts a bluetooth discovery cycle
	 */
	private void doDiscovery() {
		// If we're already discovering, stop it
		if (ba.isDiscovering()) {
			ba.cancelDiscovery();
		}
		// Request discover from BluetoothAdapter
		if (!ba.startDiscovery()) {
			Log.e(TAG, "An error happened during discovery");
		}
	}

	// Implemented as a runnable thread, mirroring the J2ME version
	// Could be made event driven later
	public void run() {
		long tStart = 0, tEnd = 0;
		discovering = true;
		doDiscovery();
		while (discovering) {
			try {
				Log.d(TAG, "Discovering is true - run is waiting");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Log.d(TAG, "DeviceFinder sleep interrupted");
			}
		}

		Log.d(TAG, "Discovering is false - handleing the devices");
		Log.d(TAG, "Found" + devicesDiscovered.size()
				+ " devices - now trying to connect...");

		Thread connectionThread = new Thread(new connectThread(
				devicesDiscovered));
		connectionThread.start();

		try {
			connectionThread.join(); 
		} catch (InterruptedException e) {
			Log.e(TAG, "connectThread interrupted", e);
		}

		Log.d(TAG, "All connectionThread are done - proceed");

		if (!newConnections.isEmpty()) {
			dm.handleNewlyDiscoveredConnections(newConnections);
		}
		newConnections.clear();
		lastDiscoveryTime = tEnd - tStart;
	}

	public long getLastDiscoveryTime() {
		return lastDiscoveryTime;
	}

	/**
	 * Called when bluetooth hardware has finished discovery and
	 * Devises should be handled
	 */
	public static void discoveryFinished() {
		discovering = false;
	}

	// --------------------------------------------------------

	public class connectThread implements Runnable {
		Vector<BluetoothDevice> devices;
		BluetoothDevice device;
		int nr = 0; // Only used for logging

		public connectThread(Vector<BluetoothDevice> devices) {
			this.devices = devices;
		}

		public void run() {

			// Done sequentially as multithreaded version 
			// worked poorly with some BT hardware
			for (BluetoothDevice device : devices) {
				Log.d(TAG, "Connection attempt " + nr++ + " starting");
				// we don't have too many neigbours already??
				if (dm.numberOfNeighbours() + newConnections.size() < BluetoothDatalink.MAX_OUT_DEGREE) {
					// do we have that device already?
					if (!dm.exists(NetworkAddress
							.castNetworkAddressToLong(device.getAddress()))) {
						BluetoothSocket socket = null;
						// try {
						Log.d(TAG,
								"Trying to established connection to device:"
										+ device.getAddress());
						try {
							socket = device
									.createRfcommSocketToServiceRecord(BT_NETWORK_UUID);
						} catch (IOException e) {
							Log.e(TAG, "Devicefinder: could not create "
									+ "socket to device", e);
						}
						try {
							socket.connect();
							Log.d(TAG,
									"Connection established to "
											+ device.getAddress());
							newConnections.addElement(socket);
						} catch (Exception e) {
							Log.e(TAG, "Datalink: could not connect to device",
									e);
							try {
								socket.close();

							} catch (Exception e1) {
								Log.e(TAG,
										"Exception fired with closing a socket"
												+ e.getMessage());
							}
							socket = null;
							device = null; // 
//							Manual garbage collection started, 
//							helps with stability on Milestone hardware
							System.gc(); 
						}
					}
				}
				devices = null;
				break;
			}
		}
	}
}
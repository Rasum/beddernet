package itu.beddernet.datalink.bluetooth;

import itu.beddernet.approuter.BeddernetService;
import itu.beddernet.common.NetworkAddress;
import itu.beddernet.common.Observer;
import itu.beddernet.datalink.DatalinkInterface;
import itu.beddernet.router.dsdv.net.RouteManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.Vector;

import android.content.Intent;
import android.util.Log;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothSocket;

/**
 * Main class for the bluetooth datalink layer implements DatalinkInterface to
 * allow communication from the Router layer.
 * 
 * @author Gober
 */
public class BluetoothDatalink implements DatalinkInterface {
	private BluetoothDevice btDevice;
	private BluetoothAdapter btAdapter;
	private long btAdd;
	private DeviceManager dm;
	private DeviceFinder df;
	private Thread dfThread = null;
	private Thread clThread = null;
	private ConnectionListener cl;
	
	public static final byte MASTER_TO_SLAVE_SWAP = 65;
	public static final byte SLAVE_TO_MASTER_SWAP = 66;
	public static final byte MASTER_TO_SLAVE_SWAP_RECEIPT= 67;

	private boolean swap = false;
	private static BluetoothDatalink dataLinkInstance;
	public static UUID BT_NETWORK_UUID = itu.beddernet.common.BeddernetInfo.BT_NETWORK_UUID;

	// Max number of neighbors to connect to.
	// This is hard coded now, should be based on device where possible like so:
	// Integer.parseInt(LocalDevice.getProperty("bluetooth.connected.devices.max"));
	public static int MAX_OUT_DEGREE = 7;
	private static String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	public boolean stillWaitingForBT = true;
	private RouteManager rm;
	private boolean idempotentGuard = false;
	public static String myConnectionURL = "";

	/**
	 * Constructor stores general information about this device to be used later
	 */
	public BluetoothDatalink(RouteManager rm) {
		dataLinkInstance = this;
		this.rm = rm;

		try {
			btAdapter = BluetoothAdapter.getDefaultAdapter();
		} catch (Exception e) {
			Log.d(TAG, "Problem with starting bluetooth default adapter"
					+ e.getMessage());
		}
	}

	/**
	 * Starts the bluetooth radio and enables the setup of BEDnet
	 */
	public void setup() {
		if (!btAdapter.isEnabled()) {
			Log.d(TAG, "BT is off. Start an activity");
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (BeddernetService.getBeddernetInstance() != null){
			BeddernetService.getBeddernetInstance().startActivity(enableIntent);
			}
		} else {
			btStatus(true); // It on. All is good good good
		}
	}

	/**
	 * @return The network address of this device
	 */
	public long getNetworkAddress() {
		Log.d(TAG, "BluetoothDatalink: networkAddress requested");
		return btAdd;
	}
	
	public boolean isDiscovering(){
	return df.discovering;
	}

	/**
	 * Method used to setup the network on the data-link level and connect to
	 * surrounding devices
	 * 
	 * @param rtrIncPcktObs
	 *            Observer in router layer to be notified about new incoming
	 *            packets
	 * @param rtrNeighbourObs
	 *            Observer in router layer to be notified about changes in
	 *            connected neighbours
	 */
	public synchronized void connectToNetwork(Observer rtrIncPcktObs,
			Observer rtrNeighbourObs) {
		try {
			// Start device manager
			dm = new DeviceManager(rtrIncPcktObs, rtrNeighbourObs, this);

			// Prepare Device finder
			df = new DeviceFinder(btAdapter, dm);

			// Start listening for incoming connections
			startListeningForConnection();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Creates and starts a ConnectionListener that listens for incoming
	 * connections.
	 */
	public void startListeningForConnection() {
		Log.i(TAG, "BluetoothDatalink: Start Listening for connections");
		cl = new ConnectionListener(dm);
		myConnectionURL = cl.getConnectionURL();
		clThread = new Thread(cl);
		clThread.start();
	}

	public void stopListeningForConnection() {
		Log.i(TAG, "BluetoothDatalink: Stop Listening for connections");
		cl.abort();
	}

	/**
	 * Closes all running threads and all open connections to disconnect from
	 * the network
	 */
	public void disconnectNetwork() {
		cl.abort();
		dm.abort();
	}

	/**
	 * Method to send packets to a given Network Address. If an attempt to send
	 * to a neighbour failes it performs actions to remove it from the neighbour
	 * list
	 * 
	 * @param na
	 *            The address to send to
	 * @param pckt
	 *            The packet to send
	 */
	public synchronized boolean sendPacket(long na, byte[] pckt) {
		while (swap) {
			try {
				Log.d(TAG,"sendPacket waiting");
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		return dm.sendPacket(na, pckt);
	}

	/**
	 * Initiates a new device discovery unless one is already running.
	 */
	public synchronized long searchNewConnections() {
		// stopListeningForConnection();
		if (dm.numberOfNeighbours() < MAX_OUT_DEGREE && !swap) {
			try {
				// cl.abort();
				// clThread.join();
				dfThread = new Thread(df);
				dfThread.start();
				dfThread.join();
				// new Thread(cl).start();

				if (dm.numberOfNeighbours() == 0) {
					return df.getLastDiscoveryTime();
				}
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		// startListeningForConnection();
		return -1;

	}

	/**
	 * Debug code, attempts to establish a connection to a specific address
	 * 
	 * @return
	 */
	public synchronized long manualConnect(String remoteAddress) {
		
		BluetoothDevice other = btAdapter.getRemoteDevice(remoteAddress);
		btAdapter.cancelDiscovery();
		BluetoothSocket socket = null;
		try {
			socket = other.createRfcommSocketToServiceRecord(BT_NETWORK_UUID);
		} catch (IOException e) {
			Log.e(TAG, "Datalink: could not create socket to device", e);
		}
		try {
			socket.connect();
			Log.e(TAG, "++++++++connected");
			
		} catch (IOException e) {
			Log.e(TAG, "Datalink: could not connect to device", e);
			e.printStackTrace();
		} 
		
		Vector<BluetoothSocket> fakeVector = new Vector<BluetoothSocket>(1);
		fakeVector.add(socket);
		dm.handleNewlyDiscoveredConnections(fakeVector);
		return -1;

	}
	
	public void swap(long dest) {
		// startTimer = System.currentTimeMillis();
		// LogDatalink.getLogDatalink().out("Swap clicked on "+dest.getAddressAsString());
		if (dm.connectionExists(dest)) {
			swap = true;
			String status = dm.getDeviceStatus(dest);
			if (status.equals("Master")) {
				// send swap with url
				// LogDatalink.getLogDatalink().out("Sending my own connection url");
				sendPacket(dest, new byte[]{MASTER_TO_SLAVE_SWAP});
			} else {
				// send swap request msg to master and get URL back
				// LogDatalink.getLogDatalink().out("Sending swap request to my master");
				sendPacket(dest, "B".getBytes());
				sendPacket(dest, new byte[]{SLAVE_TO_MASTER_SWAP});
			}
		}
	}

	private void sendSwapReceipt(long dest) {
		sendPacket(dest,new byte[]{MASTER_TO_SLAVE_SWAP_RECEIPT});
		swapComplete();
	}

	public void prepare_swap(long dest) {
		swap = true;
		sendPacket(dest, new byte[]{MASTER_TO_SLAVE_SWAP});
	}

	public void swapComplete() {
		swap = false;
		Log.i(TAG, "Datalink: Swap complete");
	}

	public void perform_swap(long dest, String url) {
		try {
			swap = true;
			Log.i(TAG, "Datalink: Performing swap");
			dm.handleBrokenConnection(dest);
			Thread.sleep(5000);
			btDevice = btAdapter.getRemoteDevice(NetworkAddress
					.castNetworkAddressToString(dest));
			BluetoothSocket conn = btDevice
					.createRfcommSocketToServiceRecord(BT_NETWORK_UUID);
			Vector<BluetoothSocket> conns = new Vector<BluetoothSocket>();
			conns.addElement(conn);
			dm.handleNewlyDiscoveredConnections(conns);
			conn.connect();
			sendSwapReceipt(dest);
		} catch (Exception ex) {
			Log.e(TAG, "Datalink: Performing swap error: " + ex.getMessage());
		}
	}

	public void breakConn(long dest) {
		try {
			dm.handleBrokenConnection(dest);
		} catch (Exception ex) {
			Log.e(TAG, "Datalink: Error in breaking connection");
		}
	}

	public String getStatus(long dest) {
		return dm.getDeviceStatus(dest);
	}

	public String getConnectionString() {
		return cl.getConnectionURL();
	}

	public void connect(long dest) {
		try {
			String addr = NetworkAddress.castNetworkAddressToString(dest);
			BluetoothSocket conn = btAdapter.getRemoteDevice(addr)
					.createRfcommSocketToServiceRecord(BT_NETWORK_UUID);
			conn.connect();
			Log.i(TAG,
					"BluetoothDatalink : connect called, will send to handle new connection");
			dm.handleNewIncomingConnection(conn);
		} catch (Exception e) {
			Log.e(TAG, "Datalink: Error in manual connection"
					,e);
		}
	}

	/**
	 * Notifies router that datalink is ready
	 * @param btStatus true if bluetooth is ready
	 */
	public void btStatus(boolean btStatus) {
		if (btStatus) {
			if (!idempotentGuard) {
				btAdd = NetworkAddress.castNetworkAddressToLong(btAdapter
						.getAddress());
				Log.d(TAG, "btAdd set");
				idempotentGuard = true;
			}
				rm.datalinkStatus(btStatus);
		}

	}

	/**
	 * Static method for returning 
	 * @return bluetooth active datalink, null if none are found
	 */
	public static BluetoothDatalink getBluetoothDatalinkInstance() {
		if (dataLinkInstance == null) {
			Log.e(TAG, "Instance of BluetoothDatalink requested, none found");
			return null;
		} else
			return dataLinkInstance;

	}

	public int numberOfNeighbours() {
		return dm.numberOfNeighbours();
	}

}

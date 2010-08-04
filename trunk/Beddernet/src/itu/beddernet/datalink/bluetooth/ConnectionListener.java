package itu.beddernet.datalink.bluetooth;

import itu.beddernet.approuter.BeddernetService;
import itu.beddernet.common.BeddernetInfo;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

/**
 * Thread that handles incoming Bluetooth connections
 * 
 * @author Gober
 */
public class ConnectionListener extends Thread {

	/** Describes the service */

	private BluetoothAdapter btAdapter;
	private boolean aborted;
	private DeviceManager dm;
	private boolean notifierOpen = false;
	private BluetoothServerSocket serverSocket;
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	private String TAG2 = ConnectionListener.class.getName();
	private Thread discoverableThread = null;
	private BluetoothSocket conn;

	public ConnectionListener(DeviceManager dm) {

		try {

			this.dm = dm;
			btAdapter = BluetoothAdapter.getDefaultAdapter();
			// serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(
			// BeddernetInfo.SDP_RECORD_NAME, BeddernetInfo.BT_NETWORK_UUID);
			// connectionURL = createURL();
			notifierOpen = false;
			openNotifier();
		} catch (Exception ex) {
			Log.e(TAG, "Error in ConnectionListener constructor:"
					+ ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * returns connection address
	 * 
	 * @return address as string (URL's not used in Android bluetooth)
	 */
	public String getConnectionURL() {
		return btAdapter.getAddress();
	}

	public void run() {
		aborted = false;
		while (!aborted && !interrupted()) {
			try {
				if (dm.numberOfNeighbours() >= BluetoothDatalink.MAX_OUT_DEGREE) {
					closeNotifier();
					sleep(3000);
				} else {
					sleep(1000);
					openNotifier();
					if (serverSocket == null) {
						Log.e(TAG, TAG2 + " : Serversocket is null, retrying");
						sleep(20000);
						openServerSocket();
					}
					Log.d(TAG, "Serversocket accept started");
					conn = serverSocket.accept();
					btAdapter.cancelDiscovery();
					if (conn == null)
						Log.e(TAG, TAG2 + " : Connection accepted is null...?");
					// TODO aren't we doing this check twice.. Remove it..
					if (dm.numberOfNeighbours() >= BluetoothDatalink.MAX_OUT_DEGREE) {
						conn.close();
						Log.d(TAG, TAG2 + " : Denied incoming connection, "
								+ "" + "too many neigbours");
					} else {
						Log.d(TAG, TAG2
								+ " : Acceped incoming connection, handling");
						dm.handleNewIncomingConnection(conn);
					}
				}
			} catch (Exception e) {
				Log.d(TAG, TAG2 + " : Exception:" + e.getMessage(), e);
				e.printStackTrace();
			}
		}
		aborted = true;
		if (serverSocket != null)
		try {
			serverSocket.close();
		} catch (IOException e) {
			Log.e("TAG", "Socket not closed", e);
		} finally {
			stopDiscoverable();
		}
	}

	private void openNotifier() {
		// create notifier now
		if (!notifierOpen) {
			// if we are not discoverable, make us discoverable
			// requestDiscoverable();
			openServerSocket();
		}
		notifierOpen = true;
	}

	/**
	 * Opens a Bluetooth serversocket, listening for incoming Beddernet
	 * connections
	 */
	private void openServerSocket() {
		Log.d(TAG, "Open server socket");
		try {
			serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(
					BeddernetInfo.SDP_RECORD_NAME, BeddernetInfo.BT_NETWORK_UUID);
		} catch (IOException e) {
			Log.e(TAG, "Could not open serverSocket: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Starts a socketmaintainer thread that tries to keep device bluetooth
	 * visible.
	 */
	@SuppressWarnings("unused")
	private void requestDiscoverable() {
		Log.d(TAG, "requestDiscoverable");
		discoverableThread = new Thread(new SocketMaintainer(aborted));
		discoverableThread.start();

	}

	public void stopDiscoverable() {
		if (discoverableThread != null) {
			Log.i(TAG, "Discoverable thread interrupted");
			discoverableThread.interrupt();
			discoverableThread = null;
		}
		notifierOpen = false;

	}

	private void closeNotifier() {
		if (serverSocket != null) {
			try {
				notifierOpen = false;
				serverSocket.close();
				serverSocket = null;
				stopDiscoverable();

			} catch (Exception ex) {
				Log.e(TAG, "CloseNotifier failed: " + ex.getMessage());
			}
		}
	}

	public void abort() {
		try {
			this.aborted = true;
			if (conn != null) {
				conn.close(); // IS nulled in finnaly
				conn = null;
			}
			stopDiscoverable();
			closeNotifier();
			Log.d(TAG, "Connectionlisterner aborting..");
		} catch (Exception ex) {
			Log.e(TAG, "ConnectionListerner abort failed", ex);
		}
	}
}

/**
 * Thread that requests bluetooth to become visible regularly
 * 
 */
class SocketMaintainer implements Runnable {

	private boolean aborted;

	SocketMaintainer(boolean aborted) {
		this.aborted = aborted;
	}

	public void run() {
		while (!Thread.interrupted() && aborted) {
			if (BluetoothAdapter.getDefaultAdapter().getState() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
				Log.e(itu.beddernet.common.BeddernetInfo.TAG,
						"Not discoverable, requesting discoverable");
				Intent discoverableIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				discoverableIntent.putExtra(
						BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
				if (BeddernetService.getBeddernetInstance() != null) {
					BeddernetService.getBeddernetInstance().startActivity(
							discoverableIntent);
				}
			}
			try {
				Thread.sleep(300000);
			} catch (InterruptedException e) {
				Log.i(itu.beddernet.common.BeddernetInfo.TAG,
						"Discoverable thread interrupted"
								+ "Discoverability will end sometime...");
			}
		}
		Log.i(itu.beddernet.common.BeddernetInfo.TAG,
				"Discoverable thread sucessfully interrupted");
	}

}

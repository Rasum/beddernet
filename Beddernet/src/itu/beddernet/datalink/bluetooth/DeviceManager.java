package itu.beddernet.datalink.bluetooth;

import itu.beddernet.common.NetworkAddress;
import itu.beddernet.common.Observer;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import android.util.Log;
import android.bluetooth.BluetoothSocket;

/**
 * Class handles tables of all devices found and devices that are connected
 * 
 * @author Gober  
 */
public class DeviceManager {

	Hashtable<Long, DeviceVO> deviceTable;
	Hashtable<Long, String> deviceStatus;

	Observer routerNeigbourObs;
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	private BluetoothDatalink bluetoothDatalink;
	private Observer packetHandlerIn;

	public DeviceManager(Observer rtrIncPcktObs, Observer routerNeigbourObs,
			BluetoothDatalink bluetoothDatalink) {
		deviceTable = new Hashtable<Long, DeviceVO>();
		deviceStatus = new Hashtable<Long, String>();
		this.routerNeigbourObs = routerNeigbourObs;
		this.bluetoothDatalink = bluetoothDatalink;
		this.packetHandlerIn = rtrIncPcktObs;

	}

	public boolean connectionExists(long add) {
		if (deviceTable.containsKey(add))
			return true;
		else
			return false;
	}

	/**
	 * Handles new incoming connections, sets status as "Slave" as it is assumed
	 * the connection was initiated by external device and is master
	 * Does not check if connection already exists
	 * 
	 * @param conn the connection to handle
	 */
	public synchronized void handleNewIncomingConnection(BluetoothSocket conn) {
		long bt = NetworkAddress.castNetworkAddressToLong(conn
				.getRemoteDevice().getAddress());
		DeviceVO device = new DeviceVO(bt, conn, bluetoothDatalink,
				packetHandlerIn, this);
		deviceTable.put(bt, device);
		new Thread(device).start();
		deviceStatus.put(bt, "Slave");
		Log.i(TAG, "New slave added:" + bt);
		neighbourTableChanged();
	}

	/**
	 * Handles connections that have been initiated on device
	 * Designates connections "Master" i.e. assumes local device is Master
	 * @param conns Vector with new bluetooth sockets to handle
	 */
	public synchronized void handleNewlyDiscoveredConnections(
			Vector<BluetoothSocket> conns) {
		Log.d(TAG, "DeviceManager: Handle new connections called, conns size: "
				+ conns.size());
		BluetoothSocket socket;
		Long bt;
		for (int i = 0; i < conns.size(); i++) {
			socket = conns.elementAt(i);
			bt = new Long(NetworkAddress.castNetworkAddressToLong(socket
					.getRemoteDevice().getAddress()));
			deviceStatus.put(bt, "Master");
			DeviceVO device = new DeviceVO(bt, socket, bluetoothDatalink,
					packetHandlerIn, this);
			new Thread(device).start();
			deviceTable.put(bt, device);
		}
		neighbourTableChanged();
	}

	// public void handleNewConnections(BluetoothSocket conn) {
	// Log.i(TAG , "DeviceManager: New connection handled: "
	// + conn.getRemoteDevice().getAddress());
	// String bt = conn.getRemoteDevice().getAddress();
	// deviceStatus.put(bt, "Master");
	// deviceTable.put(bt, new DeviceVO(bt, conn));
	// // neighbourTableChanged();
	// }

	/**
	 * If the PacketSender attempts to send a packet on an open connection, and
	 * the connection is broken, this method will close the connection and
	 * remove the device from the list of connected neighbours, alerting all
	 * observers of the change
	 * 
	 * @param BTA
	 *            the bt-address of the device with broken connection
	 */
	public synchronized void handleBrokenConnection(long BTA) {

		DeviceVO temp = deviceTable.get(BTA);
		if (temp != null) {
			deviceTable.get(BTA).close();
			deviceTable.remove(BTA);
			deviceStatus.remove(BTA);
			neighbourTableChanged();
		}
	}

	/**
	 * Counts the number of connected neighbours
	 * 
	 * @return number of neighbours
	 */
	public synchronized int numberOfNeighbours() {
		return deviceTable.size();
	}

	public Hashtable<Long, DeviceVO> getDeviceTable() {
		return deviceTable;
	}


	public boolean exists(long bluetoothAddress) {
		DeviceVO dvo = deviceTable.get(bluetoothAddress);
		if (dvo == null)
			return false;
		else
			return true;
	}

	/**
	 * Calls all the observers in the list and notifies them that the
	 * neighbour-table is changed
	 */
	private void neighbourTableChanged() {
		Log.d(TAG, "Device manager: neighbourTable changed");
		long[] addressArray = new long[deviceTable.size()];
		int i = 0;
		//Make an array of all currently connected bluetooth devices
		for (Enumeration<DeviceVO> e = deviceTable.elements(); e
				.hasMoreElements();) {
			addressArray[i++] = e.nextElement().getBtAddress();
		}
		routerNeigbourObs.update(addressArray);
	}

	/**
	 * Closes all connections and removes connected devices from neighbour list
	 */
	public void abort() {
		for (Enumeration<DeviceVO> e = deviceTable.elements(); e
				.hasMoreElements();) {
			DeviceVO dvo = e.nextElement();
			dvo.close();
		}
		deviceTable.clear();
	}

	public String getDeviceStatus(long dest) {
		String ret = (String) deviceStatus.get(dest);
		if (ret != null)
			return ret;
		else
			// not a neighbor .. return default value
			return "X";
	}

	/**
	 * Gets the device with the corresponding address
	 * @param address the bluetooth address of the requested device
	 * @return null if device is not in table, DeviceVO instance otherwise
	 */
	public DeviceVO getDevice(long address) {
		return deviceTable.get(address);
	}

	/**
	 * Sends message to recipient, should only be called from datalink
	 * @param na Bluetooth address of recipient
	 * @param pckt Beddernet message, serealized
	 * @return
	 */
	public boolean sendPacket(long na, byte[] pckt) {
		DeviceVO dvo = deviceTable.get(na);
		if (dvo!= null){
		return dvo.write(pckt);
		}
		Log.e(TAG, "Devicemanager -> sendPacket: DVO is null");
		return false;
	}
}

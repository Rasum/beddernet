package itu.beddernet.datalink.bluetooth;

import itu.beddernet.common.NetworkAddress;
import itu.beddernet.common.Observer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * Holds information about remote devices
 * 
 * @author Gober
 */
public class DeviceVO implements Runnable {

	private long btAddress;
	private BluetoothSocket conn;
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	private InputStream inputStream;
	private OutputStream outputStream;
	private BluetoothDatalink bluetoothDatalink;
	private Observer packetHandlerIn;
	private DeviceManager deviceManager;
	private boolean aborted;
	private DataInputStream din;
	@SuppressWarnings("unused")
	private DataOutputStream dout;

	public DeviceVO(long btAddress, BluetoothSocket conn,
			BluetoothDatalink bluetoothDatalink, Observer packetHandlerIn,
			DeviceManager deviceManager) {
		this.btAddress = btAddress;
		this.conn = conn;
		this.bluetoothDatalink = bluetoothDatalink;
		this.packetHandlerIn = packetHandlerIn;
		this.deviceManager = deviceManager;
		InputStream tmpIn = null;
		OutputStream tmpOut = null;
		aborted = false;
		// Get the BluetoothSocket input and output streams
		try {
			tmpIn = conn.getInputStream();
			tmpOut = conn.getOutputStream();
		} catch (IOException e) {
			Log.e(TAG, "Device connection error, input/output not established",
					e);
		}

		inputStream = tmpIn;
		outputStream = tmpOut;
		din = new DataInputStream(inputStream);
		dout = new DataOutputStream(outputStream);

	}

	public long getBtAddress() {
		return btAddress;
	}

	public void setBtAddress(long btAddress) {
		this.btAddress = btAddress;
	}

	public BluetoothSocket getConn() {
		return conn;
	}

	public void close() {
		aborted = true;
		try {
			if (conn != null) {
				conn.close();
				conn = null;
			}
		} catch (Exception ex) {
			Log.e(TAG, "Device: "
					+ NetworkAddress.castNetworkAddressToString(btAddress)
					+ " - Error in closing connection", ex);
		}
	}

	//run method is usually blocks on readInt from the input stream.
	public void run() {
		byte[] message;
		while (!aborted) {
			int bytesRead = 0;
			byte type;
			try {
				//size of incoming bytearray always preceeds array
				bytesRead = din.readInt();
				message = new byte[bytesRead];
				din.readFully(message);
				type = message[0];

			} catch (IOException e) {
				Log.e(TAG, "Device: "
						+ NetworkAddress.castNetworkAddressToString(btAddress)
						+ " - Error in reading from socket, quitting", e);
				deviceManager.handleBrokenConnection(btAddress);
				break;
			}
			Log.d(TAG, "Device: "
					+ NetworkAddress.castNetworkAddressToString(btAddress)
					+ " - Message recieved in packetlistener");
			switch (type) {

			//First byte is the control byte, datalink layer has A, B and C (65, 66, 67) 
//			reserved for datalink operations, message passed on if none of those
			case 'A':
				byte[] url = new byte[bytesRead];
				System.arraycopy(message, 0, url, 0, bytesRead);
				bluetoothDatalink.perform_swap(btAddress, new String(url));
				break;
			case 'B':
				bluetoothDatalink.prepare_swap(btAddress);
				break;
			case 'C':
				bluetoothDatalink.swapComplete();
				break;
			default:
				Log.d(TAG, "Device: "
						+ NetworkAddress.castNetworkAddressToString(btAddress)
						+ "Default case in packetlistener");
				packetHandlerIn.update(message);
				break;
			}
		}

	}

	// Syncronized to support multiple write threads
	//Currently only one write thread used
	public synchronized boolean write(byte[] pckt) {
		try {
			DataOutputStream dout = new DataOutputStream(outputStream);
			dout.writeInt(pckt.length);
			dout.write(pckt);
			dout.flush();
			return true;
		} catch (Exception e) {
			// Connection lost. Packet not sent.
			Log.e(TAG, "DeviceVO: did not manage to send packet:"
					+ e.getMessage(), e);
			e.printStackTrace();
			deviceManager.handleBrokenConnection(btAddress);
			return false;
		}
	}
}

package itu.beddernet.datalink.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import itu.beddernet.common.BeddernetInfo;

/**
 * Class that handles Bluetooth events, extends BroadcastReceiver 
 * Is registered to receive certain events even when Beddernet is off
 * so it only uses static methods
 *
 */
public class DiscoveryReceiver extends BroadcastReceiver {

	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	private String TAG2 = "DiscoveryReveicer";
	public static int bluetoothStatus = BeddernetInfo.BLUETOOTH_STATE_UNKNOWN;
	public static int bluetoothScanStatus = BeddernetInfo.BLUETOOTH_SCAN_MODE_UNKNOWN;


	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, TAG2 + ": onReceive called");
		String action = intent.getAction();

		// When discovery finds a device
		if (BluetoothDevice.ACTION_FOUND.equals(action)) {
			// Get the BluetoothDevice object from the Intent
			Log.i(TAG, TAG2 + ": Device found");
			// DeviceFinder.getGetDefaultDeviceFinder.addDevice()
			BluetoothDevice device = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			DeviceFinder.addDevice(device);
			// When the discovery process is finished:
		} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
			Log.i(TAG, TAG2 + ": Device discovery finished");

			//Notify devicefinder that discovery is finished and it can 
			//start handling devices.
			DeviceFinder.discoveryFinished();

		} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
			BluetoothDatalink datalink = BluetoothDatalink
					.getBluetoothDatalinkInstance();
			switch (BluetoothAdapter.getDefaultAdapter().getState()) {
			case BluetoothAdapter.STATE_OFF:
				bluetoothStatus = BeddernetInfo.BLUETOOTH_STATE_OFF;
				Log.i(TAG, "Discovery receiver: Bluetooth turned off");

				if (datalink != null) {
					datalink.btStatus(false);
				}
				break;
			case BluetoothAdapter.STATE_ON:
				bluetoothStatus = BeddernetInfo.BLUETOOTH_STATE_ON;
				Log.i(TAG, "Discovery receiver: Bluetooth turned on");

				if (datalink != null) {
					datalink.btStatus(true);
				} else {
					Log.i(TAG, "Discovery receiver: " +
							"Datalink not notified, was null (BEDnet off?)");
				}
				break;
			case BluetoothAdapter.STATE_TURNING_OFF:
				bluetoothStatus = BeddernetInfo.BLUETOOTH_STATE_TURNING_OFF;
				Log.i(TAG, "Discovery receiver: Bluetooth turning off");
				break;
			case BluetoothAdapter.STATE_TURNING_ON:
				bluetoothStatus = BeddernetInfo.BLUETOOTH_STATE_TUNING_ON;
				Log.i(TAG, "Discovery receiver: Bluetooth turing on");
				break;
			default:
				Log.e(TAG,"Discovery receiver: " +
						"Bluetooth default case run, should not happen");
				break;
			}
		} else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
			switch (BluetoothAdapter.getDefaultAdapter().getScanMode()) {
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
				bluetoothScanStatus = BeddernetInfo.BLUETOOTH_SCAN_MODE_CONNECTABLE;
				Log.i(TAG, "Discovery receiver: Bluetooth Connectable, not discoverable");
				break;
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
				bluetoothScanStatus = BeddernetInfo.BLUETOOTH_SCAN_MODE_CONNECTABLE_DISCOVERABLE;
				Log.i(TAG, "Discovery receiver: Bluetooth Connectable AND Discoverable");
				break;
			case BluetoothAdapter.SCAN_MODE_NONE:
				bluetoothScanStatus = BeddernetInfo.BLUETOOTH_SCAN_MODE_NONE;
				Log.i(TAG, "Discovery receiver: Bluetooth Scan NOT Connectable NOR Discoverable");
				break;
			default:
				bluetoothScanStatus = BeddernetInfo.BLUETOOTH_SCAN_MODE_UNKNOWN;
				Log.i(TAG, "Discovery receiver: Some strange new state, scan mode set as unknown");
				break;
			}
		}
	}
}

package itu.beddernet.common;

import java.util.UUID;


/**
 * Contains common Beddernet constants like UUID
 *
 */
public final class BeddernetInfo {
	public static final String TAG = "Beddernet";
	//Unique UUID String for BEDnet
	public static final String UUID_STRING = "fa87c0d0-afac-11de-8a39-0800200c9a66";
	
	public static UUID BT_NETWORK_UUID = UUID
	.fromString(UUID_STRING);

	//Name of the Service Discocery Protocol record, i.e. the name of the service type
	//INFO: this does not work with the bluetooth backport project for Android 1.5
	public static final String SDP_RECORD_NAME= "Beddernet";

	public static final int BLUETOOTH_STATE_UNKNOWN = 0;
	public static final int BLUETOOTH_STATE_OFF = 1;
	public static final int BLUETOOTH_STATE_ON = 2;
	public static final int BLUETOOTH_STATE_TURNING_OFF = 3;
	public static final int BLUETOOTH_STATE_TUNING_ON = 4;
	public static final int BLUETOOTH_SCAN_MODE_UNKNOWN = 5;
	public static final int BLUETOOTH_SCAN_MODE_CONNECTABLE = 6;
	public static final int BLUETOOTH_SCAN_MODE_CONNECTABLE_DISCOVERABLE = 7;
	public static final int BLUETOOTH_SCAN_MODE_NONE = 8;

	public static final int DISCOVERY_INTERVAL = 300000;
	public static final long SCAN_TIME = 60000;

}

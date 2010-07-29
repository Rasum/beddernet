package itu.beddernet.approuter;

import itu.beddernet.datalink.bluetooth.BluetoothDatalink;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;

public class BluetoothEnableActivity extends Activity {

	private static final int REQUEST_ENABLE_BT = 1;
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	private BluetoothDatalink datalink;
	private boolean btStatus = false;

	public BluetoothEnableActivity(BluetoothDatalink datalink) {
		Log.d(TAG, "BluetoothEnableActivity constructor");
		this.datalink = datalink;
		
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "BluetoothEnableActivity on create");
		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		this.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		super.onCreate(savedInstanceState);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				Log.d(TAG, "onActivityResult BT is now on");
				btStatus = true;
				break;

			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "onActivityResult BT not enabled");
//				Toast.makeText(this, "Could not enable bluetooth adapter",
//						Toast.LENGTH_SHORT).show();
				btStatus = false;
				break;
			}
		}
		finish();
		datalink.btStatus(btStatus);
	}
}

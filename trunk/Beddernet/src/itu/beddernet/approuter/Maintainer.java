package itu.beddernet.approuter;

import android.util.Log;
import itu.beddernet.common.BeddernetInfo;
import itu.beddernet.datalink.bluetooth.BluetoothDatalink;

/**
 * Oversees Bluetooth scatternet maintenance tasks, initial and regular scans
 *
 */
public class Maintainer extends Thread {

	private static final String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	private boolean aborted;
	private int searchIntervalTime;
	private boolean initScan;

	/**
	 * Default constructor for mainainer, uses default discovery interval
	 */
	public Maintainer() {
		searchIntervalTime = itu.beddernet.common.BeddernetInfo.DISCOVERY_INTERVAL;
	}

	/**
	 * Manual constructor for mainainer
	 * @param intervalInMinutes sets base scanning interval
	 */
	public Maintainer(int intervalInMinutes) {
		searchIntervalTime = 60 + 1000 + intervalInMinutes;
	}

	public void run() {

		aborted = false;
		while (!aborted) {
			initialScan();
			while (!aborted && !initScan) {
				normalScan();
			}
		}
		Log.i(TAG, " Maintiner exiting");
	}

	/**
	 * Handles regular  scans then the device is connected to scatternet
	 */
	private void normalScan() {

		double numberOfNeighbours = BluetoothDatalink
				.getBluetoothDatalinkInstance().numberOfNeighbours();
		// determine how long to wait between scans depending on the number of
		// neighbours
		if (numberOfNeighbours <= 0) {
			Log
					.i(TAG,
							" Maintiner has no neighbours, going to initital scan mode");
			initScan = true;
			return;
		}
		//Increase the interval for every connected device
		long currentInterval = (long) (numberOfNeighbours + 1) * searchIntervalTime;
		int initialWait = (int) (currentInterval * Math.random());
		try {
			Thread.sleep(initialWait);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Log.e(TAG, "Maintainance sleep interrupted "
					+ "discovery might start too soon", e);
			if (aborted) {
				return;
			}
		}
		Log.i(TAG, " Maintiner starting normal scan");

		long result = BluetoothDatalink.getBluetoothDatalinkInstance()
				.searchNewConnections();
		if (result != -1) {
			Log.i(TAG, "Discovery finished, no neighbours");
		} else {
			Log.i(TAG, "Discovery finished, found neighbours");
		}
		try {
			Thread.sleep(currentInterval - initialWait);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Log.e(TAG, "Maintainance sleep interrupted, discovery "
					+ "might start too soon", e);
			if (aborted) {
				return;
			}
		}
	}

	/**
	 * Handles initial scans when device is not connected to scatternet
	 */
	private void initialScan() {
		double rand = Math.random();
		Log.d(TAG, "random is = " + rand);
		if (rand < 0.33) {
			Log
					.i(TAG,
							" No devices connected, maintiner starting initial phase scan");
			long result = BluetoothDatalink.getBluetoothDatalinkInstance()
					.searchNewConnections();

			// If discovery returns that neighbours have been found we break
			// out of initial scan mode
			if (result != -1) {
				Log
						.i(TAG,
								" Maintainer found devices in initial phase scan, going to normal scan");
				return;
			}
		} else {
			try {
				Log.i(TAG, " Maintiner in initial phase mode, not scanning");

				Thread.sleep(BeddernetInfo.SCAN_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
				Log
						.e(
								TAG,
								"Maintainance sleep interrupted, discovery might start too soon",
								e);
				if (aborted) {
					return;
				}
			}
		}
		initScan = false;
	}

	public void abort() {
		Log.i(TAG, "Maintainer aborted");
		this.aborted = true;
		interrupt();
	}
}

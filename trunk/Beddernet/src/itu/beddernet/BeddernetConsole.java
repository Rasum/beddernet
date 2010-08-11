package itu.beddernet;

import itu.beddernet.approuter.BeddernetService;
import itu.beddernet.approuter.IBeddernetService;
import itu.beddernet.approuter.IBeddernetServiceCallback;
import itu.beddernet.common.NetworkAddress;
import itu.beddernet.router.dsdv.info.ConfigInfo;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class BeddernetConsole extends Activity implements ServiceConnection {

	private static final int CONTEXT_MENU_SEND_MESSAGE = 1;
	private static final int CONTECT_MENU_DISCONNECT = 2;
	private static final int CONTEXT_MENU_SEND_FILE = 3;
	private static final int CONTEXT_MENU_SEND_RTT = 4;
	private static final int CONTEXT_MENU_VIEW_SERVICES = 5;
	private static final int CONTEXT_MENU_SEND_FILE_DUPLEX = 6;

	private static final int MENU_DISCOVERY = 0;
	private static final int MENU_DISCOVERABLE = 1;
	private static final int MENU_BLUETOOTH_OFF = 2;
	private static final int MENU_BEDNET_OFF = 3;
	private static final int MENU_MANUAL_REFRESH = 4;
	private static final int MENU_SERVICES = 5;

	private static final byte FILE_MESSAGE = 1;
	private static final byte TEXT_MESSAGE = 2;
	private static final byte FILE_END = 3;
	private static final byte RTT_MESSAGE = 4;
	private static final byte RTT_MESSAGE_REPLY = 5;
	private static final byte TEST_END = 6;
	private static final byte FILE_END_ACK = 7;
	private static final byte FILE_FRANSFER_REQUEST = 8;

	private static final int bufferSize = 5000;

	private long RTTStartTime = 0;
	private long RTTEndTime = 0;

	byte[] rttMessage = new byte[9];
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	private IBeddernetService mBeddernetService;
	private Activity activity;
	private ArrayAdapter<String> mDeviceArrayAdapter;
	private ListView mDeviceView;
	public static String applicationIdentifier = "BeddernetConsole";
	public static long applicationIdentifierHash = applicationIdentifier
			.hashCode();
	@SuppressWarnings("unused")
	private String serviceConnectionStatus;
	ServiceConnection sc = this;
	public TextView outputTextView;
	private int filesPending;

	protected void onDestroy() {
		if (mBeddernetService != null) {
			try {
				mBeddernetService.unregisterCallback(mCallback,
						applicationIdentifier);
			} catch (RemoteException e) {
				Log.e(TAG, "Console could't unregister callback", e);
			}
			unbindService(sc);
		}
		super.onDestroy();
	}

	protected void onResume() {
		Log.d(TAG, "resuming");
		if (mBeddernetService == null) {
			Log.d(TAG, "the service connection is null - rebooting");
			// onCreate(null);
			Intent bindIntent = new Intent(
					"itu.beddernet.approuter.BeddernetService");
			this.bindService(bindIntent, this, Context.BIND_AUTO_CREATE);
		}
		super.onResume();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		Intent bindIntent = new Intent(
				"itu.beddernet.approuter.BeddernetService");

		this.bindService(bindIntent, this, Context.BIND_AUTO_CREATE);
		setContentView(R.layout.main);
		Button milestoneBox = (Button) findViewById(R.id.Milestone);
		milestoneBox.setOnClickListener(buttonListnener);
		Button magicBox = (Button) findViewById(R.id.Magic);
		magicBox.setOnClickListener(buttonListnener);
		Button SvalurBox = (Button) findViewById(R.id.Svalur);
		SvalurBox.setOnClickListener(buttonListnener);
		Button ituHeroButton = (Button) findViewById(R.id.ituHero);
		ituHeroButton.setOnClickListener(buttonListnener);
		Button rasmusLaptopButton = (Button) findViewById(R.id.rasmusLaptop);
		rasmusLaptopButton.setOnClickListener(buttonListnener);
		Button mr4Button = (Button) findViewById(R.id.Dongle4);
		mr4Button.setOnClickListener(buttonListnener);

		Button MSIBox = (Button) findViewById(R.id.MSI);
		MSIBox.setOnClickListener(buttonListnener);
		CheckBox maintainer = (CheckBox) findViewById(R.id.MaintainerBox);
		maintainer.setOnCheckedChangeListener(checkBoxListener);

		// Initialize the array adapter for the conversation thread
		mDeviceArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		mDeviceView = (ListView) findViewById(R.id.in);
		registerForContextMenu(mDeviceView);

		mDeviceView.setAdapter(mDeviceArrayAdapter);
		mDeviceView.setOnItemClickListener(mListListener);
		mDeviceView.setHapticFeedbackEnabled(true);
		activity = this;

		outputTextView = (TextView) findViewById(R.id.outputTextView);
	}

	/* Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_MANUAL_REFRESH, 0, "Refresh device list");
		menu.add(0, MENU_DISCOVERY, 0, "Find devices");
		menu.add(0, MENU_SERVICES, 0, "View local services");
		menu.add(0, MENU_DISCOVERABLE, 0, "Discoverable");
		menu.add(0, MENU_BLUETOOTH_OFF, 0, "Bluetooth off");
		menu.add(0, MENU_BEDNET_OFF, 0, "Bednet off");
		return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_MANUAL_REFRESH:
			refreshDeviceList();
			return true;
		case MENU_DISCOVERABLE:
			try {
				mBeddernetService.setDiscoverable(true);
			} catch (RemoteException e) {
				Log.e(TAG, "Remote Exception while making discoverable", e);
			}
			return true;
		case MENU_BEDNET_OFF:
			Log.d(TAG, "onClick: stopping service");
			finish();
			return true;
		case MENU_BLUETOOTH_OFF:
			Log.d(TAG, "Beddernet tries to manually turn Bluetooth off");
			try {
				mBeddernetService.disableBluetooth();
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to kill bluetooth", e);
			}
			return true;
		case MENU_DISCOVERY:
			Log.d(TAG, "findNeighbors in BeddernetConsole");
			new findNeighborsTask(mBeddernetService).execute(null, null, null);
			return true;

		case MENU_SERVICES:
			Log.d(TAG, "view services in BeddernetConsole");

			try {
				long[] hashes = mBeddernetService
						.getAllUAIHOnDevice(NetworkAddress
								.castNetworkAddressToString(ConfigInfo.netAddressVal));
				StringBuilder sb = new StringBuilder();
				sb.append("Service hashes on devive:\n");
				for (long l : hashes) {
					sb.append(l);
					sb.append("\n");
				}
				if (hashes != null) {
					Toast.makeText(
							this,
							"Number of hashes: " + hashes.length + " "
									+ sb.toString(), Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(this, "Hashes was null", Toast.LENGTH_LONG)
							.show();
				}
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to get services", e);
			}

			return true;
		}
		return false;
	}

	public void onServiceDisconnected(ComponentName name) {
		serviceConnectionStatus = "Service disconnected";
	}

	private void refreshDeviceList() {
		mDeviceArrayAdapter.clear();
		String[] deviceList = null;
		try {
			deviceList = mBeddernetService.getDevicesWithStatus();
		} catch (Exception e) {
			Log.e(TAG, "Remote Exception while getting list of devices", e);
		}
		if (deviceList != null && deviceList.length > 0) {
			Log.d(TAG, "getDevicesWithStatus is not null");
			for (int i = 0; i <= deviceList.length / 2; i = i + 2) {
				Log.d(TAG, "List size: " + deviceList.length);
				mDeviceArrayAdapter
						.add(deviceList[i] + ":" + deviceList[i + 1]);
			}
		}
	}

	public void onServiceConnected(ComponentName className, IBinder service) {
		try {
			Log.d(TAG, "Service connected:" + service.getInterfaceDescriptor());
		} catch (RemoteException e) {
			e.printStackTrace();
			Log.d(TAG, "Service connected but something fucked up");
		}
		mBeddernetService = IBeddernetService.Stub.asInterface(service);
		if (mBeddernetService == null)
			Log.e(TAG, "MyService is nul!!?!");
		// Synchronously
		try {
			applicationIdentifierHash = mBeddernetService.registerCallback(
					mCallback, applicationIdentifier);
			Log.d(TAG, "AIH received from server on register: "
					+ applicationIdentifierHash);
		} catch (RemoteException e) {

			Log.e(TAG,
					"Remote exception from service while registering callback: "
							+ e.getMessage());
			e.printStackTrace();
		}
	}

	private OnClickListener buttonListnener = new OnClickListener() {
		public void onClick(View src) {
			switch (src.getId()) {
			case R.id.Magic:
				try {
					mBeddernetService.manualConnect("00:22:A5:F7:6C:50");
				} catch (RemoteException e1) {
					Log.e(TAG, "Could not manually connect", e1);
				}
				refreshDeviceList();
				break;
			case R.id.Dongle4:
				try {
					mBeddernetService.manualConnect("00:15:83:18:5C:BB");
				} catch (RemoteException e1) {
					Log.e(TAG, "Could not manually connect", e1);
				}
				refreshDeviceList();
				break;
			case R.id.Milestone:
				try {
					mBeddernetService.manualConnect("00:24:BA:97:58:77");
				} catch (RemoteException e1) {
					Log.e(TAG, "Could not manually connect", e1);
				}
				refreshDeviceList();
				break;
			case R.id.Svalur:
				try {
					mBeddernetService.manualConnect("00:15:83:15:A2:B8");
				} catch (RemoteException e1) {
					Log.e(TAG, "Could not manually connect", e1);
				}
				refreshDeviceList();
				break;
			case R.id.ituHero:
				try {
					mBeddernetService.manualConnect("00:22:A5:B3:2D:3E");
				} catch (RemoteException e1) {
					Log.e(TAG, "Could not manually connect", e1);
				}
				refreshDeviceList();
				break;

			case R.id.MSI:
				try {
					mBeddernetService.manualConnect("00:22:A5:B4:78:C3");
				} catch (RemoteException e1) {
					Log.e(TAG, "Could not manually connect", e1);
				}
				refreshDeviceList();
				break;

			case R.id.rasmusLaptop:
				try {
					mBeddernetService.manualConnect("00:03:78:CB:DA:6F");
				} catch (RemoteException e1) {
					Log.e(TAG, "Could not manually connect", e1);
				}
				refreshDeviceList();
				break;
			}
		}
	};

	private OnItemClickListener mListListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

			String address = ((TextView) v).getText().toString();
			if (address != null) {
				address = address.substring(0, 17); // Ugly removes the status
				sendMessage(address);
			}
		}
	};

	@SuppressWarnings("unused")
	private void sendMulticast(String[] addresses, byte[] appMessage) {
		try {
			mBeddernetService.sendMulticast(addresses, null, appMessage,
					applicationIdentifier);
		} catch (RemoteException e) {
			Log.e(TAG, "Remote exception from service, could not send message");
			e.printStackTrace();
		}
	}

	private void sendMessage(String address) {
		Log.i(TAG,
				"BedderTestPlatform: DeviceList clicked, sending message to: "
						+ address);
		outputTextView.append("Sending message to " + address + "\n");
		try {
			byte[] message = "---Hello from BedderTestPlatform".getBytes();
			message[0] = 2;
			mBeddernetService.sendUnicast(address, null, message,
					applicationIdentifier);
		} catch (RemoteException e) {
			Log.e(TAG, "Remote exception from service, could not send message");
			e.printStackTrace();
		}
	}

	public void fileTransferComplete() {

		filesPending--;
		if (filesPending < 0)
			filesPending = 0;
		outputTextView.append("File transfer over, pending: " + filesPending);

	}

	private void sendFile(String address) {
		Log.i(TAG, "Send file called");

		// outputTextView.append("Sending file to " + address + "\n");
		InputStream input = null;
		try {
			input = activity.getResources().openRawResource(R.raw.audio);
		} catch (NotFoundException e2) {
			Log.e(TAG, "Couldn't open resource", e2);
		}
		byte[] buffer = new byte[bufferSize];
		buffer[0] = FILE_MESSAGE;
		try {
			long startTime = System.currentTimeMillis();
			while (input.read(buffer, 1, buffer.length - 1) != -1) {
				mBeddernetService.sendUnicast(address, null, buffer,
						applicationIdentifier);
			}
			byte[] end = new byte[1];
			end[0] = FILE_END;
			mBeddernetService.sendUnicast(address, null, end,
					applicationIdentifier);
			long endTime = System.currentTimeMillis();
			String result = ("File sent to " + address + "\nSending took:"
					+ (endTime - startTime) + " milliseconds");
			Log.i(TAG, result);
			// outputTextView.append(result);
			byte[] testEnd = new byte[] { TEST_END };
			mBeddernetService.sendUnicast(address, null, testEnd,
					applicationIdentifier);
		} catch (Exception e) {
			Log.e(TAG, "Error in sending to service", e);
		}
	}

	public void sendRTT(String address) {
		RTTStartTime = System.currentTimeMillis();
		rttMessage[0] = RTT_MESSAGE;
		try {
			mBeddernetService.sendUnicast(address, null, rttMessage,
					applicationIdentifier);
			outputTextView.append("Ping sent to: " + address + "\n");
		} catch (RemoteException e) {
			Log.e(TAG, "Failed to send RTT message, remote exception");
		}
	}

	@SuppressWarnings("unused")
	private void fileTransferTest(int fileIterations, String toAddress) {
		for (int i = 0; i < fileIterations; i++) {
			sendFile(toAddress);
		}
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		menu.add(0, CONTEXT_MENU_SEND_MESSAGE, 0, "Send Message");
		menu.add(0, CONTEXT_MENU_SEND_FILE, 0, "Send file");
		menu.add(0, CONTEXT_MENU_SEND_FILE_DUPLEX, 0, "Send and receive file");
		menu.add(0, CONTEXT_MENU_SEND_RTT, 0, "Send ping");
		menu.add(0, CONTEXT_MENU_VIEW_SERVICES, 0, "View services");
		menu.add(0, CONTECT_MENU_DISCONNECT, 0, "Disconnect");
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		String selectedAddress = (String) mDeviceView.getAdapter().getItem(
				info.position);
		if (selectedAddress != null) {
			selectedAddress = selectedAddress.substring(0, 17);// Removes
			// the status
		}
		switch (item.getItemId()) {
		case CONTEXT_MENU_SEND_MESSAGE:
			sendMessage(selectedAddress);
			Log.i(TAG, "Trying to send message to : " + selectedAddress + "?");
			return true;
		case CONTEXT_MENU_SEND_FILE_DUPLEX:
			new DuplexFileTest(this).execute(selectedAddress, null, null);
			Log.i(TAG, "Trying to send and receive message : "
					+ selectedAddress + "?");
			return true;
		case CONTECT_MENU_DISCONNECT:
			try {
				mBeddernetService.manualDisconnect(selectedAddress);
			} catch (RemoteException e) {
				Log.e(TAG, "Could not manually disconnect", e);
				e.printStackTrace();
			}
			return true;
		case CONTEXT_MENU_SEND_FILE:
			sendFile(selectedAddress);
			return true;
		case CONTEXT_MENU_SEND_RTT:
			sendRTT(selectedAddress);
			return true;
		case CONTEXT_MENU_VIEW_SERVICES:
			try {
				long[] hashes = mBeddernetService
						.getAllUAIHOnDevice(selectedAddress);
				StringBuilder sb = new StringBuilder();
				sb.append("Service hashes on devive:\n");
				for (long l : hashes) {
					sb.append(l);
					sb.append("\n");
				}
				if (hashes != null) {
					Toast.makeText(
							this,
							"Number of hashes: " + hashes.length + " "
									+ sb.toString(), Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(this, "Hashes was null", Toast.LENGTH_LONG)
							.show();
				}
				Log.i(TAG, "List of services on device called: "
						+ sb.toString());
			} catch (RemoteException e) {
				Log.e(TAG, "Couldn't show all hashes", e);
			}
		default:
			return super.onContextItemSelected(item);
		}
	}

	private android.widget.CompoundButton.OnCheckedChangeListener checkBoxListener = new android.widget.CompoundButton.OnCheckedChangeListener() {

		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (isChecked) {
				try {
					mBeddernetService.startMaintainer();
				} catch (Exception e) {
					Log.e(TAG, "Could not start maintainer", e);
				}
			} else {
				try {
					mBeddernetService.stopMaintainer();
				} catch (Exception e) {
					Log.e(TAG, "Could not start maintainer", e);
				}
			}
		}
	};

	private IBeddernetServiceCallback mCallback = new IBeddernetServiceCallback.Stub() {

		private FileOutputStream fileOut;
		private BufferedOutputStream out;
		private boolean transferring = false;
		private long startTime;
		private long endTime;
		private double transferTime;
		private long transferedBytes = 0;
		private double kBitsPerSek = 0;
		// private FileOutputStream fileOutput;
		private long RTTTime;

		/**
		 * This is called by the remote service regularly to tell us about new
		 * values. Note that IPC calls are dispatched through a thread pool
		 * running in each process, so the code executing here will NOT be
		 * running in our main thread like most other things -- so, to update
		 * the UI, we need to use a Handler to hop over there.
		 */

		public long getApplicationIdentifierHash() throws RemoteException {
			Log.d(TAG, "Token sent to server :" + applicationIdentifierHash);
			return applicationIdentifierHash;
		}

		public void update(String senderAddress, byte[] message)
				throws RemoteException {
			byte type = message[0];
			// Log.d(TAG, "Message received at BedderTestPlatform");
			switch (type) {

			case FILE_MESSAGE:
				if (!transferring) {
					// outputTextView.append("Receiving file from: "+
					// senderAddress+ "\n");

					startTime = System.currentTimeMillis();
					transferring = true;
					try {
						fileOut = openFileOutput("audio2.ogg", MODE_PRIVATE);
						out = new BufferedOutputStream(fileOut);
					} catch (IOException e) {
						Log
								.e(
										TAG,
										"Error in writing to stream, closing outPut stream",
										e);
					}

				}
				transferedBytes = transferedBytes + message.length;
				try {
					out.write(message, 1, message.length - 1);
				} catch (IOException e1) {
					Log.d(TAG, "Exception in BedderTestPlatform - out.write",
							e1);
				}
				break;
			case RTT_MESSAGE:
				// outputTextView.append("Received ping from: "+ senderAddress+
				// "\n");
				byte[] messageString = { RTT_MESSAGE_REPLY };
				mBeddernetService.sendUnicast(senderAddress, null,
						messageString, applicationIdentifier);
				break;
			case RTT_MESSAGE_REPLY:
				RTTEndTime = System.currentTimeMillis();
				RTTTime = RTTEndTime - RTTStartTime;
				Log.i(TAG, "RTT reply received, RTT time: " + RTTTime);
				outputTextView.append("RTT reply received, RTT time: "
						+ RTTTime + "\n");

			case TEXT_MESSAGE:
				String msg = new String(message, 1, message.length - 1);
				outputTextView.append("Message received from: " + senderAddress
						+ "Message text: " + msg + "\n");
				Log.i(TAG, "Text message received: " + msg);
				break;
			case FILE_END:
				endTime = System.currentTimeMillis();
				transferTime = endTime - startTime;
				kBitsPerSek = (transferedBytes / (transferTime / 1000)) * 8;
				String result = ("Transfer over: " + transferedBytes
						+ " bytes sent in : " + transferTime
						+ " milliseconds. kilobits per second: "
						+ (int) kBitsPerSek + "\n");
				Log.i(TAG, result);
				byte[] ackMessage = { FILE_END_ACK };
				mBeddernetService.sendUnicast(senderAddress, null, ackMessage,
						applicationIdentifier);
				outputTextView.append(result);
				try {
					out.close();
					fileOut.close();
				} catch (IOException e) {
					Log.e(TAG, "Could not close outputStreams", e);
				}
				// Clean up
				transferring = false;
				transferedBytes = 0;
				break;
			case FILE_END_ACK:
				fileTransferComplete();
				break;
			case FILE_FRANSFER_REQUEST:
				sendFile(senderAddress);
				break;
			default:
				break;
			}
		}

		public void updateWithSendersApplicationIdentifierHash(
				String senderAddress, long senderApplicationIdentifierHash,
				byte[] message) throws RemoteException {
			// Not implemented
			Log
					.i(TAG,
							"nonimplemented update method called, normal onne called instead");
			update(senderAddress, message);

		}
	};

	private class DuplexFileTest extends AsyncTask<String, Object, Object> {

		private BeddernetConsole console;

		public DuplexFileTest(BeddernetConsole console) {
			this.console = console;
		}

		protected Object doInBackground(String... params) {
			// console.duplexFiletransferTest(params[0], 5);
			duplexFileTransfer(params[0], 5);
			return null;
		}

		private void duplexFileTransfer(String address, int fileIterations) {
			for (int i = 0; i < fileIterations; i++) {
				byte[] startFileMsg = { FILE_FRANSFER_REQUEST };
				try {
					mBeddernetService.sendUnicast(address, null, startFileMsg,
							applicationIdentifier);
				} catch (RemoteException e1) {
					Log.e(TAG, "duplexFiletransferTest error", e1);
				}
				sendFile(address);
				filesPending++;
				while (true) {
					if (filesPending > 0) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						// outputTextView.append("File transfer over, pending:"
						// + filesPending);
						break;
					}
				}
			}

		}

		protected void onPostExecute() {
		}
	}
}
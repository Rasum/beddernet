package itu.beddernet.approuter;

import itu.beddernet.BeddernetConsole;
import itu.beddernet.common.NetworkAddress;
import itu.beddernet.router.RouterInterface;
import itu.beddernet.router.dsdv.DsdvRouter;
import itu.beddernet.router.dsdv.info.ConfigInfo;
import itu.beddernet.router.message.multi.MultiAppMessage;
import itu.beddernet.router.message.uni.UniAppMessage;
import itu.beddernet.router.message.uni.UniAppMessageUAIH;
import itu.beddernet.approuter.IBeddernetService;
import itu.beddernet.approuter.IBeddernetServiceCallback;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

/**
 * Main service of Beddernet
 * 
 */
public class BeddernetService extends Service {

	private static Context returnContext;
	/**
	 * List of callback items (Connected applications)
	 */
	final RemoteCallbackList<IBeddernetServiceCallback> mCallbacks = new RemoteCallbackList<IBeddernetServiceCallback>();
	private AppRouterHandler handler;
	private RouterInterface router;
	private static String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	private int connectedApplications = 0;

	Random random;
	final int HELLO_ID = 1;

	/**
	 * Holds ApplicationIdentifierHash, BroadcastItem mappings
	 */
	Hashtable<Long, IBeddernetServiceCallback> broadcastTable = new Hashtable<Long, IBeddernetServiceCallback>();

	/**
	 * Holds all clients connected with the service. Key is the clients unique
	 * applicationIdentifierHash
	 */
	protected Hashtable<Long, BeddernetClient> clientTable = new Hashtable<Long, BeddernetClient>();

	/**
	 * Oversees bluetooth maintenance thread
	 */
	private Maintainer maintainer;

	public void onCreate() {
		// start tracing to "/sdcard/BedderTrace.trace"
		// Debug.startMethodTracing("BedderTrace");
		returnContext = this;
		handler = new AppRouterHandler(this); // We don't use this
		router = new DsdvRouter(handler, this);
		router.setup();

		// Maintainer is currently optional and not started onCreate
		// startMaintainer();

		sendNotification();
	}

	/**
	 * Makes a notification appear on main screen.
	 */
	private void sendNotification() {

		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		// TODO: Use own logo
		int icon = itu.beddernet.R.drawable.notificationicon;
		CharSequence tickerText = "Beddernet Started";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		Context context = getApplicationContext();
		CharSequence contentTitle = "Beddernet";
		CharSequence contentText = "Select to manage network";
		Intent notificationIntent = new Intent(this, BeddernetConsole.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);

		mNotificationManager.notify(HELLO_ID, notification);

	}

	@Override
	public IBinder onBind(Intent intent) {
		return serviceBinder;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// Log.d(TAG, "onStart called in BeddernetService:");
		super.onStart(intent, startId);

	}

	/**
	 * The IRemoteInterface is defined through IDL
	 */
	private final IBeddernetService.Stub serviceBinder = new IBeddernetService.Stub() {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * itu.beddernet.approuter.IBEDnetService#registerCallback(itu.beddernet.approuter
		 * .IBEDnetServiceCallback, java.lang.String) Synchronously returns a
		 * applicationIdentifierhash that serves as an access key to the
		 * scatternet
		 */
		public long registerCallback(IBeddernetServiceCallback cb,
				String applicationIdentifier) {
			connectedApplications++;
			Log.i(TAG, "registerCallback called: Connected applications: "
					+ connectedApplications);
			
			long applicationIdentifierHash = 0;
			if (cb != null) {
				try {
					mCallbacks.register(cb);
					Log.i(TAG, "Callback registered, service string: "
							+ applicationIdentifier);
					applicationIdentifierHash = beddernetHash(applicationIdentifier);
					Log.i(TAG, "applicationIdentifierHash: "
							+ applicationIdentifierHash);
					BeddernetClient client = new BeddernetClient(
							applicationIdentifierHash, applicationIdentifier);
					client.setBroadcastItem(cb);
					clientTable.put(applicationIdentifierHash, client);
					broadcastTable.put(applicationIdentifierHash, cb);
					router.addUAIH(applicationIdentifierHash);

				} catch (Exception e) {
					Log.e(TAG, "Exception in regisering callback", e);
				}
			} else {

				Log.e(TAG, "Callback from client was null");
			}
			return applicationIdentifierHash;
		}

		/**
		 * Creates a Beddernet ApplicationIdentifierHash UAIH
		 * 
		 * @param uais
		 *            Unique application identifier string (UAIS)
		 * @return Unique application identifier hash (UAIH)
		 */
		private long beddernetHash(String uais) {
			int hash1 = uais.hashCode();
			int hash2 = (uais + "a").hashCode();
			long l = ((long) hash1) << 32 | hash2;
			return l;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see itu.beddernet.approuter.IBEDnetService#startMaintainer()
		 */
		public void startMaintainer() {
			if (maintainer == null) {
				maintainer = new Maintainer();

				maintainer.start();
				Log.i(TAG, "Maintainer started");
			}
		}

		public void stopMaintainer() {
			if (maintainer != null) {
				maintainer.abort();
				maintainer = null;
				Log.i(TAG, "Maintainer stopped");
			}
		}

		public void unregisterCallback(IBeddernetServiceCallback cb,
				String applicationIdentifier) {
			
			connectedApplications--;
			Log.d(TAG, "unregisterCallback called. Connected applications: "
					+ connectedApplications);

			if (connectedApplications == 0) {
				Log.d(TAG, "No applications connected");
				// onDestroy();
				// stopSelf();
			}
			
			if (cb != null) {
				mCallbacks.unregister(cb);
				long hash = beddernetHash(applicationIdentifier);
				broadcastTable.remove(hash);
				clientTable.remove(hash);
				router.removeUAIH(hash);
			}
		}

		public synchronized String[] getDevices(String applicationIdentifier)
				throws RemoteException {
			Log.d(TAG, "Available devices requested");
			// long hash = beddernetHash(applicationIdentifier);
			// Convert the longs to strings
			long[] networkAddresses = router.getAvailableDevices();
			String[] deviceList = new String[networkAddresses.length];
			for (int i = 0; i < networkAddresses.length; i++) {
				deviceList[i] = NetworkAddress
						.castNetworkAddressToString(networkAddresses[i]);
			}
			return deviceList;
		}

		public synchronized String[] getNeighbours(long token)
				throws RemoteException {
			Log.d(TAG, "Available devices requested");
			// Convert the longs to strings
			long[] networkAddresses = router.getNeighbours();
			String[] neighbourList = new String[networkAddresses.length];
			for (int i = 0; i < networkAddresses.length; i++) {
				neighbourList[i] = NetworkAddress
						.castNetworkAddressToString(networkAddresses[i]);
			}
			return neighbourList;
		}

		/**
		 * Sets the "Invokeable" variable for the applicaion. If true, BEDnet
		 * will attempt to launch the application if messages arrive for it and
		 * it is unavailable. Not implemented
		 */
		public void setInvocable(String intentURI, boolean invokable,
				String applicationIdentifier) throws RemoteException {
			// BeddernetClient tempDevice = clientTable
			// .get(beddernetHash(applicationIdentifier));
			// TODO Auto-generated method stub
		}

		public void sendMulticast(String[] networkAddress,
				String recipientApplicationIdentifier, byte[] appMessage,
				String applicationIdentifier) throws RemoteException {
			long serviceHash = beddernetHash(applicationIdentifier);

			long from = ConfigInfo.netAddressVal;
			long toAddresses[] = new long[networkAddress.length];

			// Convert to longs
			for (int i = 0; i < networkAddress.length; i++) {
				toAddresses[i] = NetworkAddress
						.castNetworkAddressToLong(networkAddress[i]);
			}
			MultiAppMessage multiMsg = new MultiAppMessage(from, toAddresses,
					serviceHash, appMessage);
			router.sendMessage(multiMsg);

		}

		/**
		 * Service hash is optional. If null is provided, the default service
		 * hash is used
		 */
		public void sendUnicast(String networkAddress,
				String recipientApplicationIdentifier, byte[] appMessage,
				String applicationIdentifier) throws RemoteException {
			long fromServiceHash = beddernetHash(applicationIdentifier);
			long networkAddressAsLong = NetworkAddress
					.castNetworkAddressToLong(networkAddress);

			if (recipientApplicationIdentifier != null) {
				long serviceHashAsLong = beddernetHash(recipientApplicationIdentifier);
				UniAppMessageUAIH msg = new UniAppMessageUAIH(
						ConfigInfo.netAddressVal, networkAddressAsLong,
						serviceHashAsLong, fromServiceHash, appMessage);
				router.sendMessage(msg);

			} else {
				UniAppMessage msg = new UniAppMessage(ConfigInfo.netAddressVal,
						networkAddressAsLong, fromServiceHash, appMessage);
				router.sendMessage(msg);
			}
		}

		public boolean getApplicationSupport(String deviceAddress,
				String applicationIdentifier) throws RemoteException {
			return router.getApplicationSupport(NetworkAddress
					.castNetworkAddressToLong(deviceAddress),
					beddernetHash(applicationIdentifier));
		}

		public void findNeighbors() throws RemoteException {
			Log.d(TAG, "findNeighbors called");
			router.searchNewNeighbours();

		}

		public void setDiscoverable(boolean discoverable) {
			Log.d(TAG, "setDiscoverable called");

			if (BluetoothAdapter.getDefaultAdapter().getState() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
				Log.e(TAG, "Not discoverable, manually requested discoverable");

				Intent discoverableIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				discoverableIntent.putExtra(
						BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
				BeddernetService.getBeddernetInstance().startActivity(
						discoverableIntent);
			}
		}

		public void manualConnect(String remoteAddress) throws RemoteException {
			router.manualConnect(remoteAddress);
		}

		public void manualDisconnect(String remoteAddress)
				throws RemoteException {
			router.manualDisconnect(remoteAddress);
		}

		public void disableBluetooth() throws RemoteException {
			BluetoothAdapter.getDefaultAdapter().disable();
		}

		public String[] getDevicesSupportingUAIS(String UAIS) {
			long beddernetHash = NetworkAddress.castNetworkAddressToLong(UAIS);
			long[] deviceListLong = router
					.getDevicesSupportingUAIH(beddernetHash);
			String[] deviceListString = new String[deviceListLong.length];
			for (int i = 0; i < deviceListLong.length; i++) {
				deviceListString[i] = NetworkAddress
						.castNetworkAddressToString(deviceListLong[i]);
			}
			return deviceListString;
		}

		public long[] getAllUAIHOnDevice(String deviceAddress) {
			return router.getAllUAIHOnDevice(NetworkAddress
					.castNetworkAddressToLong(deviceAddress));
		}

		public String[] getDevicesWithStatus() throws RemoteException {
			Log.d(TAG, "Available devices with status requested");
			long[] networkAddresses = router.getAvailableDevices();
			String[] deviceList = null;
			if (networkAddresses.length > 0) {
				deviceList = new String[networkAddresses.length * 2];
				int y = 0;
				for (int i = 0; i <= networkAddresses.length; i = i + 2) {
					deviceList[i] = NetworkAddress
							.castNetworkAddressToString(networkAddresses[y]);
					deviceList[i + 1] = router.getStatus(networkAddresses[y]);
					Log.d(TAG, "Available devices with status requested: "
							+ router.getStatus(networkAddresses[y]));
					y++;
				}
				networkAddresses = null;
			}
			return deviceList;
		}

	};

	public static Context getBeddernetInstance() {
		if (returnContext == null) {
			Log.e(TAG, "BeddernetService was null");
		}
		return returnContext;

	}

	@Override
	public void onDestroy() {
//		Toast.makeText(this, "BEDNet service stopping", Toast.LENGTH_LONG)
//		.show();
		Log.d(TAG, "Service onDestroy");
		// Abort all running threads
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
		.cancel(HELLO_ID);
		router.stopApplication();
		// stopMaintainer();
		// Debug.stopMethodTracing();
		System.gc();
		this.stopSelf();
		super.onDestroy();
	}

	@Override
	public boolean onUnbind(Intent intent) {

		return super.onUnbind(intent);
	}

	/**
	 * Called by the router when it is ready to finish startup
	 * 
	 * @param dsdvRouterStatus
	 *            true if router is ready to continue
	 */
	public void dsdvRouterStatus(boolean dsdvRouterStatus) {
		if (dsdvRouterStatus) {
			router.startApplication();
			// Add UIAHs already bound
			Set<Long> clients = clientTable.keySet();
			Log.d(TAG, "The service has" + clientTable.size()
					+ " bound applications");
			Iterator<Long> i = clients.iterator();
			while (i.hasNext()) {
				router.addUAIH(i.next());
			}

			Log.d(TAG, "BEDnet Service started");
			random = new Random();
		} else {
			Toast.makeText(this, "The service could not be started",
					Toast.LENGTH_LONG).show();
			Log.d(TAG, "The service could not be started");
			stopSelf();
		}
	}
}

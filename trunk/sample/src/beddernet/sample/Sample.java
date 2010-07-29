package beddernet.sample;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import beddernet.ipc.IBeddernetService;
import beddernet.ipc.IBeddernetServiceCallback;

public class Sample extends Activity implements ServiceConnection {

	private IBeddernetService beddernetService;
	public static String applicationIdentifier = "sample application";
	public static long applicationIdentifierHash;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind();
	}

	private void bind() {
		Intent bindIntent = new Intent(
				"itu.beddernet.approuter.BeddernetService");
		this.bindService(bindIntent, this, Context.BIND_AUTO_CREATE);
	}

	protected void onDestroy() {
		try {
			// Unregistering with the service
			beddernetService
					.unregisterCallback(callback, applicationIdentifier);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		// Unbind with the service
		unbindService(this);
		super.onDestroy();
	}

	/**
	 * Rebind to the service after resume
	 */
	protected void onResume() {
		if (beddernetService == null) {
			// the service connection is null - rebooting");
			bind();
		}
		super.onResume();
	}

	public void onServiceDisconnected(ComponentName name) {
	}

	public void onServiceConnected(ComponentName className, IBinder service) {
		beddernetService = IBeddernetService.Stub.asInterface(service);
		if (beddernetService == null)
			try {
				applicationIdentifierHash = beddernetService.registerCallback(
						callback, applicationIdentifier);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
	}

	private IBeddernetServiceCallback callback = new IBeddernetServiceCallback.Stub() {

		/**
		 * This is called by the remote service regularly to tell us about new
		 * values. Note that IPC calls are dispatched through a thread pool
		 * running in each process, so the code executing here will NOT be
		 * running in our main thread like most other things -- so, to update
		 * the UI, we need to use a Handler to hop over there.
		 */

		public long getApplicationIdentifierHash() throws RemoteException {
			return applicationIdentifierHash;
		}

		public void update(String senderAddress, byte[] message)
				throws RemoteException {
		}

		@Override
		public void updateWithSendersApplicationIdentifierHash(
				String senderAddress, long senderApplicationIdentifierHash,
				byte[] message) throws RemoteException {

		}
	};
}
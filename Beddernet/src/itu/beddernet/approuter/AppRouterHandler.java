package itu.beddernet.approuter;

import itu.beddernet.common.NetworkAddress;
import itu.beddernet.router.message.uni.UniAppMessage;
import itu.beddernet.router.message.uni.UniAppMessageUAIH;
import itu.beddernet.approuter.IBeddernetServiceCallback;

import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

/**
 * Handles incoming messages from router
 *
 */
public class AppRouterHandler extends Handler {

	BeddernetService service;
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	IBeddernetServiceCallback applicationCallback;

	public AppRouterHandler(BeddernetService beDnetService) {
		this.service = beDnetService;
	}

	public void handleMessage(Message msg) {
		Log.d(TAG, "handleMessage recieved a message, message type: " + msg.what);
		switch (msg.what) {
		case itu.beddernet.router.Message.UNICAST_APPLICATION_MSG:
			deliverMessage((UniAppMessage) msg.obj);
			break;
		case itu.beddernet.router.Message.UNICAST_APPLICATION_MESSAGE_UAIH:
			deliverServiceMessage((UniAppMessageUAIH) msg.obj);
			break;
		default:
			break;
		}
	}

	
	/**
	 * Delivers message to service
	 * @param message
	 */
	private void deliverMessage(UniAppMessage message) {

		long  hash = message.getApplicationIdentifierHash();
		Log.d(TAG, "app hash from message: " +hash );
				applicationCallback = service.broadcastTable.get(hash);
				if (applicationCallback!= null){
				try {
					applicationCallback.update(NetworkAddress
							.castNetworkAddressToString(message
									.getFromNetworkAddress()), message
							.getAppMessage());
				} catch (RemoteException e) {
					// TODO Application lifecycle policy
					Log.e(TAG, "remote exception from AppRouterObserver", e);
				}
		} else {
			Log.e(TAG, "applicationCallback from broadcast table was null");
		}
	}

	/**
	 * Delivers message to service

	 * @param message
	 */
	private void deliverServiceMessage(UniAppMessageUAIH message) {

		long  hash = message.getUaih();
		applicationCallback = service.broadcastTable.get(hash);
		if (applicationCallback!= null){
				try {
					applicationCallback.updateWithSendersApplicationIdentifierHash(NetworkAddress
							.castNetworkAddressToString(message
									.getFromNetworkAddress()),
									message.getFromUaih(), 
									message
							.getAppMessage());
				} catch (RemoteException e) {
					// TODO Application lifecycle policy
					Log.e(TAG, "remote exception from AppRouterObserver", e);
				}
		} else {
			Log.e(TAG, "Callback item was null");
		}
	}
}

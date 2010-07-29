package itu.beddernet.approuter;

import itu.beddernet.approuter.IBeddernetServiceCallback;



/**
 * Encaplulates a client application that connects to the service, it contains 
 * the clients unique application identifiers.
 *
 */
public class BeddernetClient {

	
	private boolean isInvokable;
	
	private long applicationIdentifierHash;
	private String applicationIdentifierString;
	
	
	private IBeddernetServiceCallback broadcastItem;

	public BeddernetClient(long applicationIdentifierHash, String applicationIdentifierString){
		isInvokable = false;
		this.applicationIdentifierHash = applicationIdentifierHash; 
		this.applicationIdentifierString = applicationIdentifierString;
		
	}
	
	public boolean isInvokable() {
		return isInvokable;
	}
	public void setInvokable(boolean isInvokable) {
		this.isInvokable = isInvokable;
	}
	public IBeddernetServiceCallback getBroadcastItem() {
		return broadcastItem;
	}
	public void setBroadcastItem(IBeddernetServiceCallback cb) {
		this.broadcastItem = cb;
	}
	public long getApplicationIdentifierHash() {
		return applicationIdentifierHash;
	}
	public String getApplicationIdentifierString() {
		return applicationIdentifierString;
	}
}


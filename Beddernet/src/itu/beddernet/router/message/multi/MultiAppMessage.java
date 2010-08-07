package itu.beddernet.router.message.multi;

import itu.beddernet.router.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.util.Log;

/**
 * This is a multicast message used by top level applications to exchange data
 */
public class MultiAppMessage extends MultiMessage {

	/**
	 * The message's payload
	 */
	
	


	/**
	 * Class constructor
	 * 
	 * @param msg
	 *            create a message from the byte[]
	 */
	public MultiAppMessage(byte[] msg) {
		deserialize(msg);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Class constructor
	 * @param fromNetworkAddress specifies the from address
	 * @param toNetworkAddresses specifies the from addresses
	 * @param serviceHash The applications service hash on the receiving end 
	 * @param appMessage The message
	 */
	public MultiAppMessage(long fromNetworkAddress, long[] toNetworkAddresses,
			long serviceHash, byte[] appMessage) {

		this.type = Message.MULTICAST_APPLICATION_MSG;
		this.fromNetworkAddress = fromNetworkAddress;
		this.numberOfAddresses = (byte) toNetworkAddresses.length;
		this.toNetworkAddresses = toNetworkAddresses;
		serviceHash = serviceHash;
		appMessage = appMessage;

	}

	/**
	 * The method deserializes byte[] into a message object
	 * 
	 * @param msg
	 *            the byte[] to create the message from
	 */
	private void deserialize(byte[] msg) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(msg);
			DataInputStream dis = new DataInputStream(bais);

			// TODO: for normal app message, one SH should go here
			type = dis.readByte();
			fromNetworkAddress = dis.readLong();
			numberOfAddresses = dis.readByte();
			for (int i = 0; i < numberOfAddresses; i++) {
				toNetworkAddresses[i] = dis.readLong();
			}
			serviceHash = dis.readLong();
			appMessage = new byte[dis.available()];
			dis.readFully(appMessage);
		} catch (IOException ex) {
			Log.e(TAG, "Exception in MultiAppMessage -> deserialize");
		}
	}

	/**
	 * This methods serializes the message into a byte[]
	 * @return The message as a byte[]
	 * 
	 */
	public byte[] serialize() {
		byte[] data = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeByte(type);
			dos.writeLong(fromNetworkAddress);
			dos.writeByte(numberOfAddresses);
			for (long l : toNetworkAddresses) {
				dos.writeLong(l);
			}
			dos.writeLong(serviceHash);
			dos.write(appMessage, 0, appMessage.length);
			data = baos.toByteArray();

			baos.close();
			dos.close();
			baos = null;
			dos = null;

		} catch (IOException iOException) {
			Log.e(TAG, "Exception in MultiAppMessage -> serialize");
		}

		return data;
	}

	/**
	 * 
	 * @return the message payload
	 */
	public byte[] getAppMessage() {
		return appMessage;
	}

	/**
	 * 
	 * @return the application identifier hash that this message is intended for
	 */
	public long getServiceHash() {
		return serviceHash;
	}

	public void setToAddresses(long[] toAddresses){
		toNetworkAddresses = toAddresses;
		numberOfAddresses = (byte) toAddresses.length;
	}

}

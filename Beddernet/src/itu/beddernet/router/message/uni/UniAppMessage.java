package itu.beddernet.router.message.uni;

import itu.beddernet.router.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.util.Log;
/**
 * This is a unicast message used by top level applications to exchange data
 */
public class UniAppMessage extends UniMessage {

	/**
	 * The message's payload
	 */
	private byte[] appMessage;
	
	/**
	 * Indentifies what application this message is intended for
	 */
	private long applicationIdentifierHash;

	
	/**
	 * Class constructor
	 * 
	 * @param msg
	 *            create a message from the byte[]
	 */
	public UniAppMessage(byte[] msg) {
		deserialize(msg);
	}

	
	/**
	 * Class constructor
	 * @param fromNetworkAddress specifies the to address
	 * @param toNetworkAddress specifies the from  address
	 * @param serviceHash The applications service hash on the receiving end 
	 * @param appMessage The message
	 */
	public UniAppMessage(long fromNetworkAddress, long toNetworkAddress,
			long applicationIdentifierHash, byte[] appMessage) {
		this.type = Message.UNICAST_APPLICATION_MSG;
		this.fromNetworkAddress = fromNetworkAddress;
		this.toNetworkAddress = toNetworkAddress;
		this.applicationIdentifierHash = applicationIdentifierHash;
		this.appMessage = appMessage;

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
			toNetworkAddress = dis.readLong();
			applicationIdentifierHash = dis.readLong();
			appMessage = new byte[dis.available()];
			dis.readFully(appMessage);
		} catch (IOException ex) {
			ex.printStackTrace();
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
			dos.writeLong(toNetworkAddress);
			dos.writeLong(applicationIdentifierHash);
			dos.write(appMessage, 0, appMessage.length);
			data = baos.toByteArray();

			baos.close();
			dos.close();
			baos = null;
			dos = null;

		} catch (IOException iOException) {
			Log.e(TAG, "Exception in UniAppMessage -> serialize");
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
	public long getApplicationIdentifierHash() {
		return applicationIdentifierHash;
	}

}

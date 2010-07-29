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
public class MultiAppMessageUAIH extends MultiMessage {

	/**
	 * The message's payload
	 */
	private byte[] appMessage;

	/**
	 * Indentifies what application this message is intended for
	 */
	private long uaih;

	/**
	 * Class constructor
	 * 
	 * @param msg
	 *            create a message from the byte[]
	 */
	
	/**
	 * The identifier of the sender application
	 */
	private long fromUaih;
	
	public MultiAppMessageUAIH(byte[] msg) {
		deserialize(msg);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Class constructor
	 * 
	 * @param fromNetworkAddress
	 *            specifies the from address
	 * @param toNetworkAddresses
	 *            specifies the from addresses
	 * @param serviceHash
	 *            The applications service hash on the receiving end
	 * @param appMessage
	 *            The message
	 */
	public MultiAppMessageUAIH(long fromNetworkAddress, long[] toNetworkAddresses,
			long serviceHash, long returnServiceHash, byte[] appMessage) {

		this.type = Message.MULTICAST_APPLICATION_MSG;
		this.fromNetworkAddress = fromNetworkAddress;
		this.numberOfAddresses = (byte) toNetworkAddresses.length;
		this.toNetworkAddresses = toNetworkAddresses;
		this.uaih = serviceHash;
		this.appMessage = appMessage;
		this.fromUaih = returnServiceHash;

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
			uaih = dis.readLong();
			fromUaih = dis.readLong();
			appMessage = new byte[dis.available()];
			dis.readFully(appMessage);
		} catch (IOException ex) {
			Log.e(TAG, "Exception in MultiAppMessageUAIH -> deserialize");
		}
	}

	/**
	 * This methods serializes the message into a byte[]
	 * 
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
			dos.writeLong(uaih);
			dos.writeLong(fromUaih);
			dos.write(appMessage, 0, appMessage.length);
			data = baos.toByteArray();

			baos.close();
			dos.close();
			baos = null;
			dos = null;

		} catch (IOException iOException) {
			Log.e(TAG, "Exception in MultiAppMessageUAIH -> serialize");
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
	public long getUaih() {
		return uaih;
	}

	public void setToAddresses(long[] toAddresses) {
		toNetworkAddresses = toAddresses;
		numberOfAddresses = (byte) toAddresses.length;
	}
	
	/**
	 * 
	 * @return application identifier hash that this message is send from 
	 */
	public long getFromUaih() {
		return fromUaih;
	}

}

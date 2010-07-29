package itu.beddernet.router.message.uni;

import itu.beddernet.router.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.util.Log;

/**
 * The message is a unicast message intended for contacting a service with another identifier hash than the recipient 
 *
 */
public class UniAppMessageUAIH extends UniMessage{


	private byte[] appMessage;
	private long uaih;
	private long fromUaih;

	public UniAppMessageUAIH(byte[] msg) {
		deserialize(msg);
	}
	/**
	 * Class contructor
	 * @param fromNetworkAddress specifies the to address
	 * @param toNetworkAddress specifies the from  address
	 * @param uaih The applications service hash on the receiving end 
	 * @param fromUAIH The applications service hash from the sending end
	 * @param appMessage The message
	 */
	public UniAppMessageUAIH(long fromNetworkAddress, long toNetworkAddress, long uaih,long fromUAIH, byte[] appMessage){
		this.type = Message.UNICAST_APPLICATION_MESSAGE_UAIH;
		this.fromNetworkAddress = fromNetworkAddress;
		this.toNetworkAddress = toNetworkAddress;
		this.uaih = uaih;
		this.appMessage =appMessage;
		this.fromUaih = fromUAIH;
		
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
			uaih = dis.readLong();
			fromUaih = dis.readLong();
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
			dos.writeLong(uaih);
			dos.writeLong(fromUaih);
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
	public long getUaih(){
		return uaih;
	}
	
	/**
	 * 
	 * @return application identifier hash that this message is send from 
	 */
	public long getFromUaih() {
		return fromUaih;
	}


}

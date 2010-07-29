
package itu.beddernet.common;

/**
 * 
 * Simple class that encapsulates
 */
public class NetworkAddress {
	private String networkAddress = "";
	private long networkAddressAsLong;

	public NetworkAddress(String nwAddress) {
		networkAddress = nwAddress;
		networkAddressAsLong = castNetworkAddressToLong(networkAddress);
	}

	public NetworkAddress(long nwAddress) {
		networkAddress = castNetworkAddressToString(nwAddress);
		networkAddressAsLong = nwAddress;
	}

	public static String castNetworkAddressToString(long nwAddress) {

		String address = Long.toHexString(nwAddress);

		// Bluetooth MAC is 12 digits
		StringBuilder strb = new StringBuilder(12);
		// We need colons
		StringBuilder finalAddress = new StringBuilder(17);

		// Has it been truncated, 0's removed when converted to long?
		int lenght = 12 - address.length();
		// If so add 0's
		for (int y = 0; y < lenght; y++) {
			strb.append("0");
		}
		strb.append(address);
		address = strb.toString();
		strb = null;

		for (int i = 0; i < address.length(); i = i + 2) {
			finalAddress.append(address.charAt(i));
			finalAddress.append(address.charAt(i + 1));
			finalAddress.append(":");
		}

		// Remove the last ':' and return the long as a hex string
		return finalAddress.substring(0, finalAddress.length() - 1);

	}

	public static long castNetworkAddressToLong(String networkAddress) {
		String[] arr = networkAddress.split(":");
		StringBuilder strb = new StringBuilder();
		for (String string : arr) {
			strb.append(string);
		}
		// Returns a long
		return Long.parseLong(strb.toString(), 16);

	}

	public long getAddressAsLong() {
		return networkAddressAsLong;
	}

	public String getAddressAsString() {
		return networkAddress;
	}

	public void setNetworkAddress(String networkAddress) {

		this.networkAddress = networkAddress;
		networkAddressAsLong = castNetworkAddressToLong(networkAddress);
	}

	public String toString() {
		return networkAddress;
	}

	public boolean equals(Object obj) {
		return ((NetworkAddress) obj).getAddressAsString().equals(
				networkAddress);
	}

	public boolean equals(long adr) {
		if (networkAddressAsLong == adr) {
			return true;
		} else {
			return false;
		}
	}

	public int hashCode() {
		int hash = 5;
		hash = 73
				* hash
				+ (this.networkAddress != null ? this.networkAddress.hashCode()
						: 0);
		return hash;
	}

	public int compareTo(NetworkAddress other) {
		return this.getAddressAsString().compareTo(other.getAddressAsString());
	}

}

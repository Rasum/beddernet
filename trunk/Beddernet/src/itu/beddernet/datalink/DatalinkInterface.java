/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package itu.beddernet.datalink;

import itu.beddernet.common.Observer;

/**
 * 
 * @author Gober
 */
public interface DatalinkInterface {
	public void connectToNetwork(Observer incPcktObs, Observer neighbourObs);

	public void disconnectNetwork();

	public boolean sendPacket(long na, byte[] pckt);

	public long searchNewConnections();

	public long getNetworkAddress();

	public String getStatus(long na);
}

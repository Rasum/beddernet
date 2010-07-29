package itu.beddernet.router.dsdv.minders;

import itu.beddernet.router.dsdv.info.ConfigInfo;
import itu.beddernet.router.dsdv.net.RouteManager;
import itu.beddernet.router.dsdv.net.RoutingTable;
import itu.beddernet.router.dsdv.net.RoutingTableEntry;

import java.util.Vector;

import android.util.Log;
/**
 * Periodically sends out messages to all neighbors containing the routing table
 * This is the only one who can change the Sequence Number except for RouteManager 
 * in the init method.
 * @author Arnar
 */
public class BroadcastMinder extends Thread {

    RoutingTable rt;
    RouteManager rm;
    boolean abort = false;
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
    
	/**
	 * Kills the broadcast minder
	 */
    @Override
	public void destroy() {
    	Log.d(TAG, "BroadcastMinder destroy called! ");
		super.destroy();
	}

    /**
     * Class constructor
     * @param rm routemanager for broadcasting routing tables
     * @param rt routing table for incrementing sequence numbers
     */
	public BroadcastMinder(RouteManager rm, RoutingTable rt) {
        this.rm=rm;
        this.rt=rt;
    }
    /**
     * periodically sends out the routing table
     */
    public void run(){
        while(!abort){
            try {
                Thread.sleep(ConfigInfo.periodicRouteBroadcast);
                
                //increase my seq num everytime I broadcast it
                RoutingTableEntry me = rt.getRoutingEntry(ConfigInfo.netAddressVal);
                me.increaseSeqNum();
                
                //send out my routing table
                rm.BroadcastRouteTableMessage();
                
//                logRouteTable();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            
            
        }
    }
    
    /**
     * kills the periodic sending of the routing table
     */
    public synchronized void abort(){
    	abort = true;
    }
    
    /**
     * Prints the routing table
     */
    @SuppressWarnings("unused")
	private void logRouteTable()
    {
        Vector<RoutingTableEntry> v = rt.getAllRoutesAsVector();
        Log.i(TAG , "**** Routing Table ****");
        for(int i=0; i<v.size();i++){
            RoutingTableEntry rte = v.elementAt(i);
            Log.i(TAG , "**** Entry "+i+" "+rte.toString());
        }
    }
            
}

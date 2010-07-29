/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package itu.beddernet.approuter;

import itu.beddernet.approuter.IBeddernetServiceCallback;

/**
 * Example of defining an interface for calling on to a remote service
 * (running in another process).
 */
interface IBeddernetService {
    /**
     * Often you want to allow a service to call back to its clients.
     * This shows how to do so, by registering a callback interface with
     * the service.
     */
    long registerCallback( in IBeddernetServiceCallback cb, String applicationIdentifier);
    
    /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(IBeddernetServiceCallback cb, String applicationIdentifier);
    
    /**
     * Send a unicast message.  recipientApplicationIdentifier should be null unless 
	* message should be sent to a different application address .
     */
    void sendUnicast(String networkAddress, String recipientApplicationIdentifier, in byte [] appMessage,  String applicationIdentifier);

    /**
    * Send a multicast message
    * The service can be null. Default service string is used.
    */
    void sendMulticast(in String[] networkAddress, String recipientApplicationIdentifier, in byte [] appMessage, String applicationIdentifier);
    
    /**
    * Returns a list of known addresses on the scatternet. 
    */
    String[] getDevices(String applicationIdentifier);
    
   
   
    /* Returns list of all devices on the scatternet with "applicationIdentifier", 
    * i.e. that have an app that has inserted that ss.
    */
	String[] getDevicesSupportingUAIS(String applicationIdentifier);
	
    /** 
    * Check whether this service is supported by a particular device. 
    * If applicationIdentifier is null, the applicationIdentifierHash is used
    */
    boolean getApplicationSupport(String deviceAddress, String applicationIdentifier);
    
    /**
    * This tells the scatternet whether it should try to wake the application up. this is an optional feature, implementation depends on platform etc. This would be useful e.g. to start up some messaging application or online logger etc.
	* Set as true if the app behind should be invokeable i.e. if you ever run quake the AR remembers, if someone asks the AR "does he have quake" the reply will be "yes but it's trned off" the AR could then send a request to the user, "do you want to run quake now?" need to think about what is spam, what should be allowed etc.
	* default: false
	*/
    void setInvocable(String intentURI, boolean invokable, String applicationIdentifier);
    
    /**
    * DEBUG: Forces a device discovery
    *
    */
    void findNeighbors();
    
    /**
    * DEBUG: Manually connects to a device
    *
    */
    void manualConnect(String remoteAddress);
    
    /**
    * DEBUG: Starts the Maintainer thread
    *
    */
    void startMaintainer();
    
    /**
    * DEBUG: Stops the Maintainer thread
    *
    */
    
    void stopMaintainer();
    
    /**
    * DEBUG: Manually disconnects a device
    *
    */
    void manualDisconnect(String remoteAddress);
    
    /**
    * DEBUG: Manually sets device visible to other bluetooth devices.
    *
    */
    void setDiscoverable(boolean discoverable);
    
    /**
    * DEBUG: gets list of bluetooth neighbours
    *
    */
    String[] getNeighbours(long token);

    /**
    * DEBUG: disable bluetooth 
    *
    */
	void disableBluetooth();
	
	/**
    * DEBUG: Returns a list of known addresses on the scatternet with status
    * M = Master, S = Slave, X= not a bluetooth neighbor
    *
    */
	String[] getDevicesWithStatus();
	
		
	/**
	*DEBUG
    *Returns all service hashes on a specific device. 
    */	
	long[] getAllUAIHOnDevice(String UAIH);
    
    
}

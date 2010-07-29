package itu.beddernet.router;

public interface AppRouterInterface {

//	/**
//	 * This adds a new service string (and app name, ServiceHash etc.) to the app table, 
//	 * an application can insert several service strings 
//	 * @param serviceString the service string
//	 */
//	void insertServiceString(String serviceString);
//	
//	/**
//	 * The public property is used to designate if an app is put on the list of available services on the device. 
//	 * this list can be requested by other devices (i.e.. looking for programs or services). 
//	 * this enables applications to use services like "printservice" without the router telling other devices that it supports printservice. 
//	 * default: true
//	 * @param isPublic
//	 */
//	void setPublic(boolean isPublic, String serviceString);
//	
//	/**
//	 *  Returns list of all devices on the scatternet that have that UAIS,
//	 *  (Unique Application Identifier String)
//	 * @param UAIS
//	 * @return a long[] of the devices that support the service string
//	 */
//	long[] getDevicesSupportingUAIH(long UAIH);
//	

	/**
	 * This tells the scatternet whether it should try to wake the application up. 
	 * this is an optional feature, implementation depends on platform etc. 
	 * This would be useful e.g. to start up some messaging application or online logger etc.
	 * set as true if the app behind the service string should be invokeable i.e. if you ever run quake the AR remembers,
	 * if someone asks the AR "does he have quake" the reply will be "yes but it's trned off" 
	 * the AR could then send a request to the user, "do you want to run quake now?" 
	 * need to think about what is spam, what should be allowed etc.
	 * default: false
	 * @param applicationName
	 * @param canBeInvoked
	 */
	void setInvokeable(String applicationName, boolean canBeInvoked);

	
}

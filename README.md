# beddernet

Beddernet for Android, mobile ad-hoc middleware based on the Beddernet protocol.

Beddernet runs as a service and allows applications to easily communicate with a large number of devices using a simple API.

Beddernet can be applied in highly volatile environments as it uses a simple but robust mesh algorithm that has self-healing and organizing capabilities.



# How to
I finally got my dev environment set up at home so I could take a proper look at this. It should not be necessary for the app to call specific methods on Beddernet, other than startMaintainer() once. The idea is to start maintainer by default when beddernet starts up, so the client app would not need to do that either. I guess that was removed for testing purpouses

I was calling the startMaintainer in both devices. Now I called startMaintainer just in one of them and it worked. I need to call startMaintainer just in one of them? Shouldn't the startMaintainer manage the discovering (slave)/connecting (master) proccess as you said below?

Beddernet is set up to regularly scan for devices, and to be listening for incoming connections at all other times. When a device is scanning for other devices it's generally not discoverable by other devices at the same time. Therefore we need to alternate between being discoverable and discovering, i.e. listening for incoming connections and scanning for devices, not doing both at the same time...

The current setting is for the scanner to have a 33% (Maintainer.java, line 101) chance of doing a scan when initialised, otherwise just listen for incoming connections. It then scans regularly at a certain interval. This is a sound method in theory but if you're just connecting a few devices, you may have to wait for more than a minute (you can then do "Refresh device list" in the beddernet console). It may be better to just do a manual Find devices initially for development (it should be enough to do that on one device, not both).

If I call startMaintainer in both devices, it should be the same as calling it just in one of them. Because sometimes one will be in discovering (slave) and the other in connecting (master) modes.

I am sometimes having problems connecting the devices though, I can connect one to the other works, but not the other way around... it's possible the Bluetooth implementation on either device is flaky (Galaxy Note and Nexus 7), but turning off bluetooth and turning it back on helps. his seems to happen after a connection get's broken so maybe the sockets aren't being cleaned properly, or some ports being held by the OS, I'm not sure.

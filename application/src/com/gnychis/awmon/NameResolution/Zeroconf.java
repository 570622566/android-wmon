package com.gnychis.awmon.NameResolution;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.GUIs.MainInterface;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;

// Bonjour 
public class Zeroconf extends NameResolver {
	
	static final String TAG = "Zeroconf";
	static final boolean VERBOSE = true;
	
    private boolean _waitingOnResults;
    private boolean _waitingOnThread;
    ArrayList<Interface> _supportedInterfaces;
    
    List<String> _serviceListeners;
    
    private static JmDNS zeroConf = null;
    private static MulticastLock mLock = null;
    private ServiceListener _jmdnsListener = null;

    @SuppressWarnings("unchecked")
	public Zeroconf(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Wifi.class, LAN.class));
	}

	public ArrayList<Interface> resolveSupportedInterfaces(ArrayList<Interface> supportedInterfaces) {
		debugOut("Started Zeroconf resolution");
		_supportedInterfaces = supportedInterfaces;  // make them accessible
		
		_waitingOnThread=true;
		zeroConfThread monitorThread = new zeroConfThread();
		monitorThread.execute(_nr_manager._parent);
		
		while(_waitingOnThread)  // FIXME this blocks the main thread
			try { Thread.sleep(1000); } catch(Exception e) {}
		
		debugOut("Finished Zeroconf resolution");
		return _supportedInterfaces;	// Make sure to return the _ version.
	}
	
	// The purpose of this thread is solely to initialize the Wifi hardware
	// that will be used for monitoring.
	protected class zeroConfThread extends AsyncTask<Context, Integer, String>
	{
		// Initialize the hardware
		@Override
		protected String doInBackground( Context ... params )
		{
			setUp();
			
			// We need to wait a bit for some results
			while(_waitingOnResults) { 
				try{ Thread.sleep(100); } catch(Exception e) {} 
			}

			tearDown();	// tear down the search for services
			_waitingOnThread=false;

			return "true";
		}	
		
	    @Override
	    protected void onPostExecute(String result) { }
	    
		// The application needs to request the multicast lock.  Without it the application will not
		// receive packets that are not addressed to it.  This should be disabled when the scan is complete.
		// Otherwise, you will get battery drain.
	    private boolean setUp() {
	        WifiManager wifi = (WifiManager) _nr_manager._parent.getSystemService(Context.WIFI_SERVICE);

	        WifiInfo wifiinfo = wifi.getConnectionInfo();
	        int intaddr = wifiinfo.getIpAddress();

	        try {
		        if (intaddr == 0)
		        	return false;
	
	           byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
	                    (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
	           InetAddress addr = InetAddress.getByAddress(byteaddr);

	           Log.d(TAG, String.format("found intaddr=%d, addr=%s", intaddr, addr.toString()));
	           // start multicast lock
	           mLock = wifi.createMulticastLock("TunesRemote lock");
	           mLock.setReferenceCounted(true);
	           mLock.acquire();
	           
	           _jmdnsListener = new ServiceListener() {
	        	   
	        	    /** This is mainly called whenever we get a response about an ARPA resolutions */
	                @Override
	                public void serviceAdded(ServiceEvent event) {
	                    if(event.getType().equals("_tcp.in-addr.arpa.")) {
	                    	namingResponse(cleanName(event.getName()), Interface.reverseIPAddress(event.getInfo().getName().replace("/", "")));
	                    }
	                }

	                /** This is usually called when we get a resolution from another type of service (e.g., airport) */
	                @Override
	                public void serviceResolved(ServiceEvent ev) {
	                    namingResponse(cleanName(ev.getInfo().getName()), ev.getInfo().getInet4Addresses()[0].toString().replace("/", ""));
	                }

	                @Override
	                public void serviceRemoved(ServiceEvent ev) {}
	            };

	           zeroConf = JmDNS.create(addr, "awmon");
	           
	           // Build the list of services we are listening for
	           _serviceListeners = buildServiceListenerList();
	           debugOut("Adding in the service listeners..." + Calendar.getInstance().getTime());
	           for(String service : _serviceListeners) 
	        	   zeroConf.addServiceListener(service, _jmdnsListener);
	           debugOut("...done adding service listeners!" + Calendar.getInstance().getTime());
	           
	        } catch(Exception e) { Log.e(TAG, "Error" + e); }
	        
			// Setup a handler to change the value of _waitingOnResults which blocks progress
			// until we have waited from results of a scan to trickle in.
			_waitingOnResults=true;
			Timer oneShotTimer = new Timer();
			oneShotTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					_waitingOnResults=false;
				}

			}, 10000);
			return true;
	    }
	    
	    
	    /** Handles an incoming naming response and attempts to store and cache this data with an
	     * associated interface.
	     * @param name The human recognizable identifier (e.g., Bill's iPad)
	     * @param IP The IP address associated with the interface
	     */
	    private void namingResponse(String name, String IP) {
	    	debugOut("Resolved Name: " + name + "  Address: " + IP);
	    	
	    	for(Interface iface : _supportedInterfaces)
	    		if(iface._IP!=null && iface._IP.equals(IP) && iface._ifaceName==null)	// we found the interface, and the name is null
	    			iface._ifaceName = name;	// name it
	    }
	    
	    // This function reads through the services listed in the mdns_sevice_types.txt, as well
	    // as adds some services dynamically.
	    List<String> buildServiceListenerList() {
	    	List<String> services = new ArrayList<String>();
	    	
			// Now go through each of the supported interfaces and add an ARPA request which will get us
			// a name, typically if it even has no services shared. To catch the response you need to
			// registered with "_tcp.in-addr.arpa."  You need to reverse the IP also.
			services.add("_tcp.in-addr.arpa.");
			for(Interface iface : _supportedInterfaces) {
				if(iface.hasValidIP()) {
					services.add(iface.getReverseIP() + ".in-addr.arpa.");
					debugOut("Adding in a query for " + iface._IP + " as: " + iface.getReverseIP() + ".in-addr.arpa."); 
				}
			}
	    	
			try {	// First go through the list of known service types and add each of them
				DataInputStream in = new DataInputStream(new FileInputStream("/data/data/" + MainInterface._app_name + "/files/mdns_service_types.txt"));
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				
				String service;	// Get the service, append a "_" to it and make it end with "._tcp.local."
				 while ((service = br.readLine()) != null) {
					 services.add("_" + service.replace("\n", "").replace("\r", "") + "._tcp.local.");
				 }
				in.close();
			} catch(Exception e) { Log.e(TAG, "Error opening MDNS service types text file"); }

	    	return services;
	    }
	    
	    // Give up the multicast lock and teardown, this saves us battery usage.
	    private void tearDown() {
	    	if(zeroConf!=null) {
		        for(String service : _serviceListeners) 
		        	zeroConf.removeServiceListener(service, _jmdnsListener);
		        try {
		        	zeroConf.close();
		        	zeroConf=null;
		        } catch(Exception e) { Log.e(TAG, "zeroConf close error: " + e); }
	    	}
	    	
	        if(mLock!=null)
	        	mLock.release();
	        mLock=null;
	    }
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
	
	
	/** Takes out a couple non-alphabetical characters to try and get clean names for interfaces.
	 * Otherwise you can get things like: "Georges-MacBook-Air" ... replace those "-" with spaces.
	 * @param name The name to be cleaned.
	 * @return the clean name of an interface, removing erroneous characters.
	 */
	public static String cleanName(String name) {
		name = name.replace("-", " ");
		name = name.replace(".local.", "");
		name = name.replace("'", "");
		return name;
	}
}

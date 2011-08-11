package com.gnychis.coexisyst;

import java.util.ArrayList;
import java.util.Iterator;

import android.os.Parcel;
import android.os.Parcelable;

public class ZigBeeNetwork implements Parcelable {

	public String _mac;				// the source address (of the coordinator?)
	public String _pan;  			// the network address
	public int _band;  				// the channel
	ArrayList<Integer> _lqis;		// link quality indicators (to all devices?)
	ArrayList<ZigBeeDev> _devices;	// the devices in the network
	
	public Packet _beacon;
	
    public void writeToParcel(Parcel out, int flags) {
    	out.writeString(_mac);
    	out.writeString(_pan);
    	
    	out.writeInt(_band);
    	out.writeSerializable(_lqis);
    	out.writeSerializable(_devices);
    	out.writeParcelable(_beacon, 0);
    }
    
    private ZigBeeNetwork(Parcel in) {
    	_mac = in.readString();
    	_pan = in.readString(); 	
    	_band = in.readInt();
    	_lqis = (ArrayList<Integer>) in.readSerializable();
    	_devices = (ArrayList<ZigBeeDev>) in.readSerializable();
    	
    	_beacon = in.readParcelable(Packet.class.getClassLoader());
    }
	
	public int describeContents()
	{
		return this.hashCode();
	}
	
    public static final Parcelable.Creator<ZigBeeNetwork> CREATOR = new Parcelable.Creator<ZigBeeNetwork>() {
    	public ZigBeeNetwork createFromParcel(Parcel in) {
    		return new ZigBeeNetwork(in);
    	}

		public ZigBeeNetwork[] newArray(int size) {
			return new ZigBeeNetwork[size];
		}
};
	
	public ZigBeeNetwork() {
		_band=-1;
		_lqis = new ArrayList<Integer>();
	}
	
	// Report the average RSSI
	public int lqi() {
		
		Iterator<Integer> rssis = _lqis.iterator();
		int sum=0;
		while(rssis.hasNext()) {
			int i = rssis.next().intValue();
			sum += i;
		}
		
		return sum / _lqis.size();
	}
}
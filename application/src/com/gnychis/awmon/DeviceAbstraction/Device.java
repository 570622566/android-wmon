package com.gnychis.awmon.DeviceAbstraction;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * A device is a physical thing.  Like a laptop, an access point, etc.  It can have multiple radios attached
 * which we save and represent as interfaces.
 * 
 * @author George Nychis (gnychis)
 */
public class Device implements Parcelable {
	
	public enum Mobility {		// Possible types of radios that we support
		UNKNOWN,
		MOBILE,
		FIXED,
	}

	List<Interface> _interfaces;	// Keep track of each radio detected
	private String _name;			// A name for the device, could be user generated?
	Mobility _mobile;
		
	public Device() {
		_interfaces = new ArrayList<Interface>();
		_name = null;
		_mobile=Device.Mobility.UNKNOWN;
	}
	
	public Device(List<Interface> interfaces) {
		_interfaces = interfaces;
		_name = null;
		_mobile=Device.Mobility.UNKNOWN;
	}
	
	/** 
	 * This functionality could change in the future, but right now it returns
	 * _name if it is not null (could be chosen by the user).  Otherwise, it goes
	 * through the interfaces and returns the name of one of them. 
	 * 
	 * @return a human readable name for the device, null if for some reason there is no
	 * saved name
	 */
	public String getName() {
		if(_name!=null)
			return _name;
		
		for(Interface iface : _interfaces)
			if(iface._ifaceName != null)
				return iface._ifaceName;
		
		return null;
	}
	
	// ********************************************************************* //
	// This code is to make this class parcelable and needs to be updated if
	// any new members are added to the Device class
	// ********************************************************************* //
    public int describeContents() {
        return this.hashCode();
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
    	dest.writeList(_interfaces);
    	dest.writeString(_name);
    	dest.writeInt(_mobile.ordinal());
    }

    public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() {
    	public Device createFromParcel(Parcel in) {
    		return new Device(in);
    	}

		public Device[] newArray(int size) {
			return new Device[size];
		}
    };

    private Device(Parcel source) {
    	_interfaces = new ArrayList<Interface>();
    	source.readList(_interfaces, this.getClass().getClassLoader());
    	_name = source.readString();
    	_mobile = Device.Mobility.values()[source.readInt()];
    }

}

package com.gnychis.awmon.InterfaceMerging;

import java.util.Arrays;

import android.content.Context;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.InterfacePair;
import com.gnychis.awmon.HardwareHandlers.Bluetooth;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;

/**
 * The Purpose of this heuristic is to leverage the fact that interfaces on the same device often
 * have adjacent MAC addresses.  This simply looks for pairs that have addresses with a distance 
 * from each other specific by a parameter.
 * 
 * @author George Nychis (gnychis)
 */
public class AdjacentMACs extends MergeHeuristic {
	
	private static final String TAG = "AdjacentMACs";
	private static final boolean VERBOSE = false;
	
	public static final int MAX_ADDRESS_DISTANCE = 1;	// The maximum distance to consider "adjacent"
	
	@SuppressWarnings("unchecked")
	public AdjacentMACs(Context p) {
		super(p,Arrays.asList(Wifi.class, Bluetooth.class, LAN.class));
	}

	public MergeStrength classifyInterfacePair(InterfacePair pair) {
		
		// If one or the other has an invalid MAC address, let's just bail.
		if(!pair.getLeft().hasValidIEEEmac() || !pair.getRight().hasValidIEEEmac())
			return MergeStrength.UNDETERMINED;
		
		// First, calculate the distance between the two MAC addresses by converting them to
		// long format, subtracting them, and then taking the absolute value.
		long distance = Math.abs(Interface.macStringToLong(pair.getLeft()._MAC)
				- Interface.macStringToLong(pair.getRight()._MAC));

		// If the distance is less than our tolerance, then return LIKELY, otherwise consider
		// it undetermined.  Don't return "UNLIKELY" because it is quite possible that two 
		// different interfaces on a device have unsimilar addresses.
		if(distance <= MAX_ADDRESS_DISTANCE) {
			debugOut("Likely: " + pair.getLeft()._MAC + " and " + pair.getRight()._MAC);
			return MergeStrength.LIKELY;
		}
		
		debugOut("Unlikely: " + pair.getLeft()._MAC + " and " + pair.getRight()._MAC);
		return MergeStrength.UNDETERMINED;		
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}

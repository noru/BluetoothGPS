package com.my.bluetoothgps;

import android.util.Log;

//$GPGGA,082000.00,3958.460660,N,11629.555498,E,1,08,1.3,60.339,M,-7.558,M,,0000*49
//|               |       |           |         |
//prefix      separator  Longitude   Latitude   Mode
/*	4 functions to get parameters from original signal:
getLongitude(),getLatitude(),getElevation(),getMode()*/
/**
 * @author Drew.Xiu
 *
 */
public class SingnalProcess
{
	static String[] string;
	
	//	final static String m = "m";
	//	final static String[] mode =
	//	{ "Invalid", "GPS Fix", "Dif. GPS Fix", "Invalid PPS", "Estimating" };
	//	final static String[] directions =
	//	{ "North", "South", "West", "East" };
	//	enum 
	
	public static String[] SingnalProcess(String signal)
	{
		// int i;
		String[] rtnString =
		{ "", "", "", "" };
		if (signal == null | signal == "") return rtnString;
		string = stringSplit(signal);
		rtnString[0] = string[4] + string[5]; //latitude
		rtnString[1] = string[2] + string[3]; //longitude
		rtnString[2] = string[9]; //elevation
		rtnString[3] = string[6]; //mode
		return rtnString;
	}
	
	private static String[] stringSplit(String sig) //split original signal
	{
		Log.i("sp", sig);
		return sig.split(",");
	}
	
	//	private String getConvertion(String s)
	//	{
	//		switch(s):
	//			case "E":
	//				return directions[3];
	//				break;
	//			case "W":
	//				return directions[2];
	//				break;
	//			case "S":
	//				return directions[1];
	//				break;
	//			case "N":
	//				return directions[0];
	//				break;
	//		defaul:
	//			return "error";
	//			break;
	//	}
}

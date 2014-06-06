package com.my.bluetoothgps;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class BluetoothService extends Service
{
	// a Binder class to bind the service to the activity
	public class MyBinder extends Binder
	{
		public MyBinder()
		{
			Log.i(TAG, "just new a MyBinder");
		}
		
		//return service instance to activity
		public BluetoothService getService()
		{
			return BluetoothService.this;
		}
		
	}
	
	//debug
	private final String TAG = "BluetoothService";
	
	private static final boolean flag = true;
	
	//
	
	//instance of IBinder
	private final IBinder myBinder = new MyBinder();
	
	// bind it 
	@Override
	public IBinder onBind(Intent intent)
	{
		return myBinder;
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.i(TAG, "Service created");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i(TAG, "service started");
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	//public methods
	public void testmethod()
	{
		Toast.makeText(getApplicationContext(), "fuckyou", Toast.LENGTH_SHORT);
		Log.i(TAG, "test method invoked");
	}
}

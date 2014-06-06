package com.my.bluetoothgps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.my.bluetoothgps.BluetoothService.MyBinder;

public class BluetoothGPSActivity extends Activity
{
	private class ReadThread extends Thread
	{
		@Override
		public void run()
		{
			
			try
			{
				
				// At this point, the application can pass the raw data to a parser that
				InputStream is = BluetoothGPSActivity.mBluetoothSocket.getInputStream();
				while (isConnected)
				{
					int i;
					while (true)
					{
						i = is.read();
						if (i == 36)//found "$", recording following bytes
						{
							String tmp = "$";
							for (int j = 0; j < 81; j++)
							{
								i = is.read();
								if (i == 36) break;
								byte[] t = new byte[1];
								t[0] = (byte) i;
								String temp = new String(t);
								tmp = tmp + temp;
							}
							RAW = tmp;
							break;
						}
					}
					
					Log.i(TAG, "raw data:" + RAW);
					try
					{
						STRINGARRAY = SingnalProcess.SingnalProcess(RAW);
					}
					catch (Exception e)
					{
						Log.i(TAG, e.toString());
					}
					runOnUiThread(readUpdate);
					
				}
			}
			catch (IOException ioe)
			{
			}
		}
	}
	
	private class SendThread extends Thread
	{
		byte[] buffer;
		
		public SendThread(String cmd)
		{
			buffer = (cmd + "\r\n").getBytes();
		}
		
		@Override
		public void run()
		{
			try
			{
				OutputStream os = BluetoothGPSActivity.mBluetoothSocket.getOutputStream();
				os.write(buffer);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	Runnable readUpdate = new Runnable()
	{
		
		@Override
		public void run()
		{
			tvLongitude.setText(STRINGARRAY[0]);
			tvLatitude.setText(STRINGARRAY[1]);
			tvElevation.setText(STRINGARRAY[2]);
			tvMode.setText(STRINGARRAY[3]);
			tvRawData.setText(RAW);
		}
	};
	
	//debug
	private final String TAG = "BluetoothGPSActivity";
	
	//flags
	private static boolean isConnected = false;
	public static boolean hasAnnotation = false;
	private static boolean hasSDcard = false;
	//bluetooth 
	private static final String SPP_NAME = "BT5701";
	private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
	private static BluetoothSocket mBluetoothSocket;
	private static final int REQUEST_ENABLE_BT = 1;
	public static int sample_rate = 200;
	public static String Annotation = "initial";
	
	private BluetoothAdapter mBluetoothAdapter;
	public static InputStream mBluetoothInputStream;
	private BluetoothService mBluetoothService;
	public static String[] STRINGARRAY =
	{ "", "", "", "" };
	public static String RAW;
	//others
	private final Handler hanlder = new Handler();
	private Toast mToast = null;
	private BroadcastReceiver mBroadcastReceiver;
	public static String version;
	//views
	TextView tvLongitude, tvMode, tvLatitude, tvElevation, tvRawData;
	
	EditText etCmd;
	ToggleButton tbtn_Session, btn_Single, tbtn_Tracking;
	Button btn_discover, btn_send;
	CheckBox cb_annotation;
	//the only OnClickListener in this Activity
	private final OnClickListener listener = new OnClickListener()
	{
		
		@Override
		public void onClick(View v)
		{
			//find 3 buttons on screen
			Log.i(TAG, "Button" + v.toString() + " is clicked");
			//ToggleButton tbtn_Tracking = (ToggleButton) findViewById(R.id.btn_recording);
			//Button btn_Single = (Button) findViewById(R.id.btn_single_record);
			//ToggleButton tbtn_session = (ToggleButton) findViewById(R.id.btn_session);
			
			final IO writeFileIo = new IO();
			switch (v.getId())
			{
				case R.id.btn_session:
					if (tbtn_Session.isChecked())
					{
						
						if (!hasSDcard)
						{
							giveToast(getString(R.string.sd_card_is_not_ready_can_not_use_this_function_));
							tbtn_Session.setChecked(false);
							break;
						}
						Log.i(TAG, "Session button is checked");
						tbtn_Tracking.setEnabled(true);
						btn_Single.setEnabled(true);
						btn_discover.setEnabled(false);
						try
						{
							writeFileIo.startSession();
						}
						catch (Exception e1)
						{
							Log.e(BluetoothGPSActivity.this.toString(), e1.toString());
						}
						giveToast(getString(R.string.a_new_session_is_started_));
					}
					else
					{
						Log.i(TAG, "Session button is unchecked");
						writeFileIo.endSession();
						tbtn_Tracking.setEnabled(false);
						btn_Single.setEnabled(false);
						btn_discover.setEnabled(true);
						giveToast(getString(R.string.session_is_ended_));
					}
					break;
				
				case R.id.btn_single_record:
					writeFileIo.singleRecord();
					btn_Single.setChecked(false);
					giveToast(getString(R.string.single_point_has_been_recorded_));
					if (hasAnnotation)
					{
						try
						{
							getAnnotation();
						}
						catch (IOException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					break;
				
				case R.id.btn_recording:
					
					if (tbtn_Tracking.isChecked())
					{
						Log.i(TAG, "Tracking button is checked");
						tbtn_Session.setEnabled(false);
						btn_Single.setEnabled(false);
						//start recording here
						try
						{
							writeFileIo.tracking();
						}
						catch (Exception e)
						{
							writeFileIo.endTracking();
							Log.i(TAG, "Lost connection");
							e.printStackTrace();
							giveToast("Lose Connection. Please Try Re-discover.");
							break;
						}
						giveToast(getString(R.string.start_tracking_));
					}
					else
					{
						Log.i(TAG, "Tracking button is unchecked");
						tbtn_Session.setEnabled(true);
						btn_Single.setEnabled(true);
						//stop recording here
						writeFileIo.endTracking();
						giveToast(getString(R.string.finish_tracking_a_serial_of_data_has_been_recorded_));
						if (hasAnnotation)
						{
							try
							{
								getAnnotation();
							}
							catch (IOException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					
					break;
				case R.id.btn_discovery:
					try
					{
						if (!mBluetoothAdapter.isEnabled())
						{
							giveToast(getString(R.string.your_bluetooth_adapter_is_disabled_please_turn_it_on_));
							Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
							startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
							break;
						}
						try
						{
							mBluetoothSocket.close();
							isConnected = false;
						}
						catch (Exception e)
						{
							//do nothing
							e.printStackTrace();
						}
						giveToast(getString(R.string.start_discovery_));
						tbtn_Session.setEnabled(false);
						btn_discover.setText("Re-discover");
						mBluetoothAdapter.startDiscovery();
					}
					catch (Exception e)
					{
						Log.i(TAG, "Failed in discovery");
						giveToast("Connecting Error, Please Try Restart Your Devices.");
					}
					break;
				case R.id.switch_annotation:
					if (cb_annotation.isChecked())
					{
						hasAnnotation = true;
					}
					else
					{
						hasAnnotation = false;
					}
					break;
				case R.id.btn_send:
					String cmd = etCmd.getText().toString();
					if (!cmd.startsWith("-"))
					{
						SendThread send = new SendThread(cmd);
						send.start();
						
					}
					else if (cmd.startsWith("-srate"))
					{
						String[] tmp = cmd.split(" ");
						int i = Integer.parseInt(tmp[1]);
						sample_rate = i;
						giveToast("Sample Rete is changed to " + i);
						Log.i(TAG, "Sample Rate is changed to " + i);
					}
					etCmd.setText("");
					break;
				default:
					break;
			}
		}
	};
	
	// Defines callbacks for service binding, passed to bindService()
	private final ServiceConnection mConnection = new ServiceConnection()
	{
		
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			MyBinder binder = (MyBinder) service;
			Log.i(TAG, "i created");
			mBluetoothService = binder.getService();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			
		}
	};
	
	public void getAnnotation() throws IOException
	{
		LayoutInflater inflater = getLayoutInflater();
		final View layout = inflater.inflate(R.layout.annotation, (ViewGroup) findViewById(R.layout.annotation));
		new AlertDialog.Builder(BluetoothGPSActivity.this).setIcon(android.R.drawable.ic_menu_edit).setTitle("Annotation").setView(layout).setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				EditText et = (EditText) layout.findViewById(R.id.et_annotation);
				Annotation = et.getText().toString();
				Log.i(TAG, Annotation);
				final IO writeFileIo = new IO();
				writeFileIo.appendAnnotation();
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				return;
			}
		}).show();
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		//set layout and initialize screen
		setContentView(R.layout.main);
		//register broadcast reciever
		mBroadcastReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				String action = intent.getAction();
				//				Bundle b = intent.getExtras();
				
				if (BluetoothDevice.ACTION_FOUND.equals(action))
				{
					Log.i(TAG, "found a device");
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					UUID uuid = UUID.fromString(SPP_UUID);
					
					String name = device.getName();
					Log.i(TAG, "Name is " + name + ".");
					String mac = device.getAddress();
					Log.i(TAG, "MAC Address is " + mac + ".");
					
					if (name.equals(SPP_NAME))
					{
						Log.i(TAG, "find the device");
						mBluetoothAdapter.cancelDiscovery();
						Log.i(TAG, "cancel discovery");
						try
						{
							BluetoothDevice GPSDevice = mBluetoothAdapter.getRemoteDevice(mac);
							mBluetoothSocket = GPSDevice.createRfcommSocketToServiceRecord(uuid);
							mBluetoothSocket.connect();
							Log.i(TAG, "device connected");
							isConnected = true;
							tbtn_Session.setEnabled(true);
							btn_send.setEnabled(true);
							new ReadThread().start();
							giveToast(context.getString(R.string.gps_device_is_connected_));
						}
						catch (IOException e)
						{
							Log.e(TAG, "failed connecting socked");
							e.printStackTrace();
							giveToast(context.getString(R.string.connecting_failed_try_restart_your_devices_and_discover_again_));
							btn_discover.setEnabled(true);
						}
					}
					else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
					{
						giveToast(context.getString(R.string.disconnected_with_gps_device_));
						btn_discover.setEnabled(true);
					}
					
				}
			}
		};
		IntentFilter intent = new IntentFilter();
		intent.addAction(BluetoothDevice.ACTION_FOUND);
		intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		intent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mBroadcastReceiver, intent);
		
		//initialization
		version = getVersion();
		initializeBluetooth();
		initializeScreen();
		
	}
	
	private String getVersion()
	{
		PackageManager packageManager = getPackageManager();
		PackageInfo packInfo = null;
		try
		{
			packInfo = packageManager.getPackageInfo(getPackageName(), 0);
		}
		catch (NameNotFoundException e)
		{
			return "null";
		}
		return packInfo.versionName;
	};
	
	// a give-toast function
	// parameter: string s
	// Sent s to the screen
	// seems there has a toast queue issue: 
	// if user make multiple toasts, one will not show until previous one disappear
	// fix this by simply use setText(), side effect: toasts share one duration
	private void giveToast(String s)
	{
		giveToast(s, 0);
	}
	
	private void giveToast(String s, int i)
	{
		switch (i)
		{
			case 0:
				i = Toast.LENGTH_SHORT;
				break;
			case 1:
				i = Toast.LENGTH_LONG;
				break;
			default:
				break;
		}
		if (mToast != null)
		{
			mToast.setText(s);
		}
		else
		{
			mToast = Toast.makeText(BluetoothGPSActivity.this, s, i);
			
		}
		mToast.show();
	}
	
	private void initializeBluetooth()
	{
		
		//bind bluetooth service
		Intent intent = new Intent(BluetoothGPSActivity.this, BluetoothService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null)
		{
			giveToast(getString(R.string.your_device_does_not_support_bluetooth_), 1);
			finish();
		}
		else if (!mBluetoothAdapter.isEnabled())
		{
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		//check SD card
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			hasSDcard = true;
		}
		
	}
	
	private void initializeScreen()
	{
		//set views & listener
		// textviews
		tvLongitude = (TextView) findViewById(R.id.tv_longitude);
		tvLatitude = (TextView) findViewById(R.id.tv_latitude);
		tvElevation = (TextView) findViewById(R.id.tv_elevation);
		tvMode = (TextView) findViewById(R.id.tv_mode);
		tvRawData = (TextView) findViewById(R.id.tv_original_signal);
		//buttons
		tbtn_Session = (ToggleButton) findViewById(R.id.btn_session);
		btn_discover = (Button) findViewById(R.id.btn_discovery);
		tbtn_Tracking = (ToggleButton) findViewById(R.id.btn_recording);
		btn_Single = (ToggleButton) findViewById(R.id.btn_single_record);
		cb_annotation = (CheckBox) findViewById(R.id.switch_annotation);
		btn_send = (Button) findViewById(R.id.btn_send);
		etCmd = (EditText) findViewById(R.id.et_command);
		//set listener
		tbtn_Session.setOnClickListener(listener);
		btn_Single.setOnClickListener(listener);
		tbtn_Tracking.setOnClickListener(listener);
		btn_discover.setOnClickListener(listener);
		cb_annotation.setOnClickListener(listener);
		btn_send.setOnClickListener(listener);
		//for test
		//tbtn_Session.setEnabled(true);
		
	}
	
	// Does Not Allow User to Switch Screen, if so kill itself
	@Override
	protected void onStop()
	{
		//unregister and kill itself
		this.unregisterReceiver(mBroadcastReceiver);
		super.onStop();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
}
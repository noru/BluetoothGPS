package com.my.bluetoothgps;

/*
public function: startSession(), endSession(), singleRecord(), tracking(), endTracking()
  ---correspond to button clicks in Activity.
private funtion: getTime(), getTimestamp() 
	---to get time and make a static timestamp for filename in a single session.
*/
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public class IO
{
	private final static String FOLDER = "/GPS_RDA/"; //create a folder under /sdcard
	private final static String PREFIX = "Session";
	private final static String SUFFIX = ".txt"; // suffix could be replaced on demand   
	private final static File FILEPATH = new File(Environment.getExternalStorageDirectory() + FOLDER);
	private static String TIMESTAMP = "";
	private static String FILENAME = "";
	private static StringBuffer sb = new StringBuffer();
	public static boolean flag; //Thread control
	public String annotationString = "## Annotation:\r\n" + "##[" + BluetoothGPSActivity.Annotation + "]\r\n" + "## End of Annotation\r\n";
	private static final String TAG = "IO";
	
	//get system time and return a formatted string
	private static String getTime()
	
	{
		//set format
		SimpleDateFormat format = new SimpleDateFormat("yyyy MMM d HH:mm:ss");
		// use java.util.data Data() to get time info
		return format.format((new Date()));
	}
	
	// start a new thread for tracking()
	private final Thread thread = new Thread()
	{
		@SuppressWarnings("static-access")
		@Override
		public void run()
		{
			sb.append("## Serial Record\r\n");
			while (flag)
			{
				// rewrite when Bluetooth service is ready
				sb.append(getTime() + ": " + BluetoothGPSActivity.RAW);
				Log.i(TAG, "tracking...");
				try
				{
					thread.sleep(BluetoothGPSActivity.sample_rate);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			sb.append("##End of Serial Record\r\n");
			
		}
	};
	
	public IO()
	{
		//initialize file path in SDcard
		if (FILEPATH != null && !FILEPATH.exists())
		{
			if (!FILEPATH.mkdir() && !FILEPATH.isDirectory())
			{
				Log.d(TAG, "Failed making dir");
			}
		}
	}
	
	public void appendAnnotation()
	{
		OutputStreamWriter osw;
		File file = new File(FILEPATH, FILENAME);
		try
		{
			osw = new OutputStreamWriter(new FileOutputStream(file, true));
			osw.append(annotationString);
			osw.flush();
			osw.close();
			Log.i(TAG, "Annotation appended." + annotationString);
		}
		catch (IOException e)
		{
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}
		BluetoothGPSActivity.Annotation = "";
	}
	
	public void endSession()
	{
		OutputStreamWriter osw;
		File file = new File(FILEPATH, FILENAME);
		try
		{
			osw = new OutputStreamWriter(new FileOutputStream(file, true));
			
			osw.write("\r\n## Session Ended At: " + getTime() + "\r\n");
			osw.flush();
			osw.close();
			Log.i(TAG, "Session Ended.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.i(TAG, "Failed ending this sesson.");
		}
		
		//else to do
	}
	
	public void endTracking()
	{
		//set flag to stop the thread
		flag = false;
		
		//output the StringBuffer into result.txt once and for all
		OutputStreamWriter osw;
		File file = new File(FILEPATH, FILENAME);
		try
		{
			osw = new OutputStreamWriter(new FileOutputStream(file, true));
			osw.write(sb.toString());
			Log.i(TAG, sb.toString());
			osw.flush();
			osw.close();
			sb.setLength(0);
			Log.i(TAG, "End tracking. Flushed string to txt.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.i(TAG, "Failed ending tracking.");
		}
	}
	
	public void singleRecord()
	{
		OutputStreamWriter osw;
		File file = new File(FILEPATH, FILENAME);
		try
		{
			osw = new OutputStreamWriter(new FileOutputStream(file, true));
			osw.append("## Single Record\r\n");
			osw.append(getTime() + ": " + BluetoothGPSActivity.RAW);
			osw.flush();
			osw.close();
			Log.i(TAG, "Single point recorded.");
		}
		catch (IOException e)
		{
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}
	}
	
	public void startSession() throws IOException
	{
		OutputStreamWriter osw;
		getTimestamp();
		File file = new File(FILEPATH, FILENAME);
		try
		{
			osw = new OutputStreamWriter(new FileOutputStream(file));
			osw.write("## Report Created by\r\n");
			osw.write("## GPS Raw Data Acquisition v" + BluetoothGPSActivity.version + "(For Android)\r\n");
			osw.write("## Free from Responsibility of Data Authenticity\r\n");
			osw.write("## Session Started At: " + TIMESTAMP + "\r\n\r\n");
			osw.flush();
			osw.close();
			Log.i(TAG, "Start new session.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.i(TAG, "Failed starting new sesson.");
		}
		
	}
	
	//start tracking when press Start Tracking button
	public void tracking()
	{
		flag = true;
		thread.start();
		Log.i(TAG, "Start tracking");
	}
	
	//initialize static TIMESTAMP & FILENAME when start a new session
	private final void getTimestamp()
	{
		String time = getTime();
		TIMESTAMP = time;
		FILENAME = PREFIX + time.replace(" ", "-").replace(":", "-") + SUFFIX;
		Log.i(TAG, "Timestamp: " + TIMESTAMP);
		Log.i(TAG, "Filename: " + FILENAME);
	}
}

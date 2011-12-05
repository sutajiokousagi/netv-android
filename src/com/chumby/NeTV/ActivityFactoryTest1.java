package com.chumby.NeTV;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class ActivityFactoryTest1 extends ActivityBaseNeTV implements OnClickListener
{
	//UI
	TextView _statusTextView;
	ProgressBar _progressBar;
	ImageView _loadingIcon;
	
	//Initialize Flag
	boolean _triedConnectNeTV;
	boolean _sentHandshake;
	boolean _receivedHandshake;
	String _factory_ssid;
		
	//Test flags
	long _totalTestTime;
	int _retryCounter;
	float _currentProgress;
	boolean _generatedRandomData;
	boolean _waitRandomDataSettle;
	
	boolean _performTest_Time;
	boolean _sentTime;
	boolean _passTest_Time;
	String _receivedTimeString;
	
	boolean _performTest_WifiScan;
	boolean _sentWifiScan;
	boolean _passTest_WifiScan;
	
	boolean _performTest_GetFileContents;
	boolean _sentGetFileContents;
	boolean _passTest_GetFileContents;
	String _opkgUpdateSource;
	
	boolean _performTest_ExistPSP;
	boolean _sentFileExist;
	boolean _passTest_ExistPSP;
	
	String _localMD5;
	boolean _performedTest_Upload;
	long _uploadTime;
	long _uploadSize;
	boolean _passTest_Upload;
	boolean _waitUploadSettle;
	double _uploadSpeed;
	
	boolean _performedTest_Download;
	long _downloadTime;
	boolean _passTest_Download;
	boolean _cleanUpRandomDataTest;
	double _downloadSpeed;
	
	boolean _performedTest_MD5;
	boolean _sentMD5;
	boolean _passTest_MD5;
	
	boolean _performedSwitchUI;
	boolean _sentSwitchUI;
	boolean _passSwitchUI;
	
	//Logging
	FileWriter logFileWriter;
	BufferedWriter bwriter;
	
	//List UI
	ListView _listView;
	ConsoleListAdapter consoleListViewAdapter;
	ArrayList<ConsoleListItem> consoleListItemsArray;
	
	//http://loopj.com/android-async-http/
	AsyncHttpClient _httpClient;
	
	protected static final String LOG_FILE = "netv_factory_log.log";
	protected static final String RANDOM_DATA_FILE = "random.txt";
	protected static final long RANDOM_DATA_SIZE = 2 * 1024 * 1024;
	protected static final String NETV_RANDOM_DATA_FILE = "/psp/random.txt";
	protected static final boolean ALWAYS_REGENERATE = false;
	protected static final double UPLOAD_THRESHOLD = 5.0;		//Mbps
	protected static final double DOWNLOAD_THRESHOLD = 5.0;		//Mbps
	
	// Private helper classes (this is awesome)
	// ----------------------------------------------------------------------------
	
	/*
	private AsyncHttpResponseHandler androidCommandHandler = new AsyncHttpResponseHandler()
	{
	    @Override
	    public void onSuccess(String response)
	    {
	    	Log.d(TAG, "SendAndroidHTTP: " + response);
	    	String status = response.split("</status>")[0].split("<status>")[1].trim();
	    	if (!status.equals("1"))
	    		Log.e(TAG, "SendAndroidHTTP: something is not right");
	    	_performedSwitchUI = true;
	    }
	    
	    @Override
	    public void onFailure(Throwable error)
	    {
	    	Log.e(TAG, "SendAndroidHTTP: failed");
			Log.e(TAG, "" + error);
	    }
	};
	*/
	
	// Initialization
	// ----------------------------------------------------------------------------

	/**
	 * Called when the activity is first created.
	 * 
	 * @category Initialization
	 * @see http://developer.android.com/reference/android/app/Activity.html
	 */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	Log.d(TAG, this.getLocalClassName() + " onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_factory1);

    	//Increase the default Heap memory size
    	//Otherwise, app will crash when we handle large file
    	//Android supposed to automatically increase the heap size for us,
    	//but I guess it doesn't apply to 1 big chunk of unfragmented data
    	dalvik.system.VMRuntime.getRuntime().setMinimumHeapSize(2 * RANDOM_DATA_SIZE + 6*1024*1024);   	
    	dalvik.system.VMRuntime.getRuntime().setTargetHeapUtilization(0.95f);
    	Log.d(TAG, "Minimum heap size: " + dalvik.system.VMRuntime.getRuntime().getMinimumHeapSize() );
    	Log.d(TAG, "Target heap utilization: " + dalvik.system.VMRuntime.getRuntime().getTargetHeapUtilization() );
    	
        _httpClient = new AsyncHttpClient();
        
        //Navbar UI
        ((ImageView) this.findViewById(R.id.btn_back)).setOnClickListener(this);
        _loadingIcon = (ImageView)findViewById(R.id.loading_icon);
        
        //Custom UI
    	_statusTextView = (TextView)findViewById(R.id.textViewStatus);
    	_progressBar = (ProgressBar)findViewById(R.id.progressBar1);
    	_listView = (ListView)findViewById(R.id.logList);
    	_statusTextView.setText("");
    	_progressBar.setMax(100);
    	_progressBar.setProgress(0);
    	
		consoleListItemsArray = new ArrayList<ConsoleListItem>();   
    	consoleListViewAdapter = new ConsoleListAdapter(this, consoleListItemsArray);
    	_listView.setAdapter( consoleListViewAdapter );
    	
    	try {
    		logFileWriter = new FileWriter(Environment.getExternalStorageDirectory()+ "/" + LOG_FILE, true);		//append mode
    		bwriter = new BufferedWriter(logFileWriter);
    	}
    	catch (Exception e)
    	{
    		logFileWriter = null;
    		bwriter = null;
		}
    }
    
	/**
	 * Called when the activity get focus, not visible, before onStart()
	 * 
	 * @category Initialization
	 * @see http://developer.android.com/reference/android/app/Activity.html
	 */
    @Override
    public void onResume()
    {
    	super.onResume();
    	
    	reset();
    	
    	//Start generating random data in the background
    	setStatusMessage("Generating random data...", "00b0f0");
    	Thread thread = new Thread(generateRandomTestDataRunnable);
    	thread.start();
    }
    
    /**
	 * Called when the activity is just paused, before onStop()
	 * 
	 * @category Initialization
	 * @see http://developer.android.com/reference/android/app/Activity.html
	 */
    @Override
    public void onPause()
    {
		setPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, ActivityFactoryTest1.class.getName());
		
		//Cancel unfinished tests
    	_handler.removeCallbacks(testSequenceRunnable);
    	_handler.removeCallbacks(initializeSequenceRunnable);
		
		//Back to home network
		_myApp.disconnectNeTV();
		
		try
		{
			if (bwriter != null)			bwriter.close();
			if (logFileWriter != null)		logFileWriter.close();
		}
		catch (Exception e) {

		}
		super.onPause();
    }
    
    /**
	 * Reset flags & states
	 * 
	 * @category Initialization
	 */
    public void reset()
    {
    	Log.d(TAG, this.getLocalClassName() + " reset()");
    	
    	_triedConnectNeTV = false;
    	_sentHandshake = false;
    	_receivedHandshake = false;
    	_factory_ssid = getPreferenceString(AppNeTV.PREF_WIFI_FACTORY_SSID, "");
   	
    	_currentProgress = 0;
    	_generatedRandomData = false;
    	_waitRandomDataSettle = false;
    	_localMD5 = "";
    	
    	resetTest();
    	
    	//Spin the loading icon
    	Animation animation = new RotateAnimation (0.0f, 359.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    	animation.setRepeatCount(Animation.INFINITE);
    	animation.setDuration(2000);
    	animation.setInterpolator(new Interpolator() { public float getInterpolation(float arg0) { return arg0; } });
    	_loadingIcon.startAnimation(animation);
    	_loadingIcon.setVisibility(View.VISIBLE);
    	
    	_handler.postDelayed(initializeSequenceRunnable, 3000);
    }
    
	public void resetTest()
    {
    	Log.d(TAG, this.getLocalClassName() + " resetTest()");
    	
    	_totalTestTime = System.currentTimeMillis();
    	_retryCounter = 0;
    	
    	_performTest_Time = false;
    	_sentTime = false;
    	_passTest_Time = false;
    	_receivedTimeString = "";
    	
    	_performTest_WifiScan = false;
    	_sentWifiScan = false;
     	_passTest_WifiScan = false;
    	
    	_performTest_GetFileContents = false;
    	_sentGetFileContents = false;
    	_passTest_GetFileContents = false;
    	_opkgUpdateSource = "";
    	
    	_performTest_ExistPSP = false;
    	_sentFileExist = false;
    	_passTest_ExistPSP = false;
    	
    	_performedTest_Upload = false;
    	_uploadTime = -1;
    	_passTest_Upload = false;
    	_waitUploadSettle = false;
    	_uploadSpeed = 0;
    	
    	_performedTest_Download = false;
    	_downloadTime = -1;
    	_passTest_Download = false;
    	_cleanUpRandomDataTest = false;
    	_downloadSpeed = 0;
    	
    	_performedTest_MD5 = false;
    	_passTest_MD5 = false;
    	_sentMD5 = false;	
    	
    	_performedSwitchUI = false;
    	_sentSwitchUI = false;
    	_passSwitchUI = false;
    }
    
    /**
	 * Custom behavior on Back button
	 * 
	 * @category UI Events
	 */
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_MENU)
		{
			return true;
		}

	    return super.onKeyDown(keyCode, event);    
	}
    
    /**
	 * Custom behavior on Back button (coming from the device dialog)
	 * 
	 * @category UI Events
	 */
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_MENU)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Buttons event
	 * 
	 * @category UI Events
	 */
	public void onClick(View v)
	{
    	_vibrator.vibrate(120);
    	  
    	//Sound feedback
    	if (ENABLE_SOUND_FX)
    	{
	    	_mediaPlayer = MediaPlayer.create(this, R.raw.pop);
	    	if (_mediaPlayer != null)
	    	{
	    		_mediaPlayer.start();
	    		_mediaPlayer.setOnCompletionListener(new OnCompletionListener()
	    		{
	    			public void onCompletion(MediaPlayer mp) { mp.release(); }
	    		});
	    	}
    	}

		if (v.getId() == R.id.btn_back)
		{
			finish();
			return;
		}
	}

	
    // UI Utility functions
	//----------------------------------------------------------------------------
    
    /**
	 * @category UI Utility
	 */
	@Override
	protected boolean updateNextStepUI()
	{
		// Do nothing
		return false;
	}

    /**
	 * @category UI Utility
	 */
	@Override
	protected void updateHashVariables()
	{
		// Do nothing
	}
	
	/**
	 * @category UI Utility
	 */
	protected void addToLogUI(String text)
	{
		addToLogUI(text, null);
	}
	
	/**
	 * @category UI Utility
	 */
	protected void addToLogUI(String text, String hexcolor)
	{
		if (hexcolor == null)
			hexcolor = "aaaaaa";
		//if (consoleListItemsArray.size() > 50)
    	//	consoleListItemsArray.remove(0);
    	
		String coloredText = "<font color='#" + hexcolor + "'>" + text + "</font>";
		consoleListItemsArray.add( new ConsoleListItem(coloredText, "") );
		consoleListViewAdapter.notifyDataSetChanged();
		_listView.setSelection(consoleListViewAdapter.getCount() - 1);
		
		//logListItems.add( "<font color='#" + hexcolor + "'>" + text + "</font>" );
	}
	
	/**
	 * @category UI Utility
	 */
	protected void setStatusMessage(String text)
	{
		setStatusMessage(text, null);
	}
	
	/**
	 * @category UI Utility
	 */
	protected void setStatusMessage(String text, String hexcolor)
	{
		if (hexcolor == null)
			hexcolor = "00b0f0";
		_statusTextView.setText(Html.fromHtml( "<font color='#" + hexcolor + "'>" + text + "</font>") );
	}
	
	/**
	 * @category UI Utility
	 */
	protected void updateProgressBar()
	{
		if (_currentProgress > 100)
			_currentProgress = 100;
		_progressBar.setProgress( (int)(_currentProgress) );
		long sizeMB = RANDOM_DATA_SIZE / 1024 / 1024;
		
		if (_currentProgress == 100)
		{
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			_statusTextView.setLayoutParams(params);

			_progressBar.setVisibility(View.INVISIBLE);
			setStatusMessage("Finished generating random data [" + sizeMB + "MB]");
		}
		else
		{
			setStatusMessage("Generating random data...[" + _currentProgress + "% of " + sizeMB + "MB]");
		}
	}
	
    // Test Utility functions
	//----------------------------------------------------------------------------
    
	/**
	 * @category Utility
	 */
	public String getUnixTimeString(int offsetSeconds)
	{
		//Construct time string, without the seconds
		//Example: Tue Aug 30 08:14:56

		Calendar rightNow = Calendar.getInstance();
		rightNow.add(Calendar.SECOND, offsetSeconds);
			
		DateFormat formatter = new SimpleDateFormat("EEE MMM d kk:mm:ss");
		String timeString = formatter.format(rightNow.getTime());     
		
		//Android is returning 24:00:00 instead of Unix 00:00:00. Yay!
		timeString = timeString.replace(" 24:", " 00:");
		
		return timeString;
	}
	
	/**
	 *  @category Utility	
	 */
	public boolean generateRandomTestData(String filename)
	{
		//Check if SD card is available
		String ext_state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(ext_state)) {
			addToLogUI("SD Card is not mounted", "ff0000");
	        Log.e(TAG, "SD Card is not mounted");
			return false;
		}
		if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(ext_state)) {
			addToLogUI("SD Card is not writable", "ff0000");
			Log.e(TAG, "SD Card is not writable");
			return false;
		}

		//Using older implementation with API Level 7 instead of newer one
		try
	    {
			File file = new File(Environment.getExternalStorageDirectory(), filename);
			if (!ALWAYS_REGENERATE)
			{
				if (file.exists() && file.isFile() && file.length() >= RANDOM_DATA_SIZE-1024 && file.length() <= RANDOM_DATA_SIZE+1024 )
				{
					_currentProgress = 100;
					_handler.post(updateRandomTestDataProgressRunnable);
					
					//Get MD5 (may take a long time)
					_localMD5 = getAndroidFileMD5(Environment.getExternalStorageDirectory().getPath() + "/" + filename);
					
					//No UI operation allowed here
					//addToLogUI("Using previously generated random data");
					Log.d(TAG, "Using previously generated random data");
			        Log.d(TAG, "MD5: " + _localMD5);
			        
			        _uploadSize = file.length();
			        _generatedRandomData = true;
					return true;
				}
			}
		    //if (!file.canWrite())
		    //	return false;

	        FileWriter gpxwriter = new FileWriter(file);
	        BufferedWriter out = new BufferedWriter(gpxwriter);
	        
	        long currentSizeBytes = 0;
	        float previousProgress = 0;
	        Random myRandom = new Random();
	        String randomString = "";
	        while (currentSizeBytes < RANDOM_DATA_SIZE)
	        {
	        	randomString = String.valueOf(myRandom.nextLong());
	        	currentSizeBytes += randomString.length();
	        	out.write( randomString );
	        	
	        	//Remember this is executed in a non-UI thread
	        	_currentProgress = Math.round( 100 * currentSizeBytes / RANDOM_DATA_SIZE );
	        	if (_currentProgress > 100)
	        		_currentProgress = 100;
	        	
	        	if (_currentProgress != previousProgress)
	        	{
	        		_handler.post(updateRandomTestDataProgressRunnable);
	        		try { Thread.sleep(20);	}
		        	catch (Exception e) {}
	        	}
	        	previousProgress = _currentProgress;	        	
	        }
	        out.close();
	        
	        //Get MD5 (may take a long time)
	        _localMD5 = getAndroidFileMD5(Environment.getExternalStorageDirectory().getPath() + "/" + filename);

	        //Done
	        _currentProgress = 100;
	        _handler.post(updateRandomTestDataProgressRunnable);
	        
	        //No UI operation allowed here
	        //addToLogUI("Finish generating new random data");
	        Log.d(TAG, "Finish generating new random data");
	        Log.d(TAG, "MD5: " + _localMD5);
	        
	        _generatedRandomData = true;
	    }
	    catch (IOException e)
	    {
	        Log.e(TAG, "Could not write file " + e.getMessage());
	        return false;
	    }
	    
		return true;
	}
	
	/**
	 * @category Utility
	 */
	public String getAndroidFileMD5(String fullFilePath)
	{
		String result = "";
		try
		{
			InputStream fis = new FileInputStream(fullFilePath);
			byte[] buffer = new byte[1024];
		    MessageDigest digest = MessageDigest.getInstance("MD5");
		    int numRead;
		    
		    do
		    {
		    	numRead = fis.read(buffer);
		    	if (numRead > 0)
		    		digest.update(buffer, 0, numRead);
		    }
		    while (numRead != -1);
		    fis.close();
		    byte[] b = digest.digest();
		    
		    for (int i=0; i < b.length; i++)
		       result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		catch (Exception e)
		{
			
		}

		return result;
	}
	
	/**
	 * @category Utility
	 */
	public void uploadFileToNeTV(String fullFilePath, String remotePath)
	{	
		_uploadTime = System.currentTimeMillis();
		
		File file = new File(fullFilePath);
		RequestParams params = new RequestParams();
		try
		{
		    params.put("path", remotePath);
		    params.put("cmd", MessageReceiver.COMMAND_UploadFile);
		    params.put("filedata", file);
		}
		catch(Exception e)
		{
			addToLogUI("Unable to upload file", "ff0000");
			Log.e(TAG, "Unable to upload file");
			Log.e(TAG, e.getMessage());
			_uploadSize = 0;
			_uploadTime = 0;
			_uploadSpeed = 0;
			_passTest_Upload = false;
			_performedTest_Upload = true;
			_myApp.sendAndroidJSTestConsole("Unable to upload file", "ff0000");
			return;
		}
		
		_uploadSize = file.length();
		String url = "http://" + AppNeTV.DEFAULT_IP_ADDRESS + "/bridge";
		
		//This fails on very large file
		_httpClient.cancelRequests(this, true);
		_httpClient.post(url, params, new AsyncHttpResponseHandler()
		{
		    @Override
		    public void onSuccess(String response)
		    {
		    	_uploadTime = System.currentTimeMillis() - _uploadTime;
		    	_passTest_Upload = true;
		    	
		    	//Some units used to crash here due to 'split' function on an null object
		    	long remote_size = 0;
		    	if (response.length() < 100)
		    		Log.d(TAG, "uploadFileToNeTV: " + response);
		    	
		    	try
		    	{
		    		if (response.split("<filesize>").length == 2 && response.split("</filesize>").length == 2)
		    			remote_size = Long.parseLong( response.split("</filesize>")[0].split("<filesize>")[1].trim() );
		    		else
		    			remote_size = 0;
		    	}
		    	catch (Exception e)
		    	{
		    		remote_size = 0;
		    	}
		    	
		    	//Check size
		    	if (remote_size != _uploadSize)
		    	{
		    		_passTest_Upload = false;
		    		addToLogUI("Uploaded size doesn't match", "ff0000");
		    		Log.e(TAG, "Uploaded size doesn't match");
		    	}
		    	
		    	_uploadSpeed = (remote_size * 8.0 / 1024.0 / 1024.0) / (_uploadTime / 1000.0);		    	
		    	if (_uploadSpeed < UPLOAD_THRESHOLD)
		    		_passTest_Upload = false;
		    	
		    	if (_passTest_Upload)
		    	{
		    		String message = "Upload test passed: " + String.format("%.1f",_uploadSpeed) + "Mbps";
		    		if (_uploadSpeed < UPLOAD_THRESHOLD) 	message += " < ";
		    		else									message += " > ";
		    		message += String.format("%.1f",UPLOAD_THRESHOLD) + "Mbps";
		    		
		    		addToLogUI(message, "00ff00");
		    		Log.i(TAG, message);	    		
		    		Log.i(TAG, "Local size: " + _uploadSize + " bytes");
		    		Log.i(TAG, "NeTV size:  " + remote_size + " bytes");
		    		_myApp.sendAndroidJSTestConsole(message, "00ff00");
		    	}
		    	else
		    	{
		    		String message = "Upload test failed: " + String.format("%.1f",_uploadSpeed) + "Mbps";
		    		if (_uploadSpeed < UPLOAD_THRESHOLD) 	message += " < ";
		    		else									message += " > ";
		    		message += String.format("%.1f",UPLOAD_THRESHOLD) + "Mbps";
		    		
		    		addToLogUI(message, "ff0000");
		    		Log.e(TAG, "Local size: " + _uploadSize + " bytes");
		    		Log.e(TAG, "NeTV size:  " + remote_size + " bytes");
		    		Log.e(TAG, message);
		    		_myApp.sendAndroidJSTestConsole(message, "ff0000");
		    	}
		    	
		    	_performedTest_Upload = true;
		    }
		    
		    @Override
		    public void onFailure(Throwable error)
		    {
		    	String errorMsg = (error == null) ? "" : error.toString();
		    	addToLogUI("Unable to upload file", "ff0000");
				Log.e(TAG, "Unable to upload file");
				Log.e(TAG, errorMsg);
		    	_uploadSize = 0;
				_uploadTime = 0;
				_passTest_Upload = false;
				_performedTest_Upload = true;
				_myApp.sendAndroidJSTestConsole("Unable to upload file", "ff0000");
		    }
		});
	}
	
	/**
	 * @category Utility
	 */
	public void downloadFileFromNeTV(String fullFilePath)
	{
		_downloadTime = System.currentTimeMillis();
		
		RequestParams params = new RequestParams();
		params.put("cmd", MessageReceiver.COMMAND_DownloadFile);
	    params.put("value", fullFilePath);
		
	    String url = "http://" + AppNeTV.DEFAULT_IP_ADDRESS + "/bridge";
	    _httpClient.cancelRequests(this, true);
		_httpClient.post(url, params, new AsyncHttpResponseHandler()
		{
			@Override
			public void onStart()
			{
				Log.i(TAG, "Start downloading file");
			}
			
			@Override
		    public void onFinish()
			{
				Log.i(TAG, "Finished downloading file");
		    }
			
		    @Override
		    public void onSuccess(String response)
		    {
		    	_downloadTime = System.currentTimeMillis() - _downloadTime;
		    	_passTest_Download = true;
		    	
		    	if (response.length() < 100)
		    		Log.d(TAG, "downloadFileFromNeTV: " + response);
		    	
		    	if (_uploadSize != response.length())
		    	{
		    		_passTest_Download = false;
		    		addToLogUI("Downloaded size doesn't match", "ff0000");
		    		Log.e(TAG, "Local size: " + _uploadSize + " bytes");
		    		Log.e(TAG, "NeTV size:  " + response.length() + " bytes");
		    	}
		    	
		    	_downloadSpeed = (response.length() * 8.0 / 1024.0 / 1024.0) / (_downloadTime / 1000.0);
		    	if (_downloadSpeed < DOWNLOAD_THRESHOLD)
		    		_passTest_Download = false;
		    	
		    	if (_passTest_Download)
		    	{
		    		String message = "Download test passed: " + String.format("%.1f",_downloadSpeed) + "Mbps";
		    		if (_downloadSpeed < DOWNLOAD_THRESHOLD) 	message += " < ";
		    		else									message += " > ";
		    		message += String.format("%.1f", DOWNLOAD_THRESHOLD) + "Mbps";
		    		
		    		addToLogUI(message, "00ff00");
		    		Log.i(TAG, message);
		    		Log.i(TAG, "Local size: " + _uploadSize + " bytes");
		    		Log.i(TAG, "NeTV size:  " + response.length() + " bytes");
		    		_myApp.sendAndroidJSTestConsole(message, "00ff00");
		    	}
		    	else
		    	{
			    	String message = "Download test failed: " + String.format("%.1f",_downloadSpeed) + "Mbps";
		    		if (_downloadSpeed < DOWNLOAD_THRESHOLD) 	message += " < ";
		    		else									message += " > ";
		    		message += String.format("%.1f", DOWNLOAD_THRESHOLD) + "Mbps";
		    		
		    		addToLogUI(message, "ff0000");
		    		Log.e(TAG, message);
		    		Log.e(TAG, "Local size: " + _uploadSize + " bytes");
		    		Log.e(TAG, "NeTV size:  " + response.length() + " bytes");
		    		_myApp.sendAndroidJSTestConsole(message, "ff0000");
		    	}
		    	
		    	_performedTest_Download = true;
		    }
		    
		    @Override
		    public void onFailure(Throwable error)
		    {
		    	String errorMsg = (error == null) ? "" : error.toString();
		    	addToLogUI("Unable to download file", "ff0000");
				Log.e(TAG, "Unable to download file");
				Log.e(TAG, errorMsg);
		    	_downloadTime = 0;
		    	_passTest_Download = false;
		    	_performedTest_Download = true;
		    	_myApp.sendAndroidJSTestConsole("Unable to download file", "ff0000");
		    }
		});
	}
	
	/**
	 * @category Utility
	 */
	public void addToLogFile(String text)
	{
		if (logFileWriter == null || bwriter == null)
			return;
		
	    try
	    {
	        bwriter.append(text + "\r\n");
	        //bwriter.newLine();		//linux style
	        bwriter.flush();
	    }
	    catch (Exception e)
	    {
	    }
	}
	
	// Runnable objects (for multi-threading implementation)
	//----------------------------------------------------------------------------
    
	/**
	 * Runnable object to be executed in another thread.
	 * 
	 * @see {@link #autoHighlightNetwork()}
	 */
    private Runnable generateRandomTestDataRunnable = new Runnable()
    {
    	public void run()
        {
        	generateRandomTestData(RANDOM_DATA_FILE);
        }
    };
    
    /**
	 * Runnable object to be executed in UI thread.
	 * 
	 * @see {@link #autoHighlightNetwork()}
	 */
    private Runnable updateRandomTestDataProgressRunnable = new Runnable()
    {
    	public void run()
        {
    		updateProgressBar();
        }
    };
	 	
	// Custom event
    //------------------------------------------------------------------
    
    /**
     * Handle custom events fired by the main application class when receive new socket message
     * 
     * @category CustomEvent
     */
	@Override
	public void NewMessageEvent(Bundle parameters)
	{
		//Filter out loopbacks
		String addressString = (String) parameters.get(MessageReceiver.MESSAGE_KEY_ADDRESS);
		parameters.remove(MessageReceiver.MESSAGE_KEY_ADDRESS);
		if (addressString == null || addressString.length() < 5 || _myApp.isMyIP(addressString))
			return;
		if (!addressString.equals(AppNeTV.DEFAULT_IP_ADDRESS))
			return;
		
		//Not a valid message
		if (!parameters.containsKey(MessageReceiver.COMMAND_MESSAGE))
			return;
		String commandName = (String)parameters.get(MessageReceiver.COMMAND_MESSAGE);
		if (commandName == null || commandName.length() < 2)
			return;
		commandName = commandName.toUpperCase();
		parameters.remove(MessageReceiver.COMMAND_MESSAGE);
		
		//----------------------
		
		if (commandName.equals(MessageReceiver.COMMAND_SetUrl.toUpperCase()))
		{
			Log.d(TAG, "Received SetUrl acknowledgement from " + addressString);
			return;
		}
		
		//----------------------
		
		if (commandName.equals(MessageReceiver.COMMAND_Handshake.toUpperCase()))
		{
			//Ignore message from another Android device (doesn't contain GUID and DCID)
			if (!parameters.containsKey(AppNeTV.PREF_CHUMBY_GUID) && !parameters.containsKey(AppNeTV.PREF_CHUMBY_DCID) )
				return;
							
			//Add it to device list
			Log.d(TAG, "Received handshake reply from " + addressString);
			
			//Save device info of this GUID
			saveDeviceParameters(addressString, parameters);
			
			//Print out parameters for debug
			for (String key: parameters.keySet())
				if (!key.equals(MessageReceiver.COMMAND_MESSAGE))
					Log.d(TAG, key + " = " + parameters.get(key));

			_receivedHandshake = true;
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_Time.toUpperCase()))
		{
			if (parameters.get(MessageReceiver.MESSAGE_KEY_VALUE) == null)
				return;
			String receivedTimeString = (String)parameters.get(MessageReceiver.MESSAGE_KEY_VALUE);
			if (receivedTimeString == null || receivedTimeString.length() < 5)
				return;
			if (_performTest_Time && _passTest_Time)
				return;
			receivedTimeString = receivedTimeString.replace("  ", " ");		//for date numbers < 10 (damn unix)
			_receivedTimeString = receivedTimeString;
			
			//Must contains the time within current 7 seconds
			_passTest_Time = false;
			int offsetSecond = 0;
			for (int i= -3; i<=3; i++)
				if (receivedTimeString.toUpperCase().contains( getUnixTimeString(i).toUpperCase() ))
				{
					_passTest_Time = true;
					offsetSecond = i;
					break;
				}
			
			//Pass or fail
			if (_passTest_Time)
			{
				addToLogUI("Time test passed", "00ff00");
				Log.i(TAG, "Time test passed");
				Log.i(TAG, receivedTimeString);
				_myApp.sendAndroidJSTestConsole("Time test passed", "00ff00");
			}
			else
			{
				addToLogUI("Time test failed", "ff0000");
				addToLogUI(receivedTimeString, "ff0000");
				addToLogUI(getUnixTimeString(offsetSecond), "ff0000");
				Log.e(TAG, "Time test failed");
				Log.e(TAG, receivedTimeString);
				Log.e(TAG, getUnixTimeString(offsetSecond));
				Log.e(TAG, addressString);
				_myApp.sendAndroidJSTestConsole("Time test passed", "ff0000");
			}
			
			_performTest_Time = true;
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_WifiScan.toUpperCase()))
		{
			String status = (String) parameters.get(MessageReceiver.COMMAND_STATUS);
			if (!status.equals("1"))
				return;
			if (_performTest_WifiScan && _passTest_WifiScan)
				return;
			
			_performTest_WifiScan = true;
			_passTest_WifiScan = true;
			addToLogUI("Received WifiScan response", "00ff00");
			Log.i(TAG, "Received WifiScan response");
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_GetFileContents.toUpperCase()))
		{
			//String status = (String) parameters.get(MessageReceiver.COMMAND_STATUS);
			String value = (String) parameters.get(MessageReceiver.MESSAGE_KEY_VALUE);
			if (value == null || value.length() <= 5 || value.contains("/etc/opkg/"))
				return;
			if (_performTest_GetFileContents && _passTest_GetFileContents)
				return;
						
			//Pass or fail
			_opkgUpdateSource = value;
			_passTest_GetFileContents = true;
			if (!value.contains("debug"))
			{
				_passTest_GetFileContents = true;
				addToLogUI("This is a Release build", "00ff00");
				Log.i(TAG, "This is a Release build");
				Log.i(TAG, value);
				_myApp.sendAndroidJSTestConsole("This is a Release build", "00ff00");
			}
			else
			{
				_passTest_GetFileContents = false;
				addToLogUI("This is a Debug build", "ff0000");
				addToLogUI(value, "ff0000");
				Log.e(TAG, "This is a Debug build");
				Log.e(TAG, value);
				_myApp.sendAndroidJSTestConsole("This is a Debug build", "ff0000");
			}
			
			_performTest_GetFileContents = true;
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_MD5File.toUpperCase()))
		{
			//String status = (String) parameters.get(MessageReceiver.COMMAND_STATUS);
			String value = (String) parameters.get(MessageReceiver.MESSAGE_KEY_VALUE);
			if (value == null || value.length() <= 5 || value.contains(RANDOM_DATA_FILE))
				return;
			if (_performedTest_MD5 && _passTest_MD5)
				return;

			//Pass or fail
			if (!value.trim().equals( _localMD5.trim() ))
			{
				_passTest_MD5 = false;
				addToLogUI("MD5 check failed", "ff0000");
				addToLogUI(_localMD5, "ff0000");
				addToLogUI(value, "ff0000");
				Log.e(TAG, "MD5 check failed");
				Log.e(TAG, "Local MD5: " + _localMD5);
				Log.e(TAG, "NeTV MD5:  " + value);
				_myApp.sendAndroidJSTestConsole("MD5 check failed", "ff0000");
			}
			else
			{
				_passTest_MD5 = true;
				addToLogUI("MD5 check passed", "00ff00");
				Log.i(TAG, "MD5 check passed");
				Log.i(TAG, "Local MD5: " + _localMD5);
				Log.i(TAG, "NeTV MD5:  " + value);
				_myApp.sendAndroidJSTestConsole("MD5 check passed", "00ff00");
			}
			
			_performedTest_MD5 = true;
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_UnlinkFile.toUpperCase()))
		{
			//String status = (String) parameters.get(MessageReceiver.COMMAND_STATUS);
			String value = (String) parameters.get(MessageReceiver.MESSAGE_KEY_VALUE);
			if (value == null || value.length() <= 1 || value.contains(RANDOM_DATA_FILE))
				return;
			
			if (value.toUpperCase().contains("TRUE"))
			{
				//addToLogUI("Cleaning up OK", "00ff00");
				Log.i(TAG, "Cleaning up OK [" + value + "]");
			}
			else
			{				
				//addToLogUI("Cleaning up failed", "ff0000");
				Log.e(TAG, "Cleaning up failed [" + value + "]");
			}
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_FileExists.toUpperCase()))
		{
			//String status = (String) parameters.get(MessageReceiver.COMMAND_STATUS);
			String value = (String) parameters.get(MessageReceiver.MESSAGE_KEY_VALUE);
			if (value == null || value.length() <= 1)
				return;
			if (_performTest_ExistPSP && _passTest_ExistPSP)
				return;
			
			if (value.toUpperCase().contains("FALSE"))
			{
				
				_passTest_ExistPSP = false;
				addToLogUI("/psp is NOT mounted correctly", "ff0000");
				Log.e(TAG, "/psp is NOT mounted correctly");
				_myApp.sendAndroidJSTestConsole("/psp test failed", "ff0000");
				_myApp.sendAndroidJSTestConsole("An unrecoverable firmware error was detected.", "ff0000");
				_myApp.sendAndroidJSTestConsole("Please re-burn the SD card.", "00b0f0");
				_myApp.sendAndroidJSTestConsole("<br>", "00b0f0");
				_myApp.sendAndroidJSTestConsole("Testing stopped", "ffffff");
				_retryCounter = 99;
			}
			else
			{
				_passTest_ExistPSP = true;
				addToLogUI("/psp is mounted correctly", "00ff00");
				Log.i(TAG, "/psp is mounted correctly");
				_myApp.sendAndroidJSTestConsole("/psp test passed", "00ff00");			
			}
			_performTest_ExistPSP = true;
			return;
		}
		
		//----------------------
		
		else if (commandName.equals(MessageReceiver.COMMAND_Android.toUpperCase()))
		{
			//Switching to IR Test UI
			//String status = (String) parameters.get(MessageReceiver.COMMAND_STATUS);
			
			if (_sentSwitchUI && !_performedSwitchUI)
			{
				String value = (String) parameters.get(MessageReceiver.MESSAGE_KEY_VALUE);
				if (value == null || value.length() <= 5)
					return;
				
				if (value.toUpperCase().contains("FORWARDED"))
				{
					_passSwitchUI = true;
					addToLogUI("Switched to Test UI on TV", "00ff00");
					Log.i(TAG, "Switched to Test UI on TV [" + value + "]");
				}
				else
				{				
					addToLogUI("Failed to switch to Test UI", "ff0000");
					Log.e(TAG, "Failed to switch to Test UI [" + value + "]");
				}
				
				_performedSwitchUI = true;
			}
			return;
		}
		
		Log.d(TAG, "Received command message: " + commandName);
	}
	
	// Application Logic
    //------------------------------------------------------------------
    
	/**
     * @category Application Logic
     */
	void saveDeviceParameters(String ipaddress, Bundle bundle)
	{
		setPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, ipaddress);
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_GUID))
			setPreferenceString(AppNeTV.PREF_CHUMBY_GUID, bundle.get(AppNeTV.PREF_CHUMBY_GUID).toString());

		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_GUID))
			setPreferenceString(AppNeTV.PREF_CHUMBY_GUID, bundle.get(AppNeTV.PREF_CHUMBY_GUID).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_FLASH_PLUGIN))
			setPreferenceString(AppNeTV.PREF_CHUMBY_FLASH_PLUGIN, bundle.get(AppNeTV.PREF_CHUMBY_FLASH_PLUGIN).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_HW_VERSION))
			setPreferenceString(AppNeTV.PREF_CHUMBY_HW_VERSION, bundle.get(AppNeTV.PREF_CHUMBY_HW_VERSION).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_FW_VERSION))
			setPreferenceString(AppNeTV.PREF_CHUMBY_FW_VERSION, bundle.get(AppNeTV.PREF_CHUMBY_FW_VERSION).toString());
	}
	
	/**
	 * @category Application Logic
	 */
	private void initializeSequence()
	{	
		//Stage 1
		//Start connecting to NeTV Access Point
		if (!_triedConnectNeTV && !_myApp.isConnectedNeTV())
		{
			_triedConnectNeTV = true;
			_retryCounter = 0;
			if (!_myApp.isConnectedNeTV())
	    	{
	    		if (SUPER_VERBOSE)
					_statusTextView.setText("Connecting to NeTV Access Point...");    		    		
	    		_handler.postDelayed(initializeSequenceRunnable, 3000);
	    		Log.d(TAG, this.getLocalClassName() + ": connecting to NeTV Access Point...");
	    		_myApp.connectNeTV(_factory_ssid);
	    		return;
	    	}
		}
    	if (!_myApp.isConnectedNeTV())
    	{
    		_retryCounter++;
    		if (_retryCounter <= 3) {
    			_handler.postDelayed(initializeSequenceRunnable, 3000);
    			Log.d(TAG, this.getLocalClassName() + ": waiting to be connected to NeTV Access Point...");
    			return;
    		}
    		
    		if (SUPER_VERBOSE)
				_statusTextView.setText("Could not connect to NeTV AP!");
    		Log.e(TAG, this.getLocalClassName() + ": could not connect to NeTV AP!");
    		//promptCheckNeTV();
    		_retryCounter = 0;
    		return;
    	}

		//Stage 2
		//Send handshake and wait a bit longer, to receive all handshakes
		if (!_sentHandshake)
		{
			if (SUPER_VERBOSE)
				_statusTextView.setText("Retrieving NeTV device info...");
			Log.d(TAG, this.getLocalClassName() + ": sent 1st handshake message");
			if (_retryCounter < 2)
			{
				//blasting handshake messages
				_myApp.sendHandshake();
				_retryCounter++;
				_handler.postDelayed(initializeSequenceRunnable, 300);
			}
			else
			{
				_sentHandshake = true;
				_retryCounter = 0;
				_handler.postDelayed(initializeSequenceRunnable, _receivedHandshake ? 300 : 500);
			}			
			return;
		}
					
		//Stage 3
		//Wait for handshake messages
		if (!_receivedHandshake)
		{
			_retryCounter++;
			if (_retryCounter <= 3)
			{
				_myApp.sendHandshake();
				_handler.postDelayed(initializeSequenceRunnable, 1000);
				
				if (SUPER_VERBOSE)
				{
					String animatedText = "Waiting for NeTV to reponse";
					for (int i=0; i<(_retryCounter%4); i++)			animatedText += ".";
					for (int i=0; i<3-(_retryCounter%4); i++)		animatedText += " ";
					_statusTextView.setText(animatedText);
				}
				return;
			}
			Log.e(TAG, this.getLocalClassName() + ": no handshake response for too long!");
			_receivedHandshake = true;
			_retryCounter = 0;
			return;
		}

		//Stage 5 - Switch to Android Test UI on TV
		if (!_performedSwitchUI)
		{
			if (!_sentSwitchUI)
			{
				addToLogUI("Switching to Android Test UI on TV");
				setStatusMessage("Switching to Android Test UI on TV");
				_sentSwitchUI = true;
				_passSwitchUI = false;
				_retryCounter = 0;
				_myApp.sendAndroidJSChangeView(AppNeTV.NETV_UISTATE_FACTORY_LOADING);
				//_myApp.SendAndroidHTTP("changeview", AppNeTV.NETV_UISTATE_FACTORY_LOADING, androidCommandHandler);
				_handler.postDelayed(initializeSequenceRunnable, 800);
				return;
			}
			
			_retryCounter++;
			if (_retryCounter <= 3)
			{
				_myApp.sendAndroidJSChangeView(AppNeTV.NETV_UISTATE_FACTORY_LOADING);
				_handler.postDelayed(initializeSequenceRunnable, 800);
				return;
			}
			
			_passSwitchUI = false;
			_performedSwitchUI = true;
			//addToLogUI("Failed switch to Android Test UI", "ff0000");
			Log.e(TAG, this.getLocalClassName() + ": Failed switch to Android Test UI");
			_handler.postDelayed(initializeSequenceRunnable, 1000);
			return;
		}
		
		resetTest();
		//Wait for fade in transition
    	setStatusMessage("Automatic tests are starting...");
    	_handler.postDelayed(testSequenceRunnable, 3000);
		return;
	}
	
	/**
	 * @category Application Logic
	 */
	private void testSequence()
	{
		//Test 1 - Setting time
		if (!_performTest_Time)
		{
			if (!_sentTime)
			{
				addToLogUI("Peforming time test");
				setStatusMessage("Peforming time test...");
				_sentTime = true;
				_passTest_Time = false;
				_retryCounter = 0;
				_myApp.sendSystemTime();
				_handler.postDelayed(testSequenceRunnable, 1500);
				return;
			}
			_retryCounter++;
			if (_retryCounter <= 3)
			{
				_myApp.sendSystemTime();
				_handler.postDelayed(testSequenceRunnable, 1000);
				return;
			}
			_passTest_Time = false;
			_performTest_Time = true;
			addToLogUI("Unable to verify time", "ff0000");
			Log.e(TAG, this.getLocalClassName() + ": no time response for too long!");
			_handler.postDelayed(testSequenceRunnable, 1000);
			return;
		}

		//Test 2 - Check debug or release 
		if (!_performTest_GetFileContents)
		{
			if (!_sentGetFileContents)
			{
				addToLogUI("Checking Release/Debug build");
				setStatusMessage("Checking Release/Debug build...");
				_sentGetFileContents = true;
				_passTest_GetFileContents = false;
				_retryCounter = 0;
				_myApp.sendRequestFileContent("/etc/opkg/chumby.conf");
				_handler.postDelayed(testSequenceRunnable, 1500);
				return;
			}
			_retryCounter++;
			if (_retryCounter <= 3)
			{
				_myApp.sendRequestFileContent("/etc/opkg/chumby.conf");
				_handler.postDelayed(testSequenceRunnable, 1000);
				return;
			}
			_passTest_GetFileContents = false;
			_performTest_GetFileContents = true;
			addToLogUI("Unable to verify Release/Debug build", "ff0000");
			Log.e(TAG, this.getLocalClassName() + ": no GetFileContents response for too long!");
			_handler.postDelayed(testSequenceRunnable, 1000);
			return;
		}
		
		//Test 3 - Check /psp is mounted correctly
		if (!_performTest_ExistPSP)
		{
			if (!_sentFileExist)
			{
				addToLogUI("Checking /psp mount");
				setStatusMessage("Checking /psp mount...");
				_sentFileExist = true;
				_passTest_ExistPSP = false;
				_retryCounter = 0;
				_myApp.sendRequestFileExists("/media/storage/var/psp");
				_handler.postDelayed(testSequenceRunnable, 1500);
				return;
			}
			_retryCounter++;
			if (_retryCounter <= 3)
			{
				_myApp.sendRequestFileExists("/media/storage/var/psp");
				_handler.postDelayed(testSequenceRunnable, 1000);
				return;
			}
		}
		else if (!_passTest_ExistPSP)
		{
			//Stop testing
			return;
		}
		
		
		//Wait til we finish generating random data
		if (!_generatedRandomData)
		{
			_handler.postDelayed(testSequenceRunnable, 2000);
			return;
		}
		
		//Wait for Android to save generated file to SD card
		if (!_waitRandomDataSettle)
		{
			_waitRandomDataSettle = true;
			addToLogUI("Waiting for Android to be ready");
			Log.d(TAG, "Waiting for Android to save generated file to SD card");
			_handler.postDelayed(testSequenceRunnable, 2000);
			return;
		}
		
		//Test 4 - Upload test 
		if (!_performedTest_Upload)
		{
			if (_uploadTime < 0)
			{
				addToLogUI("Performing upload test");
				setStatusMessage("Performing upload test...");
				_uploadTime = 0;
				_passTest_Upload = false;
				_retryCounter = 0;
				uploadFileToNeTV(Environment.getExternalStorageDirectory().getPath() + "/" + RANDOM_DATA_FILE, NETV_RANDOM_DATA_FILE);
				_handler.postDelayed(testSequenceRunnable, 3000);
				return;
			}
			
			//Allow up to 1 minute for this test
			_retryCounter++;
			if (_retryCounter <= 30) {
				setStatusMessage("Performing upload test... [" + (60-_retryCounter*2) + "]");
				_handler.postDelayed(testSequenceRunnable, 2000);
				return;
			}
			
			_uploadTime = 0;
			_uploadSize = 0;
			_passTest_Upload = false;
			_performedTest_Upload = true;
			addToLogUI("Failed to perform upload test", "ff0000");
			Log.e(TAG, this.getLocalClassName() + ": no upload test response for too long!");
			_handler.postDelayed(testSequenceRunnable, 2000);
			return;
		}
		
		//Wait for NeTV to save uploaded file to SD card
		if (!_waitUploadSettle)
		{
			_waitUploadSettle = true;
			addToLogUI("Waiting for NeTV to be ready");
			Log.d(TAG, "Waiting for NeTV to save uploaded file to SD card");
			_handler.postDelayed(testSequenceRunnable, 4000);
			return;
		}
		
		//Test 5 - Verify uploaded file
		if (!_performedTest_MD5)
		{
			if (!_sentMD5)
			{
				addToLogUI("Verifying uploaded file");
				setStatusMessage("Verifying uploaded file");
				_sentMD5 = true;
				_passTest_MD5 = false;
				_retryCounter = 0;
				_myApp.sendRequestMD5File(NETV_RANDOM_DATA_FILE);
				_handler.postDelayed(testSequenceRunnable, 3000);
				return;
			}
			
			_retryCounter++;
			if (_retryCounter <= 3)
			{
				_myApp.sendRequestMD5File(NETV_RANDOM_DATA_FILE);
				_handler.postDelayed(testSequenceRunnable, 4000);
				return;
			}
			
			_passTest_MD5 = false;
			_performedTest_MD5 = true;
			addToLogUI("Failed verify uploaded file (MD5)", "ff0000");
			Log.e(TAG, this.getLocalClassName() + ": no MD5 response for too long!");
			_handler.postDelayed(testSequenceRunnable, 2000);
			return;
		}
		
		//Test 6 - Download test 
		if (!_performedTest_Download)
		{
			if (_downloadTime < 0)
			{
				addToLogUI("Performing download test");
				setStatusMessage("Performing download test");
				_downloadTime = 0;
				_performedTest_Download = false;
				_retryCounter = 0;
				downloadFileFromNeTV(NETV_RANDOM_DATA_FILE);
				_handler.postDelayed(testSequenceRunnable, 3000);
				return;
			}
			
			//Allow up to 1 minute for this test
			_retryCounter++;
			if (_retryCounter <= 30) {
				setStatusMessage("Performing download test... [" + (60-_retryCounter*2) + "]");
				_handler.postDelayed(testSequenceRunnable, 2000);
				return;
			}
			
			_downloadTime = 0;
			_passTest_Download = false;
			_performedTest_Download = true;
			addToLogUI("Failed to perform download test", "ff0000");
			Log.e(TAG, this.getLocalClassName() + ": no download test response for too long!");
			_handler.postDelayed(testSequenceRunnable, 3000);
			return;
		}
		
		//Test 7 - Clean up remote file
		if (!_cleanUpRandomDataTest)
		{
			addToLogUI("Clean up random data file");
			setStatusMessage("Clean up random data file");
			_cleanUpRandomDataTest = true;
			_retryCounter = 0;
			_myApp.sendRequestUnlinkFile(NETV_RANDOM_DATA_FILE);
			_handler.postDelayed(testSequenceRunnable, 2000);
			return;
		}
		
		//Last step - Switch to IR Test UI on TV
		if (!_performedSwitchUI)
		{
			if (!_sentSwitchUI)
			{							
				addToLogUI("Switching to IR Test UI on TV");
				setStatusMessage("Switching to IR Test UI on TV");
				_sentSwitchUI = true;
				_passSwitchUI = false;
				_retryCounter = 0;
				//_myApp.SendAndroidHTTP("changeview", AppNeTV.NETV_UISTATE_FACTORY_IRREMOTE, androidCommandHandler);
				_myApp.sendAndroidJSChangeView(AppNeTV.NETV_UISTATE_FACTORY_IRREMOTE);
				_handler.postDelayed(testSequenceRunnable, 1500);
				return;
			}
			
			_retryCounter++;
			if (_retryCounter <= 3)
			{
				_myApp.sendAndroidJSChangeView(AppNeTV.NETV_UISTATE_FACTORY_IRREMOTE);
				_handler.postDelayed(testSequenceRunnable, 600);
				return;
			}
			
			_passSwitchUI = false;
			_performedSwitchUI = true;
			//addToLogUI("Failed switch to IR Test UI", "ff0000");
			Log.e(TAG, this.getLocalClassName() + ": Failed switch to IR Test UI");
			_handler.postDelayed(testSequenceRunnable, 500);
			return;
		}
		
		addToLogUI("Finished automatic testing", "00b0f0");
		addToLogUI("Continue with manual IR Test on TV now", "00b0f0");
		setStatusMessage(" ");
		_loadingIcon.setVisibility(View.INVISIBLE); 
		_totalTestTime = (System.currentTimeMillis() - _totalTestTime) / 1000;

		//Log to SD card
		writeSummaryLog();
		
		//Back to home network
		_myApp.disconnectNeTV();

		//We should show a summary screen
		//Log.d(TAG, "Going to Factory Test 2 activity...");
		//gotoNextActivity(ActivityRemoteMain.class);
		//overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
		return;
	}
	
	/**
	 * Private helper class instance
	 * 
	 * @category Application Logic
	 */
	private Runnable initializeSequenceRunnable = new Runnable()
    {
        public void run()
        {
        	initializeSequence();
        }
    };
    
    /**
	 * Private helper class instance
	 * 
	 * @category Application Logic
	 */
	private Runnable testSequenceRunnable = new Runnable()
    {
        public void run()
        {
        	testSequence();
        } 
    };
    
    /**
     * @category Application logic
     */
    public void writeSummaryLog()
    {
    	Log.d(TAG, "writeSummaryLog");
    	
    	StringBuilder stringBuilder = new StringBuilder();
    	stringBuilder.append("<log_entry>");
    	stringBuilder.append("\r\n");
    	
    	//GUID
    	stringBuilder.append("\t<" + AppNeTV.PREF_CHUMBY_GUID + ">"
    			.concat(getPreferenceString(AppNeTV.PREF_CHUMBY_GUID, ""))
    			.concat("</" + AppNeTV.PREF_CHUMBY_GUID + ">\r\n"));
    	
    	//Firmware version
    	stringBuilder.append("\t<" + AppNeTV.PREF_CHUMBY_FW_VERSION + ">"
    			.concat(getPreferenceString(AppNeTV.PREF_CHUMBY_FW_VERSION, ""))
    			.concat("</" + AppNeTV.PREF_CHUMBY_FW_VERSION + ">\r\n"));
    	
    	//Hardware version
    	stringBuilder.append("\t<" + AppNeTV.PREF_CHUMBY_HW_VERSION + ">"
    			.concat(getPreferenceString(AppNeTV.PREF_CHUMBY_HW_VERSION, ""))
    			.concat("</" + AppNeTV.PREF_CHUMBY_HW_VERSION + ">\r\n"));
    	
    	//MAC
    	stringBuilder.append("\t<mac>" + getPreferenceString(AppNeTV.PREF_CHUMBY_MAC, "") + "</mac>\r\n");
    	
    	//Android device info
    	stringBuilder.append("\t<androidModel>" + android.os.Build.MODEL + "</androidModel>\r\n");
    	stringBuilder.append("\t<androidMfr>" + android.os.Build.MANUFACTURER + "</androidMfr>\r\n");
    	stringBuilder.append("\t<androidOS>" + android.os.Build.VERSION.RELEASE + "</androidOS>\r\n");
    	stringBuilder.append("\t<androidSDK>" + android.os.Build.VERSION.SDK_INT + "</androidSDK>\r\n");
    	stringBuilder.append("\t<androidTime>" + (new Date()).toString() + "</androidTime>\r\n");
    	
    	//Environment
    	stringBuilder.append("\t<wifiCount>" + _myApp.getScanResults().size() + "</wifiCount>\r\n");
    	stringBuilder.append("\t<wifiRSSI>" + _myApp.getConnectedNetworkSignalLevel() + "</wifiRSSI>\r\n");
   	
    	//App
    	stringBuilder.append("\t<appVersion>" + _myApp.getAppVersion() + "</appVersion>\r\n");
    	stringBuilder.append("\t<appTestDuration>" + _totalTestTime + "</appTestDuration>\r\n");
    	
    	//Speeds
    	stringBuilder.append("\t<randomDataSize>" + _uploadSize + "</randomDataSize>\r\n");
    	stringBuilder.append("\t<uploadSpeed>" + _uploadSpeed + "</uploadSpeed>\r\n");
    	stringBuilder.append("\t<downloadSpeed>" + _downloadSpeed + "</downloadSpeed>\r\n");
    	
    	//MD5
    	stringBuilder.append("\t<passMD5>" + _passTest_MD5 + "</passMD5>\r\n");
    	
    	//Time test
    	stringBuilder.append("\t<passTime>" + _passTest_Time + "</passTime>\r\n");
    	stringBuilder.append("\t<netvTime>" + _receivedTimeString + "</netvTime>\r\n");
    	
    	//System critical
    	stringBuilder.append("\t<pspExists>" + _passTest_ExistPSP + "</pspExists>\r\n");
    	
    	//Release/Debug test
    	stringBuilder.append("\t<isRelease>" + _passTest_GetFileContents + "</isRelease>\r\n");
    	stringBuilder.append("\t<opkgPath>" + _opkgUpdateSource.trim() + "</opkgPath>\r\n");
    	
    	stringBuilder.append("</log_entry>");
    	addToLogFile(stringBuilder.toString());
    }

}



package com.chumby.NeTV;

import java.util.HashMap;

import com.loopj.android.http.AsyncHttpResponseHandler;

import android.app.AlertDialog;
import android.app.Dialog;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

public class ActivityConfiguring extends ActivityBaseNeTV
{
	// UI
	TextView statusTextView;
	AlertDialog promptCheckNeTVDialog;

	// Flag
	int _retryCounter;
	boolean _sentHandshake;
	boolean _receivedHandshake;
	private int _sendDataState;
	private int _waitNetworkConfigCounter;
	String _guid;

	// Multiple device
	Dialog _deviceListDialog;
	HashMap<String, Bundle> _deviceList;
	
	// Private helper classes (this is awesome)
	// ----------------------------------------------------------------------------
	
	private AsyncHttpResponseHandler setTimerHandler = new AsyncHttpResponseHandler()
	{
	    @Override
	    public void onSuccess(String response)
	    {
	    	Log.d(TAG, "SetTimeHTTP: " + response);
	    	String status = response.split("</status>")[0].split("<status>")[1].trim();
	    	if (!status.equals("1"))
	    		Log.e(TAG, "SetTimeHTTP: something is not right");
	    }
	    
	    @Override
	    public void onFailure(Throwable error)
	    {
	    	Log.e(TAG, "SetTimeHTTP: failed");
			Log.e(TAG, "" + error);
	    }
	};
	
	private AsyncHttpResponseHandler setNetworkHandler = new AsyncHttpResponseHandler()
	{
	    @Override
	    public void onSuccess(String response)
	    {		    	
	    	Log.d(TAG, "SetNetworkHTTP: " + response);
	    	String status = response.split("</status>")[0].split("<status>")[1].trim();
	    	if (!status.equals("1"))
	    		Log.e(TAG, "SetNetworkHTTP: something is not right");	    		
	    }
	    
	    @Override
	    public void onFailure(Throwable error)
	    {
	    	Log.e(TAG, "SetNetworkHTTP failed");
			Log.e(TAG, "" + error);
	    }
	};
	
	
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
		setContentView(R.layout.configuring);
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

		// Synchronize UI with NeTV
		_myApp.sendAndroidJSChangeView(AppNeTV.NETV_UISTATE_ACTIVATION);

		_retryCounter = 0;
		_sentHandshake = false;
		_receivedHandshake = false;
		_sendDataState = 0;
		_waitNetworkConfigCounter = 0;
		_guid = getPreferenceString(AppNeTV.PREF_CHUMBY_GUID, "");

		if (_deviceList == null)
			_deviceList = new HashMap<String, Bundle>();
		_deviceList.clear();

		// Setup UI
		statusTextView = (TextView) findViewById(R.id.txtActivationStatus2);
		
		_myApp.sendNeTVBrowserJavaScript("function android_timeout() { fDbg2('New timeout. Do nothing.');	}");
		_myApp.sendNeTVBrowserJavaScript("function android_timeout() { fDbg2('New timeout. Do nothing.');	}");
		_myApp.sendNeTVBrowserJavaScript("function android_timeout() { fDbg2('New timeout. Do nothing.');	}");

		initializeSequence();
	}

	@Override
	public void onPause()
	{
		setPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, ActivityConfiguring.class.getName());
		super.onPause();
	}

	/**
	 * Custom behavior on Back button
	 * 
	 * @category UI Events
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK
				|| keyCode == KeyEvent.KEYCODE_SEARCH
				|| keyCode == KeyEvent.KEYCODE_MENU) {
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	// Utility functions copied from WifiList.java
	// ----------------------------------------------------------------------------

	/**
	 * @category UI Utility
	 */
	protected boolean updateNextStepUI()
	{
		return true;
	}

	/**
	 * @category UI Utility
	 */
	protected void updateHashVariables()
	{
		// We don't have any interesting UI in this Activity
	}

	// Custom event
	// ------------------------------------------------------------------

	/**
	 * Handle custom events fired by the main application class when receive new
	 * socket message
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
		//if (!addressString.equals(AppNeTV.DEFAULT_IP_ADDRESS))
		//	return;

		//Not a valid message
		if (!parameters.containsKey(MessageReceiver.COMMAND_MESSAGE))
			return;
		String commandName = (String) parameters.get(MessageReceiver.COMMAND_MESSAGE);
		if (commandName == null)
			commandName = "";
		commandName = commandName.toUpperCase();
		parameters.remove(MessageReceiver.COMMAND_MESSAGE);

		//----------------------------------------------------
		
		if (commandName.equals(MessageReceiver.COMMAND_Time.toUpperCase()))
		{
			Log.d(TAG, "Received time confirmation from from NeTV");
			Log.d(TAG, "" + parameters.get(MessageReceiver.MESSAGE_KEY_VALUE));
		}
		else if (commandName.equals(MessageReceiver.COMMAND_Network.toUpperCase()))
		{
			Log.d(TAG, "Received network config confirmation from from NeTV");
			Log.d(TAG, "" + parameters.get(MessageReceiver.MESSAGE_KEY_VALUE));
		}
		else if (commandName.equals(MessageReceiver.COMMAND_Android.toUpperCase()))
		{
			// Echo from NeTV. Ignored
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_Handshake.toUpperCase()))
		{
			Log.d(TAG, "Received handshake reply from " + addressString);

			// Add it to device list
			_deviceList.put(addressString, parameters);

			String tmpGUID = null;
			if (parameters.containsKey(AppNeTV.PREF_CHUMBY_GUID))
				tmpGUID = parameters.get(AppNeTV.PREF_CHUMBY_GUID).toString();

			// Received this when we re-connected to 'NeTV' to check for error
			if (_myApp.isConnectedNeTV())
			{
				if (tmpGUID != null && _guid.equals(tmpGUID)) {
					// Print out parameters for debug
					for (String key : parameters.keySet())
						if (!key.equals(MessageReceiver.COMMAND_MESSAGE))
							Log.d(TAG, key + " = " + parameters.get(key));

					Log.d(TAG, "Check error code here!!");
				}
			}

			// Receive handshake when connected to 'home' network
			else if (tmpGUID != null && _guid.equals(tmpGUID))
			{
				getDeviceActivationData(parameters, tmpGUID);
				saveDeviceParameters(addressString);
				_receivedHandshake = true;
				_sendDataState = 4;
				
				// Replace manual IP setting
				String _manual_ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_MANUAL, "");
				if (_manual_ipaddress.length() > 6)
					setPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, addressString);

				// Toast it
				toast("Your device has been configured for Internet access: " + addressString);

				// Reset the browser
				if (_useTCP) {
					_myApp.ResetUrlHTTP(null);
				} else {
					_myApp.sendNeTVBrowserReset();
					_myApp.sendNeTVBrowserReset();
					_myApp.sendNeTVBrowserReset();
				}
				
				// Sound feedback
				_mediaPlayer = MediaPlayer.create(this, R.raw.tada);
				if (_mediaPlayer != null)
				{
					_mediaPlayer.start();
					_mediaPlayer.setOnCompletionListener(new OnCompletionListener()
					{
						public void onCompletion(MediaPlayer mp)
						{
							mp.release();
						}
					});
				}

				// Go to next activity with delay
				_handler.postDelayed(configuringDoneRunnable, 1000);
			}
		}
		else
		{
			Log.d(TAG, "Received command message: " + commandName);
		}
	}

	// Application Logic
	// ------------------------------------------------------------------
	
	/**
	 * @category Application Logic
	 */
	void getDeviceActivationData(Bundle parameters, String guid)
	{
		HashMap<String, String> dataHashMap = _myApp.getChumbyGUIDProfile(guid);
		if (dataHashMap.size() < 1)
		{
			parameters.putString(AppNeTV.PREF_CHUMBY_ACTIVATED, "false");
		}
		else
		{
			parameters.putString(AppNeTV.PREF_CHUMBY_ACTIVATED, "true");
			for (String key : dataHashMap.keySet())
				parameters.putString(key, dataHashMap.get(key).toString());
		}
	}

	/**
	 * @category Application Logic
	 */
	void saveDeviceParameters(String ipaddress)
	{
		Bundle bundle = _deviceList.get(ipaddress);
		setPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, ipaddress);

		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_ACTIVATED))
			setPreferenceString(AppNeTV.PREF_CHUMBY_ACTIVATED,
					bundle.get(AppNeTV.PREF_CHUMBY_ACTIVATED).toString());

		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_GUID))
			setPreferenceString(AppNeTV.PREF_CHUMBY_GUID,
					bundle.get(AppNeTV.PREF_CHUMBY_GUID).toString());

		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_USERNAME))
			setPreferenceString(AppNeTV.PREF_CHUMBY_USERNAME,
					bundle.get(AppNeTV.PREF_CHUMBY_USERNAME).toString());

		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_GUID))
			setPreferenceString(AppNeTV.PREF_CHUMBY_GUID,
					bundle.get(AppNeTV.PREF_CHUMBY_GUID).toString());

		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_FLASH_PLUGIN))
			setPreferenceString(AppNeTV.PREF_CHUMBY_FLASH_PLUGIN,
					bundle.get(AppNeTV.PREF_CHUMBY_FLASH_PLUGIN).toString());

		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_HW_VERSION))
			setPreferenceString(AppNeTV.PREF_CHUMBY_HW_VERSION,
					bundle.get(AppNeTV.PREF_CHUMBY_HW_VERSION).toString());

		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_FW_VERSION))
			setPreferenceString(AppNeTV.PREF_CHUMBY_FW_VERSION,
					bundle.get(AppNeTV.PREF_CHUMBY_FW_VERSION).toString());
	}

	/**
	 * @category Application Logic
	 */
	private void initializeSequence()
	{
		// Stage 0
		// Send time configurations
		if (_sendDataState == 0)
		{
			if (SUPER_VERBOSE)
				statusTextView.setText("Configuring system time...");
			_sendDataState++;
			if (_useTCP)		_myApp.SetTimeHTTP(setTimerHandler);
			else				_myApp.sendSystemTime();
			_handler.postDelayed(initializeSequenceRunnable, 750);
			return;
		}

		// Stage 1
		// Waiting for NeTV to be configured
		if (_sendDataState == 1)
		{
			_sendDataState++;
			_waitNetworkConfigCounter = NETWORK_CONFIG_WAIT_SECONDS;
			_receivedHandshake = false;
			if (_useTCP)		_myApp.SetNetworkHTTP(setNetworkHandler);
			else				_myApp.sendNetworkConfig();
			_handler.postDelayed(initializeSequenceRunnable, 750);
			return;
		}

		// Should be disconnected from NeTV & connected to 'home' here
		if (_sendDataState == 2)
		{
			_waitNetworkConfigCounter--;

			// Android OS will try to reconnect to NeTV if the disconnected
			// duration is short
			// We don't want this, so we explicitly delete NeTV wifi config
			if (_waitNetworkConfigCounter >= NETWORK_CONFIG_WAIT_SECONDS - 1)
			{
				_myApp.disconnectNeTV();

				// We also want to explicitly connect to 'home' network
				// This may or may not be the user's initial network
				// (PREF_PREVIOUS_NETID)
				_myApp.connectHome(getPreferenceString(AppNeTV.PREF_WIFI_SSID, ""));
			}
			else if (_waitNetworkConfigCounter == 40
					|| _waitNetworkConfigCounter == 20
					|| _waitNetworkConfigCounter == 10)
			{
				_myApp.startScan();
				if (SUPER_VERBOSE)
					statusTextView.setText("Scanning for wifi...");
			}
			else if (_waitNetworkConfigCounter > 0)
			{
				if (SUPER_VERBOSE)
					statusTextView.setText("Configuring network...[" + _waitNetworkConfigCounter + "]");

				if (_waitNetworkConfigCounter % 5 == 0)
					_myApp.sendHandshake();
			}
			else
			{
				if (SUPER_VERBOSE)
					statusTextView.setText("Please keep waiting...");
			}

			if (_waitNetworkConfigCounter <= 0 || _receivedHandshake) {
				_sendDataState++;
				_myApp.enableAllNetworkConfig();
			}
			_handler.postDelayed(initializeSequenceRunnable, 1000);
			return;
		}

		// Already successfully configured
		if (_receivedHandshake)
			return;

		// Stage 4
		// Connected back to NeTV to check for error
		// We should have a full state machine here to check for the exact error
		if (_myApp.isConnectedNeTV())
		{
			gotoNextActivity(ActivityWifiDetails.class);
			_myApp.sendHandshake();
			return;
		}

		// Stage 5
		// Check NeTV network around & connect to it
		if (_myApp.isNeTVinRange())
		{
			if (SUPER_VERBOSE)
				statusTextView.setText("Error encountered. Checking for error...");
			_myApp.connectNeTV();
			_handler.postDelayed(initializeSequenceRunnable, 8000);
			return;
		}

		// Just wait and call this init sequence again
		_myApp.sendHandshake();
		_handler.postDelayed(initializeSequenceRunnable, 3000);
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
	private Runnable configuringDoneRunnable = new Runnable()
	{
		public void run()
		{
			//Allow unactivated device, we don't need to check the 'activated' flag
			if (ALLOW_UNACTIVATED_REMOTE)
			{
				Log.i(TAG, "Going to ActivityRemoteMain...");
				gotoNextActivity(ActivityRemoteMain.class);
				overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
				return;
			}
			
			String activated = getPreferenceString(AppNeTV.PREF_CHUMBY_ACTIVATED, "false");
			if (activated.equals("true"))
			{
				Log.i(TAG, "Activated device, going to ActivityRemoteMain...");
				gotoNextActivity(ActivityRemoteMain.class);
			}
			else
			{
				Log.i(TAG, "Unactivated device, going to ActivityAccount...");
				Log.i(TAG, "Going to ActivityAccount...");
				gotoNextActivity(ActivityAccount.class);
			}
			overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
		}
	};
}

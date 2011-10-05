package com.chumby.NeTV;

import java.util.HashMap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class ActivityFactory extends ActivityBaseNeTV
{
	//UI
	TextView statusTextView;
	AlertDialog promptCheckNeTVDialog;
		
	//Flag
	int _retryCounter;
	boolean _scannedWifi;
	boolean _triedConnectNeTV;
	boolean _sentHandshake;
	boolean _receivedHandshake;
	boolean _sentSetUrl;
	boolean _receivedSetUrl;
	
	//Multiple device
	HashMap<String, Bundle> _deviceList;
	
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
        setContentView(R.layout.main);
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
    	
    	//Version number
	    ((TextView)findViewById(R.id.textViewVersion)).setText(_myApp.getAppVersion());
    	
    	statusTextView = (TextView)findViewById(R.id.textViewStatus);
    	    	
    	//Animate the chumby logo
    	AnimationSet set = new AnimationSet(true);
    	Animation animation = new TranslateAnimation(0, 0, 20, 0);
    	animation.setDuration(750);
    	animation.setInterpolator(new DecelerateInterpolator());
    	set.addAnimation(animation);
    	animation = new AlphaAnimation(0, 1);
    	animation.setDuration(500);
    	set.addAnimation(animation);
    	ImageView logo = (ImageView)findViewById(R.id.chumby_logo);
        logo.startAnimation(set);

        //Fade in the version number
        animation = new AlphaAnimation(0, 1);
    	animation.setDuration(1000);
    	set.addAnimation(animation);
    	((TextView)findViewById(R.id.textViewVersion)).startAnimation(animation);
    	
    	reset();
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
		setPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, ActivityFactory.class.getName());
		
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
    	
    	_retryCounter = 0;
    	_scannedWifi = false;
    	_triedConnectNeTV = false;
    	_sentHandshake = false;
    	_receivedHandshake = false;
    	_sentSetUrl = false;
    	_receivedSetUrl = false;
    	
    	if (promptCheckNeTVDialog != null)
    		promptCheckNeTVDialog.dismiss();
    	promptCheckNeTVDialog = null;
    	    	
    	if (_deviceList == null)
    		_deviceList = new HashMap<String, Bundle>();
    	_deviceList.clear();
    	   	
    	//Clear last IP Address
    	setPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	setPreferenceString(AppNeTV.PREF_CHUMBY_ACTIVATED, "false");
    	
		initializeSequence();
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
			reset();
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
			reset();
			return true;
		}
		return false;
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
     * Popup a modal dialog box to instruct user to check for NeTV power connection
     * 
     * @category UI Utility
     */
    private void promptCheckNeTV()
	{
    	String message = this.getString(R.string.check_netv_power);   	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message)
		       .setCancelable(true)
		       .setNegativeButton(this.getString(R.string.button_retry), new DialogInterface.OnClickListener()
		       {
		           public void onClick(DialogInterface dialog, int id)
		           {
		        	   _retryCounter = 1;
		        	   reset();
		           }
		       });
		
		promptCheckNeTVDialog = builder.create();
		promptCheckNeTVDialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;
		promptCheckNeTVDialog.show();
	}
       
 	
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
		
		if (commandName.equals(MessageReceiver.COMMAND_Handshake.toUpperCase()))
		{
			//Ignore message from another Android device (doesn't contain GUID and DCID)
			if (!parameters.containsKey(AppNeTV.PREF_CHUMBY_GUID) && !parameters.containsKey(AppNeTV.PREF_CHUMBY_DCID) )
				return;
							
			//Add it to device list
			_deviceList.put(addressString, parameters);	
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
		else if (commandName.equals(MessageReceiver.COMMAND_SetUrl.toUpperCase()))
		{
			Log.d(TAG, "Received SetUrl acknowledgement from " + addressString);
			_receivedSetUrl = true;
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
		
		String mac = bundle.get(AppNeTV.PREF_CHUMBY_MAC).toString();
		mac = mac.substring(0, Math.min(mac.length(), 23));
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_MAC))
			setPreferenceString(AppNeTV.PREF_CHUMBY_MAC, mac);
	}
	
	/**
	 * @category Application Logic
	 */
	private void initializeSequence()
	{	
		//Stage 1
		//Wait until the communication service is up
		if (!_myApp.isCommServiceRunning())
		{
			if (SUPER_VERBOSE)
				statusTextView.setText("Waiting for CommService to bind...");
			_handler.postDelayed(initializeSequenceRunnable, 600);
			Log.d(TAG, this.getLocalClassName() + ": waiting for CommService to bind...");
			_retryCounter = 0;
			_sentHandshake = false;
			_receivedHandshake = false;
			return;
		}
		
		//Stage 2
		//Check for 'NeTV' access point
		if (!_scannedWifi)
		{
			_scannedWifi = true;
			if (SUPER_VERBOSE)
				statusTextView.setText("Scanning for WiFi...[" + (_retryCounter+1) + "/3]");
			_handler.postDelayed(initializeSequenceRunnable, 4000);
			Log.d(TAG, this.getLocalClassName() + ": scanning for WiFi...[" + (_retryCounter+1) + "/3]");
			_myApp.startScan();
			return;
		}
		if (!_myApp.isNeTVinRange())
		{
			_retryCounter++;
			if (_retryCounter < 3)	{
				_handler.postDelayed(initializeSequenceRunnable, 2000);
				return;
			}
			
			if (SUPER_VERBOSE)
				statusTextView.setText("NeTV Access Point not found!");
			Log.e(TAG, this.getLocalClassName() + ": NeTV Access Point not found!");
			promptCheckNeTV();
			_retryCounter = 0;
			return;
		}

		//Stage 3
		//Start connecting to NeTV Access Point
		if (!_triedConnectNeTV && !_myApp.isConnectedNeTV())
		{
			_triedConnectNeTV = true;
			_retryCounter = 0;
			if (!_myApp.isConnectedNeTV())
	    	{
	    		if (SUPER_VERBOSE)
					statusTextView.setText("Connecting to NeTV Access Point...");    		    		
	    		_handler.postDelayed(initializeSequenceRunnable, 3000);
	    		Log.d(TAG, this.getLocalClassName() + ": connecting to NeTV Access Point...");
	    		_myApp.connectNeTV();
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
				statusTextView.setText("Could not connect to NeTV AP!");
    		Log.e(TAG, this.getLocalClassName() + ": could not connect to NeTV AP!");
    		promptCheckNeTV();
    		_retryCounter = 0;
    		return;
    	}

		//Stage 4
		//Send handshake and wait a bit longer, to receive all handshakes
		if (!_sentHandshake)
		{
			if (SUPER_VERBOSE)
				statusTextView.setText("Retrieving NeTV device info...");
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
					
		//Stage 5
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
					statusTextView.setText(animatedText);
				}
				return;
			}
			Log.e(TAG, this.getLocalClassName() + ": no handshake response for too long!");
			promptCheckNeTV();
			_retryCounter = 0;
			return;
		}
				
		//Stage 6
		//Send SetUrl to switch UI to Factory Test homepage
		if (!_sentSetUrl)
		{
			_sentSetUrl = true;
			_retryCounter = 0;
			if (SUPER_VERBOSE)
				statusTextView.setText("Switching to Factory Test UI");
			_handler.postDelayed(initializeSequenceRunnable, 2500);
			Log.d(TAG, this.getLocalClassName() + ": switching to Factory Test UI");
			_myApp.sendNeTVBrowserSetUrl(AppNeTV.FACTORY_TEST_URL);
			return;
		}	

		//Stage 7
		//Wait for SetUrl acknowledgement
		if (!_receivedSetUrl)
		{
			_retryCounter++;
			if (_retryCounter <= 3)
			{
				_handler.postDelayed(initializeSequenceRunnable, 2500);
				Log.d(TAG, this.getLocalClassName() + ": waiting for Factory Test UI");
				return;
			}
			Log.e(TAG, this.getLocalClassName() + ": no SetUrl acknowledgement for too long!");
			promptCheckNeTV();
			_retryCounter = 0;
			return;
		}
		
		Log.d(TAG, "Going to Factory Test 1 activity...");
		gotoNextActivity(ActivityFactoryTest1.class);
		overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
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
}
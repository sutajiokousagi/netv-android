package com.bunniestudios.NeTV;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class ActivitySplash extends ActivityBaseNeTV implements OnItemClickListener, OnKeyListener, OnClickListener
{
	//UI
	TextView statusTextView;
	TextView _warningTextView;
	AlertDialog _promptCheckNeTVDialog;
	ImageView _loadingIcon;
	ImageView _btnBack;
		
	//Flag
	int _retryCounter;
	boolean _sentHandshake;
	boolean _receivedHandshake;
	boolean _hasMoreHandshake;
	boolean _hotspotOn;
	boolean _factoryTestMode;
	
	//Multiple device
	Dialog _deviceListDialog;
	HashMap<String, Bundle> _deviceList;
	ArrayList<CustomListItem> deviceListForUI;
	
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
        setContentView(R.layout.activity_splash);
        
        _hotspotOn = false;
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
	    _warningTextView = (TextView)findViewById(R.id.textViewVersionWarning);
    	
    	boolean isRooted = UtilRootChecker.isRooted();
    	statusTextView = (TextView)findViewById(R.id.textViewStatus);
    	statusTextView.setText(this.getString(isRooted ? R.string.rooted_true : R.string.rooted_false));
    	
    	_loadingIcon = (ImageView)findViewById(R.id.loading_icon);
    	//_loadingIcon.setAlpha(0);
    	
    	_btnBack = (ImageView)findViewById(R.id.btn_back);
    	_btnBack.setAlpha(0);
    	
    	reset();
    	
    	//Animate the chumby logo
    	AnimationSet set = new AnimationSet(true);
    	Animation animation = new TranslateAnimation(0, 0, 20, 0);
    	animation.setDuration(750);
    	animation.setInterpolator(new DecelerateInterpolator());
    	set.addAnimation(animation);
    	animation = new AlphaAnimation(0, 1);
    	animation.setDuration(500);
    	set.addAnimation(animation);
    	ImageView logo = (ImageView)findViewById(R.id.netv_logo);
        logo.startAnimation(set);

        //Fade in the version number
        animation = new AlphaAnimation(0, 1);
    	animation.setDuration(1000);
    	set.addAnimation(animation);
    	((TextView)findViewById(R.id.textViewVersion)).startAnimation(animation);
    	
    	//Spin the loading icon
    	animation = new RotateAnimation (0.0f, 359.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    	animation.setRepeatCount(Animation.INFINITE);
    	animation.setDuration(2000);
    	animation.setInterpolator(new Interpolator() { public float getInterpolation(float arg0) { return arg0; } });
    	_loadingIcon.startAnimation(animation);
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
		setPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, ActivitySplash.class.getName());
		
		super.onPause();
    }
    
    /**
	 * Reset flags & states
	 * 
	 * @category Initialization
	 */
    public void reset()
    {
    	_retryCounter = 0;
    	_sentHandshake = false;
    	_receivedHandshake = false;
    	_hasMoreHandshake = false;
    	_factoryTestMode = false;
    	
    	if (_deviceListDialog != null)
    		_deviceListDialog.dismiss();
    	_deviceListDialog = null;
    	
    	if (_promptCheckNeTVDialog != null)
    		_promptCheckNeTVDialog.dismiss();
    	_promptCheckNeTVDialog = null;
    	    	
    	if (_deviceList == null)
    		_deviceList = new HashMap<String, Bundle>();
    	_deviceList.clear();
    	
    	//Clear last IP Address
    	//setPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	//setPreferenceString(AppNeTV.PREF_CHUMBY_ACTIVATED, "false");

    	if (!_myApp.isWifiEnabled())
    	{
    		if (_hotspotOn)
    		{
    			//resumed from Preferences
    			initializeSequence();
    		}
    		else
    		{
    			_hotspotOn = _myApp.isWifiApEnabled();
    			if (_hotspotOn)			promptHotspot();
    			else					promptEnableWifi();
    		}
    	}
    	else
    	{
    		_hotspotOn = false;
			initializeSequence();
			
			fadeLoadingIcon(true);
			
	    	//refresh AP list
			_myApp.startScan();
    	}
    	    	
		Log.i(TAG, this.getLocalClassName() + ": hotspot mode = " + _hotspotOn);
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
	
	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.btn_refresh)
			reset();
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
		
		//Assume device is activated (so that it doesn't show ActivityAccount)
		setPreferenceString(AppNeTV.PREF_CHUMBY_ACTIVATED, "true");
    	setPreferenceString(AppNeTV.PREF_CHUMBY_USERNAME, "chumby");
    	setPreferenceString(AppNeTV.PREF_CHUMBY_PASSWORD, "123456");
    	setPreferenceString(AppNeTV.PREF_CHUMBY_DEVICE_NAME, "NeTV Demo");
	}
		    
    /**
     * Popup a modal dialog box to ask if user is using mobile hotspot
     * 
     * @category UI Utility
     */
    private void promptHotspot()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.prompt_hotspot)
		       .setCancelable(false)
		       .setPositiveButton(this.getString(R.string.button_yes), new DialogInterface.OnClickListener()
		       {
		           public void onClick(DialogInterface dialog, int id)
		           {
		           	   _hotspotOn = true;
		           	   initializeSequence();
		           }
		       })
		       .setNegativeButton(this.getString(R.string.button_no_and_exit), new DialogInterface.OnClickListener()
		       {
		           public void onClick(DialogInterface dialog, int id)
		           {
		        	   System.exit(0);
		           }
		       });
		
		AlertDialog alert = builder.create();
		alert.show();
	}
    
	/**
     * Popup a modal dialog box to ask user to turn on WiFi 
     * 
     * @category UI Utility
     */
    private void promptEnableWifi()
	{
    	final String waiting_for_wifi_msg = this.getString(R.string.waiting_for_wifi);
    	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.confirm_enable_wifi)
		       .setCancelable(false)
		       .setPositiveButton(this.getString(R.string.button_continue), new DialogInterface.OnClickListener()
		       {
		           public void onClick(DialogInterface dialog, int id)
		           {
		        	   dialog.cancel();
		        	   _myApp.setWifiEnabled(true);
		        	   _sentHandshake = false;
		           	   _receivedHandshake = false;
		           	   _hotspotOn = false;
		           	   		           	   
		           	   //Wait for WiFi to stablize and continue
			           if (SUPER_VERBOSE)
			    			statusTextView.setText(waiting_for_wifi_msg);

			           _handler.postDelayed(initializeSequenceRunnable, 7000);
		        	   _myApp.startScan();
		           }
		       })
		       .setNegativeButton(this.getString(R.string.button_no_and_exit), new DialogInterface.OnClickListener()
		       {
		           public void onClick(DialogInterface dialog, int id)
		           {
		        	   System.exit(0);
		           }
		       });
		
		AlertDialog alert = builder.create();
		alert.show();
	}
    
    /**
     * Popup a modal dialog box to instruct user to check for NeTV power connection
     * 
     * @category UI Utility
     */
    private void promptCheckNeTV()
	{
		//Show a more helpful message if phone is in the wrong network    	
    	String message = this.getString(R.string.check_netv_power_manual_ip);
    	String lastSSID = getPreferenceString(AppNeTV.PREF_WIFI_SSID, "");
    	if (lastSSID.length() > 0 && _myApp.getConnectedNetworkSSID() != null && !_myApp.getConnectedNetworkSSID().equals(lastSSID))
    	{
    		message = this.getString(R.string.check_netv_power_and_network);
    		message = message.replaceAll("xxxxxxxxxx", lastSSID);
    		message = message.replaceAll("hhhhhhhhhh", _myApp.getConnectedNetworkSSID());
    	}
    	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message)
		       .setCancelable(true)
		       .setPositiveButton(this.getString(R.string.button_preferences), new DialogInterface.OnClickListener()
		       {
		           public void onClick(DialogInterface dialog, int id)
		           {
		           	   gotoNextActivity(NeTVWidgetPrefs.class);
		           }
		       })
		       .setNegativeButton(this.getString(R.string.button_retry), new DialogInterface.OnClickListener()
		       {
		           public void onClick(DialogInterface dialog, int id)
		           {
		        	   reset();
		           }
		       });
		
		_promptCheckNeTVDialog = builder.create();
		_promptCheckNeTVDialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;
		_promptCheckNeTVDialog.show();
	}
    
    /**
     * Fade in/out the spining loading icon
     * 
     * @category UI Utility
     */
    private void fadeLoadingIcon(boolean isIn)
    {
    	if (isIn)		_loadingIcon.setVisibility(View.VISIBLE);
    	else			_loadingIcon.setVisibility(View.INVISIBLE);
    	/*
    	Animation animation;
    	if (isIn)	animation = new AlphaAnimation(0, 1);
    	else		animation = new AlphaAnimation(1, 0);
		animation.setDuration(500);
		_loadingIcon.startAnimation(animation);
		*/
    }
    
    /**
     * Popup a modal dialog box to ask user to select one NeTV to control 
     * 
     * @category UI Utility
     */
    private void showDeviceListDialog()
    {
    	fadeLoadingIcon(false);
    	
    	//Data list for the dialog
    	if (deviceListForUI != null)
    		deviceListForUI.clear();
    	deviceListForUI = null;
    	deviceListForUI = new ArrayList<CustomListItem>();
    	
    	//The dialog view
    	if (_deviceListDialog != null)
    		_deviceListDialog.dismiss();
     	_deviceListDialog = null;
    	CustomDialog.Builder customBuilder = new
		CustomDialog.Builder(ActivitySplash.this);

    	//Construct data list for the dialog
    	if (_myApp.isNeTVFactoryInRange())
    	{
    		if (SUPER_VERBOSE)
    			statusTextView.setText(this.getString(R.string.factory_test_mode));
    		
    		List<ScanResult> scanresults = _myApp.getScanResults();
    		if (scanresults == null)
    			return;
    		
    		for (ScanResult result : scanresults)
    	    	if (result.SSID != null && result.SSID.contains(this.getString(R.string.wifi_netv_ssid)) )
    	    		if (result.SSID.contains(":") && result.SSID.split(":").length == 3)
    	    		{
    	    			CustomListItem listItem = new CustomListItem();
    	    			listItem.setTitle(result.SSID);
    	    			listItem.setDescription(result.BSSID);
    	    			listItem.setTag(result.SSID);
    	    			deviceListForUI.add(listItem);
    	    			
    	    			_factoryTestMode = true;
    	    		}
    		customBuilder.setTitle(this.getString(R.string.factory_test_mode));
    		customBuilder.setMessage(this.getString(R.string.select_a_device_test));
    		customBuilder.setOnClickListener(this);
    	}
    	else
    	{
	    	for (String ipaddress: _deviceList.keySet())
	    	{
				Bundle bundle = _deviceList.get(ipaddress);
				
				CustomListItem listItem = new CustomListItem();
				listItem.setTitle(bundle.getString(AppNeTV.PREF_CHUMBY_DEVICE_NAME));
				listItem.setDescription(ipaddress);
				listItem.setTag(ipaddress);
				deviceListForUI.add(listItem);
	    	}
			customBuilder.setTitle(this.getString(R.string.select_a_device));
			customBuilder.setMessage(this.getString(R.string.multiple_device_found));
			customBuilder.setOnClickListener(this);
    	}

    	//Construct the dialog view
		customBuilder.setListViewData(deviceListForUI, this);
		_deviceListDialog = customBuilder.create();
		_deviceListDialog.setOnKeyListener(this);
		_deviceListDialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;
		_deviceListDialog.show();
    }
    
   
    /**
     * Popup a modal dialog box to ask user to select one NeTV to control 
     * 
     * @category UI Utility
     */
    private void onDeviceSelected(int itemIndex)
    {
    	CustomListItem selectedUIItem = deviceListForUI.get(itemIndex);
    	if (selectedUIItem == null)
    		return;
    	String tag = selectedUIItem.getTag();
    	if (tag == null || tag.length() < 1)
    		return;
    	
    	//Factory test mode
    	if (_factoryTestMode)
    	{
    		//This will allow using NeTV in AP mode later on
			setPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, AppNeTV.DEFAULT_IP_ADDRESS);
			setPreferenceString(AppNeTV.PREF_WIFI_FACTORY_SSID, tag);
			
			gotoNextActivity(ActivityFactoryTest1.class);
			overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
			return;
    	}
    	
		//Selected an network-unconfigured device
		String unconfiguredAddress = this.getString(R.string.select_to_configure);
		if (unconfiguredAddress.equals(tag))
		{
			//This will allow using NeTV in AP mode later on
			setPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, AppNeTV.DEFAULT_IP_ADDRESS);
			
			gotoNextActivity(ActivityWifiList.class);
			overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
			return;
		}
		
		//Transfer device parameters (IP address, GUID, username, etc.)
		//into Android's preference memory for whatever purpose later
		saveDeviceParameters(tag);
		
		//Selected an unactivated (but network-configured) device
		//This flag select whether we want to allow demo/testing with unactivated device
		if (!ALLOW_UNACTIVATED_REMOTE && !tag.equals(AppNeTV.DEFAULT_IP_ADDRESS))
		{
    		Bundle deviceParams = _deviceList.get(tag);
    		if (deviceParams.containsKey(AppNeTV.PREF_CHUMBY_DEVICE_NAME))
    		{
    			String unactivatedName = this.getString(R.string.chumby_device_name_unactivated);
    			if (unactivatedName.equals(deviceParams.get(AppNeTV.PREF_CHUMBY_DEVICE_NAME).toString()))
    			{
    				gotoNextActivity(ActivityAccount.class);
        			overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
        			return;
    			}
    		}
		}
					
		//Normally show the remote control
		gotoNextActivity(ActivityRemoteMain.class);
		overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
    }
		
    
    // UI Events
	//----------------------------------------------------------------------------
    
	/**
	 * Triggered when a list item is pressed
	 * 
	 * @category UI Events
	 */
	public void onItemClick(AdapterView<?> adapter, View v, int position, long id)
	{
		onDeviceSelected(position);
		if (_deviceListDialog != null)
			_deviceListDialog.dismiss();
		_deviceListDialog = null;
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
			if (_deviceList.containsKey(addressString))
				return;
			
			//Compare version
			String minAndroid = (String) parameters.get(MessageReceiver.MESSAGE_KEY_MIN_ANDROID);
			if (minAndroid != null && minAndroid.length() > 3)
			{
				if (_myApp.requiresAndroidUpdate(minAndroid))
				{
					Log.e(TAG, this.getLocalClassName() + ": need update to minimum version " + minAndroid);
					_warningTextView.setText(this.getString(R.string.version_warning));
					_warningTextView.setVisibility(View.VISIBLE);
				}
				else
				{
					Log.d(TAG, this.getLocalClassName() + ": minAndroid " + minAndroid + " from " + addressString);
				}	
			}
							
			//Add it to device list
			_deviceList.put(addressString, parameters);
		
			if (SUPER_VERBOSE)
				statusTextView.setText("" + _deviceList.size() + " device(s) found");
			
			//Print out parameters for debug
			/*
			for (String key: parameters.keySet())
				if (!key.equals(MessageReceiver.COMMAND_MESSAGE))
					Log.d(TAG, key + " = " + parameters.get(key));
			*/

			//Get activation info of this GUID
			if (parameters.containsKey(AppNeTV.PREF_CHUMBY_GUID))
				getDeviceActivationData( parameters, parameters.get(AppNeTV.PREF_CHUMBY_GUID).toString() );
			
			_receivedHandshake = true;
			_hasMoreHandshake = true;
			return;
		}
		
		Log.d(TAG, "Received command message: " + commandName);
	}
	
	// Application Logic
    //------------------------------------------------------------------
    
	/**
     * @category Application Logic
     */
	void getDeviceActivationData(Bundle parameters, String guid)
	{
		HashMap<String,String> dataHashMap = _myApp.getChumbyGUIDProfile(guid);
		if (dataHashMap.size() < 1)
		{
			parameters.putString(AppNeTV.PREF_CHUMBY_ACTIVATED, "false");
			parameters.putString(AppNeTV.PREF_CHUMBY_DEVICE_NAME, this.getString(R.string.chumby_device_name_unactivated));
		}
		else
		{
			parameters.putString(AppNeTV.PREF_CHUMBY_ACTIVATED, "true");
			for (String key: dataHashMap.keySet())
				parameters.putString(key, dataHashMap.get(key).toString());
		}
	}
	
	/**
     * @category Application Logic
     */
	void saveDeviceParameters(String ipaddress)
	{
		setPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, ipaddress);
		
		Bundle bundle = _deviceList.get(ipaddress);
		if (bundle == null)
			return;
			
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_DEVICE_NAME))
			setPreferenceString(AppNeTV.PREF_CHUMBY_DEVICE_NAME, bundle.get(AppNeTV.PREF_CHUMBY_DEVICE_NAME).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_ACTIVATED))
			setPreferenceString(AppNeTV.PREF_CHUMBY_ACTIVATED, bundle.get(AppNeTV.PREF_CHUMBY_ACTIVATED).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_GUID))
			setPreferenceString(AppNeTV.PREF_CHUMBY_GUID, bundle.get(AppNeTV.PREF_CHUMBY_GUID).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_USERNAME))
			setPreferenceString(AppNeTV.PREF_CHUMBY_USERNAME, bundle.get(AppNeTV.PREF_CHUMBY_USERNAME).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_FLASH_PLUGIN))
			setPreferenceString(AppNeTV.PREF_CHUMBY_FLASH_PLUGIN, bundle.get(AppNeTV.PREF_CHUMBY_FLASH_PLUGIN).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_HW_VERSION))
			setPreferenceString(AppNeTV.PREF_CHUMBY_HW_VERSION, bundle.get(AppNeTV.PREF_CHUMBY_HW_VERSION).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_FW_VERSION))
			setPreferenceString(AppNeTV.PREF_CHUMBY_FW_VERSION, bundle.get(AppNeTV.PREF_CHUMBY_FW_VERSION).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_DCID_CAMP))
			setPreferenceString(AppNeTV.PREF_CHUMBY_DCID_CAMP, bundle.get(AppNeTV.PREF_CHUMBY_DCID_CAMP).toString());
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_DCID_PART))
			setPreferenceString(AppNeTV.PREF_CHUMBY_DCID_PART, bundle.get(AppNeTV.PREF_CHUMBY_DCID_PART).toString());
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_DCID_RGIN))
			setPreferenceString(AppNeTV.PREF_CHUMBY_DCID_RGIN, bundle.get(AppNeTV.PREF_CHUMBY_DCID_RGIN).toString());
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_DCID_SKIN))
			setPreferenceString(AppNeTV.PREF_CHUMBY_DCID_SKIN, bundle.get(AppNeTV.PREF_CHUMBY_DCID_SKIN).toString());
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_DCID_VERS))
			setPreferenceString(AppNeTV.PREF_CHUMBY_DCID_VERS, bundle.get(AppNeTV.PREF_CHUMBY_DCID_VERS).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_MIN_ANDROID))
			setPreferenceString(AppNeTV.PREF_CHUMBY_MIN_ANDROID, bundle.get(AppNeTV.PREF_CHUMBY_MIN_ANDROID).toString());
		
		if (bundle.containsKey(AppNeTV.PREF_CHUMBY_MAC)) 
		{
			String mac = bundle.get(AppNeTV.PREF_CHUMBY_MAC).toString();
			if (mac != null && mac.length() > 0) {
				mac = mac.substring(0, Math.min(mac.length(), 23));
				if (bundle.containsKey(AppNeTV.PREF_CHUMBY_MAC))
					setPreferenceString(AppNeTV.PREF_CHUMBY_MAC, mac);
			}
		}
	}
	
	/**
	 * @category Application Logic
	 */
	private void initializeSequence()
	{
		//Stage 0
		//Enable WiFi if it is disabled
		if (!_myApp.isWifiEnabled())
		{
			if (SUPER_VERBOSE)
				statusTextView.setText("Turning on WiFi...");
			_myApp.setWifiEnabled(true);
			Log.d(TAG, this.getLocalClassName() + ": enabling WiFi...");
			_handler.postDelayed(initializeSequenceRunnable, 5000);
		}
		
		//Stage 1
		//Wait until the communication service is up
		if (!_myApp.isCommServiceRunning())
		{
			_retryCounter = 0;
			_sentHandshake = false;
			_receivedHandshake = false;
			if (SUPER_VERBOSE)
				statusTextView.setText("Waiting for CommService to bind...");
			_handler.postDelayed(initializeSequenceRunnable, 400);
			Log.d(TAG, this.getLocalClassName() + ": waiting for CommService to bind...");
			return;
		}
		
		//Stage 2
		//Send handshake and wait a bit longer, to receive all handshakes
		if (!_sentHandshake)
		{
			if (SUPER_VERBOSE)
				statusTextView.setText("Searching for NeTV...");

			//blasting handshake messages
			_myApp.sendHandshake();
			_myApp.sendHandshake();
			_myApp.sendHandshake();
			Log.d(TAG, this.getLocalClassName() + ": broadcasting handshake message");
		
			//Send to a particular address
			String _manual_ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_MANUAL, "");
			if (_manual_ipaddress.length() >= 7)
				_myApp.sendHandshake(_manual_ipaddress);
			
			_sentHandshake = true;
			_retryCounter = 0;
			_handler.postDelayed(initializeSequenceRunnable, 1500);
			return;
		}
				
		//Stage 3
		//Wait for more handshake messages to arrive
		if (_hasMoreHandshake)
		{
			_hasMoreHandshake = false;
			_handler.postDelayed(initializeSequenceRunnable, 300);
			return;
		}
		
		//Stage 4.0
		//This happens mostly in the factory when phone's WiFi is not connected to anywhere 
		if (_myApp.isNeTVFactoryInRange())
		{
			showDeviceListDialog();
			return;
		}
		
		//Stage 4.1
		//Check NeTV network
		if (_myApp.isNeTVinRange())
		{
			//Hide the current prompt (if any)
			if (_promptCheckNeTVDialog != null)
			{
				_promptCheckNeTVDialog.dismiss();
				_promptCheckNeTVDialog = null;
			}
			
			//Automatically go to network configuration
			if (ENABLE_AUTO_SELECT_AP)
			{
				gotoNextActivity(ActivityWifiList.class);
    			overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
    			return;
			}
			
			//Add it to device list
			String unconfiguredAddress = this.getString(R.string.select_to_configure);
			if (!_deviceList.containsKey(unconfiguredAddress))
			{
				Bundle parameters = new Bundle();
				parameters.putString(AppNeTV.PREF_CHUMBY_ACTIVATED, "false");
				parameters.putString(AppNeTV.PREF_CHUMBY_DEVICE_NAME, this.getString(R.string.chumby_device_name_unconfigured));
				_deviceList.put(unconfiguredAddress, parameters);
				
				if (SUPER_VERBOSE)
					statusTextView.setText("Unconfigured NeTV found");
				Log.d(TAG, "Unconfigured NeTV found");
				
				//Similar behavior as Stage 3
				_hasMoreHandshake = true;
				_handler.postDelayed(initializeSequenceRunnable, 300);
				return;
			}
		}
		
		//Stage 4.2
		//Received some handshake messages
		if (_receivedHandshake)
		{
			//Hide the current prompt (if any)
			if (_promptCheckNeTVDialog != null)
			{
				_promptCheckNeTVDialog.dismiss();
				_promptCheckNeTVDialog = null;
			}
			
			//Only 1 device is found
			if (_deviceList.size() <= 1)
			{
				//Save the IP address, GUID, username, etc for whatever purpose later
				String ipaddress = _deviceList.entrySet().iterator().next().getKey();
	    		saveDeviceParameters(ipaddress);
	    		
	    		Log.i(TAG, "Only 1 device found: " + ipaddress);
				Log.i(TAG, "Going to RemoteControl activity...");
				gotoNextActivity(ActivityRemoteMain.class);
				overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
				return;
			}
			
			//Multiple devices found
			showDeviceListDialog();
			return;
		}
		
		//Stage 4.3
		//No handshake, but has NeTV AccessPoint
		else
		{
			if (_myApp.isNeTVinRange())
			{
				//This will allow using NeTV in AP mode later on
    			setPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, AppNeTV.DEFAULT_IP_ADDRESS);
    			
				//Automatically go to network configuration
				gotoNextActivity(ActivityWifiList.class);
    			overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
    			return;
			}
		}
		

		//Stage 4.3
		//No handshake, no NeTV Access Point, could be NeTV has not started up yet
		//Keep retrying in the background
		_retryCounter++;
		_myApp.sendHandshake();
		_handler.postDelayed(initializeSequenceRunnable, 800);
		
		if (SUPER_VERBOSE)
		{
			String animatedText = "Searching for NeTV";
			for (int i=0; i<(_retryCounter%4); i++)			animatedText += ".";
			for (int i=0; i<3-(_retryCounter%4); i++)		animatedText += " ";
			statusTextView.setText(animatedText);
		}
			
		//Already retry for too long
		if (_retryCounter == 20) {
			Log.e(TAG, this.getLocalClassName() + ": " + this.getString(R.string.check_netv_power));
			promptCheckNeTV();
		}
		
		if (_retryCounter > 65534)
			_retryCounter = 65535;
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
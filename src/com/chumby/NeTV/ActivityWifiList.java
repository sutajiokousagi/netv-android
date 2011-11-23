package com.chumby.NeTV;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

public class ActivityWifiList extends ActivityBaseNeTV implements
		OnClickListener, OnItemClickListener, OnScrollListener {
	// Wifi
	WifiInfo myWifiInfo;
	ScanResult wifiNeTV;
	List<ScanResult> wifiList;
	HashMap<String, ScanResult> wifiHashMap;

	// Flags & data
	int _retryCounter;
	boolean _myWifiScanReceiver_registered;
	String _selectedSSID;
	String _wifi_capabilities;
	boolean _triedConnectNeTV;
	boolean _receiveWifiResult;
	boolean _sentHandshake;
	boolean _receivedHandshake;
	boolean _sentSetUrl;
	boolean _receivedSetUrl;
	boolean _sentWifiScan;

	// UI
	ListView listViewWifi;
	CustomListAdapter listViewAdapter;
	ArrayList<CustomListItem> wifiListItems;
	AlertDialog alertDialog;

	// Private helper classes (this is awesome)
	// ----------------------------------------------------------------------------

	// Triggered when new WiFi scan result is available
	private BroadcastReceiver myWifiScanReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// Some devices raise this Intent almost immediately after a
			// startScan without any scan result
			// Eg. Galaxy Tab
			List<ScanResult> results = _myApp.getScanResults();
			if (results.size() > 0)
				_receiveWifiResult = true;
			else
				_myApp.startScan();
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
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, this.getLocalClassName() + " onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi_list);

		// Setup UI
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(this.getString(R.string.error_no_netv_device))
				.setCancelable(false)
				.setPositiveButton(this.getString(R.string.button_retry),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								reset();
								startWifiScan();
							}
						});
		alertDialog = builder.create();

		// Setup ListView UI
		wifiListItems = new ArrayList<CustomListItem>();
		listViewWifi = ((ListView) findViewById(R.id.list_wifi));
		listViewWifi.setOnItemClickListener(this);
		listViewAdapter = new CustomListAdapter(this, wifiListItems);
		listViewWifi.setAdapter(listViewAdapter);
		listViewWifi.setOnScrollListener(this);

		// Setup UI
		((Button) findViewById(R.id.button_refresh_wifi)).setOnClickListener(this);
		((Button) findViewById(R.id.button_next_wifi)).setOnClickListener(this);
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

		updateNextStepUI();
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
		setPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, ActivityWifiList.class.getName());

		// De-register broadcast receivers
		if (_myWifiScanReceiver_registered)
			unregisterReceiver(myWifiScanReceiver);
		_myWifiScanReceiver_registered = false;

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

		// Hide keyboard
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(listViewWifi.getWindowToken(), 0);

		// Initialize data & flags
		wifiNeTV = null;
		wifiList = null;

		_retryCounter = 0;
		_selectedSSID = "";
		_wifi_capabilities = "";
		_triedConnectNeTV = false;
		_receiveWifiResult = false;
		_sentHandshake = false;
		_receivedHandshake = false;
		_sentSetUrl = false;
		_receivedSetUrl = false;
		_sentWifiScan = false;

		initializeSequence();

		// refresh AP list
		startWifiScan();
	}

	/**
	 * Register broadcast receiver and start scanning AP
	 * 
	 * @category Initialization
	 */
	private void startWifiScan()
	{
		// Setup broadcast receivers
		if (!_myWifiScanReceiver_registered)
			this.registerReceiver(myWifiScanReceiver, new IntentFilter(
					WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		_myWifiScanReceiver_registered = true;

		// Start scanning for WiFi networks
		showBusyDialog(this.getString(R.string.connecting_netv_device));
		_myApp.startScan();
	}

	// UI Events
	// ----------------------------------------------------------------------------

	/**
	 * Triggered when a list item is pressed
	 * 
	 * @category UI Events
	 */
	public void onItemClick(AdapterView<?> adapter, View v, int position, long id)
	{
		// Data
		CustomListItem item = (CustomListItem) adapter.getItemAtPosition(position);
		Log.d(TAG, "Selecting Wifi network: " + item.getTitle());

		// UI
		clearHighlightNetwork();
		v.setBackgroundColor(Color.rgb(0, 162, 232));

		_selectedSSID = item.getTitle();
		_myApp.sendAndroidJSWifiSelect(_selectedSSID);
		updateNextStepUI();
	}

	/**
	 * Triggered when a view/control/button is pressed
	 * 
	 * @category UI Events
	 */
	public void onClick(View v)
	{
		if (v.getId() == R.id.button_refresh_wifi)
		{
			onRefreshButtonClick(v);
		}
		else if (v.getId() == R.id.button_next_wifi)
		{
			_handler.removeCallbacks(initializeSequenceRunnable);
			// Start new Activity to ask for network password phrase or Chumby
			// account if not encrypted
			startNextActivity();
		}
	}

	/**
	 * Triggered when 'Refresh' button is pressed
	 * 
	 * @category UI Events
	 */
	private void onRefreshButtonClick(View v)
	{
		showBusyDialog(this.getString(R.string.refreshing_wifi_list));

		// Register broadcast receiver if it is not registered
		if (!_myWifiScanReceiver_registered)
			registerReceiver(myWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		_myWifiScanReceiver_registered = true;

		_myApp.startScan();
		
		_receiveWifiResult = false;
		_sentWifiScan = false;
		_handler.postDelayed(initializeSequenceRunnable, 1000);
	}

	/**
	 * @category UI Events
	 */
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (wifiHashMap == null)
			return;

		// Clear highlight
		for (int i = 0; i < listViewWifi.getChildCount(); i++)
			if (listViewWifi.getChildAt(i) != null)
				listViewWifi.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);

		int idx = -1;
		if (_selectedSSID.equals(this.getString(R.string.wifi_network_other)))
		{
			idx = listViewWifi.getChildCount() - 1;
		}
		else
		{
			Iterator<String> itr = wifiHashMap.keySet().iterator();
			int tempIndex = 0;
			while (itr.hasNext())
			{
				ScanResult wifi = wifiHashMap.get(itr.next());
				if ( !_selectedSSID.contains(wifi.SSID) ) {
					tempIndex++;
					continue;
				}
				idx = tempIndex;
				break;
			}
		}

		// Not found in list
		if (idx == -1)
			return;
		if (idx < firstVisibleItem || idx > firstVisibleItem + visibleItemCount)
			return;

		// Highlight
		if (listViewWifi.getChildAt(idx - firstVisibleItem) != null)
			listViewWifi.getChildAt(idx - firstVisibleItem).setBackgroundColor(Color.rgb(0, 162, 232));
	}

	/**
	 * @category UI Events
	 */
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
			// highlightNetwork(_selectedSSID);
			// Log.i("a", "scrolling stopped...");
		} else if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
			// highlightNetwork(_selectedSSID);
		}
	}

	// UI Utility
	// ----------------------------------------------------------------------------

	/**
	 * @category UI Utility
	 */
	public void clearHighlightNetwork()
	{
		highlightNetwork(-1);
	}

	/**
	 * @category UI Utility
	 */
	public boolean highlightNetwork(String SSID)
	{
		if (SSID == null || SSID.length() < 1)
			return false;
		int idx = -1;

		if (SSID.equals(this.getString(R.string.wifi_network_other)))
		{
			idx = listViewWifi.getChildCount() - 1;
		}
		else
		{
			Iterator<String> itr = wifiHashMap.keySet().iterator();
			int tempIndex = 0;
			while (itr.hasNext())
			{
				ScanResult wifi = wifiHashMap.get(itr.next());
				if ( !SSID.contains(wifi.SSID) ) {
					tempIndex++;
					continue;
				}
				idx = tempIndex;
				break;
			}
		}

		// Not found in list
		if (idx == -1) {
			highlightNetwork(-1);
			return false;
		}

		// Found in list
		_selectedSSID = SSID;
		listViewWifi.smoothScrollToPosition(idx);
		_myApp.sendAndroidJSWifiSelect(_selectedSSID);

		// Highlight
		for (int i = 0; i < listViewWifi.getChildCount(); i++)
			listViewWifi.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
		if (listViewWifi.getChildAt(idx) != null)
			listViewWifi.getChildAt(idx).setBackgroundColor(Color.rgb(0, 162, 232));

		updateNextStepUI();
		return true;
	}

	/**
	 * @category UI Utility
	 */
	public void highlightNetwork(int position) {
		// Not in list
		if (position < 0) {
			_selectedSSID = "";
			for (int i = 0; i < listViewWifi.getChildCount(); i++)
				listViewWifi.getChildAt(i)
						.setBackgroundColor(Color.TRANSPARENT);
			updateNextStepUI();
			return;
		}

		// Highlight
		for (int i = 0; i < listViewWifi.getChildCount(); i++)
			listViewWifi.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
		if (listViewWifi.getChildAt(position) != null)
			listViewWifi.getChildAt(position).setBackgroundColor(
					Color.rgb(0, 162, 232));

		// Selecting 'Other...' item
		if (position == listViewWifi.getChildCount() - 1) {
			_selectedSSID = this.getString(R.string.wifi_network_other);
			updateNextStepUI();
			return;
		}

		// Update _selectedSSID flag
		int idx = 0;
		Iterator<String> itr = wifiHashMap.keySet().iterator();
		while (itr.hasNext()) {
			ScanResult wifi = wifiHashMap.get(itr.next());

			if (idx != position) {
				idx++;
				continue;
			}

			_selectedSSID = wifi.SSID;
			break;
		}
		updateNextStepUI();
	}

	/**
	 * @category UI Utility
	 */
	public void showWifiRetryDialog() {
		if (alertDialog != null && !alertDialog.isShowing())
			alertDialog.show();
	}

	/**
	 * @category UI Utility
	 */
	public void closeWifiRetryDialog() {
		alertDialog.dismiss();
	}

	/**
	 * @category UI Utility
	 */
	public void showWifiErrorDialog(String extraErrorString) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		String message = this.getString(R.string.error_comm_netv_device);
		if (extraErrorString.equals(""))
			message = message.concat("\n")
					.concat(this.getString(R.string.wifi_extra_error))
					.concat(" ").concat(extraErrorString);

		builder.setMessage(message)
				.setCancelable(false)
				.setPositiveButton(this.getString(R.string.button_retry),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								reset();
								startWifiScan();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * @category UI Utility
	 */
	private void startNextActivity()
	{
		// Selecting 'Other' network, configure WiFi manually
		if (_selectedSSID.equals(this.getString(R.string.wifi_network_other)))
		{
			// Other...
			_selectedSSID = "";
			_wifi_capabilities = "";
			
			gotoNextActivity(ActivityWifiDetails.class);
			return;
		}

		// Picked one WiFi network from the list
		// If no encryption, go to account/activation page directly
		ScanResult selectedWifi = wifiHashMap.get(_selectedSSID);
		if (selectedWifi.capabilities.length() < 2)
		{
			_selectedSSID = selectedWifi.SSID;
			_wifi_capabilities = "";

			setPreferenceString(AppNeTV.PREF_WIFI_ENCODING, AppNeTV.ENCODINGS[0]);
			setPreferenceString(AppNeTV.PREF_WIFI_AUTHENTICATION, AppNeTV.AUTHENTICATIONS[0]);
			setPreferenceString(AppNeTV.PREF_WIFI_ENCRYPTION, AppNeTV.ENCRYPTIONS[0]);

			String activated = getPreferenceString(AppNeTV.PREF_CHUMBY_ACTIVATED, "false");
			if (activated.equals("false"))	gotoNextActivity(ActivityAccount.class);
			else							gotoNextActivity(ActivityConfiguring.class);
			return;
		}

		// If encrypted, go to wifi details activity
		_selectedSSID = selectedWifi.SSID;
		_wifi_capabilities = selectedWifi.capabilities;
		gotoNextActivity(ActivityWifiDetails.class);
	}

	/**
	 * @category UI Utility
	 */
	public void autoHighlightNetwork()
	{
		List<WifiConfiguration> configuredWifis = _myApp.getConfiguredNetworks();
		for (WifiConfiguration configuredWifi : configuredWifis)
		{
			// Previously selected network goes out of range
			if (wifiHashMap.get(_selectedSSID) == null)
				highlightNetwork(-1);

			//Remove prefix & suffix quotes (")
			String configured_ssid = configuredWifi.SSID;
			configured_ssid = configured_ssid.substring(1, configured_ssid.length()-2);
			
			// Don't auto-select weaker networks
			if (!_selectedSSID.equals("")) {
				ScanResult olderWifi = wifiHashMap.get(_selectedSSID);
				ScanResult newerWifi = wifiHashMap.get(configured_ssid);
				if (olderWifi.level > newerWifi.level)
					continue;
			}
			
			if (highlightNetwork(configured_ssid))
			{
				updateHashVariables();
				break;
			}
		}
	}

	/**
	 * @category UI Utility
	 */
	protected boolean updateNextStepUI()
	{
		boolean enableNext = !_selectedSSID.equals("") && _receivedHandshake;
		((Button) findViewById(R.id.button_next_wifi)).setEnabled(enableNext);
		return enableNext;
	}

	/**
	 * @category UI Utility
	 */
	protected void updateHashVariables()
	{
		// We have to save at least SSID here because if selected network is un-secured,
		// ActivityWifiDetails will never get a chance to save SSID
		setPreferenceString(AppNeTV.PREF_WIFI_SSID, _selectedSSID);
		setPreferenceString(AppNeTV.PREF_WIFI_CAPABILITIES, _wifi_capabilities);
		
		//Log.d(TAG, this.getLocalClassName() + ": _selectedSSID " + getPreferenceString(AppNeTV.PREF_WIFI_SSID, ""));
		//Log.d(TAG, this.getLocalClassName() + ": _wifi_capabilities " + getPreferenceString(AppNeTV.PREF_WIFI_CAPABILITIES, ""));
	}

	// Application Logic
	// ----------------------------------------------------------------------------

	/**
	 * To be called when a new WiFi scan result (ap_scan) is available
	 * 
	 * @category Application Logic
	 */
	public void updateWifiList()
	{
		Log.d(TAG, "Updating wifi list");
		wifiList = _myApp.getScanResults();

		// Eliminates duplicates SSID by taking only strongest BSSID
		wifiHashMap = new HashMap<String, ScanResult>();
		for (ScanResult result : wifiList) {
			ScanResult tempWifi = wifiHashMap.get(result.SSID);			//no extra quotes
			if (tempWifi == null || tempWifi.level < result.level)
				wifiHashMap.put(result.SSID, result);
		}

		// Pick out 'NeTV' network
		wifiNeTV = wifiHashMap.get(this.getString(R.string.wifi_netv_ssid));
		wifiHashMap.remove(this.getString(R.string.wifi_netv_ssid));

		// Construct WiFi list for UI
		listViewWifi.clearChoices();
		wifiListItems.clear();

		// We have to keep the same order as current scan result
		List<ScanResult> wifiList = _myApp.getScanResults();
		for (ScanResult result : wifiList)
		{
			ScanResult wifi = wifiHashMap.get(result.SSID);
			if (wifi == null)
				continue;

			CustomListItem tempItem = new CustomListItem();
			tempItem.setTitle(wifi.SSID);
			tempItem.setDescription(wifi.capabilities);
			// tempItem.setLevel(wifi.level);
			// tempItem.setEncryption(wifi.capabilities);
			wifiListItems.add(tempItem);
		}

		// Lastly, add 'Other...' item
		CustomListItem otherWifi = new CustomListItem();
		otherWifi.setTitle(this.getString(R.string.wifi_network_other));
		wifiListItems.add(otherWifi);

		// Refresh the list UI
		listViewAdapter.notifyDataSetChanged();

		// Reselect previously selected item
		if (_selectedSSID != null && _selectedSSID.length() > 0) {
			highlightNetwork(_selectedSSID);
			_myApp.sendAndroidJSWifiSelect(_selectedSSID);
		}

		// NeTV network is found
		// -----------------------

		// Send the list to NeTV's Android configuration page
		//_myApp.sendAndroidJSWifiScan();
		Log.d(TAG, wifiHashMap.size() + " networks found");

		// Stop listerning for wifi scanresult
		if (_myWifiScanReceiver_registered)
			unregisterReceiver(myWifiScanReceiver);
		_myWifiScanReceiver_registered = false;

		closeBusyDialog();

		// Try to cleverly guess which network user might be selecting
		// by looking at the configured networks stored his Android device
		_handler.post(autoHighlightRunnable);
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
		// Filter out loopbacks
		String addressString = (String) parameters.get(MessageReceiver.MESSAGE_KEY_ADDRESS);
		parameters.remove(MessageReceiver.MESSAGE_KEY_ADDRESS);
		if (addressString == null || addressString.length() < 5 || _myApp.isMyIP(addressString))
			return;
		if (!addressString.equals(AppNeTV.DEFAULT_IP_ADDRESS))
			return;

		// Not a valid message
		if (!parameters.containsKey(MessageReceiver.COMMAND_MESSAGE))
			return;
		String commandName = (String) parameters.get(MessageReceiver.COMMAND_MESSAGE);
		if (commandName == null || commandName.length() < 2)
			return;
		commandName = commandName.toUpperCase();
		parameters.remove(MessageReceiver.COMMAND_MESSAGE);

		if (commandName.equals(MessageReceiver.COMMAND_Handshake.toUpperCase()))
		{
			// Ignore message from another Android device (doesn't contain GUID and DCID)
			if (!parameters.containsKey(AppNeTV.PREF_CHUMBY_GUID) && !parameters.containsKey(AppNeTV.PREF_CHUMBY_DCID))
				return;

			String tmpGUID = null;
			if (parameters.containsKey(AppNeTV.PREF_CHUMBY_GUID))
				tmpGUID = parameters.get(AppNeTV.PREF_CHUMBY_GUID).toString();

			// Save the GUID of the device we are activating
			if (!_receivedHandshake && _myApp.isConnectedNeTV() && tmpGUID != null)
			{
				setPreferenceString(AppNeTV.PREF_CHUMBY_GUID, tmpGUID);
				_receivedHandshake = true;
				updateNextStepUI();
				Log.i(TAG, "Configuring GUID: " + tmpGUID);
			}
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_Android.toUpperCase()))
		{
			// Echo. Ignored
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_WifiScan.toUpperCase()))
		{
			// Echo. Ignored
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_SetUrl.toUpperCase()))
		{
			Log.i(TAG, "Received SetUrl acknowledgement from " + addressString);
			_receivedSetUrl = true;
			return;
		}

		Log.d(TAG, "Received command message: " + commandName);
	}

	// Runnable objects (for multi-threading implementation)
	// ----------------------------------------------------------------------------

	/**
	 * Runnable object to be executed in another thread. <br>
	 * Call autoHighlightNetwork in UI Utility category
	 * 
	 * @see {@link #autoHighlightNetwork()}
	 */
	private Runnable autoHighlightRunnable = new Runnable() {
		public void run() {
			if (_selectedSSID == null || _selectedSSID.length() <= 0)
				autoHighlightNetwork();
			else
				highlightNetwork(_selectedSSID);
		}
	};

	/**
	 * @category Application Logic
	 */
	private void initializeSequence()
	{
		// Stage 1
		// Start connecting to NeTV Access Point
		if (!_triedConnectNeTV && !_myApp.isConnectedNeTV())
		{
			_triedConnectNeTV = true;
			_retryCounter = 0;
			if (!_myApp.isConnectedNeTV()) {
				_handler.postDelayed(initializeSequenceRunnable, 2000);
				Log.d(TAG, this.getLocalClassName() + ": connecting to NeTV Access Point...");
				showBusyDialog(this.getString(R.string.connecting_netv_device));
				_myApp.connectNeTV();
				return;
			}
		}
		if (!_myApp.isConnectedNeTV())
		{
			_retryCounter++;
			if (_retryCounter <= 6) {
				_handler.postDelayed(initializeSequenceRunnable, 2000);
				Log.d(TAG, this.getLocalClassName() + ": waiting to be connected to NeTV Access Point...");
				return;
			}

			Log.e(TAG, this.getLocalClassName() + ": could not connect to NeTV Access Point!");
			showWifiRetryDialog();
			_retryCounter = 0;
			return;
		}

		// Stage 2
		// Send SetUrl to switch UI to Android configuration homepage
		if (!_sentSetUrl)
		{
			_sentSetUrl = true;
			_retryCounter = 0;
			_handler.postDelayed(initializeSequenceRunnable, 750);
			Log.d(TAG, this.getLocalClassName() + ": switching to Loading/WifiList UI");
			_myApp.sendNeTVBrowserSetUrl(AppNeTV.ANDROID_CONFIG_URL);
			/*
			_myApp.SetUrlHTTP(AppNeTV.ANDROID_CONFIG_URL, new AsyncHttpResponseHandler()
			{
			    @Override
			    public void onSuccess(String response)
			    {
			    	Log.d(TAG, "SetUrlHTTP: " + response);
			    	String status = response.split("</status>")[0].split("<status>")[1].trim();
			    	if (!status.equals("1"))   		Log.e(TAG, "SetUrlHTTP: something is not right");
			    	else				    		_receivedSetUrl = true;
			    }
			    
			    @Override
			    public void onFailure(Throwable error)
			    {
			    	Log.e(TAG, "SetUrlHTTP: failed");
					Log.e(TAG, "" + error);
					
					//Try UDP version next
			    }
			});
			*/
			return;
		}

		// Stage 3
		// Wait for SetUrl acknowledgement
		if (!_receivedSetUrl)
		{
			_retryCounter++;
			if (_retryCounter <= 3)
			{
				_handler.postDelayed(initializeSequenceRunnable, 800);
				_myApp.sendNeTVBrowserSetUrl(AppNeTV.ANDROID_CONFIG_URL);
				Log.d(TAG, this.getLocalClassName() + ": waiting for Loading/WifiList UI");
				return;
			}
			Log.e(TAG, this.getLocalClassName() + ": no SetUrl acknowledgement for too long!");
			showWifiRetryDialog();
			_retryCounter = 0;
			return;
		}

		// Stage 4
		// Send handshake and wait
		if (!_sentHandshake)
		{
			Log.d(TAG, this.getLocalClassName() + ": sent handshake message");
			if (_retryCounter < 1)
			{
				// blasting handshake messages
				_myApp.sendHandshake();
				_retryCounter++;
				_handler.postDelayed(initializeSequenceRunnable, 50);
			}
			else
			{
				_sentHandshake = true;
				_retryCounter = 0;
				_handler.postDelayed(initializeSequenceRunnable, _receivedHandshake ? 200 : 500);
			}
			return;
		}

		// Stage 5
		// Wait for handshake messages
		if (!_receivedHandshake)
		{
			_retryCounter++;
			if (_retryCounter <= 3)
			{
				_myApp.sendHandshake();
				_handler.postDelayed(initializeSequenceRunnable, 500);
				return;
			}
			Log.e(TAG, this.getLocalClassName() + ": no handshake response for too long!");
			showWifiRetryDialog();
			_retryCounter = 0;
			return;
		}

		// Stage 6
		// If this fails, there is not much we can do. So just wait.
		if (!_receiveWifiResult)
		{
			_handler.postDelayed(initializeSequenceRunnable, 300);
			_sentWifiScan = false;
			return;
		}
		
		// Stage 7
		// Send wifi list
		if (!_sentWifiScan)
		{
			_myApp.sendAndroidJSWifiScan();
			_myApp.sendAndroidJSWifiScan();
			_handler.postDelayed(initializeSequenceRunnable, 500);
			_sentWifiScan = true;
			updateWifiList();
			return;
		}
		
		//Keep alive command
		_myApp.sendAndroidJSWifiSelect(_selectedSSID);
		_handler.postDelayed(initializeSequenceRunnable, 2000);
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

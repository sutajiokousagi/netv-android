package com.bunniestudios.NeTV;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.chumby.util.ChumbyLog;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class AppNeTV extends Application implements MessageReceiverInterface
{
	private static final String TAG = "NeTV";
	
	//Ops Flags
	private static final boolean ENABLE_RECEIVE = true;
	
	//Custom event
	public static final String RECEIVE_SOCKET_MESSAGE_EVENT = "com.bunniestudios.NeTV.socket_event";
	
	//URLs
	public static final String NETV_HOME_URL = "http://localhost";
	public static final String FACTORY_TEST_URL = "http://localhost/tests/index.html";
	public static final String ANDROID_CONFIG_URL = "http://localhost/html_config/index_android.html";
	
	//NeTV Android UI state names
	public static final String NETV_UISTATE_WIFI_LOADING = "loading";
	public static final String NETV_UISTATE_WIFI_LIST = "wifilist";
	public static final String NETV_UISTATE_WIFI_DETAILS = "wifidetails";
	public static final String NETV_UISTATE_ACCOUNT = "accountdetails";
	public static final String NETV_UISTATE_ACTIVATION = "configuring";
	public static final String NETV_UISTATE_REMOTE = "remote";
	public static final String NETV_UISTATE_BROWSER = "browser";
	public static final String NETV_UISTATE_TEXTINPUT = "textinput";
	public static final String NETV_UISTATE_FACTORY_LOADING = "androidtest";
	public static final String NETV_UISTATE_FACTORY_IRREMOTE = "irremote";
	public static final String NETV_UISTATE_FACTORY_RESET_BTN = "resetbtn";
	public static final String NETV_UISTATE_FACTORY_DONE = "done";
	
	//SharedPreferences keys
	protected static final String PREF_FIRSTTIME = "virgin";
	protected static final String PREF_SMS_ENABLE = "widget_sms_enable";
	protected static final String PREF_SMS_CONTENT = "widget_sms_content";
	protected static final String PREF_SMS_FILTER = "widget_sms_filter";
	protected static final String PREF_SMS_FILTER_DEFAULT = "bank account balance";
	
	protected static final String PREF_PREVIOUS_ACTIVITY = "previousActivity";
	protected static final String PREF_ACTIVATION_STATE = "activationState";
	protected static final String PREF_PREVIOUS_NETID = "previousNetworkID";
	protected static final String PREF_HOME_IP = "homeIPaddress";
	protected static final String PREF_TIME = "time";
	protected static final String PREF_TIMEZONE = "timezone";
	protected static final String PREF_WIFI_SSID = "wifi_ssid";
	protected static final String PREF_WIFI_FACTORY_SSID = "wifi_factory_ssid";
	protected static final String PREF_WIFI_ENCRYPTION = "wifi_encryption";
	protected static final String PREF_WIFI_AUTHENTICATION = "wifi_authentication";
	protected static final String PREF_WIFI_ENCODING = "wifi_encoding";
	protected static final String PREF_WIFI_PASSWORD = "wifi_password";
	protected static final String PREF_WIFI_CAPABILITIES = "wifi_capabilities";	

	protected static final String PREF_CHUMBY_ACTIVATED = "activated";
	protected static final String PREF_CHUMBY_IP_ADDRESS = "chumby_address";
	protected static final String PREF_CHUMBY_USERNAME = "chumby_username";
	protected static final String PREF_CHUMBY_PASSWORD = "chumby_password";
	protected static final String PREF_CHUMBY_DEVICE_NAME = "chumby_device_name";
	protected static final String PREF_CHUMBY_GUID = "guid";
	protected static final String PREF_CHUMBY_DCID = "dcid";
	protected static final String PREF_CHUMBY_FLASH_PLUGIN = "flashplugin";
	protected static final String PREF_CHUMBY_HW_VERSION = "hwver";
	protected static final String PREF_CHUMBY_FW_VERSION = "fwver";
	protected static final String PREF_CHUMBY_DCID_SKIN = "skin";
	protected static final String PREF_CHUMBY_DCID_CAMP = "camp";
	protected static final String PREF_CHUMBY_DCID_RGIN = "rgin";
	protected static final String PREF_CHUMBY_DCID_PART = "part";
	protected static final String PREF_CHUMBY_DCID_VERS = "vers";
	protected static final String PREF_CHUMBY_MIN_ANDROID = "minAndroid";
	protected static final String PREF_CHUMBY_MAC = "mac";
	protected static final String PREF_CHUMBY_IP_MANUAL = "manual_ip";
	
	//These are the parameters we are sending in each Activity
	protected static final String[] PREF_WIFI_LIST = { PREF_TIME, PREF_TIMEZONE };
	protected static final String[] PREF_WIFI_DETAILS = { PREF_WIFI_SSID, PREF_WIFI_PASSWORD, PREF_WIFI_ENCRYPTION, PREF_WIFI_AUTHENTICATION, PREF_WIFI_ENCODING, PREF_WIFI_CAPABILITIES };
	protected static final String[] PREF_ACCOUNT = { PREF_CHUMBY_USERNAME, PREF_CHUMBY_PASSWORD, PREF_CHUMBY_DEVICE_NAME };
	
	//Encryption types
	//For displaying on UI
	public static final String[] SECURITY = { "None", "WEP", "WPA-PSK", "WPA-EAP", "WPA2-PSK", "WPA2-EAP" };
	
	//For internal use
	public static final String[] ENCRYPTIONS = { "NONE", "WEP", "WPA", "WPA", "WPA2", "WPA2" };
	public static final String[] AUTHENTICATIONS = { "OPEN", "WEPAUTO", "WPAPSK", "WPAEAP", "WPA2PSK", "WPA2EAP" };
	public static final String[] ENCODINGS = { "", "hex", "ascii" };
	public static final String[] DEVICE_INFO = { PREF_CHUMBY_DEVICE_NAME, PREF_CHUMBY_USERNAME, PREF_CHUMBY_GUID, PREF_CHUMBY_HW_VERSION, PREF_CHUMBY_FW_VERSION, PREF_CHUMBY_MIN_ANDROID, PREF_CHUMBY_MAC, PREF_CHUMBY_IP_ADDRESS };
	public static final String[] DCID = { PREF_CHUMBY_DCID_SKIN, PREF_CHUMBY_DCID_CAMP, PREF_CHUMBY_DCID_RGIN, PREF_CHUMBY_DCID_PART, PREF_CHUMBY_DCID_VERS };
	
	//Hidden Android WiFi hotspot API sauce
	public static final int WIFI_AP_STATE_DISABLING = 0;
    public static final int WIFI_AP_STATE_DISABLED = 1;
    public static final int WIFI_AP_STATE_ENABLING = 2;
    public static final int WIFI_AP_STATE_ENABLED = 3;
    public static final int WIFI_AP_STATE_FAILED = 4;
	
	//Some fixed parameters
	public static final String		 DEFAULT_IP_ADDRESS = "192.168.100.1";
	
	//Wifi
	public WifiManager 			 	 _wifiManager;
	private MulticastLock 			 _multicastLock;
	
	//Local Data Storage
	private String					 _appVersion;
	private HashMap<String,String> 	 _hashParameters;
	private SharedPreferences 	 	 _prefs;
	
	//UDP Communication service
	String							_unicastAddress;
	boolean 						_messageReceiver_registered;
	MessageReceiver 				_messageReceiver;
	
	//http://loopj.com/android-async-http/
	AsyncHttpClient 				_httpClient;
	
	// Private Helper Class for Communication service
	//----------------------------------------------------------------------------
	
	protected CommService _myBoundService;
    private boolean _isServiceBound = false;
	
	private ServiceConnection mConnection = new ServiceConnection()
	{
	    public void onServiceConnected(ComponentName className, IBinder service)
	    {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
	    	_myBoundService = ((CommService.LocalBinder)service).getService();
	    	startCommService();
	    	_isServiceBound = true;
	    }

	    public void onServiceDisconnected(ComponentName className)
	    {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	    	_myBoundService = null;
	    }
	};	
		
	// UDP Communication Service
	//----------------------------------------------------------------------------
	
	/**
     * @category CommService
     */
	public void doBindService()
    {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
		Intent serviceIntent = new Intent (AppNeTV.this, CommService.class);
		serviceIntent.putExtra(CommService.SERVICE_STARTED_BY, "app");
    	bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

	/**
     * @category CommService
     */
	public void doUnbindService()
    {
        if (_isServiceBound)
        {
            // Detach our existing connection.
            unbindService(mConnection);
            _isServiceBound = false;
        }
    }
    
    /**
     * @category CommService
     */
    private void startCommService()
    {
    	if (_myBoundService != null)
    	{
    		_myBoundService.start("app");
    	}
    	else
    	{
	    	Intent serviceIntent = new Intent();
	    	serviceIntent.setClassName("com.chumby.udp", "com.chumby.udp.CommService");
	    	serviceIntent.putExtra(CommService.SERVICE_STARTED_BY, "app");
	    	ComponentName compName = startService(serviceIntent);
	    	if (compName == null)		ChumbyLog.d(TAG, "Communication Service failed to start");
	    	else			    		ChumbyLog.d(TAG, "Started Communication Service");
    	}
    }
    
    /**
     * @category CommService
     */
    private void stopCommService()
    {
    	if (_myBoundService != null)
    		_myBoundService.stopSelf();
    }
    
	
	// Initialization
	//----------------------------------------------------------------------------
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.d(TAG, "AppNeTV onCreate()");
				
		//Version number
		_appVersion = null;
	    _appVersion = getAppVersion();
	    Log.i(TAG, "AppNeTV: version: " + _appVersion);
	    
	    //Device info
	    Log.i(TAG, "AppNeTV: " + android.os.Build.MANUFACTURER + " - " + android.os.Build.MODEL);
	    Log.i(TAG, "AppNeTV: OS " + android.os.Build.VERSION.RELEASE  + " (Level " + android.os.Build.VERSION.SDK_INT + ")");
		
		//Setup the Shared Preferences
		_prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		initializeFirstTimePreferences();
		
		//Setup local temporary data
		_hashParameters = new HashMap<String,String>();
		
    	//Setup WiFi
    	if (_wifiManager == null)
    		_wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	_multicastLock = _wifiManager.createMulticastLock("whatiswrongwithhtc");
    	_multicastLock.setReferenceCounted(true);
    	_multicastLock.acquire();
			
		//Setup event receiver for CommService
		_messageReceiver = new MessageReceiver(this);
		if (ENABLE_RECEIVE) {
			registerReceiver(_messageReceiver, new IntentFilter(CommService.NEW_MESSAGE));
			_messageReceiver_registered = true;
		}
		
		//Setup HTTP client
		_httpClient = new AsyncHttpClient();
    	
    	//Save currently connected network
    	if (!isConnectedNeTV())
    	{    	
			//Save previously connected network ID
	    	//The rest of the application will depend on this to return users to 'home' network if quit
    		int homeNetID = getConnectedNetworkID();
    		if (homeNetID >= 0)
    		{
    			Log.i(TAG, "AppNeTV: remember home network: " + getConnectedNetworkSSID());
    			setPreferenceInt(PREF_PREVIOUS_NETID, getConnectedNetworkID());
    		}
    		//else
    		//{
    		//	enableAllNetworkConfig();
    		//}
    		
	    	//Remove all previously configured 'NeTV' network profiles (if any)
	    	removeNetworkConfig( this.getString(R.string.wifi_netv_ssid) );
	    	
	    	//Enable all network config (just to be sure)
	    	enableAllNetworkConfig();
    	}
    		
    	//Setup communication service
    	_unicastAddress = "";
    	_isServiceBound = false;
    	doBindService();
    	
    	//Service usually does not get bound immediately. This will fail for sure.
    	//queueMessage("<data><id>9</id><gs>ready</gs><msg>HelloWorld</msg></data>");
    	//queueMessage("<data><id>9</id><gs>ready</gs><msg>HelloWorld</msg></data>");
	}
		
	@Override
	public void onTerminate()
	{		
    	//Return NeTV UI to correct state
		if (isConnectedNeTV())
		{
			ResetUrlHTTP(null);
		}
		
		if (_multicastLock != null)
			_multicastLock.release();
		
		//Always unbind & stop the service (save battery)
		stopCommService();
		doUnbindService();
		
		//Remove all previously configured 'NeTV' network profiles (if any)
		boolean isConnectedNeTV = isConnectedNeTV();
		removeNetworkConfig(this.getString(R.string.wifi_netv_ssid));
        
        //Return user to his previous network if he close our app half-way (while configuring)
		if (isConnectedNeTV)
		{
			if (getPreviousNetworkID() != -1)
				_wifiManager.enableNetwork(getPreviousNetworkID(), true);
			Log.d(TAG, "Returning to home network [" + getPreviousNetworkSSID() + "]");
		}
		
		enableAllNetworkConfig();
        
        //Kill it the app if only we are not at RemoteMain or Browser activity
		//In order to let the user resume quickly at the next start
		String previousActivity = getPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, "");
    	if ( !previousActivity.equals(ActivityRemoteMain.class.getName()) &&
    		!previousActivity.equals(ActivityBrowser.class.getName()) )
    		System.exit(0);
    	
		super.onTerminate();
	}
	
	
    
	// SharedPreferences
	// We abstract this SharedPreferences so that moving to another platform is less painful
	//----------------------------------------------------------------------------
	
	/**
	 * Get a named String from _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public String getPreferenceString(String key, String defaultValue)
	{
		return _prefs.getString(key, defaultValue);
	}
	
	/**
	 * Set a named String in _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public void setPreferenceString(String key, String newValue)
	{
		SharedPreferences.Editor editor = _prefs.edit();       	
       	editor.putString(key, newValue);
       	editor.commit();
	}
	
	/**
	 * Get a named String from _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public boolean getPreferenceBoolean(String key, boolean defaultValue)
	{
		return _prefs.getBoolean(key, defaultValue);
	}
	
	/**
	 * Set a named String in _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public void setPreferenceBoolean(String key, boolean newValue)
	{
		SharedPreferences.Editor editor = _prefs.edit();       	
       	editor.putBoolean(key, newValue);
       	editor.commit();
	}
	
	/**
	 * Get a named Integer from _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public int getPreferenceInt(String key, int defaultValue)
	{
		return _prefs.getInt(key, defaultValue);
	}
	
	/**
	 * Set a named Integer in _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public void setPreferenceInt(String key, int newValue)
	{
		SharedPreferences.Editor editor = _prefs.edit();       	
       	editor.putInt(key, newValue);
       	editor.commit();
	}
	
	// Local HashMap
	// We use this instead of a bunch of variables
	// Child Activities just need to update this HashMap, it will be sent automatically to NeTV
	//----------------------------------------------------------------------------
	
	/**
	 * Get a parameter in the local HashMap
	 * 
	 * @category Local Parameters
	 */
	public String setLocalParameter(String key, String newValue)
	{
		return _hashParameters.put(key, newValue);
	}
	
	/**
	 * Set a parameter in the local HashMap
	 * 
	 * @category Local Parameters
	 */
	public String getLocalParameter(String key)
	{
		return _hashParameters.get(key);
	}
	
	/**
	 * Push local temporary parameters into Shared Preferences
	 * 
	 * @category Local Parameters
	 */
	public void saveLocalParameter()
	{
		SharedPreferences.Editor editor = _prefs.edit();
		for (HashMap.Entry<String, String> entry : _hashParameters.entrySet())
			editor.putString( entry.getKey(), entry.getValue() );
		editor.commit();
	}
	
	/**
	 * Returns the app version as in Manifest
	 * 
	 * @category Local Parameters
	 */
	public String getAppVersion()
	{
		if (_appVersion != null)
			return _appVersion;
		try
		{
		    PackageManager manager = this.getPackageManager();
		    PackageInfo info = manager.getPackageInfo("com.bunniestudios.NeTV", 0);
		    return info.versionName;
		}
		catch(NameNotFoundException nnf)
		{
		}
		return "";
	}
	
	/**
	 * Returns the true if Android app requires update
	 * 
	 * @category Local Parameters
	 */
	public boolean requiresAndroidUpdate(String minAndroid)
	{
		int min = Integer.parseInt( minAndroid.replace(".", "") );
		int ver = Integer.parseInt( getAppVersion().replace(".", "") );
		return (ver < min) ? true : false;
	}
	
	/**
	 * Set some default value for Preferences
	 * 
	 * @category Local Parameters
	 */
	private void initializeFirstTimePreferences()
	{
		if (_prefs == null)
			return;
		boolean isFirstTime = getPreferenceBoolean(PREF_FIRSTTIME, true);
		if (!isFirstTime)
			return;
		setPreferenceBoolean(PREF_FIRSTTIME, false);
	    setPreferenceBoolean(PREF_SMS_ENABLE, false);
	    setPreferenceBoolean(PREF_SMS_CONTENT, false);
	    setPreferenceString(PREF_CHUMBY_IP_MANUAL, "");
	    setPreferenceString(PREF_SMS_FILTER, PREF_SMS_FILTER_DEFAULT);
	}
	
	// Wifi Utility functions
    //----------------------------------------------------------------------------

	/**
	 * Initiate a AP scan
	 * 
	 * @category WiFi Utility
	 */
	public void startScan()
	{
		_wifiManager.startScan();
	}
	
	/**
	 * Get latest available AP scan result
	 * 
	 * @category WiFi Utility
	 */
	public List<ScanResult> getScanResults()
	{
		return _wifiManager.getScanResults();
	}
	
	/**
	 * Get configured network list (in the phone)
	 * 
	 * @category WiFi Utility
	 */
	public List<WifiConfiguration> getConfiguredNetworks()
	{
		return _wifiManager.getConfiguredNetworks();
	}
	
	/**
	 * Connect to hard-coded 'NeTV' WiFi network. This function a little slow and is blocking
	 * 
	 * @category WiFi Utility
	 */
	public boolean connectNeTV()
	{
		return connectNeTV(this.getString(R.string.wifi_netv_ssid));
	}
	public boolean connectNeTV(String ssid)
	{
		//Remove old configured network
        removeNetworkConfig(this.getString(R.string.wifi_netv_ssid));
        
        //Read this: http://kmansoft.wordpress.com/2010/04/08/adding-wifi-networks-to-known-list
        		
		//NeTV configuration (hardcoded)
        WifiConfiguration configNeTV = new WifiConfiguration();
        configNeTV.SSID = "\"".concat(ssid).concat("\"");
        configNeTV.status = WifiConfiguration.Status.DISABLED;
        configNeTV.priority = 40;

        //For open network
        configNeTV.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        configNeTV.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        configNeTV.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        configNeTV.allowedAuthAlgorithms.clear();
        configNeTV.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        configNeTV.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        configNeTV.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        configNeTV.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        configNeTV.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        configNeTV.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		        
        //Programmatically connect to NeTV
        //This function has to run on another thread as it is time consuming (disconnect, connect, dhcp, ...)
        final int netID = _wifiManager.addNetwork(configNeTV);
        enableNetwork(netID, true);

        return true;
	}
	
	/**
	 * remove all NeTV network config so that it get disconnected
	 * 
	 * @category WiFi Utility
	 */
	public boolean disconnectNeTV()
	{
		List<WifiConfiguration> configuredNetworks = getConfiguredNetworks();
		for (WifiConfiguration result : configuredNetworks)
	    	if (result.SSID != null && result.SSID.contains(this.getString(R.string.wifi_netv_ssid)))
	    		removeNetworkConfig(result.SSID);
        enableAllNetworkConfig();
        return true;
	}
	
	/**
	 * Connect to a given SSID.
	 * If the configuration is found in 'conifgured list', then use that configuration. <br>
	 * Otherwise, we use the configuration saved in our _prefs (SharedPreferences) because that's the config user *wants* NeTV device to use. <br>
	 * Mainly use to connect to 'home' network after 'NeTV' is gone.  <br>
	 * This function a little slow and is blocking
	 * 
	 * @category WiFi Utility
	 */
	public boolean connectHome(String tmp_ssid)
	{
		if (tmp_ssid == null || tmp_ssid.length() <= 0)
			return false;
		
		Log.d(TAG, "connectHome() " + tmp_ssid);
		
		//Check that we have 'tmp_ssid' already configured (in the phone)
		int configuredNetID = -1;
		List<WifiConfiguration>	configuredWifis = _wifiManager.getConfiguredNetworks();	
        for (WifiConfiguration configuredWifi : configuredWifis)
        	if (configuredWifi.SSID != null && configuredWifi.SSID.equals("\"".concat(tmp_ssid).concat("\"")))
        		configuredNetID = configuredWifi.networkId;

        //Yes, we use it to connect directly
        if (configuredNetID != -1) {
        	enableNetwork(configuredNetID, true);
    		return true;
        }
        
        //Otherwise, add it to configured networks list
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"".concat(tmp_ssid).concat("\"");
        config.status = WifiConfiguration.Status.DISABLED;
        config.priority = 40;
        
        //From SharedPreferences
        String wifi_password = getPreferenceString(PREF_WIFI_PASSWORD, "");
        String wifi_authentication = getPreferenceString(PREF_WIFI_AUTHENTICATION, "");
        
        //For open network
        if (wifi_password == null || wifi_password.equals("") || wifi_authentication.equals("OPEN"))
        {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedAuthAlgorithms.clear();
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        }
        //WEP
        else if (wifi_authentication.equals("WEPAUTO"))
        {
        	config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        	config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        	config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        	config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        	config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        	config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        	config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        	config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        	config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        	
        	// hex (perfect condition)
        	if (wifi_password.matches ("^[\\da-fA-F]+$") && (wifi_password.length()==10 || wifi_password.length()==26))
        	{
        		config.wepKeys[0] = wifi_password;
        	}
        	else if (wifi_password.length() == 5 || wifi_password.length() == 13)
        	{
        		wifi_password = StringToHex(wifi_password).toUpperCase();
        		config.wepKeys[0] = wifi_password;									//hex
        	}
        	else
        	{
        		config.wepKeys[0] = "\"".concat(wifi_password).concat("\"");		//ascii
        	}
        	config.wepTxKeyIndex = 0;
        }
        //WPA/WPA2
        else
        {
        	config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        	config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        	config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        	config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        	config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        	config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        	config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        	config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        	config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        	config.preSharedKey = "\"".concat(wifi_password).concat("\"");
        }
		        
        //Programmatically connect to NeTV
        //This has to run on another thread as it is time consuming (disconnect, connect, dhcp ...)
        configuredNetID = _wifiManager.addNetwork(config);

        //This has to run on another thread as it is quite time consuming (disconnect, connect, dhcp, ...)
        if (configuredNetID == -1)
        	return false;
        
        enableNetwork(configuredNetID, true);
        return true;
	}
	
	/**
	 * Remove all configured wifi network with given SSID or blank SSID
	 * 
	 * @category WiFi Utility
	 */
	public boolean removeNetworkConfig(String tmp_ssid)
	{
		if (tmp_ssid == null || tmp_ssid.length() <= 0)
			return false;
		
		List<WifiConfiguration>	configuredWifis = _wifiManager.getConfiguredNetworks();	
        for (WifiConfiguration configuredWifi : configuredWifis)
	    {
        	if (configuredWifi.SSID == null || 
        		tmp_ssid.equals( configuredWifi.SSID ) ||
        		configuredWifi.SSID.equals( "\"" + tmp_ssid + "\""))
        	{
        		_wifiManager.disableNetwork(configuredWifi.networkId);
        		_wifiManager.removeNetwork(configuredWifi.networkId);
        	}
	    }
        return true;
	}
	
	/**
	 * Adds a new network configuration
	 * 
	 * @category WiFi Utility
	 * @param config the network configuration (WifiConfiguration)
	 */
	public int addNetwork(WifiConfiguration config)
	{
		if (config == null)
			return -1;
		return _wifiManager.addNetwork(config);
	}
	
	/**
	 * Enable/Connect to a network in configured network list given its network ID. <br>
	 * This function create a short-life thread to perform the actual connection.
	 * 
	 * @category WiFi Utility
	 * @param netID network ID in configured network list
	 * @param disableOthers set to true to perform a connection to given network
	 */
	public void enableNetwork(final int netID, final boolean disableOthers)
	{
		if (!disableOthers) {
			_wifiManager.enableNetwork(netID, false);
			return;
		}
		
		new Thread(new Runnable()
        {
            public void run() {
            	_wifiManager.enableNetwork(netID, true);
            	Thread t = Thread.currentThread();
    			t.interrupt();
            }
        }).start();
	}
	
	/**
	 * Enable all network profiles in configured network list. <br>
	 * As a side effect, the best network may get re-connected if WiFi is enabled but not connected to any network
	 * 
	 * @category WiFi Utility
	 */
	public boolean enableAllNetworkConfig()
	{	
		List<WifiConfiguration>	configuredWifis = _wifiManager.getConfiguredNetworks();	
        for (WifiConfiguration configuredWifi : configuredWifis)
        	_wifiManager.enableNetwork(configuredWifi.networkId, false);

        return true;
	}
	
	/**
	 * Return whether WiFi is enabled or not
	 * 
	 * @category WiFi Utility
	 */
	public boolean isWifiEnabled()
	{
		return _wifiManager.isWifiEnabled();
	}
	
	/**
	 * Simply enable/disable WiFi
	 * 
	 * @category WiFi Utility
	 */
	public void setWifiEnabled(boolean isEn)
	{
		_wifiManager.setWifiEnabled(isEn);
	}
	
	/**
	 * Return whether WiFi AP (mobile hotspot) is enabled or not
	 * 
	 * @category WiFi Utility
	 */
	public boolean isWifiApEnabled()
	{
		return getWifiApState() == WIFI_AP_STATE_ENABLED;
	}
	
	/**
	 * Return current state of WiFi AP (mobile hotspot)
	 * 
	 * @category WiFi Utility
	 */
	public int getWifiApState()
	{
        try {
            Method method = _wifiManager.getClass().getMethod("getWifiApState");
            return (Integer) method.invoke(_wifiManager);
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return WIFI_AP_STATE_FAILED;
        }
	}
	
	/**
	 * Simply enable WiFi AP (mobile hotspot) with existing configurations
	 * For some reason Android SDK doesn't make this API public, we have to get around it
	 * by first getting the Method object by function name & invoking it 
	 *
	 * @see http://code.google.com/p/quick-settings/source/browse/trunk/quick-settings/src/com/bwx/bequick/handlers/WifiHotspotSettingHandler.java
	 * @see http://hi-android.info/src/android/net/wifi/WifiManager.java.html
	 * @category WiFi Utility
	 */
	public boolean setWifiApEnabled(boolean enabled)
	{
		// WiFi is always disabled when AP is enabled
        if (enabled)
        	_wifiManager.setWifiEnabled(false);

        try
        {
            /* Get the current configuration
            Method getWifiApConfigurationMethod = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            Object config = getWifiApConfigurationMethod.invoke(mWifiManager);
            */
            
            // configuration = null works for many devices
            Method setWifiApEnabledMethod = _wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            return (Boolean) setWifiApEnabledMethod.invoke(_wifiManager, null, enabled);
        }
        catch (Exception e)
        {
            Log.e(TAG, "", e);
            return false;
        }
	}
	
	/**
	 * Return true if currently connected to 'NeTV' access point
	 * 
	 * @category WiFi Utility
	 */
	public boolean isConnectedNeTV()
	{
		WifiInfo wifiInfo = _wifiManager.getConnectionInfo();
		
		if (wifiInfo == null || wifiInfo.getSSID() == null || !wifiInfo.getSSID().contains(this.getString(R.string.wifi_netv_ssid)))
			return false;
		if (wifiInfo.getBSSID() == null || wifiInfo.getRssi() == 0 || wifiInfo.getIpAddress() == 0)
			return false;
		
		return true;
	}
	
	/**
	 * Return true 'NeTV' is found in given scan result list
	 * 
	 * @category WiFi Utility
	 */
	public boolean isNeTVinRange()
	{
		List<ScanResult> scanresults = _wifiManager.getScanResults();
		if (scanresults == null)
			return false;
		
		for (ScanResult result : scanresults)
	    	if (result.SSID.equals(this.getString(R.string.wifi_netv_ssid)))
	            return true;

		return false;
	}
	
	/**
	 * Return true 'NeTV:xx:xx' is found in given scan result list
	 * 
	 * @category WiFi Utility
	 */
	public boolean isNeTVFactoryInRange()
	{
		List<ScanResult> scanresults = _wifiManager.getScanResults();
		if (scanresults == null)
			return false;
		
		for (ScanResult result : scanresults)
	    	if (result.SSID != null && result.SSID.contains(this.getString(R.string.wifi_netv_ssid)) )
	    		if (result.SSID.contains(":") && result.SSID.split(":").length == 3)
	    			return true;

		return false;
	}
	
	/**
	 * Return true if the IP address is among my IP
	 * 
	 * @category WiFi Utility
	 */
	public boolean isMyIP(String ipaddress)
	{
		if (ipaddress == null || ipaddress.length() < 5)
			return false;
		
		try
		{
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
	        {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
	            {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if ( ipaddress.equals(inetAddress.getHostAddress().toString()) )
	                	return true;
	            }
            }
	    }
		catch (SocketException ex)
	    {
	        
	    }
	    return false;
	}
	
	/**
	 * Return network ID of currently connected network
	 * 
	 * @category WiFi Utility
	 */
	public int getConnectedNetworkID()
	{
		WifiInfo wifiInfo = _wifiManager.getConnectionInfo();
		if (wifiInfo != null && wifiInfo.getSSID() != null)
			return wifiInfo.getNetworkId();
		
		return -1;
	}
	
	/**
	 * Return network SSID of currently connected network
	 * 
	 * @category WiFi Utility
	 */
	public String getConnectedNetworkSSID()
	{
		WifiInfo wifiInfo = _wifiManager.getConnectionInfo();
		if (wifiInfo != null)
			return wifiInfo.getSSID();
		
		return null;
	}
	
	/**
	 * Return signal level of currently connected network
	 * 
	 * @category WiFi Utility
	 */
	public int getConnectedNetworkSignalLevel()
	{
		WifiInfo wifiInfo = _wifiManager.getConnectionInfo();
		if (wifiInfo != null)
			return wifiInfo.getRssi();
		return 0;
	}
	
	/**
	 * Get home network ID stored in preferences
	 * 
	 * @category WiFi Utility
	 */
	public int getPreviousNetworkID()
	{
		return getPreferenceInt(PREF_PREVIOUS_NETID, -1);
	}
	
	/**
	 * Get home network SSID
	 * 
	 * @category WiFi Utility
	 */
	public String getPreviousNetworkSSID()
	{
		int netID = getPreferenceInt(PREF_PREVIOUS_NETID, -1);
		if (netID < 0)
			return "";
		
		List<WifiConfiguration>	configuredWifis = _wifiManager.getConfiguredNetworks();	
        for (WifiConfiguration configuredWifi : configuredWifis)
        	if (configuredWifi.networkId == netID)
        		return configuredWifi.SSID;
		return "";
	}
	
	/**
	 * Parse a wifi ScanResult into xml string similar to wifiscan.sh script
	 * 
	 * @category WiFi Utility
	 */
	public String parseWifiScanResult(ScanResult result)
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<wifi>");

		stringBuilder.append("<ssid>" + XMLencode(result.SSID) + "</ssid>");
		stringBuilder.append("<ch>" + result.frequency + "</ch>");
		stringBuilder.append("<lvl>" + result.level + "</lvl>");
		stringBuilder.append("<qty>" + result.level + "</qty>");
		stringBuilder.append("<address>" + result.BSSID + "</address>");
		
		//Android doesn't support adhoc!
		stringBuilder.append("<mode>Master</mode>");				
		
		String cap = result.capabilities;
		String enc = ENCRYPTIONS[0];
		String auth = AUTHENTICATIONS[0];
		
		//OPEN
		if (cap.equals(""))
		{
			enc = ENCRYPTIONS[0];
			auth = AUTHENTICATIONS[0];
		}
		else
		{
			//We go backwards in the array so WPA2 has higher priority
	    	for (int idx=SECURITY.length-1; idx>=0; idx--)
	    	{
	    		String regEx = ".*\\b" + AppNeTV.SECURITY[idx] + "\\b.*";
	    		if (!cap.matches(regEx))
	    			continue;
	    		enc = ENCRYPTIONS[idx];
	    		auth = AUTHENTICATIONS[idx];
	    		break;
	    	}
		}
		
		//Encryption for WPA or WPA2
		if (cap.contains("WPA"))
		{
			if (cap.contains("CCMP") || cap.contains("AES"))	enc = "AES";
			else												enc = "TKIP";
		}    		
		
		stringBuilder.append("<encryption>" + enc + "</encryption>");
		stringBuilder.append("<auth>" + auth + "</auth>");
		
		stringBuilder.append("</wifi>");
		return stringBuilder.toString();
	}
    
    // UDP Data Communication service
	//----------------------------------------------------------------------------
	
    /**
	 * @category DataComm Utility
	 */
    public boolean isCommServiceRunning()
    {
    	return _myBoundService != null && _myBoundService.isServiceRunning();
    }
    
    /**
	 * @category DataComm Utility
	 */
	public boolean queueMessage(HashMap<String,String> map)
	{
		if (!isCommServiceRunning())
		{
			Log.d(TAG, "CommService is not bound. Trying to re-bind...");
			doBindService();
			return false;
		}

		//Construct XML string (must have 1 root element)
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<xml>");
		
		//Command
		if (map.containsKey(MessageReceiver.COMMAND_MESSAGE)) {
			stringBuilder.append("<".concat(MessageReceiver.COMMAND_MESSAGE).concat(">"));
			stringBuilder.append( XMLencode(map.get(MessageReceiver.COMMAND_MESSAGE)) );
			stringBuilder.append("</".concat(MessageReceiver.COMMAND_MESSAGE).concat(">"));
			map.remove(MessageReceiver.COMMAND_MESSAGE);
		}
		//Status
		if (map.containsKey(MessageReceiver.STATUS_MESSAGE)) {
			stringBuilder.append("<".concat(MessageReceiver.STATUS_MESSAGE).concat(">"));
			stringBuilder.append( XMLencode(map.get(MessageReceiver.STATUS_MESSAGE)) );
			stringBuilder.append("</".concat(MessageReceiver.STATUS_MESSAGE).concat(">"));
			map.remove(MessageReceiver.STATUS_MESSAGE);
		}
		//Address
		String address = null;
		if (isConnectedNeTV())
		{
			//address = DEFAULT_IP_ADDRESS;
			if (map.containsKey(MessageReceiver.MESSAGE_KEY_ADDRESS))
				map.remove(MessageReceiver.MESSAGE_KEY_ADDRESS);
		}
		else if (map.containsKey(MessageReceiver.MESSAGE_KEY_ADDRESS))
		{
			address = map.get(MessageReceiver.MESSAGE_KEY_ADDRESS);
			map.remove(MessageReceiver.MESSAGE_KEY_ADDRESS);
		}
			
		//No parameters
		int hashMapSize = map.size();
		if (hashMapSize < 1)
		{
			stringBuilder.append("</xml>");
			return _myBoundService.sendMessage(stringBuilder.toString(), address);
		}
		
		stringBuilder.append("<data>");

		//Single parameter
		if (hashMapSize == 1)
		{
			stringBuilder.append("<value>");
			for (HashMap.Entry<String, String> entry : map.entrySet())
				stringBuilder.append( XMLencode(entry.getValue()) );
			stringBuilder.append("</value>");
		}
		
		//Multiple parameter
		else
		{
			for (HashMap.Entry<String, String> entry : map.entrySet())
			{
				stringBuilder.append("<".concat(entry.getKey()).concat(">"));
		    	stringBuilder.append( XMLencode(entry.getValue()) );
		    	stringBuilder.append("</".concat(entry.getKey()).concat(">"));
			}
		}
		
	    stringBuilder.append("</data></xml>");
	    return _myBoundService.sendMessage(stringBuilder.toString(), address);
	}
	
	/**
	 * @category DataComm Utility
	 */
	public static String XMLencode(String input)
	{
		return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");		//Not needed   .replace("'", "&apos;")
    }
	
	/**
	 * @category DataComm Utility
	 */
	public String StringToHex(String arg)
	{
		byte[] bytes = arg.getBytes();
		BigInteger bigInt = new BigInteger(bytes);
		String hexString = bigInt.toString(16);
		
		StringBuilder sb = new StringBuilder();
		while ((sb.length() + hexString.length()) < (2 * bytes.length))
			sb.append("0");
		sb.append(hexString);
		return sb.toString();
	}
	
    // HTTP Data Communication
	//----------------------------------------------------------------------------

    /**
     * @category DataComm Utility
     */
	public String sendHandshake()
	{
		return sendHandshake(null);
	}
	/**
     * Send initial UDP message to check if NeTV exist/activated on home network
 	 * Obtain hardware/software status, ID, versions, etc.
     * 
     * @category DataComm Utility
     */
	public String sendHandshake(String address)
	{
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		dataHashMap.put("type", "android");
		dataHashMap.put("version", _appVersion);
		if (address != null && address.length() > 0)
			dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, address);
		return sendSocketMessage(MessageReceiver.COMMAND_Handshake, dataHashMap);
	}
	
	/**
	 * Send a request to NeTV to point the browser at another URL
	 * 
	 * @category DataComm Utility
	 */
	public String sendNeTVBrowserSetUrl(String url)
	{
		Log.d(TAG, "Sending SetUrl command " + url);
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		if (!url.startsWith("http://") && !url.startsWith("https://"))
			url = "http://" + url;
		dataHashMap.put("value", url);
		
		//IP address for unicast transmission
    	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	if (_ipaddress.length() > 0)
    		dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _ipaddress);
    	
		return sendSocketMessage(MessageReceiver.COMMAND_SetUrl, dataHashMap);
	}
	
	/**
	 * Send a request to NeTV to point the browser at default page (Control Panel)
	 * 
	 * @category DataComm Utility
	 */
	public String sendNeTVBrowserReset()
	{
		return sendNeTVBrowserSetUrl(NETV_HOME_URL);
	}
		
	/**
     * Send a request to NeTV to get a list if WiFi perceived by NeTV
     * 
     * @category DataComm Utility
     */
	public String sendRequestWifiScan()
	{
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		return sendSocketMessage(MessageReceiver.COMMAND_WifiScan, dataHashMap);
	}
	
	/**
	 * Send a JavaScript command to NeTV
	 * Try to use specific tailor-made commands before using this command
	 * 
	 * @category DataComm Utility
	 */
	public String sendNeTVBrowserJavaScript(String javascriptString)
	{
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		dataHashMap.put("value", javascriptString);
		
		//IP address for unicast transmission
    	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	if (_ipaddress.length() > 0)
    		dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _ipaddress);
    	
		return sendSocketMessage(MessageReceiver.COMMAND_JavaScript, dataHashMap);
	}
	
	/**
     * Send a request to NeTV to get a list if WiFi perceived by NeTV
     * 
     * @category DataComm Utility
     */
	public String sendRequestFileContent(String fullpath)
	{
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		dataHashMap.put("value", fullpath);
		
		//IP address for unicast transmission
    	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	if (_ipaddress.length() > 0)
    		dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _ipaddress);
    	
		return sendSocketMessage(MessageReceiver.COMMAND_GetFileContents, dataHashMap);
	}
	
	/**
     * Send a request to NeTV to get MD5 sum of a file
     * 
     * @category DataComm Utility
     */
	public String sendRequestMD5File(String fullpath)
	{
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		dataHashMap.put("value", fullpath);
		
		//IP address for unicast transmission
    	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	if (_ipaddress.length() > 0)
    		dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _ipaddress);
    	
		return sendSocketMessage(MessageReceiver.COMMAND_MD5File, dataHashMap);
	}
	
	/**
     * Send a request to NeTV to delete a file
     * 
     * @category DataComm Utility
     */
	public String sendRequestUnlinkFile(String fullpath)
	{
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		dataHashMap.put("value", fullpath);
		
		//IP address for unicast transmission
    	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	if (_ipaddress.length() > 0)
    		dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _ipaddress);
    	
		return sendSocketMessage(MessageReceiver.COMMAND_UnlinkFile, dataHashMap);
	}
	
	/**
     * Send a request to NeTV to check if a file exists
     * 
     * @category DataComm Utility
     */
	public String sendRequestFileExists(String fullpath)
	{
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		dataHashMap.put("value", fullpath);
		
		//IP address for unicast transmission
    	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	if (_ipaddress.length() > 0)
    		dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _ipaddress);
    	
		return sendSocketMessage(MessageReceiver.COMMAND_FileExists, dataHashMap);
	}
	
	/**
     * Send a SetIFrame command (Flick feature)
     * 
     * @category DataComm Utility
     */
	public String sendMultiTabCommand(String param)
	{
		return sendMultiTabCommand(param, "load", 1);
	}
	/**
     * @category DataComm Utility
     */
	public String sendMultiTabCommand(String param, String options)
	{
		return sendMultiTabCommand(param, options, 1);
	}
	/**
     * @category DataComm Utility
     */
	public String sendMultiTabCommand(String param, String options, int tab)
	{
		if (options == null || options.length() < 1)
			options = "load";
		if (options.equals("load") && (param == null || param.length() < 1))
			return "";
		if (options.equals("load") && !param.startsWith("http://") && !param.startsWith("https://"))
			param = "http://" + param;
		
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		try {
			String encoded = URLEncoder.encode(param, "UTF-8");
			if (options.equals("html"))			dataHashMap.put("param", encoded);
			else								dataHashMap.put("param", param);
		} catch (Exception e) {
			Log.e(TAG, "NeTVApp: encoding error " + param);
			dataHashMap.put("param", ""); 
			return "";
		}
		dataHashMap.put("options", options);
		dataHashMap.put("tab", "" + tab);
		
		//IP address for unicast transmission
    	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	if (_ipaddress.length() > 0)
    		dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _ipaddress);
    	
    	if (options.equals("load"))
			Log.i(TAG, "AppNeTV: load multitab " + param);
    	
		return sendSocketMessage(MessageReceiver.COMMAND_Multitab, dataHashMap);
	}
	
	/**
     * Send a TickerEvent command
     * 
     * @category DataComm Utility
     */
	public String sendTickerEvent(String message, String title, String image, String type, String level)
	{   	
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		dataHashMap.put("message", message);
		dataHashMap.put("title", title);
		dataHashMap.put("image", image);
		dataHashMap.put("type", type);
		dataHashMap.put("level", level);
		
		//IP address for unicast transmission
    	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	if (_ipaddress.length() > 0)
    		dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _ipaddress);

		return sendSocketMessage(MessageReceiver.COMMAND_TickerEvent, dataHashMap);
	}
    
    /**
	 * @category DataComm Utility
	 */
    public String sendSimpleButton(String buttonName)
    {
    	HashMap<String,String> dataHashMap = new HashMap<String,String>();
    	dataHashMap.put("value", buttonName);
    	
    	//IP address for unicast transmission
    	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	if (_ipaddress.length() > 0)
    		dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _ipaddress);
    	
    	return sendSocketMessage(MessageReceiver.COMMAND_RemoteControl, dataHashMap);	
    }
    
    /**
	 * @category DataComm Utility
	 */
    public String sendSimpleKey(String keyName)
    {
    	HashMap<String,String> dataHashMap = new HashMap<String,String>();
    	dataHashMap.put("value", keyName);
    	
    	//IP address for unicast transmission
    	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	if (_ipaddress.length() > 0)
    		dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _ipaddress);
    	
    	return sendSocketMessage(MessageReceiver.COMMAND_Key, dataHashMap);	
    }

	/**
     * Send the selectedSSID to NeTV's Android configuration page
     * 
     * @category DataComm Utility
     */
	public String sendAndroidJSChangeView(String viewName)
	{
	    return sendAndroiJSEvent("changeview", viewName);
	}
	
	/**
     * Send a console log string to Factory Test UI
     * 
     * @category DataComm Utility
     */
	public String sendAndroidJSTestConsole(String text, String color)
	{
		if (color == null || color.length() < 2)
			return sendAndroiJSEvent("testconsole", text);
		else
			return sendAndroiJSEvent("testconsole", "<font color='" + color + "'>" + text + "</font>");
	}
	
	/**
     * Clear timeout on html_config page
     * 
     * @category DataComm Utility
     */
	public String sendAndroidJSClearTimeout()
	{
		String javaScriptString = "android_clearTimeout();";
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		dataHashMap.put("value", javaScriptString);
		
		//IP address for unicast transmission
    	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	if (_ipaddress.length() > 0)
    		dataHashMap.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _ipaddress);
    	
		return sendSocketMessage(MessageReceiver.COMMAND_JavaScript, dataHashMap);
	}
	
	/**
     * Send a WifiScan result to NeTV's Android configuration page
     * in the same format as 'data' section of internal wifiscan.sh script
     * 
     * @category DataComm Utility
     */
	public String sendAndroidJSWifiScan()
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<xml>");
		stringBuilder.append("<status>1</status>");
		stringBuilder.append("<cmd>WIFISCAN</cmd>");
		stringBuilder.append("<data>");

    	//Eliminates duplicates SSID by taking only strongest BSSID
		List<ScanResult> wifiList = getScanResults();
		HashMap<String, ScanResult> wifiHashMap = new HashMap<String, ScanResult>();
    	for (ScanResult result : wifiList)
    	{
    		if ( this.getString(R.string.wifi_netv_ssid).equals(result.SSID) )
    			continue;
    		ScanResult tempWifi = wifiHashMap.get(result.SSID);
    		if (tempWifi != null && tempWifi.level > result.level)
    			continue;
    		
    		wifiHashMap.put(result.SSID, result);
    		stringBuilder.append( parseWifiScanResult(result) );
    	}

    	wifiHashMap.clear();
    	stringBuilder.append("</data></xml>");
	    return sendAndroiJSEvent("wifiscan", stringBuilder.toString());
	}
	
	/**
     * Send the selectedSSID to NeTV's Android configuration page
     * 
     * @category DataComm Utility
     */
	public String sendAndroidJSWifiSelect(String selectedSSID)
	{
	    return sendAndroiJSEvent("wifiselect", selectedSSID);
	}
	
	/**
     * Send the current wifi details to NeTV's Android configuration page
     * 
     * @category DataComm Utility
     */
	public String sendAndroidJSWifiDetails(String ssid, String password, String enc, String auth)
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<"+PREF_WIFI_SSID+">" + ssid + "</"+PREF_WIFI_SSID+">");
		stringBuilder.append("<"+PREF_WIFI_PASSWORD+">" + password + "</"+PREF_WIFI_PASSWORD+">");
		stringBuilder.append("<"+PREF_WIFI_ENCRYPTION+">" + enc + "</"+PREF_WIFI_ENCRYPTION+">");
		stringBuilder.append("<"+PREF_WIFI_AUTHENTICATION+">" + auth + "</"+PREF_WIFI_AUTHENTICATION+">");
	    return sendAndroiJSEvent("wifidetails", stringBuilder.toString());
	}
	
	/**
     * Send the current account details to NeTV's Android configuration page
     * 
     * @category DataComm Utility
     */
	public String sendAndroidJSAccountDetails(String username, String password, String devicename)
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<"+PREF_CHUMBY_USERNAME+">" + username + "</"+PREF_CHUMBY_USERNAME+">");
		stringBuilder.append("<"+PREF_CHUMBY_PASSWORD+">" + password + "</"+PREF_CHUMBY_PASSWORD+">");
		stringBuilder.append("<"+PREF_CHUMBY_DEVICE_NAME+">" + devicename + "</"+PREF_CHUMBY_DEVICE_NAME+">");
	    return sendAndroiJSEvent("accountdetails", stringBuilder.toString());
	}
	
	/**
     * Send an Android JavaScript event to NeTVServer -> NeTVBrowser
     * 
     * @category DataComm Utility
     */
	public String sendAndroiJSEvent(String eventName, String data)
	{
		try
		{
			data = URLEncoder.encode(data, "UTF-8");		//UTF-8 here migh cause problem for TickerEvent
			HashMap<String,String> dataHashMap = new HashMap<String,String>();
			dataHashMap.put("eventname", eventName);
			dataHashMap.put("eventdata", data);
			return sendSocketMessage(MessageReceiver.COMMAND_Android, dataHashMap);
		}
		catch(Exception e)
		{
			
		}
		return "";
	}
	
	/**
	 * @category DataComm Utility
	 */
	public String sendSystemTime()
	{
		//Construct time & timezone string
		//We add 1 seconds as our tranmission & scripts causes some delay in the middle
		//$1 is time formatted in GMT time as "yyyy.mm.dd-hh:mm:ss"
		//$2 is standard timezone ID string formated as "Asia/Singapore"
		//see http://developer.android.com/reference/java/util/TimeZone.html#getID()
			
		Calendar rightNow = Calendar.getInstance();
		rightNow.add(Calendar.SECOND, 1);
		int yyyy = rightNow.get(Calendar.YEAR);
		int mm = rightNow.get(Calendar.MONTH)+1;		//why is it starting from zero?
		int dd = rightNow.get(Calendar.DAY_OF_MONTH);
		
		int hh = rightNow.get(Calendar.HOUR_OF_DAY);
		int min = rightNow.get(Calendar.MINUTE);
		int ss = rightNow.get(Calendar.SECOND);
		String timeString = String.format("%04d.%02d.%02d-%02d:%02d:%02d", yyyy,mm,dd,hh,min,ss);
		
		TimeZone timezone = rightNow.getTimeZone();
		String timezoneString = timezone.getID();
				
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		dataHashMap.put(PREF_TIME, timeString);
		dataHashMap.put(PREF_TIMEZONE, timezoneString);
		
		Log.d(TAG, "Sending time configuration " + timeString + " | " + timezoneString);
		return sendSocketMessage(MessageReceiver.COMMAND_Time, dataHashMap);
	}
			
	/** 
	 * @category DataComm Utility
	 */
	public String sendNetworkConfig()
	{
		cleanUpNetworkConfig();
		
        //Now we throw everything to NeTV. yay!
        Log.d(TAG, "Sending network config:");
        HashMap<String,String> dataHashMap = new HashMap<String,String>();        
		for (int i=0; i<PREF_WIFI_DETAILS.length; i++)
		{
			String key = PREF_WIFI_DETAILS[i];
			String value = getPreferenceString(PREF_WIFI_DETAILS[i], "");
			dataHashMap.put(key, value);
			Log.d(TAG, "  " + key + " = " + value);
		}
		
		return sendSocketMessage(MessageReceiver.COMMAND_Network, dataHashMap);
	}

	/**
	 * Post a HTTP Request to NeTV's web server with data as key/value pairs. <br>
	 * To be parsed by a CGI/shell script located in www/cgi-bin folder
	 * 
	 * @category DataComm Utility
	 */
	private String sendSocketMessage(String commandName, HashMap<String,String> parameters)
	{
		if (parameters == null)
			parameters = new HashMap<String,String>();
		
		parameters.put(MessageReceiver.COMMAND_MESSAGE, commandName);
		queueMessage(parameters);
		return "";
	}

	/** 
	 * @category DataComm Utility
	 */
	private void cleanUpNetworkConfig()
	{
		//These parameters does not get passed (null) if selected wifi is un-secured
		//We need to massage them first before sending out
		
		SharedPreferences.Editor editor = _prefs.edit();
        
        String wifi_encryption = getPreferenceString(PREF_WIFI_ENCRYPTION, "");
        if (wifi_encryption.equals(""))
        	editor.putString(PREF_WIFI_ENCRYPTION, ENCRYPTIONS[0]);
        
        String wifi_authentication = getPreferenceString(PREF_WIFI_AUTHENTICATION, "");
        if (wifi_authentication.equals(""))
        	editor.putString(PREF_WIFI_AUTHENTICATION, AUTHENTICATIONS[0]);
                
        //Blank password field
        String wifi_password = getPreferenceString(PREF_WIFI_PASSWORD, "");
        editor.putString(PREF_WIFI_PASSWORD, wifi_password);
        
        //The following validations will be done again in NeTVServer
        
        //No password given
        if (wifi_password.length() < 1)
        {
        	editor.putString(PREF_WIFI_ENCRYPTION, ENCRYPTIONS[0]);
        	editor.putString(PREF_WIFI_AUTHENTICATION, AUTHENTICATIONS[0]);
        	editor.putString(PREF_WIFI_ENCODING, ENCODINGS[0]);
        }
        
        //WEP
        else if (wifi_encryption.equals(ENCRYPTIONS[1]))
        {
        	// WEPAUTO
        	editor.putString(PREF_WIFI_AUTHENTICATION, AUTHENTICATIONS[1]);
        	
    		// hex (perfect condition)
        	if (wifi_password.matches ("^[\\da-fA-F]+$") && (wifi_password.length()==10 || wifi_password.length()==26))
        	{
           		editor.putString(PREF_WIFI_ENCODING, ENCODINGS[1]);
        	}
        	// directly convert ascii characters into hexadecimal characters
        	else if (wifi_password.length()==5 || wifi_password.length()==13)
        	{
        		editor.putString(PREF_WIFI_ENCODING, ENCODINGS[1]);
        	}
    		// ascii
        	else
        		editor.putString(PREF_WIFI_ENCODING, ENCODINGS[2]);
        }
        
        //WPA
        else
        {
        	editor.putString(PREF_WIFI_ENCODING, ENCODINGS[0]);
        	
        	//TKIP-only configuration
        	if (!wifi_encryption.contains("AES"))
        		editor.putString(PREF_WIFI_ENCRYPTION, "TKIP");
        }
        	
        editor.commit();
	}

	
	
	
	
	/**
	 * 
	 * HTTP version of the commands
	 * 
	 */	
	
	
	/** 
	 * @category DataComm Utility
	 */
	public String GetHTTPBridgeURL()
	{
		if (isConnectedNeTV())
			return "http://" + AppNeTV.DEFAULT_IP_ADDRESS + "/bridge";

		//Manual IP override
		String _manual_ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_MANUAL, "");
		if (_manual_ipaddress.length() > 6)
			return "http://" + _manual_ipaddress + "/bridge";

		//Last known good IP
		String _good_ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
		if (_good_ipaddress.length() > 6)
			return "http://" + _good_ipaddress + "/bridge";
		
		return "http://" + AppNeTV.DEFAULT_IP_ADDRESS + "/bridge";
	}
	
	/**
	 * Send a request to NeTV to point the browser at default page (Control Panel)
	 * 
	 * @category DataComm Utility
	 */
	public void ResetUrlHTTP(AsyncHttpResponseHandler handler)
	{
		SetUrlHTTP(NETV_HOME_URL, handler);
	}
	
	/**
	 * Send a request to NeTV to point the browser at another URL
	 * 
	 * @category DataComm Utility
	 */
	public void SetUrlHTTP(String url, AsyncHttpResponseHandler handler)
	{
		if (!url.startsWith("http://") && !url.startsWith("https://"))
			url = "http://" + url;
		
		RequestParams params = new RequestParams();
		params.put(MessageReceiver.COMMAND_MESSAGE, MessageReceiver.COMMAND_SetUrl);
		params.put(MessageReceiver.MESSAGE_KEY_VALUE, url);	
		Log.d(TAG, "Sending SetUrl command (HTTP) " + url);
				
		//Cancel current on going request (if any)
		_httpClient.cancelRequests(this, true);
		
		//External event handler
		if (handler != null) {
			_httpClient.post(GetHTTPBridgeURL(), params, handler);
			return;
		}
		
		//Default event handler
		_httpClient.post(GetHTTPBridgeURL(), params, new AsyncHttpResponseHandler()
		{
		    @Override
		    public void onSuccess(String response)
		    {
		    	Log.d(TAG, "SetUrlHTTP: " + response);
		    	String status = response.split("</status>")[0].split("<status>")[1].trim();
		    	if (!status.equals("1"))
		    		Log.e(TAG, "SetUrlHTTP: something is not right");
		    }
		    
		    @Override
		    public void onFailure(Throwable error)
		    {
		    	Log.e(TAG, "SetUrlHTTP: failed");
				Log.e(TAG, "" + error);
		    }
		});
	}
	
	/**
	 * @category DataComm Utility
	 */
	public void SetTimeHTTP(AsyncHttpResponseHandler handler)
	{
		//Construct time & timezone string
		//We add 1 seconds as our tranmission & scripts causes some delay in the middle
		//$1 is time formatted in GMT time as "yyyy.mm.dd-hh:mm:ss"
		//$2 is standard timezone ID string formated as "Asia/Singapore"
		//see http://developer.android.com/reference/java/util/TimeZone.html#getID()
			
		Calendar rightNow = Calendar.getInstance();
		rightNow.add(Calendar.SECOND, 1);
		int yyyy = rightNow.get(Calendar.YEAR);
		int mm = rightNow.get(Calendar.MONTH)+1;		//why is it starting from zero?
		int dd = rightNow.get(Calendar.DAY_OF_MONTH);
		
		int hh = rightNow.get(Calendar.HOUR_OF_DAY);
		int min = rightNow.get(Calendar.MINUTE);
		int ss = rightNow.get(Calendar.SECOND);
		String timeString = String.format("%04d.%02d.%02d-%02d:%02d:%02d", yyyy,mm,dd,hh,min,ss);
		
		TimeZone timezone = rightNow.getTimeZone();
		String timezoneString = timezone.getID();
				
		RequestParams params = new RequestParams();
		params.put(MessageReceiver.COMMAND_MESSAGE, MessageReceiver.COMMAND_Time);
		params.put(AppNeTV.PREF_TIME, timeString);
		params.put(AppNeTV.PREF_TIMEZONE, timezoneString);	
		Log.d(TAG, "Sending time configuration (HTTP) " + timeString + " | " + timezoneString);
				
		//Cancel current on going request (if any)
		_httpClient.cancelRequests(this, true);
		
		//External event handler
		if (handler != null) {
			_httpClient.post(GetHTTPBridgeURL(), params, handler);
			return;
		}
		
		//Default event handler
		_httpClient.post(GetHTTPBridgeURL(), params, new AsyncHttpResponseHandler()
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
		});
	}
	
	/**
	 * @category DataComm Utility
	 */
	public void SetNetworkHTTP(AsyncHttpResponseHandler handler)
	{
		cleanUpNetworkConfig();
		RequestParams params = new RequestParams();
		params.put(MessageReceiver.COMMAND_MESSAGE, MessageReceiver.COMMAND_Network);
		
		for (int i=0; i<AppNeTV.PREF_WIFI_DETAILS.length; i++)
		{
			String key = AppNeTV.PREF_WIFI_DETAILS[i];
			String value = getPreferenceString(AppNeTV.PREF_WIFI_DETAILS[i], "");
			params.put(key, value);
		}
		
		//Cancel current on going request (if any)
		_httpClient.cancelRequests(this, true);
		
		//External event handler
		if (handler != null) {
			_httpClient.post(GetHTTPBridgeURL(), params, handler);
			return;
		}
		
		//Default event handler
		_httpClient.post(GetHTTPBridgeURL(), params, new AsyncHttpResponseHandler()
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
		});
	}
	
	/**
     * Send a TickerEvent command
     * 
     * @category DataComm Utility
     */
	public void TickerEventHTTP(String message, String title, String image, String type, String level, AsyncHttpResponseHandler handler)
	{   	
		RequestParams params = new RequestParams();
		params.put(MessageReceiver.COMMAND_MESSAGE, MessageReceiver.COMMAND_TickerEvent);
		params.put("message", message);
		params.put("title", title);
		params.put("image", image);
		params.put("type", type);
		params.put("level", level);
		
    	//Cancel current on going request (if any)
		_httpClient.cancelRequests(this, true);
		
		//External event handler
		if (handler != null) {
			_httpClient.post(GetHTTPBridgeURL(), params, handler);
			return;
		}
		
		//Default event handler
		_httpClient.post(GetHTTPBridgeURL(), params, new AsyncHttpResponseHandler()
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
		});
	}
    
	/**
     * @category DataComm Utility
     */
	public void MultiTabHTTP(String param, AsyncHttpResponseHandler handler)
	{
		MultiTabHTTP(param, "load", 1, handler);
	}
	/**
     * @category DataComm Utility
     */
	public void MultiTabHTTP(String param, String options, AsyncHttpResponseHandler handler)
	{
		MultiTabHTTP(param, options, 1, handler);
	}
	/**
     * @category DataComm Utility
     */
	/**
     * @category DataComm Utility
     */
	public void MultiTabHTTP(String param, String options, int tab, AsyncHttpResponseHandler handler)
	{
		if (options == null || options.length() < 1)
			options = "load";
		if (options.equals("load") && (param == null || param.length() < 1))
			return;
		if (options.equals("load") && !param.startsWith("http://") && !param.startsWith("https://"))
			param = "http://" + param;
		
		RequestParams params = new RequestParams();
		params.put(MessageReceiver.COMMAND_MESSAGE, MessageReceiver.COMMAND_Multitab);
		try {
			String encoded = URLEncoder.encode(param, "UTF-8");
			if (options.equals("html"))			params.put("param", encoded);
			else								params.put("param", param);
		} catch (Exception e) {
			Log.e(TAG, "NeTVApp: encoding error " + param);
			params.put("param", ""); 
			return;
		}
		
		params.put("options", options);
		params.put("tab", "" + tab);

    	if (options.equals("load"))
			Log.i(TAG, "MultiTabHTTP: load " + param);
    	
    	//Cancel current on going request (if any)
		_httpClient.cancelRequests(this, true);
		
		//External event handler
		if (handler != null) {
			_httpClient.post(GetHTTPBridgeURL(), params, handler);
			return;
		}
		
		//Default event handler
		_httpClient.post(GetHTTPBridgeURL(), params, new AsyncHttpResponseHandler()
		{
		    @Override
		    public void onSuccess(String response)
		    {		    	
		    	Log.d(TAG, "MultiTabHTTP: " + response);
		    	String status = response.split("</status>")[0].split("<status>")[1].trim();
		    	if (!status.equals("1"))
		    		Log.e(TAG, "MultiTabHTTP: something is not right");	    		
		    }
		    
		    @Override
		    public void onFailure(Throwable error)
		    {
		    	Log.e(TAG, "MultiTabHTTP failed");
				Log.e(TAG, "" + error);
		    }
		});
	}
	
	/**
	 * Send an Android command
	 * 
	 * @category DataComm Utility
	 */
	public void SendAndroidHTTP(String eventname, String eventdata, AsyncHttpResponseHandler handler)
	{
		RequestParams params = new RequestParams();
		params.put(MessageReceiver.COMMAND_MESSAGE, MessageReceiver.COMMAND_Android);
		params.put("eventname", eventname);
		params.put("eventdata", eventdata);
		Log.d(TAG, "Sending SendAndroid command (HTTP) " + eventname);
				
		//Cancel current on going request (if any)
		_httpClient.cancelRequests(this, true);
		
		//External event handler
		if (handler != null) {
			_httpClient.post(GetHTTPBridgeURL(), params, handler);
			return;
		}
		
		//Default event handler
		_httpClient.post(GetHTTPBridgeURL(), params, new AsyncHttpResponseHandler()
		{
		    @Override
		    public void onSuccess(String response)
		    {
		    	Log.d(TAG, "SendAndroidHTTP: " + response);
		    	String status = response.split("</status>")[0].split("<status>")[1].trim();
		    	if (!status.equals("1"))
		    		Log.e(TAG, "SendAndroidHTTP: something is not right");
		    }
		    
		    @Override
		    public void onFailure(Throwable error)
		    {
		    	Log.e(TAG, "SendAndroidHTTP: failed");
				Log.e(TAG, "" + error);
		    }
		});
	}
	
	// Application logic
	//----------------------------------------------------------------------------
	
	/**
	 * This will be called after the UDP Service receives new message and hashes it nicely
	 * 
	 * @category Application Logic
	 */
	public void ProcessMessage(HashMap<String,String> hashMessage)
	{
		//Update unicast address so next time we transmit in unicast
		if (hashMessage.containsKey(MessageReceiver.MESSAGE_KEY_ADDRESS))
			_unicastAddress = (String) hashMessage.get(MessageReceiver.MESSAGE_KEY_ADDRESS);
			
		//Simly forward the message to the listerning Activities
		//Create a new Intent and dump the HashMap content as Extras
		Intent intent = new Intent(RECEIVE_SOCKET_MESSAGE_EVENT);
		for (Map.Entry<String, String> entry: hashMessage.entrySet())
			intent.putExtra(entry.getKey(), entry.getValue());
		sendBroadcast( intent );
	}

	
	HashMap<String,String> getChumbyGUIDProfile(String guid)
	{
		HashMap<String,String> dataHashMap = new HashMap<String,String>();
		
	    HttpClient httpclient = new DefaultHttpClient();
	    //HttpGet httpget = new HttpGet("http://xml.chumby.com/xml/chumbies/?id=" + guid);
	    HttpGet httpget = new HttpGet("http://www.chumby.com/xapis/device/authorize/" + guid);

	    try
	    {
	    	ResponseHandler<String> responseHandler = new BasicResponseHandler();
	        String responseString = httpclient.execute(httpget, responseHandler);
	        
	        if (responseString.contains("<name>") && responseString.contains("</name>"))
	        	dataHashMap.put(PREF_CHUMBY_DEVICE_NAME, responseString.split("</name>")[0].split("<name>")[1]);
	        
	        /*
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			InputSource inSource = new InputSource(new StringReader(responseString));
			Document doc = builder.parse(inSource); //possible DOM exception here
			doc.getDocumentElement().normalize();
	
			NodeList nodeList = doc.getFirstChild().getChildNodes();
	
			//Parse at most 1 levels of XML
			for (int i = 0; i < nodeList.getLength(); i++)
			{
				Node node = nodeList.item(i);
				int numChild = node.getChildNodes().getLength();
				if (numChild == 1 && node.getFirstChild().getNodeType() != Node.ELEMENT_NODE)
				{
					if (node.getNodeName().equalsIgnoreCase("name"))
						dataHashMap.put(PREF_CHUMBY_DEVICE_NAME, node.getTextContent());
					else
						dataHashMap.put(node.getNodeName(), node.getTextContent());
				}
				else if (numChild == 0 && node.getAttributes() != null)
				{
					if (node.getAttributes().getNamedItem("username") != null)
					{
						String username = node.getAttributes().getNamedItem("username").getTextContent();
						if (username != null)
							dataHashMap.put(PREF_CHUMBY_USERNAME, username);
					}
				}
			}
	         */
	    } 
	    catch (Exception e)
	    {
	    	dataHashMap.clear();
	    }
	    return dataHashMap;
	} 
}

package com.bunniestudios.NeTV;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public abstract class ActivityBaseNeTV extends Activity implements CustomEventReceiverInterface
{
	protected static final String TAG = "NeTV";
	
	//Debug flags (all in one place)
	protected boolean SUPER_VERBOSE = true;					//universal
	protected boolean ENABLE_SOUND_FX = false;				//universal
	protected boolean ENABLE_TEXT_INPUT = false;			//ActivityRemoteMain
	protected boolean ENABLE_AUTO_SELECT_AP = false;		//ActivitySplash
	protected boolean ALLOW_UNACTIVATED_REMOTE = false;		//ActivitySplash
	protected int NETWORK_CONFIG_WAIT_SECONDS = 60;			//ActivityActivation
		
	//Flags
	protected boolean 				_bGoingNextState;
	
	//Multi-threading and delay
	protected static final Handler	_handler = new Handler();
	
	//App
	AppNeTV							_myApp;
	
	//Vibrator
	protected Vibrator 				_vibrator;
	
	//Sound
	MediaPlayer 					_mediaPlayer;
	
	//Receive data
	private boolean 				_customEventReceiver_registered;
	private CustomEventReceiver 	_customEventReceiver;
	
	//UI
	protected ProgressDialog		_busyDialog;
	
	//TCP or UDP
	protected boolean				_useTCP;
	
	// Initialization
	//----------------------------------------------------------------------------
	
	/**
	 * Called when the activity is first created.
	 * 
	 * @category Initialization
	 * @see http://developer.android.com/reference/android/app/Activity.html
	 */
	@Override
    public void onCreate(Bundle savedInstanceState)
    {   
    	super.onCreate(savedInstanceState);
    	
    	//Setup custom event mechanism
    	_customEventReceiver = new CustomEventReceiver(this);
    	
    	if (!_customEventReceiver_registered)
    		registerReceiver(_customEventReceiver, new IntentFilter(AppNeTV.RECEIVE_SOCKET_MESSAGE_EVENT));
    	_customEventReceiver_registered = true;
    	    	
    	//Setup vibrator
    	_vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
    	
    	//Setup media player
    	_mediaPlayer = MediaPlayer.create(this, R.raw.pop);
    	            	
    	//Setup UI
    	_busyDialog = null;
    	
    	//Setup parent application reference pointer
		_myApp = (AppNeTV)getApplicationContext();
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
    	_bGoingNextState = false;
    	
		String firmware = getPreferenceString(AppNeTV.PREF_CHUMBY_FW_VERSION, "");
		if (firmware.length() > 0 && Integer.parseInt(firmware) > 11)
			_useTCP = true; 
		if (_useTCP)
			Log.d(TAG, this.getLocalClassName() + ": using HTTP/TCP");
    	
    	//CommService always get stopped & unbinded no matter where the app is onPause()
    	//This is neccessary mostly when user terminate/screen timeout/power button at ActivityRemoteMain
    	if (!_myApp.isCommServiceRunning())
    	{
			Log.d(TAG, "App resumed. Rebinding CommService...");
			_myApp.doBindService();
		}
    	    	
    	super.onResume();
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
    	Log.d(TAG, this.getLocalClassName() + " onPause()");
    	    	
    	//Stop listerning for socket message
    	if (_customEventReceiver_registered)
    		unregisterReceiver(_customEventReceiver);
    	_customEventReceiver_registered = false;
    	
    	//Close busy dialog if showing
    	closeBusyDialog();

    	//Save shared variable for next activity
    	updateHashVariables();
        
    	//Return user to his previous network if he close our app half-way
    	if (!_bGoingNextState)
    	{
    		_myApp.sendMultiTabCommand("", "hide");
    		_myApp.onTerminate();
    	}

    	super.onPause();
	}
    
	// SharedPreferences
	// We abstract this SharedPreferences so that moving to another platform is less painful
	//----------------------------------------------------------------------------
	
	/**
	 * Get a named String from _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	protected String getPreferenceString(String key, String defaultValue)
	{
		return _myApp.getPreferenceString(key, defaultValue);
	}
	
	/**
	 * Set a named String in _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	protected void setPreferenceString(String key, String newValue)
	{
		_myApp.setPreferenceString(key, newValue);
	}
	
	/**
	 * Get a named Integer from _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	protected int getPreferenceInt(String key, int defaultValue)
	{
		return _myApp.getPreferenceInt(key, defaultValue);
	}
	
	/**
	 * Set a named Integer in _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	protected void setPreferenceInt(String key, int newValue)
	{
		_myApp.setPreferenceInt(key, newValue);
	}
	
	/**
	 * Get a named Boolean from _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	protected boolean getPreferenceBoolean(String key, boolean defaultValue)
	{
		return _myApp.getPreferenceBoolean(key, defaultValue);
	}
	
	/**
	 * Set a named Integer in _prefs (SharedPreferences)
	 * 
	 * @category Boolean Preferences
	 */
	protected void setPreferenceBoolean(String key, boolean newValue)
	{
		_myApp.setPreferenceBoolean(key, newValue);
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
	protected String setLocalParameter(String key, String newValue)
	{
		return _myApp.setLocalParameter(key, newValue);
	}
	
	/**
	 * Set a parameter in the local HashMap
	 * 
	 * @category Local Parameters
	 */
	protected String getLocalParameter(String key)
	{
		return _myApp.getLocalParameter(key);
	}
	
	// UI Utility functions (should we have them here at all?)
    //----------------------------------------------------------------------------
	
	/**
	 * Going to next Activity with default transition
	 * 
	 * @category UI Utility
	 */
	protected void gotoNextActivity(Class<?> cls)
	{
		_bGoingNextState = true;
		startActivity( new Intent(this, cls) );
		overridePendingTransition(R.anim.slide_left, R.anim.slide_left);
	}
	
	/**
	 * Show busy dialog with a message and optional title
	 * 
	 * @category UI Utility
	 */
	protected void showBusyDialog(String title, String message)
	{
		//Close the old one (if any)
		closeBusyDialog();
		
		_busyDialog = ProgressDialog.show(this, title, message, true);
	}
	
	/**
	 * Show a simple message with just 1 button
	 * 
	 * @category UI Utility
	 */
	protected void showSimpleMessageDialog(String message)
	{
		closeBusyDialog();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message)
		       .setCancelable(false)
		       .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) { }
		        });
		AlertDialog alert = builder.create();
		alert.show();
	}
	/**
	 * @category UI Utility
	 */
	protected void showSimpleMessageDialog(int resID)
	{
		showSimpleMessageDialog(this.getString(resID));
	}
	
	/**
	 * Show busy dialog with a message and default title (empty title)
	 * Overload of the showBusyDialog(title,message) function
	 * 
	 * @category UI Utility
	 */
	protected void showBusyDialog(String message)
	{
		if (message != null)
		{
			showBusyDialog("", message);
			return;
		}
		
		//Nothing to do
		if (_busyDialog == null || _busyDialog.isShowing())
			return;
			
		//Show previously shown busy dialog
		_busyDialog.show();
	}
	
	/**
	 * Close busy dialog if it is being shown
	 * 
	 * @category UI Utility
	 */
	protected void closeBusyDialog()
	{
    	if (_busyDialog != null && _busyDialog.isShowing())
			_busyDialog.dismiss();
	}
	
	/**
	 * Hide soft keyboard
	 * 
	 * @category UI Utility
	 */
	protected void hideKeyboard(EditText editText)
	{
		if (editText == null) {
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		} else {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
		}
	}
	
	/**
	 * Show a simple Toast messsage
	 * 
	 * @category UI Utility
	 */
	protected void toast(String text)
	{
		Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT*2/3);
		toast.setGravity(Gravity.BOTTOM, 0, 0);
		toast.show();
	}
	
	/**
	 * Sub-class requires to implement this to check conditions allowing user
	 * to proceed to next step, and enable UI controls accordingly
	 * 
	 * @category WiFi Utility
	 */
	protected abstract boolean updateNextStepUI();
	
	/**
	 * Sub-class requires to implement this to transfer UI variables into _hashParameters
	 * 
	 * @category WiFi Utility
	 */
	protected abstract void updateHashVariables();
	
	// UI utilities
	//----------------------------------------------------------------------------

	/**
	 * Runnable object to be executed in UI thread. <br>
	 * Update UI after send activation time/wifi/acccount to NeTV
	 * 
	 * @see {@link #sendActivationTimeRunnable()}
	 */
    public Runnable updateUIRunnable = new Runnable()
	{
    	public void run()
		{
    		updateNextStepUI();
		}
	};
	    
	// Custom event
    //------------------------------------------------------------------
    
    /**
     * Override this to handle custom events fired by the main application class
     * 
     * @category CustomEvent
     */
	public void NewMessageEvent(Bundle parameters)
	{
		
	}
	
}

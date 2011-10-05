package com.chumby.NeTV;

import android.accounts.Account;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.chumby.Chumby;
import com.chumby.DaughterCardID;
import com.chumby.controlpanellite.chumbynetwork.ActivationByUserName;
import com.chumby.controlpanellite.chumbynetwork.AndroidStats;
import com.chumby.controlpanellite.chumbynetwork.Authorization;
import com.chumby.controlpanellite.chumbynetwork.Device;
import com.chumby.controlpanellite.chumbynetwork.XAPI;
import com.chumby.controlpanellite.chumbynetwork.XAPIRequest;
import com.chumby.http.HttpConnectionManager;
import com.chumby.util.ChumbyLog;

public class ActivityAccount extends ActivityBaseNeTV implements OnClickListener, TextWatcher
{
	//UI
	EditText 			txtUsername;
	EditText 			txtPassword;
	EditText 			txtDeviceName;
	CheckBox 			checkbox;
	Button 				btnNotNow;
	Button 				btnActivate;
	
	ActivationByUserName _activationModule;

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
		setContentView(R.layout.account_details);
		
    	//Setup UI
		txtUsername = (EditText)findViewById(R.id.txtChumbyUsername);
		txtUsername.addTextChangedListener(this);
    	txtPassword = (EditText)findViewById(R.id.txtChumbyPassword);
    	txtPassword.addTextChangedListener(this);
    	txtDeviceName = (EditText)findViewById(R.id.txtChumbyDeviceName);
    	txtDeviceName.addTextChangedListener(this);
    	checkbox = (CheckBox)findViewById(R.id.chkShowChumbyPassword);
    	checkbox.setOnClickListener(this);
    	btnNotNow = (Button)findViewById(R.id.button_not_now);
    	btnNotNow.setOnClickListener(this);
    	btnActivate = (Button)findViewById(R.id.button_next_account);
    	btnActivate.setOnClickListener(this);
    	
    	Chumby chumby = Chumby.getInstance();
    	chumby.guid = getPreferenceString(AppNeTV.PREF_CHUMBY_GUID, "");
    	chumby.firmwareVersion = getPreferenceString(AppNeTV.PREF_CHUMBY_HW_VERSION, "");
    	chumby.hardwareVersion = getPreferenceString(AppNeTV.PREF_CHUMBY_FW_VERSION, "");
        XAPI.setContext(getApplicationContext());
        XAPIRequest.flushQueue();
        HttpConnectionManager.flushQueue();
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
    	
    	//Manual IP override
		String _manual_ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_MANUAL, "");
		if (_manual_ipaddress.length() > 6)
			setPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, _manual_ipaddress);
    	
    	//Synchronize UI with NeTV (may not be necessary)
    	_myApp.sendAndroidJSChangeView(AppNeTV.NETV_UISTATE_ACCOUNT);
    	
    	String previousActivity = getPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, "");
    	
    	if ( previousActivity.equals(ActivityConfiguring.class.getName()) )
    	{
    		setPreferenceString(AppNeTV.PREF_CHUMBY_USERNAME, getPreferenceString(AppNeTV.PREF_CHUMBY_USERNAME, ""));
    		setPreferenceString(AppNeTV.PREF_CHUMBY_PASSWORD, getPreferenceString(AppNeTV.PREF_CHUMBY_PASSWORD, ""));
    		setPreferenceString(AppNeTV.PREF_CHUMBY_DEVICE_NAME, getPreferenceString(AppNeTV.PREF_CHUMBY_DEVICE_NAME, ""));
    	}
    	else
    	{
    		setPreferenceString(AppNeTV.PREF_CHUMBY_USERNAME, "");
    		setPreferenceString(AppNeTV.PREF_CHUMBY_PASSWORD, "");
    		setPreferenceString(AppNeTV.PREF_CHUMBY_DEVICE_NAME, this.getString(R.string.product_name));
    	}
    	   	    	    	    					
		//Determine if we're in portrait, and whether we're showing or hiding the keyboard with this toggle.
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean hasFullkeyboard = getResources().getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY;
        boolean isKeyboardHidden = getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES;

    	//Setup Keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (isPortrait && !hasFullkeyboard && isKeyboardHidden)
        {
        	imm.showSoftInput(txtUsername, InputMethodManager.SHOW_IMPLICIT);
        }
        else
        {
        	imm.hideSoftInputFromWindow(txtUsername.getWindowToken(), 0);
        }
        
        if (checkbox.isChecked())    	txtPassword.setTransformationMethod(SingleLineTransformationMethod.getInstance());
	    else 							txtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());

        //Update UI with previous settings
    	txtUsername.setText(getPreferenceString(AppNeTV.PREF_CHUMBY_USERNAME, ""));
    	txtPassword.setText(getPreferenceString(AppNeTV.PREF_CHUMBY_PASSWORD, ""));
    	txtDeviceName.setText(getPreferenceString(AppNeTV.PREF_CHUMBY_DEVICE_NAME, this.getString(R.string.product_name)));

    	//Keyboard focus
    	if (txtUsername.getText().equals(""))    		txtUsername.requestFocus();
    	else if (txtPassword.getText().equals("")) 		txtPassword.requestFocus();
    	else if (txtDeviceName.getText().equals(""))	txtDeviceName.requestFocus();
    	
    	//Transfer information to Chumby's classes
    	DaughterCardID dcid = DaughterCardID.getInstance();
    	for (int i=0; i<AppNeTV.DCID.length; i++)
    		dcid.setProperty(AppNeTV.DCID[i], getPreferenceString(AppNeTV.DCID[i], ""));
    	Chumby chumby = Chumby.getInstance();
    	chumby.guid = getPreferenceString(AppNeTV.PREF_CHUMBY_GUID, "");
    	Log.d(TAG, this.getLocalClassName() + ": GUID " + getPreferenceString(AppNeTV.PREF_CHUMBY_GUID, "") );
    	    	
    	updateNextStepUI();
        updateHashVariables();
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
    	//Hide the keyboard 
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(txtPassword.getWindowToken(), 0);
		
		setPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, ActivityAccount.class.getName());
		
		super.onPause();
    }
	
	 /**
	 * Disable Back button
	 * 
	 * @category Initialization
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
			return true;

	     return super.onKeyDown(keyCode, event);    
	}
    
    // UI Events
	//----------------------------------------------------------------------------
        
    public void onClick(View arg0)
    {
    	if (arg0.getId() == R.id.chkShowChumbyPassword)
    	{
	    	if (checkbox.isChecked())    	txtPassword.setTransformationMethod(SingleLineTransformationMethod.getInstance());
		    else 							txtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance()); 
    	}
    	else if (arg0.getId() == R.id.button_not_now)
    	{
    		Log.i(TAG, this.getLocalClassName() + ": not now");
    		gotoNextActivity(ActivityRemoteMain.class);
    		overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
    	}
    	else if (arg0.getId() == R.id.button_next_account)
    	{
    		onbtnActivate();
    	}
    }
    
    private void onbtnActivate()
    {
    	String userName = txtUsername.getText().toString().trim();
    	String password = txtPassword.getText().toString().trim();
    	String deviceName = txtDeviceName.getText().toString().trim();
    	
    	//Sanity checks
    	if (userName.length() < 4) {
    		showSimpleMessageDialog(R.string.chumby_username_too_short);
    		return;
    	}
    	if (deviceName.length() < 1) {
    		showSimpleMessageDialog(R.string.chumby_device_name_too_short);
    		return;
    	}
    	
    	//Busy dialog
    	showBusyDialog(this.getString(R.string.please_wait_while_activating));
    	
    	//We should already have Internet access here
    	_activationModule = new ActivationByUserName();
    	
       	Handler handler = new Handler()
       	{
			public void handleMessage(Message message)
			{			
				switch(message.what)
				{
					case ActivationByUserName.ACTIVATED:
						onSuccessfulActivation();
						break;
					case ActivationByUserName.NOT_ACTIVATED_BAD_USER:
						badRegistrationUser();
						break;
					case ActivationByUserName.NOT_ACTIVATED_BAD_DEVICE:
						badRegistrationDevice();
						break;
					case ActivationByUserName.CANT_ACTIVATE:
						badTryRegister();
				}
			}
    	};
    	_activationModule.activate(userName,password,deviceName,handler);    	
    }
    
    // Activation Events
	//----------------------------------------------------------------------------
    
    protected void onSuccessfulActivation()
    {   	
    	try {
    		Log.i(TAG, this.getLocalClassName() + ": successful activation");
			Thread.sleep(2000); //sleep two seconds before authorizing
		} catch (Exception e) {
			ChumbyLog.i(TAG+".gotTryRegister(): failed to sleep");
		}
		authorize();
	}
	
    protected void badTryRegister()
	{
		Log.e(TAG, "badTryRegister(): not activated");
	}
	
	protected void badRegistrationUser()
	{	   	
		Log.i(TAG, this.getLocalClassName() + ": badRegistrationUser()");
		showSimpleMessageDialog(R.string.invalid_user);
	}

	protected void badRegistrationDevice()
	{	   	
		Log.i(TAG, this.getLocalClassName() + ": badRegistrationDevice()");
		showSimpleMessageDialog(R.string.duplicate_device);
	}

    //-----------
	
	protected void authorize()
	{
		Log.i(TAG, this.getLocalClassName() + ".authorize()");
    	Handler handler = new Handler() {
			public void handleMessage(Message message) {
				switch(message.what) {
					case Authorization.AUTHORIZED:
						gotAuthorized(message.what,message.obj);
						break;
					case Authorization.NOT_AUTHORIZED:
						badAuthorization(message.what,message.obj);
						break;
					case Authorization.CANT_AUTHORIZE:
						cantAuthorize(message.what,message.obj);
						break;
				}
			}
    	};
        (new Authorization()).authorize(handler);
    }

    protected void gotAuthorized(int response,Object obj) {
    	Log.i(TAG, this.getLocalClassName() + ".gotAuthorized()");
    	authenticate();
    }
    
    protected void badAuthorization(int response,Object obj) {
    	Log.i(TAG, this.getLocalClassName() + ".badAuthorization()");
    	//registerStep1();
    	//registerByUserName();
    }

    protected void cantAuthorize(int response,Object obj) {
    	ChumbyLog.i(TAG+".cantAuthorize()");
    	////errorAlert(R.string.cant_authorize);
    }

    //-----------
    
    protected void authenticate()
    {
    	//ChumbyLog.i(TAG+".authenticate()");
		Handler authHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case XAPI.AUTHENTICATED:
						didAuthenticate(message.what,message.obj);
						break;
					case XAPI.NOT_AUTHENTICATED:
						badAuthentication(message.what,message.obj);
						break;
				}
			}
		};
		XAPI.getInstance().authorizeDLA(Authorization.chumbyName,authHandler);   	
    }

    protected void didAuthenticate(int response,Object obj) {
    	Log.i(TAG, this.getLocalClassName() + ".didAuthenticate()");
    	fetchDevice();
    	//Persistence.currentAccount = new Account();
    	//fetchAccount(Persistence.currentAccount);
    	AndroidStats.sendStats(this);
    }
    
    protected void badAuthentication(int response,Object obj) {
    	ChumbyLog.i(TAG+".badAuthentication()");
    	//errorAlert(R.string.cant_authenticate_device);
    }
    
    protected void gotAccount(Account account) {
    	Log.i(TAG, this.getLocalClassName() + ".gotAccount(): "+account);
    	fetchDevice();
    }

    //-----------
    
    protected void fetchDevice()
    {
    	//ChumbyLog.i(TAG+".fetchDevice()");
    	Handler handler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case Device.GOT_INFO:
						gotDevice(message.what,message.obj);
						break;
					case Device.BAD_INFO:
						badDevice(message.what,message.obj);
						break;
				}
			}
    	};
        Device.getInstance().fetch(handler);
    }
 
    protected void gotDevice(int response, Object obj)
    {
    	Log.i(TAG, this.getLocalClassName() + ".gotDevice()");
    	
    	// Reset/reload the browser (to reflect new activation status)
		if (_useTCP) {
			_myApp.ResetUrlHTTP(null);
		} else {
			_myApp.sendNeTVBrowserReset();
			_myApp.sendNeTVBrowserReset();
			_myApp.sendNeTVBrowserReset();
		}
		
    	closeBusyDialog();
    	gotoNextActivity(ActivityRemoteMain.class);
		overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
    }
    
    protected void badDevice(int response,Object value) {
    	Log.i(TAG, this.getLocalClassName() + ".badDevice()");
    	//errorAlert(R.string.cant_fetch_device);
    }
    
    // UI Events
	//----------------------------------------------------------------------------
    
    public void afterTextChanged(Editable s)
    {
    	
    }
 
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {
        
    }
 
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {
    	updateHashVariables();
    	updateNextStepUI();
    }
    
    // UI Utility functions
	//----------------------------------------------------------------------------
    
    /**
	 * @category UI Utility
	 */
    public boolean updateNextStepUI()
    {
    	boolean enableNext = txtPassword.getText().length() > 1 && txtUsername.getText().length() > 1 && txtDeviceName.getText().length() > 1;
   		btnActivate.setEnabled(enableNext);
   			
   		return enableNext;
    }
    
    /**
	 * @category UI Utility
	 */
    protected void updateHashVariables()
    {
    	setPreferenceString(AppNeTV.PREF_CHUMBY_USERNAME, txtUsername.getText().toString().trim());
    	setPreferenceString(AppNeTV.PREF_CHUMBY_PASSWORD, txtPassword.getText().toString().trim());
    	setPreferenceString(AppNeTV.PREF_CHUMBY_DEVICE_NAME, txtDeviceName.getText().toString().trim());
    	
    	//Send to NeTV's Android configuration page
    	String username = getPreferenceString(AppNeTV.PREF_CHUMBY_USERNAME, "");
    	String password = getPreferenceString(AppNeTV.PREF_CHUMBY_PASSWORD, "");
    	String devicename = getPreferenceString(AppNeTV.PREF_CHUMBY_DEVICE_NAME, "");
    	String passwordMask = "";
    	for (int i=0; i<password.length(); i++)
    		passwordMask += "xx";
    	_myApp.sendAndroidJSAccountDetails(username, passwordMask, devicename);
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
		// Filter out loopbacks
		String addressString = (String) parameters.get(MessageReceiver.MESSAGE_KEY_ADDRESS);
		parameters.remove(MessageReceiver.MESSAGE_KEY_ADDRESS);
		if (addressString == null || addressString.length() < 5 || _myApp.isMyIP(addressString))
			return;
		//if (!addressString.equals(AppNeTV.DEFAULT_IP_ADDRESS))
		//	return;
		
		// Not a valid message
		if (!parameters.containsKey(MessageReceiver.COMMAND_MESSAGE))
			return;
		String commandName = (String) parameters.get(MessageReceiver.COMMAND_MESSAGE);
		parameters.remove(MessageReceiver.COMMAND_MESSAGE);
		if (commandName == null || commandName.length() < 2)
			return;
		commandName = commandName.toUpperCase();
		
		//----------------------------------------------------------------------
		
		if (commandName.equals(MessageReceiver.COMMAND_Handshake.toUpperCase()))
		{
			//Echo of myself. Ignored
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_Android.toUpperCase()))
		{
			//Echo of myself. Ignored
			return;
		}
		
		Log.d(TAG, "Received command message: " + commandName);
	}

}

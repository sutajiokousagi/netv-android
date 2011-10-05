package com.chumby.NeTV;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

public class ActivityWifiDetails extends ActivityBaseNeTV implements OnItemSelectedListener, OnClickListener, TextWatcher
{	
	//UI
	EditText 			txtSSID;
	EditText 			txtPassword;
	CheckBox 			checkbox;
	Spinner 			spinnerEncryption;
	Button 				btnBack;
	Button 				btnNext;

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
		setContentView(R.layout.wifi_details);
		
		//spinner
		spinnerEncryption = (Spinner)findViewById(R.id.spinnerEncryption);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, AppNeTV.SECURITY);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEncryption.setAdapter(spinnerAdapter);
		
		//Setup UI
		txtSSID = (EditText)findViewById(R.id.txtSSID);
    	txtPassword = (EditText)findViewById(R.id.txtPassword);
    	checkbox = (CheckBox)findViewById(R.id.chkShowWifiPassword);
    	checkbox.setOnClickListener(this);
    	btnBack = (Button)findViewById(R.id.button_back_wifi);
    	btnBack.setOnClickListener(this);
    	btnNext = (Button)findViewById(R.id.button_next_wifi);
    	btnNext.setOnClickListener(this);
    	
    	String previousActivity = getPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, "");
    	
    	if (previousActivity.equals(ActivityWifiList.class.getName()))
    	{
    		btnNext.setText(this.getString(R.string.button_next));
    	}
    	else
    	{
    		btnNext.setText(this.getString(R.string.button_activate));
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
    	
    	//Synchronize UI with NeTV
    	_myApp.sendAndroidJSChangeView(AppNeTV.NETV_UISTATE_WIFI_DETAILS);
    	
    	if (checkbox.isChecked())    	txtPassword.setTransformationMethod(SingleLineTransformationMethod.getInstance());
	    else 							txtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        
    	EditText currentFocus = null;
    	
    	//Selected "Other..." wifi network
    	String selectedSSID = getPreferenceString(AppNeTV.PREF_WIFI_SSID, "");
    	Log.d(TAG, this.getLocalClassName() + ": selectedSSID " + selectedSSID);
		if (selectedSSID.equals("") || selectedSSID.equals(this.getString(R.string.wifi_network_other)))
		{
			setEncryptionType( getPreferenceString(AppNeTV.PREF_WIFI_CAPABILITIES, "") );
			currentFocus = txtSSID;
			txtSSID.setText("");
			txtSSID.requestFocus();
		}
    	//Update UI with previous settings
		else
		{
			//The server is getting some error, we need to display it to the user here.
			setEncryptionType( getPreferenceString(AppNeTV.PREF_WIFI_CAPABILITIES, "") );
			currentFocus = txtPassword;
			checkbox.setEnabled(true);
			txtSSID.setText( getPreferenceString(AppNeTV.PREF_WIFI_SSID, "") );
			txtPassword.setText( getPreferenceString(AppNeTV.PREF_WIFI_PASSWORD, "") );
			txtPassword.requestFocus();
		}
		
		//Determine if we're in portrait, and whether we're showing or hiding the keyboard with this toggle.
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean hasFullkeyboard = getResources().getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY;
        boolean isKeyboardHidden = getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES;
        
        //Setup Keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (isPortrait && !hasFullkeyboard && isKeyboardHidden)
        {
        	imm.showSoftInput(currentFocus, InputMethodManager.SHOW_IMPLICIT);
        }
        else
        {
        	imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
        
        //UI events
        txtSSID.addTextChangedListener(this);
    	txtPassword.addTextChangedListener(this);
    	spinnerEncryption.setOnItemSelectedListener(this);
    				
        updateNextStepUI();       
        updateHashVariables();
        
        _myApp.sendNeTVBrowserJavaScript("function android_timeout() { fDbg2('New timeout. Do nothing.');	}");
        _myApp.sendNeTVBrowserJavaScript("function android_timeout() { fDbg2('New timeout. Do nothing.');	}");
        _myApp.sendNeTVBrowserJavaScript("function android_timeout() { fDbg2('New timeout. Do nothing.');	}");      
    }
    
    @Override
    public void onPause()
    {
    	setPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, ActivityWifiDetails.class.getName());
    	    	
    	super.onPause();
    }
    
    // UI Events
	//----------------------------------------------------------------------------
    
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        updateNextStepUI();
        updateHashVariables();
    }
 
    public void onNothingSelected(AdapterView<?> parent)
    {
    	
    }
    
    //-----------
    
    public void onClick(View arg0)
    {
    	if (arg0.getId() == R.id.chkShowWifiPassword)
    	{
    		int selStart = txtPassword.getSelectionStart();
    		int selEnd = txtPassword.getSelectionEnd();
	    	if (checkbox.isChecked())    	txtPassword.setTransformationMethod(SingleLineTransformationMethod.getInstance());
		    else 							txtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
	    	txtPassword.setSelection(selStart,selEnd);
    	}
    	else if (arg0.getId() == R.id.button_back_wifi)
    	{
    		finish();
    	}
    	else if (arg0.getId() == R.id.button_next_wifi)
    	{   		
        	String previousActivity = getPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, "");
        	if (previousActivity.equals(ActivityWifiList.class.getName()))
        	{
    			String activated = getPreferenceString(AppNeTV.PREF_CHUMBY_ACTIVATED, "false");
    			if (activated.equals("false"))		gotoNextActivity(ActivityAccount.class);
    			else								gotoNextActivity(ActivityConfiguring.class);
        	}
        	else
        	{
        		//Because we were coming back from some error in ActivityActivation
        		gotoNextActivity(ActivityConfiguring.class);
        	}
    	}
    	updateNextStepUI();
    }
    
    //-----------
    
    public void afterTextChanged(Editable s)
    {
    	
    }
 
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {
        
    }
 
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {
    	updateNextStepUI();
    	updateHashVariables();
    }
    
    // UI Utility functions
	//----------------------------------------------------------------------------
    
    /**
     * Update UI elements with correct configurations given Wifi 'capabilities' string
	 * @category UI Utility
	 */
    public void setEncryptionType(String capabilities)
    {
    	if (capabilities == null || capabilities == "")
    	{
    		spinnerEncryption.setSelection(0);
    		updateNextStepUI();
    		txtSSID.requestFocus();
    		return;
    	}
    	
    	//We go backwards to WPA2 has higher priority
    	for (int idx=AppNeTV.SECURITY.length-1; idx>=0; idx--)
    	{
    		String regEx = ".*\\b" + AppNeTV.SECURITY[idx] + "\\b.*";
    		if (!capabilities.matches(regEx))
    			continue;
    		spinnerEncryption.setSelection(idx);
    		updateNextStepUI();
    		
    		if (txtSSID.getText().length() > 1)		txtPassword.requestFocus();
    		else									txtSSID.requestFocus();
    		break;
    	}
    }
    
    /**
	 * @category UI Utility
	 */
    protected boolean updateNextStepUI()
    {
    	int index = spinnerEncryption.getSelectedItemPosition();
    	boolean enableNext = (txtSSID.getText().length() > 1);

    	//No encryption
		txtPassword.setEnabled(index != 0);
		checkbox.setEnabled(index != 0);
		
		//Additionally password must not be blank
    	if (index != 0)
    		enableNext = enableNext && (txtPassword.getText().length() > 1);

    	btnNext.setEnabled(enableNext);
    	return enableNext;
    }
    
    /**
	 * @category UI Utility
	 */
    protected void updateHashVariables()
    {
    	setPreferenceString(AppNeTV.PREF_WIFI_SSID, txtSSID.getText().toString().trim());
    	setPreferenceString(AppNeTV.PREF_WIFI_PASSWORD, txtPassword.getText().toString().trim());
    	
    	int selectedSecurity = spinnerEncryption.getSelectedItemPosition();
    	if (selectedSecurity < 0 || selectedSecurity >= AppNeTV.AUTHENTICATIONS.length)
    		selectedSecurity = 0;
    	
    	//This is only applicable to encrypted non-"Other..." network
    	setPreferenceString(AppNeTV.PREF_WIFI_ENCRYPTION, AppNeTV.ENCRYPTIONS[selectedSecurity]);
		if (getPreferenceString(AppNeTV.PREF_WIFI_CAPABILITIES, "").contains("CCMP") || getPreferenceString(AppNeTV.PREF_WIFI_CAPABILITIES, "").contains("AES"))
			setPreferenceString(AppNeTV.PREF_WIFI_ENCRYPTION, "AES");
		
		setPreferenceString(AppNeTV.PREF_WIFI_AUTHENTICATION, AppNeTV.AUTHENTICATIONS[selectedSecurity]);
		
    	//Send to NeTV's Android configuration page
    	String ssid = getPreferenceString(AppNeTV.PREF_WIFI_SSID, "");
    	String password = getPreferenceString(AppNeTV.PREF_WIFI_PASSWORD, "");
    	String enc = getPreferenceString(AppNeTV.PREF_WIFI_ENCRYPTION, "");
    	String auth = getPreferenceString(AppNeTV.PREF_WIFI_AUTHENTICATION, "");
    	String passwordMask = "";
    	for (int i=0; i<password.length(); i++)
    		passwordMask += "x";
    	_myApp.sendAndroidJSWifiDetails(ssid, passwordMask, enc, auth);
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
		//String addressString = (String) parameters.get(MessageReceiver.MESSAGE_KEY_ADDRESS);
		parameters.remove(MessageReceiver.MESSAGE_KEY_ADDRESS);
		
		if (!parameters.containsKey(MessageReceiver.COMMAND_MESSAGE))
			return;
		String commandName = (String) parameters.get(MessageReceiver.COMMAND_MESSAGE);
		if (commandName == null)
			commandName = "";
		commandName = commandName.toUpperCase();
		parameters.remove(MessageReceiver.COMMAND_MESSAGE);
		
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
	
	
	/**
	 * @category Application Logic
	 */
	private void initializeSequence()
	{
		//Send to NeTV's Android configuration page
    	String ssid = getPreferenceString(AppNeTV.PREF_WIFI_SSID, "");
    	String password = getPreferenceString(AppNeTV.PREF_WIFI_PASSWORD, "");
    	String enc = getPreferenceString(AppNeTV.PREF_WIFI_ENCRYPTION, "");
    	String auth = getPreferenceString(AppNeTV.PREF_WIFI_AUTHENTICATION, "");
    	String passwordMask = "";
    	for (int i=0; i<password.length(); i++)
    		passwordMask += "x";
    	
		//Keep alive command
    	_myApp.sendAndroidJSWifiDetails(ssid, passwordMask, enc, auth);
		_handler.postDelayed(initializeSequenceRunnable, 1500);
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



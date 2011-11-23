package com.chumby.NeTV;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

public class ActivityTextInput extends ActivityBaseNeTV implements OnClickListener, AsyncImageLoader.AsyncImageCallback
{
	// Screen resolution;
	DisplayMetrics _displayMetrics;
	int _screenWidth;
	int _screenHeight;

	// UI
	ImageView _screenshotImageView;
	Matrix _screenshotImageViewMatrix;
	AsyncImageLoader _imageLoader;
	
	//Flags
	String _ipaddress;
	String _screenshotPath;
	Bitmap _screenshotImage;
		
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
		Log.d(TAG, "ActivityTextInput onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_textinput);

		// Screen resolution
		_displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(_displayMetrics);
		_screenWidth = _displayMetrics.widthPixels;
		_screenHeight = _displayMetrics.heightPixels;

		_screenshotImageViewMatrix = null;
		_screenshotImageView = (ImageView)findViewById(R.id.screenshot);
		//_screenshotImage.setOnTouchListener(this);
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
		
		// IP address for unicast transmission
		_ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");

		// Synchronize UI with NeTV (currently do nothing on NeTV)
		_myApp.sendAndroidJSChangeView(AppNeTV.NETV_UISTATE_TEXTINPUT);

		// Setup Keyboard
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(_screenshotImageView, InputMethodManager.SHOW_IMPLICIT);

		// Asynchronous image loader
		if (_ipaddress.length() <= 0)
			return;

		_screenshotPath = "http://" + _ipaddress + "/tmp/netvserver/focused_input.png";
		_imageLoader = new AsyncImageLoader(_screenshotPath, this);
		initializeSequence();
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
		setPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, ActivityTextInput.class.getName());

		super.onPause();
	}
	
	// UI Events
	// ----------------------------------------------------------------------------

	/**
	 * Customize Back button & send keyboard events to NeTV
	 * 
	 * @category UI Events
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		// Go back to previous screen
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			finish();
			return true;
		}

		// Do not send special keys to NeTV
		if (keyCode == KeyEvent.KEYCODE_MENU
				|| keyCode == KeyEvent.KEYCODE_SEARCH
				|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
				|| keyCode == KeyEvent.KEYCODE_VOLUME_UP
				|| keyCode == KeyEvent.KEYCODE_HOME
				|| keyCode == KeyEvent.KEYCODE_CAMERA
				|| keyCode == KeyEvent.KEYCODE_POWER)
			return super.onKeyDown(keyCode, event);

		String buttonName = "" + event.getDisplayLabel();
		if (buttonName.length() > 0)
			_myApp.sendSimpleKey("" + event.getDisplayLabel());
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		// This will disable keyboard on Nexus S & alike
		if (keyCode == KeyEvent.KEYCODE_MENU)
			return true;

		return super.onKeyLongPress(keyCode, event);
	}

	/**
	 * Buttons event
	 * 
	 * @category UI Events
	 */
	public void onClick(View v)
	{
		//if (v.getId() == R.id.btn_widget)
		//	_myApp.sendSimpleButton("widget");
	}

	// UI Utility functions
	// ----------------------------------------------------------------------------

	/**
	 * @category UI Utility
	 */
	public boolean updateNextStepUI() {
		return true;
	}

	/**
	 * @category UI Utility
	 */
	protected void updateHashVariables() {

	}

	// Custom event
	// ------------------------------------------------------------------

	/**
	 * Handle custom events fired by the main application class after
	 * pre-processing the message
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

		if (commandName.equals(MessageReceiver.COMMAND_TextInput.toUpperCase()))
		{
			String id = (String)parameters.get("id");
			String value = (String)parameters.get(MessageReceiver.MESSAGE_KEY_VALUE);
			if (id == null || id.length() <= 0)
			{
				Log.d(TAG, "Received " + commandName + ": (defocus)");
				finish();
				return;
			}
			
			//Refresh screenshot
			_imageLoader.reload();
			Log.d(TAG, "Received " + commandName + ": " + id + " -> " + value);
		}
		else if (commandName.equals(MessageReceiver.COMMAND_Key.toUpperCase()))
		{
			//Ignore status message
		}

		Log.d(TAG, "Received command message: " + commandName);
	}
	
	// Network
	// ----------------------------------------------------------------------------

	/**
	 * @category Network
	 */
	public void onImageReceived(String url, Bitmap bm)
	{
		if (bm == null) {
			System.err.println("Could not load picture '" + url + "'!");
			return;
		}
		
		// Filter out the pink color
		int width = bm.getWidth();
		int height = bm.getHeight();
		int pink = Color.argb(255, 240,0,240);
		int replace = Color.argb(0,0,0,0);
		for (int x=0; x<width; x++)
			for (int y=0; y<height; y++)
				if (bm.getPixel(x, y) == pink)
					bm.setPixel(x, y, replace);
		
		// We cheated and let Android handle the transformation for us
		_screenshotImage = bm;

		// We are not currently in UI thread, we need to post it to UI thread
		_handler.post(updateScreenshot);
	}
	
	// ----------------------------------------------------------------------------

	private Runnable updateScreenshot = new Runnable()
	{
		public void run()
		{
			if (_screenshotImage != null)
				_screenshotImageView.setImageBitmap(_screenshotImage);
		}
	};

	/**
	 * @category Application Logic
	 */
	private void initializeSequence()
	{
		//Refresh screenshot
		_imageLoader.reload();
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
}

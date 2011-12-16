package com.bunniestudios.NeTV;

import java.io.File;
import java.io.InputStream;

import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class ActivityRemoteMain extends ActivityBaseNeTV implements OnClickListener
{
	// Screen resolution;
	DisplayMetrics _displayMetrics;
	int _screenWidth;
	int _screenHeight;

	// UI
	TextView _statusTextView;
	TextView _warningTextView;

	// Flags
	String _ipaddress;
	int _retryCounter;
	boolean _triedConnectNeTV;

	// Cool features
	boolean _doingCoolStuff;
	String pendingUrl;
	String pendingMIMEType;
	Uri pendingUri;

	// Upload
	long _uploadTime;
	double _uploadSpeed;
	String _uploadFilename;
	boolean _performed_Upload;
	boolean _pass_Upload;
	boolean _shownPhoto;
	
	private static final int ACT_MENU_ID = Menu.FIRST + 1;
	private static final int PREF_MENU_ID = Menu.FIRST + 2;
	
	protected static final String IMAGE_HTML = "<html>" +
				"<script type='text/javascript'>function load() { center_img.height = window.innerHeight; }</script>" +
				"<body style='margin:0; overflow:hidden;' onLoad='load()' onresize='load()'>" +
				"<table width='100%' height='100%' cell-padding='0' cell-spacing='0'>" +
				"<tr><td width='100%' height='100%' align='center' valign='middle'>" +
				"<img id='center_img' height='window.innerHeight' src='xxxxxxxxxx' />" +
				"</tr></td></table></body></html>";

	// http://loopj.com/android-async-http/
	AsyncHttpClient _httpClient;
	
	// Private helper classes (this is awesome)
	// ----------------------------------------------------------------------------
	
	private AsyncHttpResponseHandler multitabHandler = new AsyncHttpResponseHandler()
	{
	    @Override
	    public void onSuccess(String response)
	    {
	    	Log.d(TAG, "MultiTabHTTP: " + response);
	    	String status = response.split("</status>")[0].split("<status>")[1].trim();
	    	if (!status.equals("1"))
	    		Log.e(TAG, "MultiTabHTTP: something is not right");	    
	   		_shownPhoto = true;
	    }
	    
	    @Override
	    public void onFailure(Throwable error)
	    {
	    	Log.e(TAG, "MultiTabHTTP failed");
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
		Log.d(TAG, "ActivityRemoteMain onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_remote);
		
		//Version check
		_warningTextView = (TextView)findViewById(R.id.textViewVersionWarning);
		String minAndroid = getPreferenceString(AppNeTV.PREF_CHUMBY_MIN_ANDROID, "");
		if (minAndroid != null && minAndroid.length() > 3)
			if (_myApp.requiresAndroidUpdate(minAndroid))
			{
				Log.e(TAG, this.getLocalClassName() + ": need update to minimum version " + minAndroid);
				_warningTextView.setText(this.getString(R.string.version_warning_short));
				_warningTextView.setVisibility(View.VISIBLE);
			}
			else
			{
				_warningTextView.setVisibility(View.GONE);
			}

		// HTTP client for uploading photos/files
		_httpClient = new AsyncHttpClient();

		// Screen resolution
		_displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(_displayMetrics);
		_screenWidth = _displayMetrics.widthPixels;
		_screenHeight = _displayMetrics.heightPixels;
		
		reset();
		
		// if initiated from a share http:// link event (file manager apps)
		String url = getIntent().getDataString();
		if (url != null && url.length() > 5) {
			pendingUrl = url;
			Log.d(TAG, this.getLocalClassName() + ": has pending URL: " + pendingUrl);
			_doingCoolStuff = true;
		}

		// if initiated from a share image event (gallery app)
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			Uri uri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
			if (uri != null) {
				pendingUri = uri;
				Log.d(TAG, this.getLocalClassName() + ": has pending URI: " + uri.getPath());
				_doingCoolStuff = true;
			}
		}

		//We are expecting "images/*" here
		pendingMIMEType = getIntent().getType();
		if (pendingMIMEType != null)	Log.d(TAG, this.getLocalClassName() + ": pendingMIMEType: " + pendingMIMEType);
		else							pendingMIMEType = "";
		
		// Remote control buttons
		((Button) this.findViewById(R.id.btn_controlpanel)).setOnClickListener(this);
		((Button) this.findViewById(R.id.btn_widget)).setOnClickListener(this);
		((Button) this.findViewById(R.id.btn_up)).setOnClickListener(this);
		((Button) this.findViewById(R.id.btn_down)).setOnClickListener(this);
		((Button) this.findViewById(R.id.btn_left)).setOnClickListener(this);
		((Button) this.findViewById(R.id.btn_right)).setOnClickListener(this);
		((Button) this.findViewById(R.id.btn_center)).setOnClickListener(this);
		((Button) this.findViewById(R.id.btn_brwsr)).setOnClickListener(this);
		((ImageView) this.findViewById(R.id.btn_back)).setOnClickListener(this);
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
		_statusTextView = (TextView) findViewById(R.id.textViewRemoteTitle);
		_statusTextView.setText(_ipaddress);

		// Synchronize UI with NeTV (currently do nothing on NeTV)
		_myApp.sendAndroidJSChangeView(AppNeTV.NETV_UISTATE_REMOTE);

		// Determine if we're in portrait, and whether we're showing or hiding
		// the keyboard with this toggle.
		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		boolean hasFullkeyboard = getResources().getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY;
		boolean isKeyboardHidden = getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES;

		// Setup Keyboard
		// InputMethodManager imm =
		// (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (isPortrait && !hasFullkeyboard && isKeyboardHidden) {

		} else {

		}

		updateNextStepUI();
		updateHashVariables();

		initializeSequence();

		// Some fancy intro animation on the buttons
		// Note: if animation is disabled by the system (low-end devices)
		// this won't be running. Too bad.
		// -----------------------------------------

		Button button = (Button) findViewById(R.id.btn_left);
		int offset = 80; // button.getHeight() doesn't work until it is rendered
		int duration = 1200;
		int durationAlpha = 1800;

		Animation animation = new TranslateAnimation(-offset, 0, 0, 0);
		animation.setDuration(duration);
		animation.setInterpolator(new DecelerateInterpolator());
		button = (Button) findViewById(R.id.btn_left);
		button.startAnimation(animation);

		animation = new TranslateAnimation(offset, 0, 0, 0);
		animation.setDuration(duration);
		animation.setInterpolator(new DecelerateInterpolator());
		button = (Button) findViewById(R.id.btn_right);
		button.startAnimation(animation);

		animation = new TranslateAnimation(0, 0, -offset, 0);
		animation.setDuration(duration);
		animation.setInterpolator(new DecelerateInterpolator());
		button = (Button) findViewById(R.id.btn_up);
		button.startAnimation(animation);

		animation = new TranslateAnimation(0, 0, offset, 0);
		animation.setDuration(duration);
		animation.setInterpolator(new DecelerateInterpolator());
		button = (Button) findViewById(R.id.btn_down);
		button.startAnimation(animation);

		animation = new AlphaAnimation(0, 1);
		animation.setDuration(durationAlpha);
		button = (Button) findViewById(R.id.btn_center);
		button.startAnimation(animation);
		
		animation = new AlphaAnimation(0, 1);
		animation.setDuration(durationAlpha);
		button = (Button) findViewById(R.id.btn_controlpanel);
		button.startAnimation(animation);
		
		animation = new AlphaAnimation(0, 1);
		animation.setDuration(durationAlpha);
		button = (Button) findViewById(R.id.btn_brwsr);
		button.startAnimation(animation);

		animation = new AlphaAnimation(0, 1);
		animation.setDuration(durationAlpha);
		button = (Button) findViewById(R.id.btn_widget);
		button.startAnimation(animation);
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
		setPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, ActivityRemoteMain.class.getName());

		// This could be due to screen time out or press home button or power button
		if (_doingCoolStuff)
		{
			//_myApp.sendMultiTabCommand("", "hide");
			_myApp.MultiTabHTTP("", "hide", null);
			Log.d(TAG, this.getLocalClassName() + ": return UI to default tab");
			finish();
		}
		reset();

		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		String activated = getPreferenceString(AppNeTV.PREF_CHUMBY_ACTIVATED, "false");		//why is this always 'true'?
		if (!activated.equalsIgnoreCase("true"))
		{
			menu.add(Menu.NONE, ACT_MENU_ID, Menu.NONE, this.getString(R.string.button_activate))
				.setIcon(android.R.drawable.ic_menu_manage)
				.setAlphabeticShortcut('a');
		}
		
		menu.add(Menu.NONE, PREF_MENU_ID, Menu.NONE, this.getString(R.string.button_preferences))
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setAlphabeticShortcut('e');

		return(super.onCreateOptionsMenu(menu));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case ACT_MENU_ID:
				startActivity(new Intent(this, ActivityAccount.class));
				return(true);
			
			case PREF_MENU_ID:
				startActivity(new Intent(this, NeTVWidgetPrefs.class));
				return(true);
		}

		return(super.onOptionsItemSelected(item));
	}

	/**
	 * Reset flags & states
	 * 
	 * @category Initialization
	 */
	public void reset()
	{
		_retryCounter = 0;
		_triedConnectNeTV = false;

		_doingCoolStuff = false;
		pendingMIMEType = "";
		pendingUrl = "";
		pendingUri = null;
		_uploadSpeed = 0;
		_uploadFilename = "";
		_pass_Upload = false;
		_performed_Upload = false;
		_shownPhoto = false;
	}
	
	/**
	 * Go back to previous activity depends on what we are doing
	 * 
	 * @category Initialization
	 */
	public boolean goBack()
	{
		// Return to browser/photo gallery/external app if we are coming from there
		if (_doingCoolStuff) {
			finish();
			return false;
		}

		gotoNextActivity(ActivitySplash.class);
		overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
		return true;
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
		// Go back to Splash screen
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			/*
			// Return to browser/photo gallery/external app if we are coming from there
			if (_doingCoolStuff)
				return super.onKeyDown(keyCode, event);

			gotoNextActivity(ActivitySplash.class);
			overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
			return true;
			*/
			return goBack();
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
		if (v.getId() == R.id.btn_controlpanel)			_myApp.sendSimpleButton("cpanel");
		else if (v.getId() == R.id.btn_widget)			_myApp.sendSimpleButton("widget");
		else if (v.getId() == R.id.btn_up)				_myApp.sendSimpleButton("up");
		else if (v.getId() == R.id.btn_down)			_myApp.sendSimpleButton("down");
		else if (v.getId() == R.id.btn_left)			_myApp.sendSimpleButton("left");
		else if (v.getId() == R.id.btn_right)			_myApp.sendSimpleButton("right");
		else if (v.getId() == R.id.btn_center)			_myApp.sendSimpleButton("center");
		else if (v.getId() == R.id.btn_brwsr)			onBrowserButton();
		else if (v.getId() == R.id.btn_back)			goBack();
		
		//Feedback
		_vibrator.vibrate(120);

		// Sound feedback
		if (ENABLE_SOUND_FX)
		{
			_mediaPlayer = MediaPlayer.create(this, R.raw.pop);
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
		}
	}


	/*
	 * @category UI Events
	 */
	private void onBrowserButton() {
		gotoNextActivity(ActivityBrowser.class);
	}

	// UI Utility functions
	// ----------------------------------------------------------------------------

	/**
	 * @category UI Utility
	 */
	public boolean updateNextStepUI() {
		boolean enableNext = false;

		return enableNext;
	}

	/**
	 * @category UI Utility
	 */
	protected void updateHashVariables() {

	}

	/**
	 * @category UI Utility
	 */
	protected void setStatusMessage(String text) {
		setStatusMessage(text, null);
	}

	/**
	 * @category UI Utility
	 */
	protected void setStatusMessage(String text, String hexcolor) {
		if (hexcolor == null)
			hexcolor = "00b0f0";
		_statusTextView.setText(Html.fromHtml("<font color='#" + hexcolor + "'>" + text + "</font>"));
	}

	// Utility
	// ----------------------------------------------------------------------------

	/**
	 * @category Utility
	 */
	public void uploadPhotoFileToNeTV(String fullFilePath) {
		_uploadTime = System.currentTimeMillis();
		_uploadSpeed = 0;
		_pass_Upload = false;
		_performed_Upload = false;
		_uploadFilename = "";
		fullFilePath = fullFilePath.replace("file://", "");
		Log.d(TAG, this.getLocalClassName() + ": uploading photo: "
				+ fullFilePath);

		String mimeType = pendingMIMEType;
		if (mimeType.equals("image/jpeg"))
			_uploadFilename = "android_" + fullFilePath.replace("/", "__")
					+ ".jpg";
		else
			_uploadFilename = "android_" + fullFilePath.replace("/", "__")
					+ "." + mimeType.split("/")[1];

		File file = new File(fullFilePath);
		RequestParams params = new RequestParams();
		try {
			params.put("path", "/tmp/" + _uploadFilename);
			params.put("cmd", MessageReceiver.COMMAND_UploadFile);
			params.put("filedata", file);
		} catch (Exception error) {
			Log.e(TAG, "Unable to upload file 1");
			Log.e(TAG, error.toString());
			Toast.makeText(this, "Unable to send photo to NeTV",
					Toast.LENGTH_SHORT).show();
			_performed_Upload = true;
			return;
		}

		String url = "http://" + _ipaddress + "/bridge";
		Log.d(TAG, this.getLocalClassName() + ": uploading to: " + url);

		// This fails on very large file
		_httpClient.cancelRequests(this, true);
		_httpClient.post(url, params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				_uploadTime = System.currentTimeMillis() - _uploadTime;
				_pass_Upload = true;

				String filesize = response.split("</filesize>")[0]
						.split("<filesize>")[1].trim();
				long remote_size = 0;
				if (response.length() < 100)
					Log.d(TAG, "uploadPhotoFileToNeTV: " + response);

				try {
					remote_size = Long.parseLong(filesize);
				} catch (NumberFormatException nfe) {
					remote_size = 0;
				}

				_uploadSpeed = (remote_size * 8.0 / 1024.0 / 1024.0)
						/ (_uploadTime / 1000.0);
				_performed_Upload = true;

				Log.i(TAG, "Uploaded size: " + remote_size + " bytes");
				Log.i(TAG, "Uploaded time: " + _uploadTime + " ms");
				Log.i(TAG,
						"Uploaded speed: "
								+ String.format("%.2f", _uploadSpeed) + " Mbps");
			}

			@Override
			public void onFailure(Throwable error) {
				Log.e(TAG, "Unable to upload file 2");
				Log.e(TAG, error.toString());
				Toast.makeText(getApplicationContext(),
						"Unable to send photo to NeTV", Toast.LENGTH_SHORT)
						.show();
				_performed_Upload = true;
			}
		});
	}

	/**
	 * @category Utility
	 */
	public void uploadPhotoToNeTV(Uri uri) {
		_uploadTime = System.currentTimeMillis();
		_uploadSpeed = 0;
		_pass_Upload = false;
		_performed_Upload = false;
		_uploadFilename = "";

		InputStream stream = null;
		try {
			stream = getContentResolver().openInputStream(uri);
		} catch (Exception e) {
			Log.e(TAG, "photo not found: " + uri.getPath());
			_performed_Upload = true;
			return;
		}

		Log.d(TAG,
				this.getLocalClassName() + ": uploading photo: "
						+ uri.getPath());

		String mimeType = pendingMIMEType;
		if (mimeType.equals("image/jpeg"))
			_uploadFilename = "android_" + uri.getPath().replace("/", "__")
					+ ".jpg";
		else
			_uploadFilename = "android_" + uri.getPath().replace("/", "__")
					+ "." + mimeType.split("/")[1];

		RequestParams params = new RequestParams();
		try {
			params.put("path", "/tmp/" + _uploadFilename);
			params.put("cmd", MessageReceiver.COMMAND_UploadFile);
			params.put("filedata", stream);
		} catch (Exception error) {
			Log.e(TAG, "Unable to upload file 1");
			Log.e(TAG, error.toString());
			Toast.makeText(this, "Unable to send photo to NeTV",
					Toast.LENGTH_SHORT).show();
			_performed_Upload = true;
			return;
		}

		String url = "http://" + _ipaddress + "/bridge";
		Log.d(TAG, this.getLocalClassName() + ": uploading to: " + url);

		// This fails on very large file
		_httpClient.cancelRequests(this, true);
		_httpClient.post(url, params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response)
			{
				//String cmd = response.split("</cmd>")[0].split("<cmd>")[1].trim();
				//
				String filesize = "";
				try
				{
					filesize = response.split("</filesize>")[0].split("<filesize>")[1].trim();
					if (response.length() < 100)
						Log.d(TAG, "uploadPhotoFileToNeTV: " + response);
				}
				catch (Exception e)
				{
					return;
				}
				
				_uploadTime = System.currentTimeMillis() - _uploadTime;
				_pass_Upload = true;

				long remote_size = 0;
				try {
					remote_size = Long.parseLong(filesize);
				} catch (NumberFormatException nfe) {
					remote_size = 0;
				}

				_uploadSpeed = (remote_size * 8.0 / 1024.0 / 1024.0) / (_uploadTime / 1000.0);
				_performed_Upload = true;

				Log.i(TAG, "Uploaded size:  " + remote_size + " bytes");
				Log.i(TAG, "Uploaded time:  " + _uploadTime + " ms");
				Log.i(TAG,
						"Uploaded speed:  "
								+ String.format("%.2f", _uploadSpeed) + " Mbps");
			}

			@Override
			public void onFailure(Throwable error) {
				Log.e(TAG, "Unable to upload file 2");
				Log.e(TAG, error.toString());
				Toast.makeText(getApplicationContext(),
						"Unable to send photo to NeTV", Toast.LENGTH_SHORT)
						.show();
				_performed_Upload = true;
			}
		});
	}

	/**
	 * @category Utility
	 */
	public void sendHTTPSetUrlCommand(String weburl) {
		RequestParams params = new RequestParams();
		params.put("cmd", MessageReceiver.COMMAND_ControlPanel);
		params.put("value", "SetUrl " + weburl);

		String url = "http://" + _ipaddress + "/bridge";
		Log.d(TAG, this.getLocalClassName() + ": SetUrl to: " + weburl);

		_httpClient.cancelRequests(this, true);
		_httpClient.post(url, params, new AsyncHttpResponseHandler() {
			public void onSuccess(String response) {
				Log.d(TAG, response);
			}

			public void onFailure(Throwable error) {
				Toast.makeText(getApplicationContext(), error.toString(),
						Toast.LENGTH_SHORT).show();
				Log.e(TAG, error.toString());
			}
		});
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

		// Receive screenshot path
		/*
		 * if (commandName.equals(MessageReceiver.COMMAND_GetScreenshotPath)) {
		 * String _screenshotPath = (String)
		 * parameters.get(MessageReceiver.MESSAGE_KEY_VALUE); Log.d(TAG,
		 * "Update screenshot path: " + _screenshotPath); if (SUPER_VERBOSE)
		 * toast("Screenshot path: " + _screenshotPath);
		 * 
		 * if (_screenshotPath != null && _screenshotPath.length() >= 10 &&
		 * ENABLE_SCREENSHOT) { _imageLoader.changePath(_screenshotPath);
		 * _imageLoader.reload(); } }
		 */

		if (commandName.equals(MessageReceiver.COMMAND_RemoteControl.toUpperCase()))
		{
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_SetUrl.toUpperCase()))
		{
			Log.d(TAG, "Received " + commandName);
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_Multitab.toUpperCase()))
		{
			_shownPhoto = true;
			pendingMIMEType = "";
			pendingUrl = "";
			pendingUri = null;
			String value = (String)parameters.get(MessageReceiver.MESSAGE_KEY_VALUE);
			if (value == null)			Log.d(TAG, "Received " + commandName);
			else						Log.d(TAG, "Received " + commandName + ": " + value);
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_UnlinkFile.toUpperCase()))
		{
			String value = (String)parameters.get(MessageReceiver.MESSAGE_KEY_VALUE);
			if (value == null)			Log.d(TAG, "Received " + commandName);
			else						Log.d(TAG, "Received " + commandName + ": " + value);
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_TextInput.toUpperCase()))
		{
			String id = (String)parameters.get("id");
			String value = (String)parameters.get(MessageReceiver.MESSAGE_KEY_VALUE);
			if (id == null || id.length() <= 0)	{
				Log.d(TAG, "Received " + commandName + ": (defocus)");
				return;
			}
			
			if (ENABLE_TEXT_INPUT) {
				Log.d(TAG, "Received " + commandName + ": " + id + " -> " + value);			
				gotoNextActivity(ActivityTextInput.class);
			}
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_Key.toUpperCase()))
		{
			//Ignore status message
		}

		Log.d(TAG, "Received command message: " + commandName);
	}

	/**
	 * @category Application Logic
	 */
	private void initializeSequence()
	{
		// Stage 0 - for the DEMO in AP mode
		// Start connecting to NeTV Access Point
		if ((_ipaddress == null || _ipaddress.length() < 5 || _ipaddress.equals(AppNeTV.DEFAULT_IP_ADDRESS))
				&& !_myApp.isConnectedNeTV()) {
			// Just to be sure
			setPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, AppNeTV.DEFAULT_IP_ADDRESS);
			_ipaddress = AppNeTV.DEFAULT_IP_ADDRESS;

			if (!_triedConnectNeTV)
			{
				_triedConnectNeTV = true;
				_retryCounter = 0;
				setStatusMessage("Connecting to NeTV Access Point...");
				_handler.postDelayed(initializeSequenceRunnable, 3000);
				Log.d(TAG, this.getLocalClassName() + ": Connecting to NeTV Access Point...");
				_myApp.connectNeTV();
				return;
			}

			_retryCounter++;
			if (_retryCounter <= 3) {
				_handler.postDelayed(initializeSequenceRunnable, 3000);
				Log.d(TAG, this.getLocalClassName() + ": Waiting to be connected to NeTV Access Point...");
				return;
			}

			setStatusMessage("Could not connect to NeTV AP!", "ff0000");
			Log.e(TAG, this.getLocalClassName() + ": Could not connect to NeTV AP! (Demo mode)");
			_retryCounter = 0;
			return;
		}

		// Stage 1
		// Wait until the communication service is up
		if (!_myApp.isCommServiceRunning())
		{
			setStatusMessage("Waiting for CommService to bind...");
			Log.d(TAG, this.getLocalClassName() + ": Waiting for CommService to bind...");
			_handler.postDelayed(initializeSequenceRunnable, 300);
			return;
		}

		// Stage 2
		// Flush out pending image
		if ( (pendingMIMEType != null && pendingMIMEType.startsWith("image"))
			&& (pendingUri != null || (pendingUrl != null && pendingUrl.length() > 5))
			|| (_uploadFilename != null && _uploadFilename.length() > 5))
		{
			// Stage 2.0
			if (pendingUri != null) {
				setStatusMessage("Sending photo to TV...");
				uploadPhotoToNeTV(pendingUri);
				pendingUri = null;
				_retryCounter = 0;
				_handler.postDelayed(initializeSequenceRunnable, 1000);
				return;
			}
			if (pendingUrl != null && pendingUrl.length() > 5)
			{
				setStatusMessage("Sending photo to TV...");
				uploadPhotoFileToNeTV(pendingUrl);
				pendingUrl = "";
				_handler.postDelayed(initializeSequenceRunnable, 1000);
				return;
			}

			// Stage 2.1
			if (!_performed_Upload)
			{
				_handler.postDelayed(initializeSequenceRunnable, 1000);
				return;
			}
			if (!_pass_Upload)
			{
				setStatusMessage("Failed to send photo to TV!", "ff0000");
				_handler.postDelayed(initializeSequenceRunnable, 2000);
				return;
			}

			// Stage 2.2
			if (!_shownPhoto)
			{
				_retryCounter++;
				if (_retryCounter <= 3)
				{
					// Send command to show photo another tab
					String htmlString = IMAGE_HTML.replace("xxxxxxxxxx", AppNeTV.NETV_HOME_URL + "/tmp/netvserver/" + _uploadFilename);
					if (_useTCP)		_myApp.MultiTabHTTP(htmlString, "html", multitabHandler);
					else				_myApp.sendMultiTabCommand(htmlString, "html");
					Log.d(TAG, this.getLocalClassName() + ": showing photo on TV (Multitab/HTML command)");
					setStatusMessage("Showing photo on TV");
					
					_handler.postDelayed(initializeSequenceRunnable, 1500);
					return;
				}
				
				//Give up sending
				pendingMIMEType = "";
				pendingUrl = "";
				pendingUri = null;
				setStatusMessage("Failed to show photo on TV!", "ff0000");
				_handler.postDelayed(initializeSequenceRunnable, 2000);
				return;
			}
		}

		// Delete the photo once we have shown it on the browser (awesome!)
		if (_uploadFilename != null && _uploadFilename.length() > 0)
			_myApp.sendRequestUnlinkFile("/tmp/" + _uploadFilename);

		// Show the IP address of the NeTV we are controlling
		String _manual_ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_MANUAL, "");
		if (_manual_ipaddress.length() > 6)
			setStatusMessage(this.getString(R.string.controlling) + " " + _ipaddress + " (" + this.getString(R.string.manual_ip) + ")");
		else
			setStatusMessage(this.getString(R.string.controlling) + " " + _ipaddress);
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

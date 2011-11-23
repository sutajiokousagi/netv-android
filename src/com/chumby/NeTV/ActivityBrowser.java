package com.chumby.NeTV;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpResponseHandler;

public class ActivityBrowser extends ActivityBaseNeTV implements OnClickListener, OnTouchListener
{	
	//Screen resolution;
	DisplayMetrics 		_displayMetrics;
	int					_screenWidth;
	int					_screenHeight;
	
	//UI
	TextView			_statusTextView;
	EditText			_urlTextEdit;
	WebView				_webView;
	MyWebViewClient		_myWebViewClient;
	ImageView 			_loadingIcon;
	ImageView			_btnForward;
	ImageView			_btnBackward;
			
	//Flags
	String 				_ipaddress;
	int 				_retryCounter;
	boolean 			_triedConnectNeTV;
	
	//Cool features
	String				pendingUrl;
	String				pendingMIMEType;
	boolean				_doingCoolStuff;

	
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
	    	else
	    		pendingUrl = "";
	    }
	    
	    @Override
	    public void onFailure(Throwable error)
	    {
	    	Log.e(TAG, "MultiTabHTTP failed");
			Log.e(TAG, "" + error);
	    }
	};

	
	private class MyWebViewClient extends WebViewClient
	{
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url)
	    {
	        view.loadUrl(url);
	        updateBrowserUrl(url);
	        
	        //Let the retry mechanism kick in
	        _retryCounter = 0;
	        pendingUrl = url;
	        initializeSequence();
	        
	        return true;
	    }
	    
	    @Override
	    public void onPageFinished(WebView view, String url)
	    {
	        _loadingIcon.setVisibility(View.INVISIBLE);
	    }
	    
	    @Override
	    public void onPageStarted(WebView view, String url, Bitmap favicon)
	    {
	        //_loadingIcon.setVisibility(View.VISIBLE);
	    }
	}
	

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
		Log.d(TAG, "ActivityBrowser onCreate()");
    	super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browser);
		
    	reset();
    	
    	//if initiated from a http:// link event
    	String url = getIntent().getDataString();
    	if (url != null && url.length() > 8) {
    		pendingUrl = url;
    		_doingCoolStuff = true;
    		Log.d(TAG, this.getLocalClassName() + ": has pending URL: " + pendingUrl);
    	}
    	
    	pendingMIMEType = getIntent().getType();
    	if (pendingMIMEType != null)	Log.d(TAG, this.getLocalClassName() + ": pendingMIMEType: " + pendingMIMEType);
    	else				    		pendingMIMEType = "";

	    //Screen resolution
	    _displayMetrics = new DisplayMetrics();
	    getWindowManager().getDefaultDisplay().getMetrics(_displayMetrics);
	    _screenWidth = _displayMetrics.widthPixels;
	    _screenHeight = _displayMetrics.heightPixels;
	    
	    //UI
	    ((ImageView) this.findViewById(R.id.btn_back)).setOnClickListener(this);
	    _loadingIcon = (ImageView)findViewById(R.id.loading_icon);
    	_btnBackward = (ImageView)this.findViewById(R.id.btn_backward);
    	_btnBackward.setOnClickListener(this);
    	_btnBackward.setVisibility(View.INVISIBLE);
    	_btnForward = (ImageView)this.findViewById(R.id.btn_forward);
    	_btnForward.setOnClickListener(this);
    	_btnForward.setVisibility(View.INVISIBLE);
    	_statusTextView = (TextView)findViewById(R.id.textViewRemoteTitle);
    	_urlTextEdit = (EditText)findViewById(R.id.textViewUrl);
    	
    	//TextEdit
    	_urlTextEdit.setOnEditorActionListener(new TextView.OnEditorActionListener(){
    		public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
    			if(actionId == EditorInfo.IME_ACTION_GO){
    				_retryCounter = 0;
    				pendingUrl = _urlTextEdit.getText().toString().trim();
    				initializeSequence();
    				_loadingIcon.setVisibility(View.VISIBLE);
    				hideKeyboard(_urlTextEdit);
    			}
    			return true;
    		}
    	});

    	//Webview
    	_myWebViewClient = new MyWebViewClient();
    	_webView = (WebView)findViewById(R.id.webView1);
    	_webView.getSettings().setJavaScriptEnabled(true);
    	_webView.setOnTouchListener(this);
    	_webView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/15.0.874.121 Safari/535.2");		//prevent loading mobile version
    	_webView.getSettings().setBuiltInZoomControls(true);
    	//_webView.getSettings().setLoadWithOverviewMode(true);
    	_webView.getSettings().setUseWideViewPort(true);
    	
    	_myWebViewClient = new MyWebViewClient();
    	_webView.setWebViewClient(_myWebViewClient);
    	
    	_webView.setPictureListener(new PictureListener() {
    	    public void onNewPicture(WebView view, Picture arg1) {
    	    	_loadingIcon.setVisibility(View.INVISIBLE);
    	    	if (_webView.canGoBack())		_btnBackward.setVisibility(View.VISIBLE);
    	    	else							_btnBackward.setVisibility(View.INVISIBLE);
    	    	if (_webView.canGoForward())	_btnForward.setVisibility(View.VISIBLE);
    	    	else							_btnForward.setVisibility(View.INVISIBLE);
    	    }   
    	});
    	
    	//Spin the loading icon
    	Animation animation = new RotateAnimation (0.0f, 359.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    	animation.setRepeatCount(Animation.INFINITE);
    	animation.setDuration(2000);
    	animation.setInterpolator(new Interpolator() { public float getInterpolation(float arg0) { return arg0; } });
    	_loadingIcon.startAnimation(animation);
    	_loadingIcon.setVisibility(View.INVISIBLE);
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
    	   	   	
    	//IP address for unicast transmission
    	_ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
    	   	   	
    	//Synchronize UI with NeTV (currently do nothing on NeTV)
    	_myApp.sendAndroidJSChangeView(AppNeTV.NETV_UISTATE_BROWSER);
      	    	    					
		//Determine if we're in portrait, and whether we're showing or hiding the keyboard with this toggle.
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean hasFullkeyboard = getResources().getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY;
        boolean isKeyboardHidden = getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES;

    	//Setup Keyboard
        //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (isPortrait && !hasFullkeyboard && isKeyboardHidden)
        {
        	
        }
        else
        {
        	
        }
                
        updateNextStepUI();
        updateHashVariables();

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
		setPreferenceString(AppNeTV.PREF_PREVIOUS_ACTIVITY, ActivityBrowser.class.getName());
		
		//_myApp.sendMultiTabCommand("", "hide");
		_myApp.MultiTabHTTP("", "closeall", null);
		Log.d(TAG, this.getLocalClassName() + ": return UI to default tab");
		
		reset();
				
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
    	_triedConnectNeTV = false;
    		
		pendingUrl = "";
		pendingMIMEType = "";
    }
    	
    
    // UI Events
	//----------------------------------------------------------------------------
    
	/**
	 * Customize Back button & send keyboard events to NeTV
	 * 
	 * @category UI Events
	 */
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
			return super.onKeyDown(keyCode, event);

		//Do not send special keys to NeTV 
		if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SEARCH
				|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP
				|| keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_CAMERA 
				|| keyCode == KeyEvent.KEYCODE_POWER)
			return super.onKeyDown(keyCode, event);

	    return super.onKeyDown(keyCode, event);    
	}
    
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event)
    {
    	//This will disable keyboard on Nexus S & alike
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
    	_vibrator.vibrate(120);
    	  
    	//Sound feedback
    	if (ENABLE_SOUND_FX)
    	{
	    	_mediaPlayer = MediaPlayer.create(this, R.raw.pop);
	    	if (_mediaPlayer != null)
	    	{
	    		_mediaPlayer.start();
	    		_mediaPlayer.setOnCompletionListener(new OnCompletionListener()
	    		{
	    			public void onCompletion(MediaPlayer mp) { mp.release(); }
	    		});
	    	}
    	}

		if (v.getId() == R.id.btn_go)
		{
			_retryCounter = 0;
			pendingUrl = _urlTextEdit.getText().toString().trim();
			initializeSequence();
			_loadingIcon.setVisibility(View.VISIBLE);
			hideKeyboard(_urlTextEdit);
			return;
		}
		else if (v.getId() == R.id.btn_back)
		{
			finish();
			return;
		}
		else if (v.getId() == R.id.btn_backward)
		{
			_webView.goBack();
			return;
		}
		else if (v.getId() == R.id.btn_forward)
		{
			_webView.goForward();
			return;
		}
	}
	
	/**
	 * Raw touch event
	 * 
	 * @category UI Events
	 */
	public boolean onTouch(View arg0, MotionEvent arg1)
	{
		if (arg0 == _webView)
		{
			if (arg1.getAction() == MotionEvent.ACTION_MOVE)
			{
				float xPos = 0;
				float yPos = (float) _webView.getScrollY() / _webView.getContentHeight();
				_myApp.sendMultiTabCommand(xPos + "," + yPos, "scrollf");
			}
		}
		return false;
	}
	
	/*
	 * @category UI Events
	 */
	private void updateBrowserUrl(String newUrl)
	{	
		if (newUrl != null)										_urlTextEdit.setText(newUrl);
		else													_urlTextEdit.setText("www.chumby.com");
	}

    // UI Utility functions
	//----------------------------------------------------------------------------
    
    /**
	 * @category UI Utility
	 */
    public boolean updateNextStepUI()
    {
   		return false;
    }
    
    /**
	 * @category UI Utility
	 */
    protected void updateHashVariables()
    {

    }
    
    /**
	 * @category UI Utility
	 */
	protected void setStatusMessage(String text)
	{
		setStatusMessage(text, null);
	}
	
	/**
	 * @category UI Utility
	 */
	protected void setStatusMessage(String text, String hexcolor)
	{
		if (hexcolor == null)
			hexcolor = "00b0f0";
		_statusTextView.setText(Html.fromHtml( "<font color='#" + hexcolor + "'>" + text + "</font>") );
	}
		
	// Custom event
    //------------------------------------------------------------------
    
    /**
     * Handle custom events fired by the main application class after pre-processing the message
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
		parameters.remove(MessageReceiver.COMMAND_MESSAGE);
		if (commandName == null || commandName.length() < 2)
			return;
		commandName = commandName.toUpperCase();
		
		//----------------------------------------------------
	
		if (commandName.equals(MessageReceiver.COMMAND_RemoteControl.toUpperCase()))
		{
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_TickerEvent.toUpperCase()))
		{
			Log.d(TAG, "Received command message: " + commandName);
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_SetUrl.toUpperCase()))
		{
			Log.d(TAG, "Received command message: " + commandName);
			return;
		}
		else if (commandName.equals(MessageReceiver.COMMAND_Multitab.toUpperCase()))
		{
			//Acknowledgement from scrolling command
			if (pendingUrl == null || pendingUrl.length() < 8)
				return;
			
			pendingUrl = "";
			String value = (String)parameters.get(MessageReceiver.MESSAGE_KEY_VALUE);
			if (value == null)			Log.d(TAG, "Received " + commandName);
			else						Log.d(TAG, "Received " + commandName + ": " + value);
			return;
		}
		
		Log.d(TAG, "Received command message: " + commandName);
	}
	
	/**
	 * @category Application Logic
	 */
	private void initializeSequence()
	{
		//Stage 0 - for the DEMO in AP mode
		//Start connecting to NeTV Access Point
		if ((_ipaddress == null || _ipaddress.length() < 5 || _ipaddress.equals(AppNeTV.DEFAULT_IP_ADDRESS))
				&& !_myApp.isConnectedNeTV())
		{
			//Just to be sure
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
		
		//Stage 1
		//Wait until the communication service is up
		if (!_myApp.isCommServiceRunning())
		{
			setStatusMessage("Waiting for CommService to bind...");
			Log.d(TAG, this.getLocalClassName() + ": Waiting for CommService to bind...");
			_handler.postDelayed(initializeSequenceRunnable, 500);
			_retryCounter = 0;
			return;
		}

		//Stage 2
		//Flush out pending URL (HTTP://)
		if (pendingUrl != null && pendingUrl.length() > 8)
		{
			_retryCounter++;
			if (_retryCounter <= 1)
			{
				//DEMO mode
				if (_myApp.isConnectedNeTV()) {
					pendingUrl = AppNeTV.NETV_HOME_URL + "/test.html";			//point to some pre-defined page
					setStatusMessage("Sending URL doesn't work in DEMO mode", "ff0000");
				}
				else
				{
					setStatusMessage("Sending URL to TV...");
				}
				updateBrowserUrl(pendingUrl);
				
				//Clean up pending URL
				if (!pendingUrl.startsWith("http://") && !pendingUrl.startsWith("https://"))
					pendingUrl = "http://" + pendingUrl;
				_webView.loadUrl(pendingUrl);
				
				Log.i(TAG, this.getLocalClassName() + ": sending URL " + pendingUrl);
				if (_useTCP)		_myApp.MultiTabHTTP(pendingUrl, multitabHandler);
				else				_myApp.sendMultiTabCommand(pendingUrl);
				_handler.postDelayed(initializeSequenceRunnable, 2000);
				return;
			}
			else if (_retryCounter <= 3)
			{
				Log.d(TAG, this.getLocalClassName() + ": retrying...[" + _retryCounter + "/3]");
				setStatusMessage("Sending URL to TV...");
				if (_useTCP)		_myApp.MultiTabHTTP(pendingUrl, multitabHandler);
				else				_myApp.sendMultiTabCommand(pendingUrl);
				_handler.postDelayed(initializeSequenceRunnable, 2000);
				return;
			}
			
			//Failed to send URL to NeTV. Give up
			Log.e(TAG, this.getLocalClassName() + ": failed to send URL to NeTV");
			setStatusMessage("Failed to send URL to NeTV", "ff0000");
			pendingUrl = "";
			_handler.postDelayed(initializeSequenceRunnable, 2000);
			return;
		}
		
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


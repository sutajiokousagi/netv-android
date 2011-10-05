package com.chumby.NeTV;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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

public class Activation extends Activity {
	private static final String TAG = "LoadingActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.loading);
        PackageInfo packageInfo = null;
        String packageName = getPackageName();
                
        try {
        	packageInfo = getPackageManager().getPackageInfo(packageName, 0);
        } catch (Exception e) {
        	
        }
		
        Chumby.getInstance().softwareVersion = packageInfo==null ? "1.0" : packageInfo.versionName; //getString(R.string.app_version);
        Chumby.getInstance().hardwareVersion = "10.1";
        DaughterCardID.getInstance().fromString(getResourceText(R.raw.dcid));
        buildGUID();
        XAPI.setContext(getApplicationContext());
        XAPIRequest.flushQueue();
        HttpConnectionManager.flushQueue();
    }
 
    public static String removeCharFromString(String aString, char unwantedChar) {
        Character character = null;
        StringBuffer result = new StringBuffer(aString.length());
        for (int i = 0; i < aString.length(); i++) {
            character = new Character(aString.charAt(i));
            if (character!=unwantedChar) {
                result.append(aString.charAt(i));
            }
        }
        return result.toString();
    }

    // manufacture a GUID for this device
    private void buildGUID()
    {
    	String a_id = Secure.getString(getContentResolver(),Secure.ANDROID_ID);
    	if (a_id==null) {
    		a_id = "9774D56D682E549C"; // treat null as emulator
    	} else {
    		a_id = a_id.toUpperCase();
    	}
    	ChumbyLog.i(TAG+".buildGUID(): ANDROID_ID="+a_id);
    	if (a_id.equals("9774D56D682E549C") /* emulator */ || a_id.equals("DEAD00BEEF") /* hacked archos */) {
    		ChumbyLog.i(TAG+".buildGUID(): device uses emulator/bogus ID "+a_id);
    		TelephonyManager telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
    		a_id = telephonyManager.getDeviceId();
    		if (a_id!= null && !a_id.equals("000000000000000")) { // some tablets have bogus IMEI
    			ChumbyLog.i(TAG+".buildGUID(): has IMEI '"+a_id+"'");
    			while (a_id.length()<16) { // left F-pad any IDs that are too short (should always happen)
    				a_id = "F"+a_id;
    			}
    		} else {
    			ChumbyLog.i(TAG+".buildGUID(): device has no or invalid IMEI, checking wifi");
    		    WifiManager wm = (WifiManager)getSystemService(WIFI_SERVICE);
    		    if (wm!=null) {
    		    	a_id = wm.getConnectionInfo().getMacAddress();
    		    	if (a_id!=null) {
    		    		a_id = removeCharFromString(a_id,':').toUpperCase();
    	    			while (a_id.length()<16) { // left A-pad any IDs that are too short (should always happen)
    	    				a_id = "A"+a_id;
    	    			}
    		    	} else {
    		    		ChumbyLog.i(TAG+".buildGUID(): device has no wifi MAC");
    		    		a_id = "0000000000000001";
    		    	}
    		    } else {
    		    	ChumbyLog.i(TAG+".buildGUID(): device has no wifi");
		    		a_id = "0000000000000002";
    		    }
   			
    		}
    	}
    	while (a_id.length()<16) { // left zero-pad any IDs that are too short (~6%?)
    		a_id = "0"+a_id;
    	}
    	if (a_id.length()>16) { // take tail of IDs that are too long (should never happen)
    		a_id = a_id.substring(a_id.length()-16);
    	}
		Chumby.getInstance().guid = "368657D6-2697-4627-"+a_id.substring(0,4)+"-"+a_id.substring(4);
		ChumbyLog.i(TAG+".buildGUID(): "+Chumby.getInstance().guid);
    }
      
    @Override
    public void onResume()
    {
    	super.onResume();
    	authorize();
	}

    @Override
    public void onPause() {
    	//ChumbyLog.i(TAG+".onPause()");
    	super.onPause();
    }
 
    @Override
    public void onConfigurationChanged(Configuration newConfiguration)
    {
    	super.onConfigurationChanged(newConfiguration);
    	ChumbyLog.i(TAG+".onConfigurationChanged(): orientation="+Integer.toString(newConfiguration.orientation));
    }
 
    // go to the next view (WidgetPlayer)
    private void nextView()
    {
    	Log.d(TAG, "going to next view");
    }

    private void registerByUserName() {
    	registerByUserName("","","");
    }
    
    private void registerByUserName(String userName,String password,String deviceName) {
    	//ChumbyLog.i(TAG+".registerByUserName()");
        LayoutInflater factory = LayoutInflater.from(this);
        final View registerView = factory.inflate(R.layout.register3, null);
    	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
    	dialogBuilder.setTitle(R.string.activate_device);
    	dialogBuilder.setView(registerView);
       	dialogBuilder.setNegativeButton(R.string.button_back,new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialogInterface, int whichButton) {
    			registerStep1();
    		}
    	});
    	dialogBuilder.setPositiveButton(R.string.button_next,new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialogInterface, int whichButton) {
    			EditText editText = (EditText)registerView.findViewById(R.id.username);
    			String userName = editText.getText().toString();
    			editText = (EditText)registerView.findViewById(R.id.password);
    			String password = editText.getText().toString();
    			editText = (EditText)registerView.findViewById(R.id.device_name);
    			String deviceName = editText.getText().toString();
    			acceptRegisterByUserName(userName,password,deviceName);
    		}
    	});
   	AlertDialog dialog = dialogBuilder.create();
	EditText editText = (EditText)registerView.findViewById(R.id.username);
	editText.setText(userName);
	editText = (EditText)registerView.findViewById(R.id.password);
	editText.setText(password);
	editText = (EditText)registerView.findViewById(R.id.device_name);
	editText.setText(deviceName);
	dialog.show();
    }

    public void acceptRegisterByUserName(String userName,String password,String deviceName) {
    	if (userName.length()<4)
    		badRegisterAlert(R.string.chumby_username_too_short,userName,password,deviceName);
    	else if (deviceName.length()<1) {
    		badRegisterAlert(R.string.chumby_device_name_too_short,userName,password,deviceName);    		
    	} else if (!validDeviceName(deviceName)) {
    		badRegisterAlert(R.string.chumby_device_name_bad_syntax,userName,password,deviceName);    		
    	} else {
    		tryRegister(userName,password,deviceName);
    	}
    }

    protected boolean validDeviceName(String s) {
    	if (s.length()<1) return false;
    	if (s.length()>150) return false;
    	if (!Pattern.matches("\\A[^\\x00-\\x20\\x7F]+([^\\x00-\\x1f\\x7F]+){0,149}\\z",s)) return false;
    	return true;
    }

    public void badRegisterAlert(int resID, String userName,String password,String deviceName) {
    	badRegisterAlert(getString(resID),userName,password,deviceName);
    }
  
	public void badRegisterAlert(String message,final String userName, final String password,final String deviceName) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message)
		       .setCancelable(false)
		       .setPositiveButton(R.string.button_ok,  new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   registerByUserName(userName,password,deviceName);
    	           }
		        });
		AlertDialog alert = builder.create();
		alert.show();
	}

	ActivationByUserName activationByUserName;

	private void tryRegister(final String userName,final String password,final String deviceName) {
		//ChumbyLog.i(TAG+".tryRegister()");
		activationByUserName = new ActivationByUserName();
       	Handler handler = new Handler() {
			public void handleMessage(Message message) {
				switch(message.what) {
					case ActivationByUserName.ACTIVATED:
						gotTryRegister();
						break;
					case ActivationByUserName.NOT_ACTIVATED_BAD_USER:
						badRegistrationUser(userName,password,deviceName);
						break;
					case ActivationByUserName.NOT_ACTIVATED_BAD_DEVICE:
						badRegistrationDevice(userName,password,deviceName);
						break;
					case ActivationByUserName.CANT_ACTIVATE:
						badTryRegister();
				}
			}
    	};
    	activationByUserName.activate(userName,password,deviceName,handler);
    }

	private void badRegistrationUser(final String userName,final String password,final String deviceName) {	
		ChumbyLog.i(TAG+".badRegistrationUser()");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.invalid_user)
		       .setCancelable(false)
		       .setPositiveButton(R.string.button_ok,  new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
   	                	registerByUserName(userName,password,deviceName);
    	           }
		        });
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void badRegistrationDevice(final String userName,final String password,final String deviceName) {	
		ChumbyLog.i(TAG+".badRegistrationDevice()");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.duplicate_device)
		       .setCancelable(false)
		       .setPositiveButton(R.string.button_ok,  new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
   	                	registerByUserName(userName,password,deviceName);
    	           }
		        });
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void gotTryRegister() {
		//ChumbyLog.i(TAG+".gotTryRegister");
		try {
			ChumbyLog.i(TAG+".gotTryRegister(): sleeping for 2 seconds");
			Thread.sleep(2000); // XXX DSM sleep two seconds before authorizing
		} catch (Exception e) {
			ChumbyLog.i(TAG+".gotTryRegister(): failed to sleep");
		}
		authorize();
	}
	
	private void badTryRegister() {
		//ChumbyLog.i(TAG+".badTryRegister(): not activated");
		errorAlert("Cannot register");
	}

	private void registerStep1()
	{
    	//ChumbyLog.i(TAG+".registerStep1()");
        LayoutInflater factory = LayoutInflater.from(this);
        final View registerView = factory.inflate(R.layout.register, null);
    	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
    	dialogBuilder.setTitle("Activate chumby");
    	dialogBuilder.setView(registerView);
       	dialogBuilder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialogInterface, int whichButton) {
    			cancelRegistration();
    		}
    	});
       	dialogBuilder.setNeutralButton("Create account",new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialogInterface, int whichButton) {
       			createAccount();
       		}
    	});
    	dialogBuilder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialogInterface, int whichButton) {
       			registerByUserName();
       		}
    	});
   	AlertDialog dialog = dialogBuilder.create();
    dialog.show();
    }

	private void createAccount() {
		String url = "http://www.chumby.com/account/new?devicetype=android";
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
		finish();
	}

	/*
	private void registerStep1() {
    	ChumbyLog.i(TAG+".registerStep1()");
        LayoutInflater factory = LayoutInflater.from(this);
        final View registerView = factory.inflate(R.layout.register, null);
    	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
    	dialogBuilder.setTitle(R.string.activate_device);
    	dialogBuilder.setView(registerView);
       	dialogBuilder.setNegativeButton(R.string.alert_dialog_cancel,new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialogInterface, int whichButton) {
    			cancelRegistration();
    		}
    	});
    	dialogBuilder.setPositiveButton(R.string.alert_dialog_next,new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialogInterface, int whichButton) {
    			registerStep2();
    		}
    	});
   	AlertDialog dialog = dialogBuilder.create();
    dialog.show();
    }
    */

    private void cancelRegistration() {
    	this.finish();
    }

    /*
    private void registerStep2() {
    	//ChumbyLog.i(TAG+".registerStep2()");
        LayoutInflater factory = LayoutInflater.from(this);
        final View registerView = factory.inflate(R.layout.register2, null);
    	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
    	dialogBuilder.setTitle(R.string.activate_device);
    	dialogBuilder.setView(registerView);
       	dialogBuilder.setNegativeButton(R.string.alert_dialog_back,new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialogInterface, int whichButton) {
    			registerStep1();
    		}
    	});
       	dialogBuilder.setPositiveButton(R.string.alert_dialog_next,new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialogInterface, int whichButton) {
    			long hash = 0;
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_0_0)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_0_1)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_0_2)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_0_3)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_1_0)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_1_1)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_1_2)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_1_3)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_2_0)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_2_1)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_2_2)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_2_3)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_3_0)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_3_1)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_3_2)).isChecked() ? 1 : 0);
    			hash = (hash<<1) + (((RegisterOval)registerView.findViewById(R.id.b_3_3)).isChecked() ? 1 : 0);
   				//ChumbyLog.i(" hash = "+Long.toString(hash));
    			registerStep3(hash);
    		}
    	});
   	AlertDialog dialog = dialogBuilder.create();
    dialog.show();
    }
    */
  
    /*
	ProgressDialog authorizingProgressDialog;
	Activation activation;

	private boolean pollingAuthorizations;
	*/
	
	/*
	private void registerStep3(long hash) {
		//ChumbyLog.i(TAG+".registerStep3()");
    	authorizingProgressDialog = ProgressDialog.show(
    			LoadingActivity.this,
    			"Activating...",
    			"",
    			true,
    			true,
    			new DialogInterface.OnCancelListener() {
    				public void onCancel(DialogInterface dialogInterface) {
    					stopPollingAuthorizations();
    					registerStep2(); // go back to grid
    				}
    			});
    	tryActivate(hash);
	}
	*/

	/*
	private void tryActivate(long hash) {
		//ChumbyLog.i(TAG+".tryActivate()");
    	activation = new Activation();
       	Handler handler = new Handler() {
			public void handleMessage(Message message) {
				switch(message.what) {
					case Activation.ACTIVATED:
						gotTryActivated(message.what,message.obj);
						break;
					case Activation.NOT_ACTIVATED:
						badTryActivation(message.what,message.obj);
				}
			}
    	};
    	activation.activate(hash,handler);
    }

	private void gotTryActivated(int response,Object obj) {
		//ChumbyLog.i(TAG+".gotTryActivated");
		startPollingAuthorizations();
	}
	
	private void badTryActivation(int response,Object obj) {
		ChumbyLog.i(TAG+".badTryActivation(): not activated");
		errorAlert(R.string.cant_activate);
	}
	 */

	/*
	private static final long POLL_INTERVAL = 5000; // ms = 5 seconds
	private static final int POLL_AUTHORIZATIONS = 999999;
	private final LoadingActivity loadingActivity = this;
	
	private final Handler authorizationHandler = new Handler() {
		public void handleMessage(Message message) {
			switch (message.what) {
				case POLL_AUTHORIZATIONS:
					loadingActivity.tryAuthorize();
					break;
				default:
					super.handleMessage(message);
			}
		}
	};
	*/

	/*
	private void startPollingAuthorizations() {
		pollingAuthorizations = true;
		authorizationHandler.sendMessageDelayed(Message.obtain(authorizationHandler,POLL_AUTHORIZATIONS),POLL_INTERVAL);
	}

	private void stopPollingAuthorizations() {
		pollingAuthorizations = false;
		authorizationHandler.removeMessages(POLL_AUTHORIZATIONS); // kill any pending messages
	}

   private void tryAuthorize() {
	   //ChumbyLog.i(TAG+".authorize()");
	   Handler handler = new Handler() {
		   public void handleMessage(Message message) {
			   switch(message.what) {
			   		case Authorization.AUTHORIZED:
						gotTryAuthorized(message.what,message.obj);
						break;
					case Authorization.NOT_AUTHORIZED:
						badTryAuthorization(message.what,message.obj);
				}
		   }
	   };
	   (new Authorization()).authorize(handler);
    }
    
    void gotTryAuthorized(int response,Object obj) {
    	//ChumbyLog.i(TAG+".gotTryAuthorized()");
    	authorizingProgressDialog.dismiss();
    	stopPollingAuthorizations(); // XXX DSM shouldn't happen; just in case
    	authenticate();
    }
 
    void badTryAuthorization(int response,Object obj) {
    	//ChumbyLog.i(TAG+".badTryAuthorization()");
    	if (pollingAuthorizations) {
    		ChumbyLog.i(TAG+".badAuthorization(): requeuing request");
    		authorizationHandler.sendMessageDelayed(Message.obtain(authorizationHandler,POLL_AUTHORIZATIONS),POLL_INTERVAL);
    	} else {
    		// XXX DSM should not happen, just in case
    		ChumbyLog.i(TAG+".badAuthorization(): ignoring");
    	}
    }
    */
    
    //--------------------------------------------
    
    public void errorAlert(int resID)
    {
		errorAlert(getString(resID));
	}

	public void errorAlert(String message)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message)
		       .setCancelable(false)
		       .setPositiveButton(R.string.button_ok,  new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
   	                	acceptErrorAlert();
    	           }
		        });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	protected void acceptErrorAlert()
	{
		//ChumbyLog.i(TAG+".acceptErrorAlert()");
	}
    
    protected String getResourceText(int resID)
    {
    	try
    	{
	    	InputStream is = getApplicationContext().getResources().openRawResource(resID);
	    	if (is != null) {
	    		Writer writer = new StringWriter();
	    		char[] buffer = new char[1024];
	    		try {
	    			Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
	    			int n;
	    			while ((n = reader.read(buffer)) != -1) {
	    				writer.write(buffer, 0, n);
	    			}
	    		} finally {
	    			is.close();
	    		}
	    		return writer.toString();
	    	} else {        
	    		return "";
	    	}
    	}
    	catch (Exception ex)
    	{
    		return "";
    	}
    }
    
    //--------------------------------------------
    
    protected void authorize()
    {
    	Log.i(TAG, this.getLocalClassName() + ": authorize()");
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
    	//ChumbyLog.i(TAG+".gotAuthorized()");
    	authenticate();
    }
    
    protected void badAuthorization(int response,Object obj) {
    	//ChumbyLog.i(TAG+".badAuthorization()");
    	registerStep1();
    	//registerByUserName();
    }

    protected void cantAuthorize(int response,Object obj)
    {
    	Log.d(TAG, "Cannot authorize");
    }

    //--------------------------------------------

    protected void authenticate() {
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
    
    protected void didAuthenticate(int response,Object obj)
    {
    	//ChumbyLog.i(TAG+".didAuthenticate()");
    	fetchDevice();
    	AndroidStats.sendStats(this);
    }
 
    protected void badAuthentication(int response,Object obj)
    {
    	Log.i(TAG, ".badAuthentication()");
    }
    
    //--------------------------------------------

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
    	nextView();
    }
    
    protected void badDevice(int response,Object value)
    {
    	Log.d(TAG, "badDevice()");
    }
}

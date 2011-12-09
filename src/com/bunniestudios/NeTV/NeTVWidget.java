package com.bunniestudios.NeTV;

/*
 * This class mirrors AppNeTV in terms of binding to service and use it 
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.RemoteViews;

//NOTE: AppWidgetProvider is merely a derived class from BroadcastReceiver 
//that has 4 functions: onDeleted, onDisabled, onEnabled, onUpdate
//onReceive can also be overriden directly
public class NeTVWidget extends AppWidgetProvider
{
	protected static final String TAG = "NeTV";
	
	//Multi-threading and delay
	protected static final Handler	_handler = new Handler();
	
	//Context
	Context							_context;
	
	//Preferences
	boolean							_enableSMS;
	boolean							_enableSMSContent;
	String							_SMSFilter;
	
	//---------------------------------------------------------------------------
    
    @Override
    public void onReceive(Context context, Intent intent) 
    {
    	_context = context;
    	_enableSMS = getPreferenceBoolean(AppNeTV.PREF_SMS_ENABLE, false);
    	_enableSMSContent = getPreferenceBoolean(AppNeTV.PREF_SMS_CONTENT, false);
    	_SMSFilter = getPreferenceString(AppNeTV.PREF_SMS_FILTER, "");
    	
    	//This will fire off the 4 convenient functions of AppWidgetProvider class
    	super.onReceive(context, intent);
    	
        //Check if we receive some extra data from the Intent
        Bundle bundle = intent.getExtras();
        if (bundle == null)
        	return;
        
        //Check if we receive an SMS
        Object[] pdus = (Object[])bundle.get("pdus");
        
    	if (pdus != null)
    	{
    		if (!_enableSMS) {
    			Log.i(TAG, "NeTVWidget: " + pdus.length + " SMS received. SMS broadcasting is disabled");
    			return;
    		}
    		
            //Construct an Intent to start CommService
            Intent serviceIntent = new Intent(context, CommService.class);
            serviceIntent.putExtra(CommService.SERVICE_STARTED_BY, "widget");
            
        	String _ipaddress = getPreferenceString(AppNeTV.PREF_CHUMBY_IP_ADDRESS, "");
        	if (_ipaddress.length() > 0)
        		serviceIntent.putExtra("address", _ipaddress);
        	        	
    		//Process the SMS message 1-by-1
        	ArrayList<String> pendingPackages = new ArrayList<String>();
	        for (int i=0; i<pdus.length; i++)
	        {
	        	String title = "";
	        	String message = "";
	        	SmsMessage msg = SmsMessage.createFromPdu((byte[])pdus[i]);

	        	if (!_enableSMSContent)
	        	{
	        		title = "";
	        		message = context.getResources().getText(R.string.got_new_sms_from) + " " + msg.getOriginatingAddress().trim();
	        		Log.i(TAG, "SMS from " +  msg.getOriginatingAddress().trim() + " (not showing content)");
	        	}
	        	else if (hasRestrictedContent(msg.getMessageBody(), _SMSFilter))
	        	{
	        		title = "";
	        		message = context.getResources().getText(R.string.got_new_sms_from) + " " + msg.getOriginatingAddress().trim();
	        		Log.i(TAG, "Restricted SMS from " +  msg.getOriginatingAddress().trim() );
	        	}
	        	else
	        	{
	        		title = msg.getOriginatingAddress().trim();
	        		message = msg.getMessageBody().trim();
	        		Log.i(TAG, "Unrestricted SMS from " +  msg.getOriginatingAddress().trim());
	        	}        	
	        	pendingPackages.add( getTickerEventPackageString(message, title, "", "sms", "") );        	
	        }
	        serviceIntent.putStringArrayListExtra(CommService.PENDING_PACKAGES, pendingPackages);
	        
	    	//Start communication service
	    	ComponentName compName = context.startService(serviceIntent);
	    	if (compName == null)  		Log.e(TAG, "Communication Service failed to start");
	    	else			    		Log.d(TAG, "Started Communication Service");
    	}
    }

    @Override
    //When last widget is removed from home screen
    public void onDisabled (Context context)
    {
    	setPreferenceBoolean(AppNeTV.PREF_SMS_ENABLE, false);
    	Log.i(TAG, "NeTVWidget: all deleted. SMS broadcasting feature is disabled");
    	
    	super.onDisabled(context);
    }
    
    @Override
    //When a new widget is add to home screen, not necessary the first one
    public void onEnabled(Context context)
    {
    	Log.i(TAG, "NeTVWidget: created");
    	super.onEnabled(context);
    }
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        // Perform this loop procedure for each App Widget that belongs to this provider
    	final int N = appWidgetIds.length;
        for (int i=0; i<N; i++)
        {
            int appWidgetId = appWidgetIds[i];
            
            // Get the layout for the widget
            RemoteViews views = getWidgetUI(context, _enableSMS);
            
            // When user clicks on widget, launch this widget again with WIDGET_CONTROL
            Intent settingIntent = new Intent(context, NeTVWidgetPrefs.class);
            settingIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            settingIntent.setData(Uri.parse(settingIntent.toUri(Intent.URI_INTENT_SCHEME)));
            
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, settingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.netv_widget, pendingIntent);
            
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        
        super.onUpdate(context, appWidgetManager, appWidgetIds);
            
        // To prevent any Application Not Response (ANR) timeouts in 5 seconds, we perform the update in a service
        /*
    	Intent updateIntent = new Intent(context, UpdateService.class);
    	updateIntent(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
    	updateIntent(Uri.parse(updateIntent(Intent.URI_INTENT_SCHEME)));
    	updateIntent.putExtra("sms", _enableSMS);
        context.startService(updateIntent);
        */
    }
    
    /*
     * Return true if some keywords in SMS Filter are matched
     */
    public static boolean hasRestrictedContent(String text, String filter)
    {
    	if (text == null || filter == null || text.length() < 4 || filter.length() < 4)
    		return false;
    	text = text.toLowerCase().trim();
    	filter = filter.toLowerCase().trim();
    	
    	String delims = " .,{};/\\@&"; 
    	StringTokenizer st = new StringTokenizer(filter, delims);
    	while (st.hasMoreTokens())
    	{
    		String token = st.nextToken().toLowerCase();
    		if (text.contains( token )) {
    			Log.i(TAG, "NeTVWidget: matched filter '" + token + "'");
   				return true;
    		}
    	}
		return false;
    }
    
    /*
     * Construct a RemoteViews for widget UI
     * Note: static method
     */
    public static RemoteViews getWidgetUI(Context context, boolean enableSMS)
    {
    	RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_netv);
        if (enableSMS)		views.setImageViewResource(R.id.widget_icon, R.drawable.sms_on);
        else				views.setImageViewResource(R.id.widget_icon, R.drawable.sms_off);
        
        return views;
    }

    /*
    public static class UpdateService extends Service
    {
    	boolean _enableSMS = false;
    	
        @Override
        public void onStart(Intent intent, int startId)
        {
        	_enableSMS = intent.getBooleanExtra("sms", false);
        	
            // Build the widget UI
            RemoteViews updateViews = buildWidgetUI(this);

            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, NeTVWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        @Override
        public IBinder onBind(Intent intent)
        {
            return null;
        }

        //
        // Build a widget interface
        //
        public RemoteViews buildWidgetUI(Context context)
        {
            // Build an update that holds the updated widget contents
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_netv);
            if (_enableSMS)		views.setImageViewResource(R.id.widget_icon, R.drawable.sms_on);
            else				views.setImageViewResource(R.id.widget_icon, R.drawable.sms_off);

            //String wordTitle = matcher.group(1);
            //views.setTextViewText(R.id.word_title, wordTitle);

            // When user clicks on widget, launch this widget again with WIDGET_CONTROL
            Intent settingIntent = new Intent(context, NeTVWidget.class);
            settingIntent.setAction(WIDGET_CONTROL);                
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, settingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.netv_widget, pendingIntent);

            return views;
        }
    }
	*/
    
	
    // Bits and pieces of AppNeTV
    //------------------------------------------------------------------
    
	/**
	 * Get a named String from _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public String getPreferenceString(String key, String defaultValue)
	{
		SharedPreferences _prefs = PreferenceManager.getDefaultSharedPreferences(_context);
		return _prefs.getString(key, defaultValue);
	}
	
	/**
	 * Set a named String in _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public void setPreferenceString(String key, String newValue)
	{
		SharedPreferences _prefs = PreferenceManager.getDefaultSharedPreferences(_context);
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
		SharedPreferences _prefs = PreferenceManager.getDefaultSharedPreferences(_context);
		return _prefs.getBoolean(key, defaultValue);
	}
	
	/**
	 * Set a named String in _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public void setPreferenceBoolean(String key, boolean newValue)
	{
		SharedPreferences _prefs = PreferenceManager.getDefaultSharedPreferences(_context);
		SharedPreferences.Editor editor = _prefs.edit();       	
       	editor.putBoolean(key, newValue);
       	editor.commit();
	}
	
	/**
     * Send a TickerEvent command
     * 
     * @category DataComm Utility
     */
	public String getTickerEventPackageString(String message, String title, String image, String type, String level)
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
    	
    	dataHashMap.put(MessageReceiver.COMMAND_MESSAGE, MessageReceiver.COMMAND_TickerEvent);
		return queueMessage(dataHashMap);
	}
	
	/**
	 * @category DataComm Utility
	 */
	public String queueMessage(HashMap<String,String> map)
	{
		//Inject unicast address
		/*
		if (_unicastAddress.length() >= 5 && !map.containsKey(MessageReceiver.MESSAGE_KEY_ADDRESS))
			map.put(MessageReceiver.MESSAGE_KEY_ADDRESS, _unicastAddress);
		 */
		
		//Construct XML string (must have 1 root element)
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<xml>");
		
		//Command
		if (map.containsKey(MessageReceiver.COMMAND_MESSAGE)) {
			stringBuilder.append("<".concat(MessageReceiver.COMMAND_MESSAGE).concat(">"));
			stringBuilder.append( map.get(MessageReceiver.COMMAND_MESSAGE) );
			stringBuilder.append("</".concat(MessageReceiver.COMMAND_MESSAGE).concat(">"));
			map.remove(MessageReceiver.COMMAND_MESSAGE);
		}
		//Status
		if (map.containsKey(MessageReceiver.STATUS_MESSAGE)) {
			stringBuilder.append("<".concat(MessageReceiver.STATUS_MESSAGE).concat(">"));
			stringBuilder.append( map.get(MessageReceiver.STATUS_MESSAGE) );
			stringBuilder.append("</".concat(MessageReceiver.STATUS_MESSAGE).concat(">"));
			map.remove(MessageReceiver.STATUS_MESSAGE);
		}
		
		//Removed address field
		if (map.containsKey(MessageReceiver.MESSAGE_KEY_ADDRESS))
			map.remove(MessageReceiver.MESSAGE_KEY_ADDRESS);
			
		//No parameters
		int hashMapSize = map.size();
		if (hashMapSize < 1)
		{
			stringBuilder.append("</xml>");
			return stringBuilder.toString();
		}
		
		stringBuilder.append("<data>");

		//Single parameter
		if (hashMapSize == 1)
		{
			stringBuilder.append("<value>");
			for (HashMap.Entry<String, String> entry : map.entrySet())
				stringBuilder.append( entry.getValue() );
			stringBuilder.append("</value>");
		}
		
		//Multiple parameter
		else
		{
			for (HashMap.Entry<String, String> entry : map.entrySet())
			{
				stringBuilder.append("<".concat(entry.getKey()).concat(">"));
		    	stringBuilder.append( entry.getValue() );
		    	stringBuilder.append("</".concat(entry.getKey()).concat(">"));
			}
		}
		
	    stringBuilder.append("</data></xml>");    
		return stringBuilder.toString();
	}
}

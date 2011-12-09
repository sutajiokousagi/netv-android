
package com.bunniestudios.NeTV;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.Toast;


public class NeTVWidgetPrefs extends PreferenceActivity implements OnPreferenceClickListener
{
	int 			_appWidgetId;
	AppNeTV			_myApp;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.widget_prefs);
		
		//Setup parent application reference pointer
		_myApp = (AppNeTV)getApplicationContext();
		
		//Save the widget ID if this is initiated from a Widget
		_appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    	Bundle extras = getIntent().getExtras();
    	if (extras != null)
    	    _appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		
    	//Custom Reset button
    	Preference btnResetCPanel = (Preference) findPreference("reset_cpanel");
        btnResetCPanel.setOnPreferenceClickListener(this);
        
        //Device information
        for (int i=0; i<AppNeTV.DEVICE_INFO.length; i++)
        {
        	Preference customPref = findPreference(AppNeTV.DEVICE_INFO[i]);
        	if (customPref == null)
        		continue;
        	String value = getPreferenceString(AppNeTV.DEVICE_INFO[i], "");
        	customPref.setSummary(value);
        }
	}

    @Override
	public void onPause()
	{
    	if (_appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
    	{
	    	//Update the widget. This is so dumb!
	    	updateWidgetUI();
	    	
	    	// Make sure we pass back the original appWidgetId
	        Intent resultValue = new Intent();
	        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, _appWidgetId);
	        setResult(RESULT_OK, resultValue);
    	}
    	finish();
    	super.onPause();
	}
    
    private void updateWidgetUI()
    {
    	//Update the widget. This is so dumb!
    	final Context context = NeTVWidgetPrefs.this;
    	boolean _enableSMS = getPreferenceBoolean(AppNeTV.PREF_SMS_ENABLE, false);
    	
    	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);   	
    	RemoteViews views = NeTVWidget.getWidgetUI(context, _enableSMS);
    	appWidgetManager.updateAppWidget(_appWidgetId, views);
    }
    
    //------------------------------------------------------------------
    // Bits and pieces of AppNeTV
    //------------------------------------------------------------------
    
	/**
	 * Get a named String from _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public String getPreferenceString(String key, String defaultValue)
	{
		SharedPreferences _prefs = PreferenceManager.getDefaultSharedPreferences(NeTVWidgetPrefs.this);
		return _prefs.getString(key, defaultValue);
	}
	
	/**
	 * Set a named String in _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public void setPreferenceString(String key, String newValue)
	{
		SharedPreferences _prefs = PreferenceManager.getDefaultSharedPreferences(NeTVWidgetPrefs.this);
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
		SharedPreferences _prefs = PreferenceManager.getDefaultSharedPreferences(NeTVWidgetPrefs.this);
		return _prefs.getBoolean(key, defaultValue);
	}
	
	/**
	 * Set a named String in _prefs (SharedPreferences)
	 * 
	 * @category Shared Preferences
	 */
	public void setPreferenceBoolean(String key, boolean newValue)
	{
		SharedPreferences _prefs = PreferenceManager.getDefaultSharedPreferences(NeTVWidgetPrefs.this);
		SharedPreferences.Editor editor = _prefs.edit();       	
       	editor.putBoolean(key, newValue);
       	editor.commit();
	}

    //------------------------------------------------------------------
    // UI Event
    //------------------------------------------------------------------

	public boolean onPreferenceClick(Preference preference)
	{
		if (AppNeTV.PREF_SMS_ENABLE.equals(preference.getKey()))
		{
			updateWidgetUI();
			return false;
		}
		else if ("reset_cpanel".equals(preference.getKey()))
		{
			Toast.makeText(getBaseContext(), "Reseting NeTV UI...", Toast.LENGTH_LONG).show();
			if (_myApp != null)
			{
				_myApp.sendNeTVBrowserReset();
				_myApp.ResetUrlHTTP(null);
			}
			return true;
		}
		return false;
	}
}


package com.chumby.controlpanellite.chumbynetwork;

import java.util.Properties;

import org.w3c.dom.Document;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.chumby.Chumby;
import com.chumby.DaughterCardID;
import com.chumby.util.XMLHelper;
import com.chumby.util.XMLUtil;

public class ActivationByUserName
{
	private static final String TAG = "NeTV";

	public static final int ACTIVATING = 0;
	public static final int ACTIVATED = 1;
	public static final int NOT_ACTIVATED_BAD_USER = -1;
	public static final int NOT_ACTIVATED_BAD_DEVICE = -2;
	public static final int CANT_ACTIVATE = -3;

	public ActivationByUserName()
	{
	}
	
	public void activate(String userName,String password,String deviceName,final Handler handler)
	{
		Chumby chumby = Chumby.getInstance();
		String[] pathItems = {"device","activate",chumby.guid};
		
		Properties params = new Properties();
		params.setProperty("chumby[name]",deviceName);
		params.setProperty("username", userName);
		params.setProperty("pw", password);
		
		DaughterCardID.getInstance().args(params); // add DCID stuff
		
		Handler xmlHandler = new Handler()
		{
			public void handleMessage(Message message) {
				switch (message.what) {
					case XMLHelper.DID_SUCCEED: {
						Document document = (Document)message.obj;
						Log.i(TAG, "ActivationbyUserName: activate(): DID_SUCCEED "+XMLHelper.nodeToString(document));
						if (XMLUtil.firstChildOfType(document, "success")!=null) {
							Log.i(TAG, "ActivationbyUserName: activate(): successful activation");
							handler.sendMessage(Message.obtain(handler, ACTIVATED, this));
						} else {
							String errorMessage = XMLUtil.firstValueOfType(document, "error");
							Log.i(TAG, "ActivationbyUserName: activate(): error:"+errorMessage);
							if (errorMessage.contains("Missing user")) {
								Log.i(TAG, "ActivationByUserName:  -- bad user");
								handler.sendMessage(Message.obtain(handler, NOT_ACTIVATED_BAD_USER, this));
							} else if (errorMessage.contains("Validation failed: Name has already been taken")) {
								Log.i(TAG, "ActivationByUserName:  -- bad device");
								handler.sendMessage(Message.obtain(handler, NOT_ACTIVATED_BAD_DEVICE, this));
							} else {
								Log.i(TAG, "ActivationbyUserName: activate(): unknown error response: "+errorMessage);
								handler.sendMessage(Message.obtain(handler, NOT_ACTIVATED_BAD_USER, this));								
							}
						}
						break;
					}
					case XMLHelper.DID_ERROR: {
						Log.e(TAG, "ActivationbyUserName: activate(): DID_ERROR ");
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, CANT_ACTIVATE ,e));
						break;
					}
				}
			}
		};
		handler.sendMessage(Message.obtain(handler, ACTIVATING, this));
		XAPI.request(pathItems,params,XAPI.GET,false,xmlHandler);
	}

	//
	// possible responses:
	//
	// <success/>
	//
	// <error>
	//   Missing user
	// </error>
	//
	// <error>
	//   Validation failed: Name has already been taken
	// </error>
	//
	//private boolean parseResponse(Document document) {
	//	ChumbyLog.i(TAG+".parseResponse(): "+XMLHelper.nodeToString(document));
	//	return XMLUtil.firstChildOfType(document, "success")!=null;
	//}
}

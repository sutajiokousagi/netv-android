package com.chumby.controlpanellite.chumbynetwork;

import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.chumby.Chumby;
import com.chumby.util.ChumbyLog;
import com.chumby.util.GUID;
import com.chumby.util.XMLHelper;
import com.chumby.util.XMLUtil;

public class Device {
	private static final String TAG = "Device";
	public String id;
	public String idAlt;
	public String name;
	public String userID;
	public String userIDAlt;
	public String userName;
	public String profileID;
	public String profileName;
	public boolean anonymous;
	
	private static Device _instance;

	public static final int FETCHING_INFO = 0;
	public static final int GOT_INFO =  1;
	public static final int BAD_INFO = -1;

	public static final int ADDING_PROFILE = 0;
	public static final int ADDED_PROFILE =  1;
	public static final int CANT_ADD_PROFILE = -1;

	public static final int DEACTIVATING = 0;
	public static final int DEACTIVATED = 1;
	public static final int NOT_DEACTIVATED = -1;
	public static final int CANT_DEACTIVATE = -2;

	private Device() {
		name = "(Unknown "+TAG+")";
		Device._instance = this;
		anonymous = true;
	}
	
	public static Device getInstance() {
		if (Device._instance==null) {
			new Device();
		}
		return Device._instance;
	}
	
	public String toString() {
		return name;
	}

	public void fetch() {
		fetch(new Handler());
	}
	
	public void fetch(Handler handler) {
		fetch(Chumby.getInstance().guid,handler);
	}

	//See first command here https://internal.chumby.com/wiki/index.php/XAPI_Device
	public void fetch(String id, final Handler handler) {
		String[] pathItems = {"device","index",id};
		final Device device = this;
		Handler xmlHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case XMLHelper.DID_SUCCEED: {
						Document document = (Document)message.obj;
						if (parseResponse(document)) {
							handler.sendMessage(Message.obtain(handler, GOT_INFO, device));
						} else {
							handler.sendMessage(Message.obtain(handler, BAD_INFO, device));							
						}
						break;
					}
					case XMLHelper.DID_ERROR: {
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, BAD_INFO ,e));
						break;
					}
				}
			}
		};
		handler.sendMessage(Message.obtain(handler, FETCHING_INFO, this));
		XAPI.request(pathItems,null,XAPI.GET,true,xmlHandler);
	}

	public void setDefaultProfile(String profileID) {
		setDefaultProfile(profileID,new Handler());
	}
	
	public void setDefaultProfile(String profileID,final Handler handler) {
		String[] pathItems = {"device","set_default_profile"};
		Properties params = new Properties();
		params.setProperty("profile", profileID);
		final Device device = this;
		Handler xmlHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case XMLHelper.DID_SUCCEED: {
						Document document = (Document)message.obj;
						if (parseResponse(document))
							handler.sendMessage(Message.obtain(handler, GOT_INFO, device));
						else {
							handler.sendMessage(Message.obtain(handler, BAD_INFO, device));							
						}
						break;
					}
					case XMLHelper.DID_ERROR: {
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, BAD_INFO ,e));
						break;
					}
				}
			}
		};
		handler.sendMessage(Message.obtain(handler, FETCHING_INFO));
		XAPI.request(pathItems,params,XAPI.GET,true,xmlHandler);
	}

	// handles XML of the form:
	//
	//<chumby updated="Tue Dec 09 12:44:16 -0800 2008" authorized="Tue Dec 09 12:44:16 -0800 2008" created="Wed Nov 19 09:10:05 -0800 2008" id="0A9DC001-E2B5-83A3-9A51-1F032545011A">
	//	<name>bad_audio_dpf</name>
	//	<user id="D743152A-B65B-11DD-AACB-0030485CC034">vagabundo1</user>
	//	<profile id="FD4CF498-B65B-11DD-AACB-0030485CC034">Default</profile>
	//	<dcid version="" hash="" />
	//	<control_panel name="Control Panel" enable="false" />
	//</chumby>
	//

	private boolean parseResponse(Document document) {
		ChumbyLog.i(TAG+".parseResponse()");
		ChumbyLog.i(XMLHelper.nodeToString(document));
		try {
			Element chumbyNode = XMLUtil.firstChildOfType(document,"chumby");
			if (chumbyNode!=null) {
				this.anonymous = (chumbyNode.getAttribute("anonymous").equals("true"));
				this.id = chumbyNode.getAttribute("id");
				this.idAlt = GUID.guidOf(this.id);
				this.name = XMLUtil.firstValueOfType(chumbyNode,"name");
				Element userNode = XMLUtil.firstChildOfType(chumbyNode,"user");
				if (userNode!=null) {
					this.userID = userNode.getAttribute("id");
					this.userIDAlt = GUID.guidOf(this.userID);
					this.userName = XMLUtil.valueOf(userNode);
				} else {
					Log.w(TAG, "Device: parseResponse(): missing user node");
					Log.w(TAG, "Device: " + XMLHelper.nodeToString(document));
					return false;
				}
				Element profileNode = XMLUtil.firstChildOfType(chumbyNode,"profile");
				if (profileNode!=null) {
					this.profileID = profileNode.getAttribute("id");
					this.profileName = XMLUtil.valueOf(profileNode);
				} else {
					Log.w(TAG, "Device: parseResponse(): missing profile node");
					Log.w(TAG, "Device: " + XMLHelper.nodeToString(document));
					return false;
				}
			} else {
				Log.w(TAG, "Device: parseResponse(): missing chumby node");
				Log.w(TAG, "Device: " + XMLHelper.nodeToString(document));
				return false;
			}
		} catch (Exception ex) {
			Log.i(TAG, "Device: " + ex.getMessage());
			Log.i(TAG, "Device: " + XMLHelper.nodeToString(document));
			return false;
		}
		return true;
	}
	
	public void addProfile(String name,final Handler handler) {
		String[] pathItems = {"profile","add"};
		Properties params = new Properties();
		params.setProperty("name", name);
		final Device device = this;
		Handler xmlHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case XMLHelper.DID_SUCCEED: {
						Document document = (Document)message.obj;
						if (parseAddResponse(document)) {
							handler.sendMessage(Message.obtain(handler, ADDED_PROFILE, device));
						} else {
							handler.sendMessage(Message.obtain(handler, CANT_ADD_PROFILE, device));							
						}
						break;
					}
					case XMLHelper.DID_ERROR: {
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, CANT_ADD_PROFILE, device));
						break;
					}
				}
			}
		};
		handler.sendMessage(Message.obtain(handler, ADDING_PROFILE));
		XAPI.request(pathItems,params,XAPI.GET,true,xmlHandler);
	}
	
	private boolean parseAddResponse(Document document) {
		//Log.i("Device.parseAddResponse()");
		//Log.i(XMLHelper.nodeToString(document));
		return XMLUtil.firstChildOfType(document, "error")==null;
	}
	
	public void deactivate() {
		deactivate(new Handler());
	}

	public void deactivate(final Handler handler) {
		String[] pathItems = {"device","deregister"};
		final Device device = this;
		Handler xmlHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case XMLHelper.DID_SUCCEED: {
						Document document = (Document)message.obj;
						if (parseDeactivateResponse(document))
							handler.sendMessage(Message.obtain(handler, DEACTIVATED, device));
						else {
							handler.sendMessage(Message.obtain(handler, NOT_DEACTIVATED, device));							
						}
						break;
					}
					case XMLHelper.DID_ERROR: {
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, CANT_DEACTIVATE ,e));
						break;
					}
				}
			}
		};
		handler.sendMessage(Message.obtain(handler, DEACTIVATING));
		XAPI.request(pathItems,null,XAPI.GET,true,xmlHandler);
	}

	private boolean parseDeactivateResponse(Document document) {
		//Log.i("parseDeactivateResponse()");
		//Log.i(XMLHelper.nodeToString(document));
		return XMLUtil.firstChildOfType(document, "error")==null;
	}

}

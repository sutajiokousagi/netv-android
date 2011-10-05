package com.chumby.controlpanellite.chumbynetwork.notifications;

import com.chumby.Chumby;
import com.chumby.controlpanellite.chumbynetwork.XAPI;
import com.chumby.util.XMLHelper;
import com.chumby.util.XMLUtil;
import com.chumby.util.ChumbyLog;
import com.chumby.events.*;
import org.w3c.dom.*;

import java.util.*;

import android.os.*;

public class NotificationEvents extends EventDispatcher.EventDispatcherImpl {
	private static final String TAG = "NotificationEvents";

	public static final String PROFILE_CREATED = "profileCreated";
	public static final String PROFILE_DELETED = "profileDeleted";
	public static final String PROFILE_UPDATED = "profileUpdated";
	public static final String WIDGET_CREATED = "widgetCreated";
	public static final String WIDGET_DELETED = "widgetDeleted";
	public static final String WIDGET_UPDATED = "widgetUpdated";
	public static final String CHUM_CREATED = "chumCreated";
	public static final String CHUM_DELETED = "chumDeleted";
	public static final String CHUM_UPDATED = "chumUpdated";
	public static final String MESSAGE_CREATED = "messageCreated";
	public static final String MESSAGE_DELETED = "messageDeleted";
	public static final String DEVICE_CREATED = "deviceCreated";
	public static final String DEVICE_UPDATED = "deviceUpdated";
	public static final String DEVICE_DELETED = "deviceDeleted";

	private static NotificationEvents _instance;
	
	private String lastTimeStamp;

	private NotificationEvents() {
		_instance = this;
		lastTimeStamp = Long.toString((new Date()).getTime()/1000);
	}
	
	public static NotificationEvents getInstance() {
		if (_instance==null) {
			new NotificationEvents();
			start();
		}
		return _instance;
	}
	
	public void fetch() {
		ChumbyLog.i(TAG+".fetch()");
		if (!XAPI.hasNetwork()) {
			ChumbyLog.i(TAG+".fetch(): no network, skipping");
		}
		String[] pathItems = {"notify","list"};
		Properties params = new Properties();
		if (lastTimeStamp!=null) {
			//ChumbyLog.i(TAG+".fetch(): sending timeStamp "+lastTimeStamp);
			params.put("timestamp",lastTimeStamp);
		}
		Handler xmlHandler = new Handler() {
			public void handleMessage(Message message) {
				//ChumbyLog.i("NotificationEvents(): "+message);
				switch (message.what) {
					case XMLHelper.DID_SUCCEED: {
						Document document = (Document)message.obj;
						parseResponse(document);
						break;
					}
					case XMLHelper.DID_ERROR: {
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						break;
					}
				}
			}
		};
		XAPI.request(pathItems,params,XAPI.GET,true,xmlHandler);
	}
	
	private void parseResponse(Document document) {
		//ChumbyLog.i(TAG+".parseResponse()");
		//ChumbyLog.i(XMLHelper.nodeToString(document));
		Element notificationsNode = XMLUtil.firstChildOfType(document,"notifications");
		if (notificationsNode!=null) {
			lastTimeStamp = notificationsNode.getAttribute("timestamp");
			//ChumbyLog.i(TAG+".parseResponse(): new lastTimeStamp: "+lastTimeStamp);
			ArrayList<Element> notificationNodes = XMLUtil.childrenOfType(notificationsNode, "notification");
			for (int i=0;i<notificationNodes.size();i++) {
				Element notificationNode = notificationNodes.get(i);
				NotificationType type = NotificationType.parse(notificationNode.getAttribute("type"));
				ChumbyLog.i(TAG+".parseResponse(): processing notification "+type.toString());
				if (type==NotificationType.PROFILE) {
					ChannelNotification notification = new ChannelNotification();
					notification.parse(notificationNode);
					NotificationAction action = notification.action;
					if (action==NotificationAction.CREATE) {
						dispatchEvent(new Event(PROFILE_CREATED,notification));
					} else if (action==NotificationAction.DELETE) {
						dispatchEvent(new Event(PROFILE_DELETED,notification));					
					} else if (action==NotificationAction.UPDATE) {
						dispatchEvent(new Event(PROFILE_UPDATED,notification));
					} else {
						ChumbyLog.w(TAG+".parseResponse() unknown action "+action.toString()+" for "+type.toString());
					}
				} else if (type==NotificationType.CHUM) {
					ChumNotification notification = new ChumNotification();
					notification.parse(notificationNode);
					NotificationAction action = notification.action;
					if (action==NotificationAction.CREATE) {
						dispatchEvent(new Event(CHUM_CREATED,notification));
					} else if (action==NotificationAction.DELETE) {
						dispatchEvent(new Event(CHUM_DELETED,notification));					
					} else if (action==NotificationAction.UPDATE) {
						dispatchEvent(new Event(CHUM_UPDATED,notification));
					} else {
						ChumbyLog.w(TAG+".parseResponse() unknown action "+action.toString()+" for "+type.toString());
					}				
				} else if (type==NotificationType.DEVICE) {
					DeviceNotification notification = new DeviceNotification();
					notification.parse(notificationNode);
					NotificationAction action = notification.action;
					if (notification.id.equals(Chumby.getInstance().guid)) {
						if (action==NotificationAction.CREATE) {
							dispatchEvent(new Event(DEVICE_CREATED,notification));
						} else if (action==NotificationAction.DELETE) {
							dispatchEvent(new Event(DEVICE_DELETED,notification));					
						} else if (action==NotificationAction.UPDATE) {
							dispatchEvent(new Event(DEVICE_UPDATED,notification));
						} else {
							ChumbyLog.w(TAG+".parseResponse() unknown action "+action.toString()+" for "+type.toString());
						}
					} else {
						ChumbyLog.w(TAG+".parseResponse(): device id mismatch:"+notification.id);
					}
				} else if (type==NotificationType.MESSAGE) {
					MessageNotification notification = new MessageNotification();
					notification.parse(notificationNode);
					NotificationAction action = notification.action;
					if (action==NotificationAction.CREATE) {
						dispatchEvent(new Event(MESSAGE_CREATED,notification));
					} else if (action==NotificationAction.DELETE) {
						dispatchEvent(new Event(MESSAGE_DELETED,notification));					
					} else {
						ChumbyLog.w(TAG+".parseResponse() unknown action "+action.toString()+" for "+type.toString());
					}				
				} else if (type==NotificationType.WIDGET_INSTANCE) {
					WidgetNotification notification = new WidgetNotification();
					notification.parse(notificationNode);
					NotificationAction action = notification.action;
					if (action==NotificationAction.CREATE) {
						dispatchEvent(new Event(WIDGET_CREATED,notification));
					} else if (action==NotificationAction.DELETE) {
						dispatchEvent(new Event(WIDGET_DELETED,notification));					
					} else if (action==NotificationAction.UPDATE) {
						dispatchEvent(new Event(WIDGET_UPDATED,notification));
					} else {
						ChumbyLog.w(TAG+".parseResponse() unknown action "+action.toString()+" for "+type.toString());
					}				
				} else {
					ChumbyLog.w("NotificationEvents.parseResponse(): unhandled type "+type.toString());
				}
			}
		}
	}

	//
	// simple polling system using delayed Messages
	//
	private static final long POLL_INTERVAL = 60000; // ms
	private static final int POLL_NOTIFICATIONS = 0;

	public static void start() {
		//ChumbyLog.i("NotificationEvents.start()");
		final Handler handler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case POLL_NOTIFICATIONS:
						NotificationEvents.getInstance().fetch();
						break;
					default:
						super.handleMessage(message);
				}
				this.sendMessageDelayed(Message.obtain(this,POLL_NOTIFICATIONS),POLL_INTERVAL);			
			}
		};
		handler.sendMessageDelayed(Message.obtain(handler,POLL_NOTIFICATIONS),POLL_INTERVAL);
	}
}

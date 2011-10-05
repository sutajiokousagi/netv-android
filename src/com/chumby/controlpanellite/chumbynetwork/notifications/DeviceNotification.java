package com.chumby.controlpanellite.chumbynetwork.notifications;

import com.chumby.util.XMLHelper;
import com.chumby.util.XMLUtil;
import com.chumby.util.ChumbyLog;
import org.w3c.dom.*;

public class DeviceNotification extends Notification {
	private static final String TAG = "DeviceNotification";
	
	public String id;
	public String name;
	public String registration;
	
	public static final String COMPLETED = "completed";
	
	public void parse(Element node) {
		//ChumbyLog.i(TAG+".parse() "+XMLHelper.nodeToString(node));
		super.parse(node);
		Element messageNode = XMLUtil.firstChildOfType(node,"message");
		if (messageNode!=null) {
			Element chumbyNode = XMLUtil.firstChildOfType(messageNode,"chumby");
			if (chumbyNode!=null) {
				id = chumbyNode.getAttribute("id");
				Element detailNode = XMLUtil.firstChildOfType(chumbyNode, "detail");
				if (detailNode!=null) {
					name = detailNode.getAttribute("name");
					registration = detailNode.getAttribute("registration");
				} else {
					ChumbyLog.w(TAG+".parse(): missing detail node "+XMLHelper.nodeToString(chumbyNode));				
				}
				this.action = NotificationAction.parse(chumbyNode.getAttribute("action"));
			} else {
				ChumbyLog.w(TAG+".parse(): missing chumby node "+XMLHelper.nodeToString(messageNode));				
			}
		} else {
			ChumbyLog.w(TAG+".parse(): missing message node in "+XMLHelper.nodeToString(node));
		}
	}
}

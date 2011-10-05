package com.chumby.controlpanellite.chumbynetwork.notifications;

import com.chumby.util.XMLHelper;
import com.chumby.util.XMLUtil;
import com.chumby.util.ChumbyLog;
import org.w3c.dom.*;

public class ChannelNotification extends Notification {
	private static final String TAG = "ChannelNotification";
	
	public String id;
	public String name;
	
	public void parse(Element node) {
		//ChumbyLog.i(TAG+".parse() "+XMLHelper.nodeToString(node));
		super.parse(node);
		Element messageNode = XMLUtil.firstChildOfType(node,"message");
		if (messageNode!=null) {
			Element profileNode = XMLUtil.firstChildOfType(messageNode,"profile");
			if (profileNode!=null) {
				id = profileNode.getAttribute("id");
				Element detailNode = XMLUtil.firstChildOfType(profileNode, "detail");
				if (detailNode!=null) {
					name = detailNode.getAttribute("name");
				} else {
					ChumbyLog.w(TAG+".parse(): missing detail node "+XMLHelper.nodeToString(profileNode));				
				}
				this.action = NotificationAction.parse(profileNode.getAttribute("action"));
			} else {
				ChumbyLog.w(TAG+".parse(): missing profile node "+XMLHelper.nodeToString(messageNode));				
			}
		} else {
			ChumbyLog.w(TAG+".parse(): missing message node in "+XMLHelper.nodeToString(node));
		}
	}
}

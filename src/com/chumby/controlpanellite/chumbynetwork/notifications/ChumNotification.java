package com.chumby.controlpanellite.chumbynetwork.notifications;

import com.chumby.util.XMLHelper;
import com.chumby.util.XMLUtil;
import com.chumby.util.ChumbyLog;
import org.w3c.dom.*;

public class ChumNotification extends Notification {
	private static final String TAG = "ChumNotification";
	
	public String id;
	
	public void parse(Element node) {
		//ChumbyLog.i(TAG+".parse()");
		super.parse(node);
		Element messageNode = XMLUtil.firstChildOfType(node,"message");
		if (messageNode!=null) {
			Element friendshipNode = XMLUtil.firstChildOfType(messageNode,"friendship");
			if (friendshipNode!=null) {
				id = friendshipNode.getAttribute("id");
			} else {
				ChumbyLog.w(TAG+".parse(): missing friendship node "+XMLHelper.nodeToString(messageNode));				
			}
			this.action = NotificationAction.parse(friendshipNode.getAttribute("action"));
		} else {
			ChumbyLog.w(TAG+".parse(): missing message node in "+XMLHelper.nodeToString(node));
		}
	}
}

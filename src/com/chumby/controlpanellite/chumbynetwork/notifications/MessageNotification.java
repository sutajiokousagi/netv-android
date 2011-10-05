package com.chumby.controlpanellite.chumbynetwork.notifications;

import com.chumby.util.XMLHelper;
import com.chumby.util.XMLUtil;
import com.chumby.util.ChumbyLog;
import org.w3c.dom.*;

public class MessageNotification extends Notification {
	private static final String TAG = "MessageNotification";
	
	public String id;
	public String name;
	
	public void parse(Element node) {
		//ChumbyLog.i(TAG+".parse()");
		super.parse(node);
		Element messageNode = XMLUtil.firstChildOfType(node,"message");
		if (messageNode!=null) {
			Element message2Node = XMLUtil.firstChildOfType(messageNode,"message");
			if (message2Node!=null) {
				this.action = NotificationAction.parse(message2Node.getAttribute("action"));
			} else {
				ChumbyLog.w(TAG+".parse(): missing message subnode "+XMLHelper.nodeToString(messageNode));				
			}
		} else {
			ChumbyLog.w(TAG+".parse(): missing message node in "+XMLHelper.nodeToString(node));
		}
	}
}

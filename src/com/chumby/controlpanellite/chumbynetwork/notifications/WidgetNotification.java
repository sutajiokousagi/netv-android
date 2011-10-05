package com.chumby.controlpanellite.chumbynetwork.notifications;

import com.chumby.util.XMLHelper;
import com.chumby.util.XMLUtil;
import com.chumby.util.ChumbyLog;
import org.w3c.dom.*;

public class WidgetNotification extends Notification {
	private static final String TAG = "WidgetNotification";
	
	public String id;
	public String profileID;
	
	public void parse(Element node) {
		//ChumbyLog.i(TAG+".parse()");
		super.parse(node);
		Element messageNode = XMLUtil.firstChildOfType(node,"message");
		if (messageNode!=null) {
			Element widgetInstanceNode = XMLUtil.firstChildOfType(messageNode,"widgetinstance");
			if (widgetInstanceNode!=null) {
				id = widgetInstanceNode.getAttribute("id");
				Element detailNode = XMLUtil.firstChildOfType(widgetInstanceNode, "detail");
				if (detailNode!=null) {
					profileID = detailNode.getAttribute("profile_id");
				} else {
					ChumbyLog.w(TAG+".parse(): missing detail node "+XMLHelper.nodeToString(widgetInstanceNode));				
				}
				this.action = NotificationAction.parse(widgetInstanceNode.getAttribute("action"));
			} else {
				ChumbyLog.w(TAG+".parse(): missing widgetinstance node "+XMLHelper.nodeToString(messageNode));				
			}
		} else {
			ChumbyLog.w(TAG+".parse(): missing message node in "+XMLHelper.nodeToString(node));
		}
	}
}

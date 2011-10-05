package com.chumby.controlpanellite.chumbynetwork.notifications;

import com.chumby.util.XMLUtil;
import org.w3c.dom.*;

public class Notification {
	String id;
	NotificationType type;
	NotificationAction action;
	String from;
	String to;
	String subject;
	String message;
	String status;
	
	public Notification() {
		// nothing
	}
	
	public void parse(Element node) {
		id = node.getAttribute("id");
		type = NotificationType.parse(node.getAttribute("type"));
		//action = NotificationAction.parse(node.getAttribute("action"));
		from = XMLUtil.firstValueOfType(node,"from");
		to = XMLUtil.firstValueOfType(node,"to");
		subject = XMLUtil.firstValueOfType(node,"subject");
		message = XMLUtil.firstValueOfType(node,"message");
		status = XMLUtil.firstValueOfType(node,"status");
	}
}

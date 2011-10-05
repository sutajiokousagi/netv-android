package com.chumby.controlpanellite.chumbynetwork.notifications;

import com.chumby.controlpanellite.chumbynetwork.notifications.Notification;

public class NotificationEvent {
	String type;
	Notification notification;

	public NotificationEvent(String t,Notification n) {
		type = t;
		notification = n;
	}
}

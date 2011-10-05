package com.chumby.controlpanellite.chumbynetwork.notifications;

public class NotificationAction {
	private static final String TAG="NotificationAction";
	public final static NotificationAction CREATE = new NotificationAction("create");
	public final static NotificationAction DELETE = new NotificationAction("delete");
	public final static NotificationAction UPDATE = new NotificationAction("update");
	public final static NotificationAction INVALID = new NotificationAction("invalid");
	
	String type;
	
	private NotificationAction(String t) {
		type = t;
	}
	
	public static String toString(NotificationAction action) {
		return action.type;
	}
	
	public String toString() {
		return "["+TAG+" "+type+"]";
	}

	public static NotificationAction parse(String s) {
		s = s.toLowerCase();
		if (s.equals("create")) return CREATE;
		if (s.equals("delete")) return DELETE;
		if (s.equals("update")) return UPDATE;
		return INVALID;
 	}
}

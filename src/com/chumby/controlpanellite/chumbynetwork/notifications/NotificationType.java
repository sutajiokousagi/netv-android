package com.chumby.controlpanellite.chumbynetwork.notifications;
//import com.chumby.util.ChumbyLog;

public class NotificationType {
	private static final String TAG="NotificationType";

	public final static NotificationType PROFILE= new NotificationType("profile");
	public final static NotificationType WIDGET_INSTANCE = new NotificationType("widgetinstance");
	public final static NotificationType CHUM = new NotificationType("chum");
	public final static NotificationType MESSAGE= new NotificationType("message");
	public final static NotificationType DEVICE= new NotificationType("chumby");
	public final static NotificationType SYSTEM= new NotificationType("system");
	public final static NotificationType INVALID= new NotificationType("invalid");
	
	private String type;
	
	private NotificationType(String t) {
		type = t;
	}

	public String toString() {
		return "["+TAG+" "+type+"]";
	}

	public static String toString(NotificationType notificationType) {
		return  notificationType.type;
	}
	
	public static NotificationType parse(String s) {
		//ChumbyLog.i(TAG+".parse(): "+s);
		s = s.toLowerCase();
		if (s.equals("profile")) return PROFILE;
		if (s.equals("widgetinstance")) return WIDGET_INSTANCE;
		if (s.equals("chum")) return CHUM;
		if (s.equals("message")) return MESSAGE;
		if (s.equals("chumby")) return DEVICE;
		if (s.equals("system")) return SYSTEM;
		return NotificationType.INVALID;
	}
}

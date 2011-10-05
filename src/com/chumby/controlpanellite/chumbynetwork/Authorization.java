package com.chumby.controlpanellite.chumbynetwork;

//import java.util.Date;
import java.util.Properties;

import com.chumby.Chumby;
import com.chumby.util.XMLHelper;
import com.chumby.util.XMLUtil;
import com.chumby.util.ChumbyLog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.os.*;

public class Authorization {
	private static final String TAG = "Authorization";
	
	public static final int AUTHORIZING = 0;
	public static final int AUTHORIZED = 1;
	public static final int NOT_AUTHORIZED = -1;
	public static final int CANT_AUTHORIZE = -2;

	public String id;
	public String name;
	public boolean authorized;
	
	public static String chumbyID = "";
	public static String chumbyName = "";
	public static boolean isAuthorized = false;
	
	public Authorization() {
		authorized = false;
	}
	
	public void authorize() {
		authorize(new Handler());
	}

	public void authorize(final Handler handler) {
		//ChumbyLog.i(TAG+".authorize()");
		Chumby chumby = Chumby.getInstance();
		final Authorization authorization = this;
		//String path = chumby.makeURL("/xml/authorize?id="+chumby.guid+"&hw="+chumby.hardwareVersion+"&sw="+chumby.softwareVersion+"&fw="+chumby.firmwareVersion+"&config="+chumby.platform+"&nocache="+Long.toString((new Date().getTime())));
		String[] pathItems = {"device","authorize",chumby.guid};
		Properties params = new Properties();
		params.setProperty("hw",chumby.hardwareVersion);
		params.setProperty("sw", chumby.softwareVersion);
		params.setProperty("fw", chumby.firmwareVersion);
		params.setProperty("config", chumby.platform);
		Handler xmlHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case XMLHelper.DID_SUCCEED: {
						Document document = (Document)message.obj;
						parseResponse(document);
						handler.sendMessage(Message.obtain(handler, isAuthorized ? AUTHORIZED : NOT_AUTHORIZED,authorization));
						break;
					}
					case XMLHelper.DID_ERROR: {
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, CANT_AUTHORIZE ,e));
						break;
					}
				}
			}
		};
		handler.sendMessage(Message.obtain(handler,AUTHORIZING,this));
		//ChumbyLog.i(TAG+".authorize(): "+path);
		//XMLHelper.load(path,xmlHandler);
		XAPI.request(pathItems,params,XAPI.GET,XAPI.INSECURE,XAPI.USE_HTTPS,xmlHandler);		
	}
	
	private void parseResponse(Document document) {
		ChumbyLog.i(TAG+".parseResponse()");
		ChumbyLog.i(XMLHelper.nodeToString(document));
		Element chumbyNode = XMLUtil.firstChildOfType(document,"chumby");
		this.authorized = false;
		Authorization.chumbyID = ""; // assume not authorized
		Authorization.chumbyName = "";
		Authorization.isAuthorized = false;
		if (chumbyNode!=null) {
			this.id = chumbyNode.getAttribute("id");
			Element nameNode = XMLUtil.firstChildOfType(chumbyNode,"name");
			if (nameNode!=null) {
				this.name = XMLUtil.valueOf(nameNode);
				this.authorized = true;
				Authorization.chumbyID = this.id;
				Authorization.chumbyName = this.name;
				Authorization.isAuthorized = true;
			}
		}
	}
}

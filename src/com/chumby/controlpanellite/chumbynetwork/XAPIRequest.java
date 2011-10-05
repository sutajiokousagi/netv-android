package com.chumby.controlpanellite.chumbynetwork;

import java.util.*;
import org.w3c.dom.Document;
import com.chumby.util.XMLHelper;
import com.chumby.util.ChumbyLog;
import com.chumby.controlpanellite.chumbynetwork.XAPI;

import android.os.*;

public class XAPIRequest {
	private static final String TAG = "XAPIRequest";

	String[] pathItems;
	Properties params;
	String method;
	boolean secure;
	boolean useHTTPS;
	Handler handler;
	
	private static ArrayList<XAPIRequest> _queue;
	private static XAPIRequest currentRequest;
	
	public XAPIRequest(String[] pathItems,Properties params, String method,boolean secure,final Handler handler) {
		this.pathItems = pathItems;
		this.params = params;
		this.method = method;
		this.secure = secure;
		this.useHTTPS = XAPI.USE_HTTP;
		this.handler = handler;
		queue().add(this);
		processNextItem();
		
	}
	
	public XAPIRequest(String[] pathItems,Properties params, String method,boolean secure,boolean useHTTPS,final Handler handler) {
		this.pathItems = pathItems;
		this.params = params;
		this.method = method;
		this.secure = secure;
		this.useHTTPS = useHTTPS;
		this.handler = handler;
		queue().add(this);
		processNextItem();
	}
	
	static public void flushQueue() {
		if (_queue!=null && _queue.size()>0) {
			//ChumbyLog.i(TAG+".flushQueue(): killing queue with "+Integer.toString(_queue.size())+" entries");
			_queue = new ArrayList<XAPIRequest>();
		}
		currentRequest = null;
	}

	public String toString() {
		String s = "";
		if (pathItems!=null) {
			for (int i=0;i<pathItems.length;i++) {
				s = s+"/"+pathItems[i];
			}
		}
		return "[XAPIRequest pathItems:"+s+"]";
	}

	private static ArrayList<XAPIRequest> queue() {
		if (_queue==null) {
			_queue = new ArrayList<XAPIRequest>();
		}
		return _queue;
	}

	private static void processNextItem() {
		//ChumbyLog.i(TAG+".processNextItem()");
		if (currentRequest==null) {
			if (queue().size()!=0) {
				currentRequest = queue().get(0);
				queue().remove(0);
				currentRequest.process();
			} else {
				//ChumbyLog.i(TAG+".processNextItem(): queue empty");
			}
		} else {
			//ChumbyLog.w(TAG+".processNextItem(): currentRequest still in progress: "+currentRequest.toString());
		}
	}

	private void process() {
		//ChumbyLog.i(TAG+".process()");
		checkAuthentication();
	}
	
	private void checkAuthentication() {
		//ChumbyLog.i(TAG+".checkAuthentication()");
		if (secure) {
			if (!XAPI.checkAuth()) {
				doAuthentication();
			} else {
				checkRenewal();
			}
		} else {
			processRequest();
		}
	}

	private void doAuthentication() {
		//ChumbyLog.i(TAG+".doAuthentication()");
		Handler authHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case XAPI.AUTHENTICATED:
					case XAPI.NOT_AUTHENTICATED:
						didAuthenticate();
						break;
				}
			}
		};
		XAPI.getInstance().authorizeDLA(Authorization.chumbyName,authHandler);
	}

	private void didAuthenticate() {
		//ChumbyLog.i(TAG+".didAuthenticate()");
		if (!XAPI.checkAuth()) { // still not authenticated?
			// XXX DSM probably ought to trigger ovals here - handle elsewhere?
			//ChumbyLog.w(TAG+".didAuthenticate(): authentication required");
		} else {
			this.checkRenewal(); // check for renewal
		}
	}

	private void checkRenewal() {
		//ChumbyLog.i(TAG+".checkRenewal()");
		if (XAPI.shouldRenew()) {
			doRenew();
		} else {
			processRequest();
		}
	}
	
	private void doRenew() {
		//ChumbyLog.i(TAG+".doRenew()");
		Handler renewHandler = new Handler() {
			public void handleMessage(Message message) {
				didRenew(message.what);
			}
		};
		XAPI.getInstance().renew(renewHandler);
	}

	private void didRenew(int response) {
		//ChumbyLog.i(TAG+".didRenew()");
		processRequest();
	}
	
	private void processRequest() {
		String url = makeURL();
		if (useHTTPS && url.startsWith("http://")) {
			url = url.replace("http://", "https://");
		}
		//ChumbyLog.i(TAG+".processRequest() url="+url);
		Handler xmlHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
				case XMLHelper.DID_SUCCEED:
					didProcessRequest(message.what,(Document)message.obj);
					break;
				case XMLHelper.DID_ERROR:
					didFailRequest(message.what,message.obj);
					break;
				default:
					ChumbyLog.e(TAG+".processRequest.xmlHandler.handleMessage(): unknown message: "+message.what);
					break;
				}
			}
		};
		if (method.equals(XAPI.GET)) {
			XMLHelper.load(url,xmlHandler);
		} else {
			XMLHelper.sendVarsAndLoad(url,params,xmlHandler);
		}
	}
	
	private void didProcessRequest(int response,Document document) {
		//ChumbyLog.i(TAG+".didProcessRequest()");
		handler.sendMessage(Message.obtain(handler,response,document));
		currentRequest = null;
		processNextItem();
	}

	private void didFailRequest(int response,Object obj) {
		ChumbyLog.w(TAG+".didFailRequest(): "+Integer.toString(response));
		handler.sendMessage(Message.obtain(handler,response,obj));
		currentRequest = null;
		processNextItem();
	}

	private String makeURL() {
		if (secure) {
			return XAPI.makeURLSecure(pathItems, params, method);
		} else {
			return XAPI.makeURLInsecure(pathItems, params, method);
		}
	}
}


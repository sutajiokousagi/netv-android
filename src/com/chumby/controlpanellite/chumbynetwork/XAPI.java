package com.chumby.controlpanellite.chumbynetwork;


import com.chumby.util.*;
import com.chumby.Chumby;
import com.chumby.controlpanellite.chumbynetwork.XAPIRequest;

import java.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.content.Context;
import android.os.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class XAPI {
	private static final String TAG = "XAPI";

	public static final int AUTHENTICATING = 0;
	public static final int AUTHENTICATED = 1;
	public static final int NOT_AUTHENTICATED = -1;
	public static final int CANT_AUTHENTICATE = -2;
	
	private static boolean authenticated = false;
	static String oauth_consumer_secret;
	static String oauth_consumer_key;
	static long oauth_session_expires;
	static long oauth_should_renew;
	static long oauth_nonce = (new Date()).getTime();
	
	public final static String GET = "GET";
	public final static String POST = "POST";

	public final static Boolean SECURE = true;
	public final static Boolean INSECURE = false;
	
	public final static Boolean USE_HTTPS = true;
	public final static Boolean USE_HTTP = false;
	
	private static XAPI _instance;
	
	public static Context context;
	
	private XAPI() {
		XAPI._instance = this;
		XAPI.authenticated = false;
	}
	
	public static XAPI getInstance() {
		if (_instance==null) {
			new XAPI();
		}
		return XAPI._instance;
	}

	public static void setContext(Context c) {
		context = c;
	}

	public static boolean hasNetwork() {
		ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivityManager!=null) {
			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
			if (networkInfo!=null) {
				return networkInfo.isConnected();
			}
		}
		return false;
	}

	// sends an authorization _request of the form:
	//
	// <auth_request type="usr">
	//  <username>user</username>
	//  <password encoding='MD5-HEX'>70ff9f33926b1865edde645073a37bd1fffba984</password>
	//  <chumby>12345678-ABCD-1234-ABCD-0123456789AB</chumby>
	//  <oauth_consumer_secret>a81be4e9b20632860d20a64c054c4150</oauth_consumer_secret> <!-- client generated key used for signing _requests -->
	// </auth_request>
	//
	public void authorizeUSR(String userName, String password,final Handler handler) {
		XAPI.oauth_consumer_secret = GUID.md5(GUID.md5(userName+password+password));
		XMLBuilder b = XMLBuilder.create("auth_request").a("type", "usr");
		b.et("username",userName);
		b.et("password",password).a("encoding", "MD5-HEX");
		b.et("chumby",Chumby.getInstance().guid);
		b.et("oauth_consumer_secret",XAPI.oauth_consumer_secret);
		Handler authHandler = new Handler() {
			public void handleMessage(Message message) {
				//ChumbyLog.i("Authorization.handleMessage(): "+message);
				switch (message.what) {
					case XMLHelper.DID_SUCCEED: {
						Document document = (Document)message.obj;
						parseAuthorizeResponse(document);
						handler.sendMessage(Message.obtain(handler, authenticated ? AUTHENTICATED : NOT_AUTHENTICATED));
						break;
					}
					case XMLHelper.DID_ERROR: {
						Exception e = (Exception) message.obj;
						//e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, CANT_AUTHENTICATE ,e));
						break;
					}
				}
			}
		};
		handler.sendMessage(Message.obtain(handler,AUTHENTICATING));
		XMLHelper.sendAndLoad(Chumby.getInstance().makeSecureURL("/xapis/auth/create"),b.getDocument(),authHandler);
		b = null;
	}

	// sends an auth _request of the form:
	//
	// <auth_request type='dla'>
	//  <chumby>12345678-ABCD-1234-ABCD-0123456789AB</chumby>
	//  <auth_id>12345678-ABCD-1234-ABCD-0123456789AB</auth_id>
	//  <oauth_consumer_secret>a81be4e9b20632860d20a64c054c4150</oauth_consumer_secret> <!-- client generated key used for signing _requests -->
	// </auth_request>
	//
	public void authorizeDLA(String name,final Handler handler) {
		//ChumbyLog.i(TAG+".authorizeDLA(): "+name);
		String guid = Chumby.getInstance().guid;
		XAPI.oauth_consumer_secret = GUID.md5(GUID.md5(guid)).toUpperCase();
		String auth_id = GUID.asGUID(GUID.md5(GUID.md5(guid+name))).toUpperCase();
		XMLBuilder b = XMLBuilder.create("auth_request").a("type","dla");
		b.et("auth_id",auth_id);
		b.et("chumby",guid);
		b.et("oauth_consumer_secret",XAPI.oauth_consumer_secret);
		Handler authHandler = new Handler() {
			public void handleMessage(Message message) {
				//ChumbyLog.i(TAG+"authorizeDLA.authHandler.handleMessage(): "+message);
				switch (message.what) {
					case XMLHelper.DID_SUCCEED: {
						Document document = (Document)message.obj;
						parseAuthorizeResponse(document);
						handler.sendMessage(Message.obtain(handler, authenticated ? AUTHENTICATED : NOT_AUTHENTICATED));
						break;
					}
					case XMLHelper.DID_ERROR: {
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, CANT_AUTHENTICATE ,e));
						break;
					}
				}
			}
		};
		handler.sendMessage(Message.obtain(handler,AUTHENTICATING));
		XMLHelper.sendAndLoad(Chumby.getInstance().makeSecureURL("/xapis/auth/create"),b.getDocument(),authHandler);
		b = null;
	}
	
	// parse responses of the form
	//
	//<oauth_session valid_for="599">B5EECABC-5792-11DE-9068-0030488D0168</oauth_session>
	//
	private void parseAuthorizeResponse(Document document) {
		//ChumbyLog.i(TAG+".parseAuthorizeResponse()");
		//ChumbyLog.i(XMLHelper.nodeToString(document));
		Element oauthSessionNode = XMLUtil.firstChildOfType(document,"oauth_session");
		if (oauthSessionNode!=null) {
			String validFor = oauthSessionNode.getAttribute("valid_for");
			long msToExpiration;
			try {
				msToExpiration = Integer.parseInt(validFor)*1000;
			} catch (Exception ex) {
				ChumbyLog.i(ex.getMessage());
				msToExpiration = 1000*60*10; // ten minutes
			}
			long msNow = (new Date()).getTime();
			XAPI.oauth_session_expires = msNow + msToExpiration;
			XAPI.oauth_should_renew = msNow + Math.round(0.8*msToExpiration);
			try {
				XAPI.oauth_consumer_key = oauthSessionNode.getFirstChild().getNodeValue();
			} catch (Exception ex) {
				//ChumbyLog.i(ex.getMessage());
				ChumbyLog.w(TAG+"parseAuthorizationResponse(): exception parsing oauthSession node");
				ChumbyLog.w(XMLHelper.nodeToString(document));
				XAPI.oauth_consumer_key = null;
			}
			// XXX DSM TEST XAPI.oauth_consumer_key = "ABCDABCD-ABCD-ABCD-ABCD-ABCDABCDABCD";
			XAPI.authenticated = XAPI.oauth_consumer_key != null;
			oauthSessionNode = null;
		} else {
			XAPI.authenticated = false;			
		}
	}
	
	public static boolean checkAuth() {
		//ChumbyLog.i("XAPI.checkAuth(): authenticated:"+XAPI.authenticated+" expires:"+XAPI.oauth_session_expires);
		XAPI.authenticated = XAPI.authenticated && (new Date()).getTime()<XAPI.oauth_session_expires;
		//ChumbyLog.i(" - "+XAPI.authenticated);
		return XAPI.authenticated;
	}
	
	public static boolean shouldRenew() {
		long now = (new Date()).getTime();
		// once session expires, it can't be renewed - a new auth is needed
		return (now < XAPI.oauth_session_expires) && (now > XAPI.oauth_should_renew);
	}
	
	public void renew(final Handler handler) {
		//ChumbyLog.i(TAG+".renew()");
		XAPI.bumpNonce();
		String[] pathItems = {"auth", "renew"};
		Handler renewHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case XMLHelper.DID_SUCCEED: {
						Document document = (Document)message.obj;
						parseAuthorizeResponse(document);
						handler.sendMessage(Message.obtain(handler, AUTHENTICATED));
						break;
					}
					case XMLHelper.DID_ERROR: {
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, CANT_AUTHENTICATE ,e));
						break;
					}
				}
			}
		};
		XMLHelper.load(XAPI.makeURLSecure(pathItems,null,XAPI.GET),renewHandler);
		pathItems = null;
	}

	private static String join(String[] s, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        for (int i=0;i<s.length;i++) {
        	buffer.append(s[i]);
        	if (i<s.length-1) {
        		buffer.append(delimiter);
        	}
        }
        String result = buffer.toString();
        buffer = null;
        return result;
    }
	
	public static String makeURLSecure(String[] pathItems,Properties params, String method) {
		XAPI.bumpNonce();
		Properties secureParams = XAPI.addStandardParams(params);
		secureParams.put("oauth_signature",XAPI.signatureMD5Of(pathItems,secureParams));
		String result = join(pathItems,"/");
		if (!method.equals(XAPI.POST) && secureParams!=null) {
			boolean isFirst = true;
			for (Enumeration<?> e = secureParams.propertyNames(); e.hasMoreElements();) {
				String key = (String)e.nextElement();
				String value = secureParams.getProperty(key);
				result += (isFirst ? "?" : "&")+urlEscape(key)+"="+urlEscape(value);
				isFirst = false;
			}
		}
		secureParams = null;
		return Chumby.getInstance().makeURL("/xapis/"+result);
	}

	public static String makeURLInsecure(String[] pathItems,Properties params, String method) {
		XAPI.bumpNonce();
		String result = "";
		result = join(pathItems,"/");
		if (!method.equals(XAPI.POST) && params!=null) {
			boolean isFirst = true;
			for (Enumeration<?> e = params.propertyNames(); e.hasMoreElements();) {
				String key = (String)e.nextElement();
				String value = params.getProperty(key);
				result += (isFirst ? "?" : "&")+urlEscape(key)+"="+urlEscape(value);
				isFirst = false;
			}
		}
		return Chumby.getInstance().makeURL("/xapis/"+result);
	}

	public static XAPIRequest request(String[] pathItems,Properties params,String method,boolean secure,Handler handler) {
		return new XAPIRequest(pathItems,params,method,secure,handler);
	}

	public static XAPIRequest request(String[] pathItems,Properties params,String method,boolean secure,boolean useHTTPS,Handler handler) {
		return new XAPIRequest(pathItems,params,method,secure,useHTTPS,handler);
	}

	private static String pathSignaturePartOf(String[] pathItems) {
		String result = join(pathItems,"/");
		result = urlEscape("/xapis/"+result);
		//ChumbyLog.i(TAG+".pathSignaturePartOf()");
		//ChumbyLog.i(result);
		return result;
	}
	
	private static String paramsSignaturePartOf(Properties params) {
		String result = "";
		if (params!=null) {
			ArrayList<String> keys = new ArrayList<String>();
			for (Enumeration<?> e = params.propertyNames(); e.hasMoreElements();) {
				keys.add((String)e.nextElement());
			}
			Collections.sort(keys);
			Iterator<String> iter = keys.iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				String value = params.getProperty(key);
				result += urlEscapeDouble(key)+"%3D"+urlEscapeDouble(value); // %3D = "="
				if (iter.hasNext()) {
					result = result+"%26";
				}
			}
		}
		//ChumbyLog.i(TAG+".paramsSignaturePartOf():");
		//ChumbyLog.i(result);
		return result;
	}

	private static String baseStringOf(String[] pathItems,Properties params) {
		String pathPart = XAPI.pathSignaturePartOf(pathItems);
		String paramsPart = XAPI.paramsSignaturePartOf(params);
		String result;
		if (paramsPart.equals("")) {
			result = pathPart;
		} else {
			result = pathPart+"&"+paramsPart;
		}
		//ChumbyLog.i(TAG+".baseStringOf():");
		//ChumbyLog.i(result);
		return result;
	}

	private static String signatureMD5Of(String[] pathItems, Properties params) {
		String baseString = XAPI.baseStringOf(pathItems, params);
		return GUID.md5(baseString+"&"+XAPI.oauth_consumer_secret+"&");
	}

	private static Properties addStandardParams(Properties params) {
		if (params==null) {
			params = new Properties();
		}
		params.setProperty("oauth_consumer_key", XAPI.oauth_consumer_key);
		params.setProperty("oauth_nonce",Long.toString(XAPI.oauth_nonce));
		params.setProperty("oauth_signature_method", "MD5-HEX");
		return params;
	}

	/*
	public static String urlEscape(String s) {
		s = s.replace("%","%25");
		s = s.replace(" ","%20");
		s = s.replace(",","%2C");
		s = s.replace("<","%3C");
		s = s.replace(">","%3E");
		s = s.replace("#","%23");
		s = s.replace("{","%7B");
		s = s.replace("}","%7D");
		s = s.replace("|","%7C");
		s = s.replace("\\","%5C");
		s = s.replace("^","%5E");
		s = s.replace("~","%7E");
		s = s.replace("[","%5B");
		s = s.replace("]","%5D");
		s = s.replace("`","%60");
		s = s.replace(";","%3B");
		s = s.replace("/","%2F");
		s = s.replace("?","%3F");
		s = s.replace(":","%3A");
		s = s.replace("@","%40");
		s = s.replace("=","%3D");
		s = s.replace("&","%26");
		s = s.replace("$","%24");
		return s;
	}

	public static String urlEscapeDouble(String s) {
		s = s.replace("%","%2525");
		s = s.replace("[","%255B");
		s = s.replace("]","%255D");
		s = s.replace("/","%252F");
		s = s.replace("?","%253F");
		s = s.replace(":","%253A");
		s = s.replace(" ","%2520");
		s = s.replace(",","%252C");
		s = s.replace("<","%253C");
		s = s.replace(">","%253E");
		s = s.replace("#","%2523");
		s = s.replace("{","%257B");
		s = s.replace("}","%257D");
		s = s.replace("|","%257C");
		s = s.replace("\\","%255C");
		s = s.replace("^","%255E");
		s = s.replace("~","%257E");
		s = s.replace("`","%2560");
		s = s.replace(";","%253B");
		s = s.replace("@","%2540");
		s = s.replace("=","%253D");
		s = s.replace("&","%2526");
		s = s.replace("$","%2524");
		return s;
		
	}
	*/

	public static String urlEscape(String s) {
		s = s.replace("%","%25");
		s = s.replace(" ","%20");
		s = s.replace("!","%21");
		s = s.replace("\"","%22");
		s = s.replace("#","%23");
		s = s.replace("$","%24");
		s = s.replace("&","%26");
		s = s.replace("'","%27");
		s = s.replace("(","%28");
		s = s.replace(")","%29");
		s = s.replace("*","%2A");
		s = s.replace("+","%2B");
		s = s.replace(",","%2C");
		//s = s.replace(".","%2E");
		s = s.replace("/","%2F");
		s = s.replace(":","%3A");
		s = s.replace(";","%3B");
		s = s.replace("<","%3C");
		s = s.replace("=","%3D");
		s = s.replace(">","%3E");
		s = s.replace("?","%3F");
		s = s.replace("@","%40");
		s = s.replace("[","%5B");
		s = s.replace("\\","%5C");
		s = s.replace("]","%5D");
		s = s.replace("^","%5E");
		//s = s.replace("_","%5F");
		s = s.replace("`","%60");
		s = s.replace("{","%7B");
		s = s.replace("|","%7C");
		s = s.replace("}","%7D");
		//s = s.replace("~","%7E");
		return s;
	}

	public static String urlEscapeDouble(String s) {
		s = s.replace("%","%2525");
		s = s.replace(" ","%2520");
		s = s.replace("!","%2521");
		s = s.replace("\"","%2522");
		s = s.replace("#","%2523");
		s = s.replace("$","%2524");
		s = s.replace("&","%2526");
		s = s.replace("'","%2527");
		s = s.replace("(","%2528");
		s = s.replace(")","%2529");
		s = s.replace("*","%252A");
		s = s.replace("+","%252B");
		s = s.replace(",","%252C");
		//s = s.replace(".","%252E");
		s = s.replace("/","%252F");
		s = s.replace(":","%253A");
		s = s.replace(";","%253B");
		s = s.replace("<","%253C");
		s = s.replace("=","%253D");
		s = s.replace(">","%253E");
		s = s.replace("?","%253F");
		s = s.replace("@","%2540");
		s = s.replace("[","%255B");
		s = s.replace("\\","%255C");
		s = s.replace("]","%255D");
		s = s.replace("^","%255E");
		//s = s.replace("_","%255F");
		s = s.replace("`","%2560");
		s = s.replace("{","%257B");
		s = s.replace("|","%257C");
		s = s.replace("}","%257D");
		//s = s.replace("~","%257E");
		return s;
		
	}

	private static void bumpNonce() {
		XAPI.oauth_nonce += (1 + Math.random()*42);
	}

}

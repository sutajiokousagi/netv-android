package com.chumby.util;

import java.io.StringReader;
import org.xml.sax.InputSource;
import com.chumby.http.*;
import com.chumby.util.ChumbyLog;

import java.util.Enumeration;
import java.util.Properties;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import android.util.Xml;
import android.os.*;

import org.xmlpull.v1.XmlSerializer;

import org.w3c.dom.*;

/**
 * Utility class to fetch and parse XML documents
 * @author Duane Maxwell
 *
 */
public class XMLHelper {
	private static final String TAG = "XMLHelper";
	/**
	 * Result for failed requests
	 */
	public static final int DID_ERROR = 0;
	/**
	 * Result for successful requests
	 */
	public static final int DID_SUCCEED = 1;
	/**
	 * Result for failed XML parsing
	 */
	public static final int DID_PARSE_ERROR = -1;
	
	/**
	 * Converts an XML {@link Node} to a String
	 * @param node {@link Node} to convert
	 * @return {@link String} representation of the XML document
	 */
	public static String nodeToString(Node node) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			serializer.startDocument("utf-8", true);
			nodeToStringAux(node,serializer);
			serializer.endDocument();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		String s = writer.toString();
		writer = null;
		serializer = null;
		return s;
	}

	private static void nodeToStringAux(Node node,XmlSerializer serializer) {
		try {
			switch (node.getNodeType()) {
			case Node.ELEMENT_NODE:
				{
					serializer.startTag("",node.getNodeName());
					NamedNodeMap attributes = node.getAttributes();
					for (int i=0;i<attributes.getLength();i++) {
						Attr attribute = (Attr)attributes.item(i);
						serializer.attribute("", attribute.getName(), attribute.getValue());
						attribute = null;
					}
					NodeList children = node.getChildNodes();
					for (int i=0;i<children.getLength();i++) {
						nodeToStringAux(children.item(i),serializer);
					}
					children = null;
					serializer.endTag("",node.getNodeName());
				}
				break;
			case Node.TEXT_NODE:
				serializer.text(node.getNodeValue());
				break;
			case Node.DOCUMENT_NODE:
				nodeToStringAux(node.getFirstChild(),serializer);
				break;
			case Node.CDATA_SECTION_NODE:
				serializer.text(node.getNodeValue());
				break;
			default:
				ChumbyLog.w("unknown node type "+node.getNodeType());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Parses a {@link String} to an XML {@link Document}
	 * @param s {@link String} containing an unparsed XML document
	 * @return {@link Document} parsed representation of the XML document
	 */
	public static Document stringToDocument(String s) {
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
            StringReader reader = new StringReader( s );
            InputSource inputSource = new InputSource( reader );           
            doc = db.parse(inputSource);
        } catch (Exception ex) {
			ex.printStackTrace();
        }
		db = null;
		return doc;
	}

	/**
	 * Reads a {@link InputStream} into a {@link String}
	 * @param is {@link InputStream} opened stream from which to read the characters
	 * @return {@link String} the read data
	 * @throws IOException
	 */
	public static String streamToString(InputStream is) throws IOException {
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;
			try {
				InputStreamReader inputStreamReader = new InputStreamReader(is, "utf-8");
				BufferedReader reader = new BufferedReader(inputStreamReader);
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
				inputStreamReader = null;
				reader = null;
			} finally {
				is.close();
			}
			String s = sb.toString();
			sb = null;
			return s;
		} else {        
			return "";
		}
	}

	/**
	 * POST an XML {@link Document} and read and parse the resulting XML {@link Document}
	 * @param path {@link String} the URL of the server to which to POST the document
	 * @param sendDocument {@link Document} to send to the server as raw POST data
	 */
	public static void sendAndLoad(String path,Document sendDocument) {
		sendAndLoad(path,sendDocument,new Handler());
	}

	/**
	 * POST an XML {@link Document} and read and parse the resulting XML {@link Document}
	 * @param path {@link String} the URL of the server to which to POST the document
	 * @param sendDocument {@link Document} to send to the server as raw POST data
	 * @param handler {@link Handler} to call with the results
	 */
	public static void sendAndLoad(final String path,Document sendDocument,final Handler handler) {
		//ChumbyLog.i("XMLHelper.sendAndLoad(): "+path);
		String documentXML = nodeToString(sendDocument.getFirstChild());
		Handler httpHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case HttpConnection.DID_SUCCEED: {
						Document document = (Document)message.obj;
						handler.sendMessage(Message.obtain(handler, DID_SUCCEED, document));
						break;
					}
					case HttpConnection.DID_ERROR: {
						//Exception e = (Exception) message.obj;
						ChumbyLog.w(TAG+".sendAndLoad(): error on "+path);
						//e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, DID_ERROR,null));
						//e = null;
						break;
					}
				}
			}
		};
		new HttpConnection(httpHandler).postXML(path, documentXML);
	}

	/**
	 * POST using conventional name=value pairs, read and parse the resulting XML {@link Document}
	 * @param path {@link String} the URL of the server
	 * @param params {@link Properties} name/value pairs to send as POST data
	 */
	public static void sendVarsAndLoad(String path,Properties params) {
		sendVarsAndLoad(path,params,new Handler());
	}

	/**
	 * POST using conventional name=value pairs, read and parse the resulting XML {@link Document}
	 * @param path {@link String} the URL of the server
	 * @param params {@link Properties} name/value pairs to send as POST data
	 * @param handler {@link Handler} to call with the results
	 */
	public static void sendVarsAndLoad(String path,Properties params, final Handler handler) {
		//ChumbyLog.i(TAG+".sendVarsAndLoad(): "+path);
		String data = "";
		boolean isFirst = true;
		for (Enumeration<?> e = params.propertyNames(); e.hasMoreElements();) {
			String key = (String)e.nextElement();
			String value = params.getProperty(key);
			//ChumbyLog.i(" - "+key+" = "+value);
			data += (isFirst ?"" : "&")+urlEscape(key)+"="+urlEscape(value);
			isFirst = false;
		}
		//ChumbyLog.i(TAG+".sendVarsAndload(): data="+data);
		Handler httpHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case HttpConnection.DID_SUCCEED: {
						Document document = (Document)message.obj;
						handler.sendMessage(Message.obtain(handler, DID_SUCCEED, document));
						break;
					}
					case HttpConnection.DID_ERROR: {
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, DID_ERROR,e));
						break;
					}
				}
			}
		};
		new HttpConnection(httpHandler).postXML(path, data);
	}

	/**
	 * Perform a simple GET request for an XML {@link Document}
	 * @param path {@link String} the URL of the server
	 */
	public static void load(String path) {
		load(path,new Handler());
	}

	/**
	 * Perform a simple GET request for an XML {@link Document}
	 * @param path {@link String} the URL of the server
	 * @param handler {@link Handler} to call with the results
	 */
	public static void load(String path,final Handler handler) {
		//ChumbyLog.i(TAG+".load(): "+path);
		Handler httpHandler = new Handler() {
			public void handleMessage(Message message) {
				//ChumbyLog.i(TAG+".load.handleMessage(): "+message);
				switch (message.what) {
					case HttpConnection.DID_SUCCEED: {
						Document document = (Document)message.obj;
						handler.sendMessage(Message.obtain(handler, DID_SUCCEED, document));
						break;
					}
					case HttpConnection.DID_ERROR: {
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						handler.sendMessage(Message.obtain(handler, DID_ERROR,e));
						break;
					}
				}
			}
		};
		new HttpConnection(httpHandler).getXML(path);
	}

	/**
	 * Do a simple RFC2396 encoding
	 * @param s {@link String} text to encode
	 * @return {@link String} encoded text
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
		s = s.replace(".","%2E");
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
		s = s.replace("_","%5F");
		s = s.replace("`","%60");
		s = s.replace("{","%7B");
		s = s.replace("|","%7C");
		s = s.replace("}","%7D");
		s = s.replace("~","%7E");
		return s;
	}

}

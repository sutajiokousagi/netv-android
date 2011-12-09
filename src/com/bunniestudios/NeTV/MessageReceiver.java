package com.bunniestudios.NeTV;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MessageReceiver extends BroadcastReceiver
{
	public static final String TAG = "NeTV";
	public static final String STATUS_MESSAGE = "status";
	public static final String COMMAND_MESSAGE = "cmd";
	public static final String MESSAGE_KEY_VALUE = "value";
	public static final String MESSAGE_KEY_ADDRESS = "address";
	public static final String MESSAGE_KEY_MESSAGE = "message";
	public static final String MESSAGE_KEY_MIN_ANDROID = "minAndroid";
	public static final String MESSAGE_KEY_AUTHORIZED_CALLER = "Authorized-Caller";
	
	public static final String COMMAND_Handshake = "Hello";
	public static final String COMMAND_SetUrl = "SetUrl";
	public static final String COMMAND_RemoteControl = "RemoteControl";
	public static final String COMMAND_Key = "Key";
	public static final String COMMAND_WifiScan = "WifiScan";
	public static final String COMMAND_Time = "SetTime";
	public static final String COMMAND_Network = "SetNetwork";
	public static final String COMMAND_Account = "SetAccount";
	public static final String COMMAND_Android = "Android";
	public static final String COMMAND_GetFileContents = "GetFileContents";
	public static final String COMMAND_UnlinkFile = "UnlinkFile";
	public static final String COMMAND_FileExists = "FileExists";
	public static final String COMMAND_DownloadFile = "DownloadFile";
	public static final String COMMAND_UploadFile = "UploadFile";
	public static final String COMMAND_MD5File = "MD5File";
	public static final String COMMAND_Multitab = "Multitab";
	public static final String COMMAND_TickerEvent = "TickerEvent";
	public static final String COMMAND_ControlPanel = "ControlPanel";
	public static final String COMMAND_JavaScript = "JavaScript";
	public static final String COMMAND_TextInput = "TextInput";
	
	public static final String COMMAND_CancelActivation = "CancelActivation";
	public static final String COMMAND_STATUS = "GetActivationStatus";

	MessageReceiverInterface _parent = null;

	public MessageReceiver(MessageReceiverInterface parent)
	{
		_parent = parent;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String message = intent.getStringExtra("message");
		String address = intent.getStringExtra(MESSAGE_KEY_ADDRESS);
		parseMessageXML(message, address);
	}

	/**
	 * @category Utility
	 */
	private boolean parseMessageXML(String xmlString, String address)
	{
		HashMap<String, String> data = null;
		
		//There is a bug in <hwver> tag, this temporarily fix it
		xmlString = xmlString.replace("<?xml version='1.0'?>", "");

		try
		{
			// DOM Parser (as opposed to SAX parser)
			// XML has to have one and only root node,
			// otherwise the following exception will be raised:
			// org.w3c.dom.DOMException: Only one root element allowed
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputSource inSource = new InputSource(new StringReader(xmlString));
			Document doc = builder.parse(inSource); //possible DOM exception here
			doc.getDocumentElement().normalize();

			data = new HashMap<String, String>();
			data.put(MESSAGE_KEY_ADDRESS, address);
			parseXMLNode(data, doc.getFirstChild().getChildNodes() );
			
			//Parse at most 2 levels of XML
			/*
			for (int i = 0; i < nodeList.getLength(); i++)
			{
				Node node = nodeList.item(i);
				int numChild = node.getChildNodes().getLength();
				if (numChild == 1 && node.getFirstChild().getNodeType() != Node.ELEMENT_NODE)
				{
					data.put(node.getNodeName(), node.getTextContent());
				}
				else
				{
					NodeList subNodeList = node.getChildNodes();
					for (int j = 0; j < subNodeList.getLength(); j++)
					{
						Node subNode = subNodeList.item(j);
						int numSubChild = subNode.getChildNodes().getLength();
						if (numSubChild == 1 && subNode.getFirstChild().getNodeType() != Node.ELEMENT_NODE)
							data.put(subNode.getNodeName(), subNode.getTextContent());
					}
				}
			}*/
			
		} catch (ParserConfigurationException pce) {
			//Log.e(TAG, "sax parse error, ignored", pce);
			Log.e(TAG, "sax parse error, safe to ignore");
			data = null;
		} catch (SAXException se) {
			//Log.e(TAG, "sax error, ignored", se);
			Log.e(TAG, "sax error, safe to ignore");
			data = null;
		} catch (IOException ioe) {
			//Log.e(TAG, "sax parse io error, safe to ignore", ioe);
			Log.e(TAG, "sax parse io error, safe to ignore");
			data = null;
		} catch (DOMException domex) {
			//Log.e(TAG, "DOM exception, ignored. Invalid XML message received. Ignored.", domex);
			Log.e(TAG, "DOM exception, safe to ignore. (Invalid XML message received)");
			data = null;
		} catch (Exception ex) {
			Log.e(TAG, "General Exception catched in MessageReceiver. Should be checked.", ex);
			data = null;
		}

		if (data == null || data.size() <= 1)
			return false;

		//Post-processing by parent
		_parent.ProcessMessage(data);
		return true;
	}
	
	/**
	 * @category Utility
	 */
	private void parseXMLNode(HashMap<String, String> dataMap, NodeList nodeList)
	{
		for (int i = 0; i < nodeList.getLength(); i++)
		{
			Node node = nodeList.item(i);
			int numChild = node.getChildNodes().getLength();
			if (numChild == 1 && node.getFirstChild().getNodeType() != Node.ELEMENT_NODE)
			{
				dataMap.put(node.getNodeName(), node.getTextContent());
			}
			else
			{
				parseXMLNode(dataMap, node.getChildNodes() );
			}
		}
	}
};
package com.chumby;

import java.util.*;

import org.w3c.dom.*;

//import com.chumby.util.ChumbyLog;
import com.chumby.util.XMLHelper;
import com.chumby.util.XMLUtil;

/**
 * Implements a simplifed version of the Chumby device "DaughterCard ID" system
 * @author Duane Maxwell
 *
 */
public class DaughterCardID {
	//private static final String TAG="DaughterCardID";
	/**
	 * 
	 */
	private Properties items;

	private static DaughterCardID _instance;
	
	public DaughterCardID() {
		items = new Properties();
	}
	
	public static DaughterCardID getInstance() {
		if (_instance==null) {
			_instance = new DaughterCardID();
		}
		return _instance;
	}
	
	/**
	 * Returns a String that can be concatenated in a simple HTTP GET request
	 */
	public String args() {
		String result = "";
		for (Enumeration<?> e = items.propertyNames(); e.hasMoreElements();) {
			String key = (String)e.nextElement();
			String value = items.getProperty(key);
			result += "&dcid_"+key+"="+value;
		}
		//ChumbyLog.i(TAG+".args(): "+result);
		return result;
	}

	/**
	 * Merges into a {@link Properties} structure, typically used in a OAuth request
	 * @param props {@link Properties} existing propery list into which to merge the dcid values
	 */
	public void args(Properties props) {
		for (Enumeration<?> e = items.propertyNames(); e.hasMoreElements();) {
			String key = (String)e.nextElement();
			String value = items.getProperty(key);
			props.setProperty("dcid_"+key,value);
		}
	}

	/**
	 * Simple parser for one-level-deep XML file with DCID data
	 * <p>
	 * A typical file looks something like:<br><br>
	 * <code>
	 * &lt;?xml version="1.0" encoding="utf-8"?&gt;<br>
	 * &lt;chum&gt;<br>
     * &nbsp;&lt;vers&gt;0002&lt;/vers&gt;<br>
     * &nbsp;&lt;rgin&gt;0001&lt;/rgin&gt;<br>
     * &nbsp;&lt;skin&gt;0001&lt;/skin&gt;<br>
     * &nbsp;&lt;part&gt;1000&lt;/part&gt;<br>
     * &nbsp;&lt;camp&gt;1004&lt;/camp&gt;<br>
     * &lt;/chum&gt;<br>
     * </code>
	 * @param s {@link String} of raw XML data
	 *
	 */
	public void fromString(String s) {
		Document doc = XMLHelper.stringToDocument(s);
		if (doc!=null) {
			Node chumNode = XMLUtil.firstChildOfType(doc, "chum");
			if (chumNode!=null) {
				ArrayList<Element> nodes = XMLUtil.children(chumNode);
				for (int i=0;i<nodes.size();i++) {
					Node node = nodes.get(i);
					String name = node.getNodeName();
					String value = node.getFirstChild().getNodeValue();
					items.put(name,value);
					//ChumbyLog.i(TAG+".fromString(): adding ("+name+","+value+")");
				}
			}
		}
	}
	
	public void setProperty(String key, String value)
	{
		items.put(key,value);
	}
}
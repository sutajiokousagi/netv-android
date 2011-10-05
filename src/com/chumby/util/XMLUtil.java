package com.chumby.util;

import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLUtil {
	
	public static Element firstChildOfType(Node x,String s) {
		if (x!=null) {
			Node n = x.getFirstChild();
			Node last = x.getLastChild();
			while (n!=null) {
				if (n.getNodeName().equals(s)) {
					return (Element)n;
				}
				if (n==last) break; // DSM handle possible bug in getNextSibling()
				n = n.getNextSibling();
			}
		}
		return null;
	}

	public static Element firstDescendantOfType(Node x,String s) {
		Node n = firstChildOfType(x,s);
		if (n!=null) return (Element)n;
		n = (Element)x.getFirstChild();
		Node last = x.getLastChild();
		while (n!=null) {
			Element k = firstDescendantOfType((Node)n,s);
			if (k!=null) return k;
			if (n==last) break;
			n = n.getNextSibling();
		}
		return null;
	}
	
	public static ArrayList<Element> childrenOfType(Node x,String s) {
		ArrayList<Element> a = new ArrayList<Element>();
		Node n = x.getFirstChild();
		Node last = x.getLastChild();
		while (n!=null) {
			if (n.getNodeName().equals(s)) {
				a.add((Element)n);
			}
			if (n==last) break;
			n = n.getNextSibling();
		}
		return a;
	}
	
	public static ArrayList<Element> children(Node x) {
		ArrayList<Element> a = new ArrayList<Element>();
		NodeList nodeList = x.getChildNodes();
		for (int i=0;i<nodeList.getLength();i++) {
			Node n = nodeList.item(i);
			if (n.getNodeType()==Node.ELEMENT_NODE) {
				a.add((Element)n);
			}
		}
		return a;
	}

	//
	// silliness - the XML parser produces separate TEXT nodes when it encounters entities, so we have to concat them
	//
	public static String firstValueOfType(Node x,String s) {
		Node n = x.getFirstChild();
		Node last = x.getLastChild();
		while (n!=null) {
			if (n.getNodeName().equals(s)) {
				NodeList nodes = n.getChildNodes();
				String result = "";
				for (int i=0;i<nodes.getLength();i++) {
					Node m = nodes.item(i);
					result += m.getNodeValue();
				}
				return result;
			}
			if (n==last) break;
			n = n.getNextSibling();
		}
		return null;
		
	}
	
	public static String valueOf(Node x) {
		if (x!=null) {
			NodeList nodes = x.getChildNodes();
			String result = "";
			for (int i=0;i<nodes.getLength();i++) {
				Node m = nodes.item(i);
				result += m.getNodeValue();
			}
			return result;
		}
		return null;
	}
}

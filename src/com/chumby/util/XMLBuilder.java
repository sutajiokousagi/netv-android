package com.chumby.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import com.chumby.util.ChumbyLog;

public class XMLBuilder {
	private Document document;
	private Element node;
	
	private XMLBuilder(Document d) {
		document = d;
		node = null;
	}
	
	static public XMLBuilder create() {
		try {
			DocumentBuilderFactory dfb = DocumentBuilderFactory.newInstance();
			DocumentBuilder db =  dfb.newDocumentBuilder();
			XMLBuilder builder = new XMLBuilder(db.newDocument());
			builder.node = (Element)builder.document;
			return builder;
		} catch (Exception ex) {
			ChumbyLog.w(ex.getClass().toString());
			ChumbyLog.w(ex.getMessage());
			return null;
		}
		
	}
	static public XMLBuilder create(String name) {
		try {
			DocumentBuilderFactory dfb = DocumentBuilderFactory.newInstance();
			DocumentBuilder db =  dfb.newDocumentBuilder();
			XMLBuilder builder = new XMLBuilder(db.newDocument());
			builder.node = builder.document.createElement(name);
			builder.document.appendChild(builder.node);
			return builder;
		} catch (Exception ex) {
			ChumbyLog.w(ex.getClass().toString());
			ChumbyLog.w(ex.getMessage());
			return null;
		}
	}
	
	public XMLBuilder element(String name) {
		XMLBuilder builder = new XMLBuilder(document);
		Element newNode = document.createElement(name);
		node.appendChild(newNode);
		builder.node = newNode;
		return builder;
	}
	
	public XMLBuilder elem(String name) {
		return element(name);
	}
	
	public XMLBuilder e(String name) {
		return element(name);
	}

	public XMLBuilder attribute(String name,String value) {
		node.setAttribute(name,value);
		return this;
	}
	
	public XMLBuilder attr(String name,String value) {
		return attribute(name,value);
	}
	
	public XMLBuilder a(String name,String value) {
		return attribute(name,value);
	}
	
	public XMLBuilder attribute(String name,long value) {
		return attribute(name,Long.toString(value));
	}
	
	public XMLBuilder attr(String name,long value) {
		return attribute(name,Long.toString(value));
	}
	
	public XMLBuilder a(String name,long value) {
		return attribute(name,Long.toString(value));
	}

	public XMLBuilder text(String s) {
		node.appendChild(document.createTextNode(s));
		return this;
	}
	
	public XMLBuilder t(String s) {
		return text(s);
	}
	
	public XMLBuilder elementText(String name,String value) {
		return element(name).text(value);
	}
	
	public XMLBuilder et(String name,String value) {
		return elementText(name,value);
	}

	public XMLBuilder up() {
		XMLBuilder builder = new XMLBuilder(document);
		builder.node = (Element)node.getParentNode();
		return builder;
	}
	
	public XMLBuilder root() {
		XMLBuilder builder = new XMLBuilder(document);
		builder.node = (Element)document;
		return builder;
	}
	
	public Document getDocument() {
		return document;
	}

	public String toString() {
		return document.toString();
	}
}

package com.chumby.events;

/**
 * Encapsulates an event sent by an {@link EventDispatcher} to multiple {@link EventListener} clients
 * @author Duane Maxwell
 * @see EventDispatcher
 * @see EventListener
 */
public class Event {
	/**
	 * Type of the event
	 */
	public String type;
	/**
	 * The {@link Object} affected by the event
	 */
	public Object target;
	
	/**
	 * Constructor
	 * @param type of the event
	 */
	public Event(String type) {
		this.type = type;
	}
	
	/**
	 * Constructor
	 * @param type of the event
	 * @param target of the event
	 */
	public Event(String type, Object target) {
		this.type = type;
		this.target = target;
	}
	
	public String toString() {
		return "[Event type="+this.type+"]";
	}
}

package com.chumby.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import com.chumby.util.ChumbyLog;

/**
 * Dispatches {@link Event} messages to {@link EventListener} clients
 * @author Duane Maxwell
 * @see Event
 * @see EventListener
 */
public interface EventDispatcher {
	 
	/**
	 * Register an event listener
	 * @param type		{@link String} the name of the event to listen to
	 * @param listener	{@link EventListener} the listener
	 */
	public void addEventListener(String type, EventListener listener);
	/**
	 * Unregister an event listener
	 * @param type		{@link String} the name of the event registered to
	 * @param listener	{@link EventListener} the listener
	 */
	public void removeEventListener(String type, EventListener listener);
	/**
	 * Remove all listeners
	 */
	public void removeAllListeners();
	/**
	 * Dispatch an Event
	 * @param event	{@link Event}
	 */
	public void dispatchEvent(Event event) ;
	/**
	 * Query to see if there are any listeners registered to a certain event name
	 * @param type	{@link String} the event name to query
	 * @return	boolean true if there are any listeners for this {@link Event} type
	 */
	public boolean willTrigger(String type);
 
 
	public class EventDispatcherImpl implements EventDispatcher {
		private static final String TAG="EventDispatcherImpl";
		protected HashMap<String , ArrayList<EventListener>> _listenersMap;
		protected EventDispatcher _eventTarget;
 
		/**
		 * Constructor - intended for inheritance
		 */
		public EventDispatcherImpl() {
			removeAllListeners();
		}
 
		/**
		 * Constructor - intended for composition
		 * @param eventTarget EventListener to use as the event target in the Event sent
		 */
		public EventDispatcherImpl(EventDispatcher eventTarget) {
			_eventTarget = eventTarget;
		}
 
		public void addEventListener(String type, EventListener listener) {
			removeEventListener(type, listener);
			ArrayList<EventListener> listeners = _listenersMap.get(type);
			if ( listeners == null ) {
				listeners = new ArrayList<EventListener>();
				_listenersMap.put(type, listeners);
			}
			listeners.add(listener);
		}
 
		public void removeEventListener(String type, EventListener listener) {
			ArrayList<EventListener> listeners = (ArrayList<EventListener>) _listenersMap.get(type);
			if ( listeners != null ) {
				Iterator<EventListener> items = listeners.iterator();
		        while( items.hasNext() ) {
		            if ( (EventListener) items.next() == listener ) {
		            	items.remove();
		            	return;
		            }
		        }
		        if ( listeners.isEmpty() ) {
		        	_listenersMap.remove(type);
		        }
			}
		}
 
		public void removeAllListeners() {
			_listenersMap = new HashMap<String , ArrayList<EventListener>>();
		}
 
		public void dispatchEvent(Event event) {
			ChumbyLog.i(TAG+".dispatchEvent() event "+event.toString());
			ArrayList<EventListener> listeners = (ArrayList<EventListener>) _listenersMap.get(event.type);
			if ( listeners != null ) {
				if (event.target==null) {
					event.target = _eventTarget != null ? _eventTarget : this;
				}
				Iterator<EventListener> items = listeners.iterator();
		        while( items.hasNext() ) {
		            ( (EventListener) items.next() ).run(event);
		        }
			}
		}
 
		public boolean willTrigger(String type) {
			ArrayList<EventListener> listeners = (ArrayList<EventListener>) _listenersMap.get(type);
			return listeners == null || listeners.isEmpty();
		}
	}

}

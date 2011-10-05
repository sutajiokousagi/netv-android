package com.chumby.http;

import java.util.ArrayList;

import com.chumby.util.ChumbyLog;

/**
* Simple connection manager to throttle connections
*
* @author Greg Zavitz
* @author Duane Maxwell (modifications by Chumby Industries)
*/
public class HttpConnectionManager {
	 private static final String TAG="HttpConnectionManager";
     
	 /**
	  * Determines the number of simultaneous HTTP connections to be used
	  */
     public static final int MAX_CONNECTIONS = 5;
 
     private ArrayList<Runnable> active = new ArrayList<Runnable>();
     private ArrayList<Runnable> queue = new ArrayList<Runnable>();
 
     private static HttpConnectionManager instance;
 
     public static HttpConnectionManager getInstance() {
          if (instance == null)
               instance = new HttpConnectionManager();
          return instance;
     }
 
     public static synchronized void flushQueue() {
    	 getInstance().active = new ArrayList<Runnable>();
    	 getInstance().queue = new ArrayList<Runnable>();
     }

     public synchronized boolean cancel(Runnable runnable) {
    	 return queue.remove(runnable);
     }
 
     public synchronized void push(Runnable runnable) {
          queue.add(runnable);
          if (active.size() < MAX_CONNECTIONS)
               startNext();
     }
 
     private synchronized void startNext() {
          if (!queue.isEmpty()) {
        	  Runnable next = null;
        	   try {
	               next = (Runnable)queue.get(0);
        	   } catch (Exception ex) {
           		   ChumbyLog.i(TAG+".startNext() failed to get next request");
        	   }
        	   try {
	               if (!queue.isEmpty()) queue.remove(0); // XXX DSM help with race
        	   } catch (Exception ex) {
        		   ChumbyLog.i(TAG+".startNext() failed to remove request");
        	   }
	           if (next!=null) {
	        	   active.add(next);	 
	               Thread thread = new Thread(next);
	               thread.start();
	           }
          }
     }
 
     public synchronized void didComplete(Runnable runnable) {
          active.remove(runnable);
          startNext();
     }
 
}
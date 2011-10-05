package com.chumby.http;

import java.io.*;
import com.chumby.util.ChumbyLog;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
 
import android.os.*;
import android.graphics.*;
 
/**
* Asynchronous HTTP connections
*
* @author Greg Zavitz & Joseph Roth
* @author Duane Maxwell (modifications by Chumby Industries)
*/
public class HttpConnection implements Runnable {
	 private static final String TAG="HttpConnection";
 
	 /**
	  * Status when starting the request
	  */
     public static final int DID_START = 100;
     /**
      * Status when request finishes unsuccessfully
      */
     public static final int DID_ERROR = 101;
     /**
      * Status when request finishes unsuccessfully
      */
     public static final int DID_SUCCEED = 102;
 
     /**
      * Selector for simple GET request
      */
     private static final int GET = 0;// best used for simple text responses
     /**
      * Selector for simple POST requests
      */
     private static final int POST = 1;
     /**
      * Selector for simple PUT requests
      */
     private static final int PUT = 2;
     /**
      * Selector for simple DELETE requests
      */
     private static final int DELETE = 3;
     /**
      * Selector for GET requests that result in a {@link Bitmap} response
      */
     private static final int BITMAP = 4; // special command for bitmap responses such as JPEGs

     /**
      * Selector for GET requests that result in an XML document
      */
     private static final int GET_XML = 5; // special selectors for XML responses
     /**
      * Selector for POST requests that result in an XML document
      */
     private static final int POST_XML = 6;
 
     /**
      * The URL for the request
      */
     private String url;
     /**
      * The method of the request (GET, POST, PUT, DELETE, etc)
      */
     private int method;
     /**
      * The {@link Handler} for the response
      */
     private Handler handler;
     /**
      * The data to be sent for a POST
      */
     private String data;
 
     /**
      * The {@link HttpClient} handling the request
      */
     private HttpClient httpClient;
 
     /**
      * Constructor
      */
     public HttpConnection() {
          this(new Handler());
     }
 
     /**
      * Constructor
      * @param _handler the {@link Handler} to process the response
      */
     public HttpConnection(Handler _handler) {
          handler = _handler;
     }
 
     /**
      * Utility function to create a request
      *
      * The function creates a request and posts it on a queue
      *
      * @param method the HTTP method to use (GET, POST, PUT, DELETE, BITMAP, GET_XML, POST_XML)
      * @param url the URL of the request
      * @param data any extra parameters for a POST request (null for other request types)
      */
     public void create(int method, String url, String data) {
          this.method = method;
          this.url = url;
          this.data = data;
          HttpConnectionManager.getInstance().push(this);
     }
 
     /**
      * Cancel this request, removing it from the request queue
      */
     public void cancel() {
    	 HttpConnectionManager.getInstance().cancel(this);
     }
 
     /**
      * Convenience function to create a simple GET request
      * @param url the URL to fetch
      */
     public void get(String url) {
          create(GET, url, null);
     }
 
     /**
      * Convenience function for a POST request
      * @param url the URL of the server to POST to
      * @param data the data to be sent
      */
     public void post(String url, String data) {
          create(POST, url, data);
     }
 
     /**
      * Convenience function for a PUT request
      * @param url the URL of the server to PUT to
      * @param data the data to be sent
      */
     public void put(String url, String data) {
          create(PUT, url, data);
     }
 
     /**
      * Convenience function for a DELETE request
      * @param url the URL of the server from which to delete the item
      */
     public void delete(String url) {
          create(DELETE, url, null);
     }
  
     /**
      * Convenience function for a GET request that results in a {@link Bitmap}
      * @param url the of the server from which to request the bitmap
      */
     public void bitmap(String url) {
    	 create(BITMAP, url, null);
     }
 
     /**
      * Convenience function to GET some XML
      * @param url the URL for the server from which to get the XML
      */
     public void getXML(String url) {
    	 create(GET_XML, url, null);
     }
     
     /**
      * Convenience function to POST some date, and get an XML response
      * @param url the URL to which to post the data
      * @param data the data to send 
      */
     public void postXML(String url, String data) {
    	 create(POST_XML, url, data);
     }
 
     /**
      * Process this request
      */
     public void run() {
          handler.sendMessage(Message.obtain(handler, HttpConnection.DID_START));
          httpClient = new DefaultHttpClient();
          HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 25000);
          HttpConnectionParams.setSoTimeout(httpClient.getParams(), 60000);
          try {
               HttpResponse response = null;
               switch (method) {
               case GET:
                    response = httpClient.execute(new HttpGet(url));
                    break;
               case POST:
                    HttpPost httpPost = new HttpPost(url);
                    httpPost.setEntity(new StringEntity(data));
                    httpPost.addHeader("Content-Type","application/x-www-form-urlencoded");
                    response = httpClient.execute(httpPost);
             	    processEntity(response.getEntity());
                    break;
               case PUT:
                    HttpPut httpPut = new HttpPut(url);
                    httpPut.setEntity(new StringEntity(data));
                    response = httpClient.execute(httpPut);
             	    processEntity(response.getEntity());
                    break;
               case DELETE:
                    response = httpClient.execute(new HttpDelete(url));
             	    processEntity(response.getEntity());
                    break;
               case BITMAP:
                   response = httpClient.execute(new HttpGet(url));
                   processBitmapEntity(response.getEntity());
                   break;
               case GET_XML:
            	   response = httpClient.execute(new HttpGet(url));
            	   processXMLEntity(response.getEntity());
            	   break;
               case POST_XML:
                   HttpPost httpPostXML = new HttpPost(url);
                   httpPostXML.setEntity(new StringEntity(data));
                   httpPostXML.addHeader("Content-Type","application/x-www-form-urlencoded");
                   response = httpClient.execute(httpPostXML);
            	   processXMLEntity(response.getEntity());
                   break; 
               }
          } catch (Exception e) {
        	  ChumbyLog.w(TAG+".run(): failure "+e.getMessage());
        	  Message message = Message.obtain(handler,HttpConnection.DID_ERROR, e);
        	  message.getData().putString("url",url);
              handler.sendMessage(message);
          }
          HttpConnectionManager.getInstance().didComplete(this);
     }
 
     /**
      * Process the result of a successful HTTP request
      * @param entity the {@link org.apache.http.HTTPEntity} of the server response
      * @throws IllegalStateException
      * @throws IOException
      */
     private void processEntity(HttpEntity entity) throws IllegalStateException,IOException {
          BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
          String line, result = "";
          while ((line = br.readLine()) != null)
               result += line;
          line = null;
          Message message = Message.obtain(handler, DID_SUCCEED, result);
          result = null;
          message.getData().putString("url", url);
          handler.sendMessage(message);
          message = null;
     } 
 
     /**
      * Process the response of a successful HTTP request as a Bitmap
      * @param entity entity the {@link HttpEntity} of the server response
      * @throws IOException
      */
     private void processBitmapEntity(HttpEntity entity) throws IOException {
         BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
         Bitmap bm = BitmapFactory.decodeStream(bufHttpEntity.getContent());
         bufHttpEntity = null;
         Message message = Message.obtain(handler, DID_SUCCEED, bm);
         bm = null;
         message.getData().putString("url",url);
         handler.sendMessage(message);
         message = null;
    }
     
     /**
      * Process the response of a successful HTTP request as an XML document
      * @param entity entity the {@link HttpEntity} of the server response
      * @throws IOException
      */
     private void processXMLEntity(HttpEntity entity) throws IOException {
         Document doc = null;
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         DocumentBuilder db;
         
         try {
             db = dbf.newDocumentBuilder();
             doc = db.parse(entity.getContent());
 		 } catch (Exception ex) {
			ex.printStackTrace();
         }
 		 db = null;
         Message message = Message.obtain(handler, DID_SUCCEED, doc);
         doc = null;
         message.getData().putString("url", url);
         handler.sendMessage(message);
         message = null;
     }
}

/* example:

public void downloadTwitterStream() {
Handler handler = new Handler() {
     public void handleMessage(Message message) {
          switch (message.what) {
          case HttpConnection.DID_START: {
               text.setText("Starting connection...");
               break;
          }
          case HttpConnection.DID_SUCCEED: {
               String response = (String) message.obj;
               text.setText(response);
               break;
          }
          case HttpConnection.DID_ERROR: {
               Exception e = (Exception) message.obj;
               e.printStackTrace();
               text.setText("Connection failed.");
               break;
          }
          }
     }
};
new HttpConnection(handler)
          .get("http://twitter.com/statuses/user_timeline/69177017.rss");
}
*/
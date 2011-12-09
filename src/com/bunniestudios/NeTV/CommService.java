package com.bunniestudios.NeTV;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class CommService extends Service
{
	private static final String TAG = "NeTV";
	public static final String SERVICE_STARTED_BY = "startedBy";
	public static final String PENDING_PACKAGES = "pendingPackages";
	
	public static final int DEFAULT_PORT = 8082;
	public static final String MULTICAST_GROUP = "225.0.0.37";
	public static final String NEW_MESSAGE = "com.bunniestudios.NeTV.new_message";
		
	private static final int BUFFER_SIZE = 2048;
	private static final boolean USE_MULTICAST = true;		//needs CHANGE_WIFI_MULTICAST_STATE permission
	private static final boolean TX_QUEUE = false;			//use transmit message queue instead of sending directly
	
	private int 						_port;
	private String 						_myIP; 
	private boolean 					_bServiceStopping;
	private String						_startedBy;
	
	private SharedPreferences 			_prefs;
	private Thread 						_thread;
	private DatagramSocket 				_mySocket;
	private LinkedList<DatagramPacket> 	_transmitQueue;
	private InetAddress 				_broadcastAddress;
	private NotificationManager 		_notificationManager;
	
	// Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.local_service_started;
    
	
	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder
    {
    	public CommService getService()
    	{
    		//Return self after onCreate() is called
            return CommService.this;
        }
    }
    
    // This is the object that receives interactions from clients.
    private final IBinder mBinder = new LocalBinder();
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		_thread = null;
		_bServiceStopping = false;
		_startedBy = null;
		_transmitQueue = new LinkedList<DatagramPacket>();
		
		try
		{
			//_broadcastAddress = USE_MULTICAST ? InetAddress.getByName(MULTICAST_GROUP) : getBroadcastAddress();
			_broadcastAddress = getBroadcastAddress();
		}
		catch(IOException e)
		{
			_broadcastAddress = null;
		}
		
		//Get the xml/preferences.xml preferences
		_prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		_port = Integer.parseInt(_prefs.getString("portPref", "" + DEFAULT_PORT));
		
		//Display a notification about us starting.  We put an icon in the status bar.
		_notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);       
        //showNotification();
		
		//Setup socket
		try
		{
			_mySocket = USE_MULTICAST ? new MulticastSocket(_port) : new DatagramSocket(_port);
			_mySocket.setSoTimeout(100);
			_mySocket.setBroadcast(true);
			_mySocket.setReuseAddress(true);
			
			if (USE_MULTICAST)
			{
				((MulticastSocket)_mySocket).joinGroup( InetAddress.getByName(MULTICAST_GROUP) );
				Log.d(TAG, "CommService: created, using multicast mode (" + MULTICAST_GROUP + ")");
			}
			else
			{
				Log.d(TAG, "CommService: created, using broadcast mode");
			}
		}
		catch(Exception e)
		{
			Log.e(TAG, e.toString());
			Log.d(TAG, "CommService: error while creating");
		}		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// Who created this service
		_startedBy = intent.getStringExtra(SERVICE_STARTED_BY);
		
		// Pending message created by NeTVWidget receiver
		ArrayList<String> pendingPackages = intent.getStringArrayListExtra(PENDING_PACKAGES);
		String extraAddress = intent.getStringExtra("address");
		
		if (pendingPackages != null)
		{
			Iterator<String> iterator = pendingPackages.iterator();
			while(iterator.hasNext())
			{
				if (extraAddress != null)		sendMessage(iterator.next().toString(), extraAddress);
				else							sendMessage(iterator.next().toString(), null);
			}
		}
		
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		start(_startedBy);
		return START_STICKY;
	}
	
	/*
	 * Just start the thread
	 */
	public void start(String byWho)
	{
		_startedBy = byWho;
		if (_startedBy != null)
			Log.i(TAG, "CommService: started by '" + _startedBy + "'");
		
		startListenThread();
	}
		
	@Override
	public void onDestroy()
	{
		//Gracefully stop the running thread
		_bServiceStopping = true;
		
		//Cancel the persistent notification.
		hideNotification();
		
		/*
		//Wait for all messages to be transmitted
		int count = 0;
		while (_thread != null)
		{
			try {
				Thread.sleep(100);
				count++;
				if (count > 10)
					break;
			}
			catch (Exception e) {
				
			}
		}
		*/
				
		Log.d(TAG, "CommService: destroying...");
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}
	
	/**
     * Return if the binder process is still alive
     */
	public boolean isServiceRunning()
	{
		return !_bServiceStopping && _mySocket != null;
	}
	
	/**
     * Create a thread and run BlockingSendReceive function
     */
	private void startListenThread()
	{
		_thread = new Thread(new Runnable()
		{
			public void run()
			{
				BlockingSendReceive();
				
				//Thread tempThread = _thread;
				_thread = null;
				
				//Deprecated :(
				//if (tempThread != null)
				//	tempThread.stop();
			}
		});
		_thread.start();
	}
	
	/**
     * Blocking receiving & sending function
     * To be run in a separate thread
     */
	private void BlockingSendReceive()
	{
		Intent intent = new Intent(NEW_MESSAGE);
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		boolean receivedData = false;
		int tempCounter = 0;
		
		//This could be 3G IP Address when WiFi was not enabled initially
		_myIP = getWifiIpAddress();
		Log.d(TAG, "CommService: thread started");
				
		while (true)
		{ 
			if (_bServiceStopping || _mySocket == null)
            {            	
            	Log.d(TAG, "CommService: thread stopped");
            	if (_mySocket != null)
            		_mySocket.close();
                return; 
            }

			//Laggy behaviour without this
            try
            {
            	Thread.sleep(15);
            }
            catch (Exception e)
            {
                continue; 
            }
            
            // Wait to receive a datagram
            receivedData = false;
            try
            {
            	//if (USE_MULTICAST)
            	//	((MulticastSocket)_mySocket).receive(packet);
            	//else
            		_mySocket.receive(packet);
                receivedData = true; 
            }
            catch (SocketTimeoutException e)
            {
                 
            }
            catch (IOException e)
            {
            	//Log.d(TAG, "Socket IOException (Receiving)");
            }
            
            //Process received packet if any
            if (receivedData)
            {
            	if (_myIP == null)
	            	_myIP = getWifiIpAddress();
            	
	            // Convert the contents to a string, and display them 
	            String message = new String(buffer, 0, packet.getLength());
	            
	            InetAddress senderAddress = packet.getAddress();
	            
	            // Ignore loopback messages
	            if (!senderAddress.getHostAddress().equals(_myIP))
	            {
		            // Inject local data
		            intent.putExtra(MessageReceiver.MESSAGE_KEY_MESSAGE, message);
		            intent.putExtra(MessageReceiver.MESSAGE_KEY_ADDRESS, senderAddress.getHostAddress());
		            
		            // This is slow & blocking on HTC phones
					//intent.putExtra("hostname", senderAddress.getHostName());

					sendBroadcast(intent);
	            }
            }
            
            //Send packets in queue
            if (TX_QUEUE)
            {
                tempCounter = 0;
	            while (_transmitQueue.size() > 0 && !_bServiceStopping)
	            {
	            	DatagramPacket tempPacket = null;
	            	synchronized (_transmitQueue) {
	            		tempPacket = _transmitQueue.removeFirst();
					}
	            	if (tempPacket == null) {
	            		Log.d(TAG, "Unable to get lock on _transmitQueue");
	            		break;
	            	}
	            	
	            	try
	                {
	            		_mySocket.send(tempPacket);
	            		tempCounter++;
	                }
	                catch (IOException e)
	                {
	                	//This error occurs when we switch between networks. Safe to ignore
	                	//Log.d(TAG, "Socket IOException (Sending)");
	                }
	            }
            }
            
            //If this was started by a BroadcastReceiver (widget), we stop here
            if (_startedBy == null || _startedBy.equals("widget"))
            {
            	Log.i(TAG, "CommService: sent " + tempCounter + " messages");
            	Log.i(TAG, "CommService: stopping...");
            	_bServiceStopping = true;
            }

            packet.setLength(buffer.length);
		}
	}
	
	/**
     * Obtain a proper network broadcast address by masking own IP address with netmask
     */
	public InetAddress getBroadcastAddress() throws IOException
	{
		WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
	    DhcpInfo dhcp = wifiManager.getDhcpInfo();
	    if (dhcp == null)
	    	return null;

	    int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
	    byte[] quads = new byte[4];
	    for (int k = 0; k < 4; k++)
	      quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
	    
	    return InetAddress.getByAddress(quads);
	}
	
	/**
     * Get local IP address
     */
	public String getWifiIpAddress()
	{
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		if (!wifiManager.isWifiEnabled())
			return null;
		
	    try
	    {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
	        {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
	            {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress())
	                    return inetAddress.getHostAddress().toString();
	            }
	        }
	    }
	    catch (SocketException ex)
	    {
	        Log.e(TAG, ex.toString());
	    }
	    return null;
	}
	
	
	/**
     * Send a String message directly
     */
	public boolean sendMessage(String message, String address)
	{
		return sendMessage(message.getBytes(), message.length(), address);
	}
	
	/**
     * Send a byte[] message directly
     */
	public boolean sendMessage(byte[] data, int length, String address)
	{
		if (data == null || length <= 0)
			return false;
		
		//Refresh the broadcast address every time
		try
		{
			//_broadcastAddress = USE_MULTICAST ? InetAddress.getByName(MULTICAST_GROUP) : getBroadcastAddress();
			_broadcastAddress = getBroadcastAddress();
		}
		catch(IOException e)
		{
			_broadcastAddress = null;
		}
		
		InetAddress inetaddr = _broadcastAddress;
		try
		{
			if (address != null && address != "")
				inetaddr = InetAddress.getByName(address); 
		}
		catch(IOException e)
		{
			inetaddr = null;
		}
		
		if (inetaddr == null)
			return false;
		
		DatagramPacket tempPacket = new DatagramPacket(data, length, inetaddr, _port);
		
		if (TX_QUEUE)
		{
			//Mutex
			synchronized (_transmitQueue) {
				_transmitQueue.addLast(tempPacket);
			}
		}
		else
		{
			try
	        {
	    		_mySocket.send(tempPacket);
	        }
	        catch (IOException e)
	        {
	        	return false;
	        }
		}
		
		return true;
	}
	
	/**
     * Show a notification while this service is running.
     */
    protected void showNotification()
    {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Communication service for NeTV";

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, ActivitySplash.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);

        // Send the notification.
        _notificationManager.notify(NOTIFICATION, notification);
    }
    
    /**
     * Hide notification icon
     */
    protected void hideNotification()
    {
        _notificationManager.cancel(NOTIFICATION);
    }
}
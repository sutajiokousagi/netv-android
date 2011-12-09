package com.bunniestudios.NeTV;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CustomEventReceiver extends BroadcastReceiver
{
	CustomEventReceiverInterface _parent = null;
	
	public CustomEventReceiver(CustomEventReceiverInterface parent)
	{
		_parent = parent;
	}
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		/*
		 * A Bundle is functionally equivalent to a standard Map. The reason we didn't
			just use a Map is because in the contexts where Bundle is used, the only
			things that are legal to put into it are primitives like Strings, ints, and
			so on. Because the standard Map API lets you insert arbitrary Objects, this
			would allow developers to put data into the Map that the system can't
			actually support, which would lead to weird, non-intuitive application
			errors. Bundle was created to replace Map with a typesafe container that
			makes it explicitly clear that it only supports primitives. 
		 */
		
		// Pass the whole Bundle (Android-specific, not Java data type)
		_parent.NewMessageEvent( intent.getExtras() );
	}
};
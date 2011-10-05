package com.chumby.controlpanellite.chumbynetwork;

import java.util.Date;
import java.util.Locale;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.util.DisplayMetrics;
import android.app.Activity;
import android.content.res.Configuration;

import com.chumby.Chumby;
import com.chumby.util.GUID;
import com.chumby.util.XMLBuilder;
import com.chumby.util.XMLHelper;
import com.chumby.util.ChumbyLog;

public class AndroidStats {
	private final static String TAG="AndroidStats";
	private final static String VERSION = "1";

	static AndroidStats _instance;
	
	static AndroidStats getInstance() {
		if (_instance == null) {
			_instance = new AndroidStats();
		}
		return _instance;
	}

	private void toXML(XMLBuilder parent,Activity a) {
		XMLBuilder node = parent;
		long from = (new Date()).getTime()/1000;
		long to = from;
		node.a("version",VERSION);
		node.a("from",from);
		node.a("to", to);
		node.a("guid",Chumby.getInstance().guid);
		node.a("platform",Chumby.getInstance().platform);
		node.a("validation", GUID.guidOf(Chumby.getInstance().guid+Long.toString(from)+Long.toString(to)).toUpperCase());
		addFields(node,a);
	}
	
	private void addField(XMLBuilder node,String name,String value) {
		node.e("field").a("name", name).a("value", value);
	}

	private void addFields(XMLBuilder node,Activity a) {
		addField(node,"VERSION.CODENAME",Build.VERSION.CODENAME);
		addField(node,"VERSION.INCREMENTAL",Build.VERSION.INCREMENTAL);
		addField(node,"VERSION.RELEASE",Build.VERSION.RELEASE);
		addField(node,"VERSION.SDK_INT",Integer.toString(Build.VERSION.SDK_INT));
		addField(node,"BOARD",Build.BOARD);
		addField(node,"BOOTLOADER",Build.BOOTLOADER);
		addField(node,"BRAND",Build.BRAND);
		addField(node,"CPU_ABI",Build.CPU_ABI);
		addField(node,"CPU_ABI2",Build.CPU_ABI2);
		addField(node,"DEVICE",Build.DEVICE);
		addField(node,"DISPLAY",Build.DISPLAY);
		addField(node,"FINGERPRINT",Build.FINGERPRINT);
		addField(node,"HARDWARE",Build.HARDWARE);
		addField(node,"HOST",Build.HOST);
		addField(node,"ID",Build.ID);
		addField(node,"MANUFACTURER",Build.MANUFACTURER);
		addField(node,"MODEL",Build.MODEL);
		addField(node,"PRODUCT",Build.PRODUCT);
		addField(node,"RADIO",Build.RADIO);
		addField(node,"TAGS",Build.TAGS);
		addField(node,"TIME",Long.toString(Build.TIME));
		addField(node,"TYPE",Build.TYPE);
		addField(node,"USER",Build.USER);
    	Display display = a.getWindowManager().getDefaultDisplay();
    	DisplayMetrics metrics = new DisplayMetrics();
    	display.getMetrics(metrics);
		addField(node,"DISPLAY.WIDTH",Integer.toString(display.getWidth()));
		addField(node,"DISPLAY.HEIGHT",Integer.toString(display.getHeight()));
		addField(node,"DISPLAY.ROTATION",Integer.toString(display.getRotation()));
		addField(node,"DISPLAY.PIXEL_FORMAT",Integer.toString(display.getPixelFormat()));
		addField(node,"DISPLAY.METRICS.DENSITY",Float.toString(metrics.density));
		addField(node,"DISPLAY.METRICS.DENSITY_DPI",Integer.toString(metrics.densityDpi));
		addField(node,"DISPLAY.METRICS.WIDTH_PIXELS",Integer.toString(metrics.widthPixels));
		addField(node,"DISPLAY.METRICS.HEIGHT_PIXELS",Integer.toString(metrics.heightPixels));
		addField(node,"DISPLAY.METRICS.XDPI",Float.toString(metrics.xdpi));
		addField(node,"DISPLAY.METRICS.YDPI",Float.toString(metrics.ydpi));
		addField(node,"DISPLAY.METRICS.SCALED_DENSITY",Float.toString(metrics.scaledDensity));
		
		Configuration config = a.getResources().getConfiguration();
		addField(node,"CONFIG.KEYBOARD",Integer.toString(config.keyboard));
		addField(node,"CONFIG.KEYBOARD_HIDDEN",Integer.toString(config.keyboardHidden));
		addField(node,"CONFIG.KEYBOARD_HIDDEN_HARD",Integer.toString(config.hardKeyboardHidden));
		addField(node,"CONFIG.NAVIGATION",Integer.toString(config.navigation));
		addField(node,"CONFIG.NAVIGATION_HIDDEN",Integer.toString(config.navigationHidden));
		addField(node,"CONFIG.TOUCHSCREEN",Integer.toString(config.touchscreen));
		addField(node,"CONFIG.SCREEN_LAYOUT",Integer.toString(config.screenLayout));
		addField(node,"CONFIG.UI_MODE",Integer.toString(config.uiMode));
		addField(node,"CONFIG.ORIENTATION",Integer.toString(config.orientation));
		addField(node,"CONFIG.FONT_SCALE",Float.toString(config.fontScale));
		
		Locale locale = config.locale;
		if (locale!=null) {
			addField(node,"LOCALE.COUNTRY_CODE",locale.getCountry());
			addField(node,"LOCALE.COUNTRY",locale.getDisplayCountry());
			addField(node,"LOCALE.LANGUAGE_CODE",locale.getLanguage());
			addField(node,"LOCALE.LANGUAGE",locale.getDisplayLanguage());
			addField(node,"LOCALE.VARIANT_CODE",locale.getVariant());
			addField(node,"LOCALE.VARIANT",locale.getDisplayVariant());
		}
	}
	
	private void send(Activity a) {
		//ChumbyLog.i(TAG+".send()");
		XMLBuilder builder = XMLBuilder.create("platform");
		toXML(builder,a);
		//ChumbyLog.i(XMLHelper.nodeToString(builder.getDocument()));
		Handler xmlHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case XMLHelper.DID_SUCCEED: {
						//ChumbyLog.i(TAG+".send.handler.handleMessage(): success");
						break;
					}
					case XMLHelper.DID_ERROR: {
						ChumbyLog.w(TAG+".send.handler.handleMessage(): failure");
						//Exception e = (Exception) message.obj;
						//e.printStackTrace();
						break;
					}
				}
			}
		};
		XMLHelper.sendAndLoad(Chumby.getInstance().makeURL("/duas/platform"), builder.getDocument(),xmlHandler);
	}
	
	public static void sendStats(Activity a) {
		getInstance().send(a);
	}
}

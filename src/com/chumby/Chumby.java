package com.chumby;
import android.os.Build;
import com.chumby.util.ChumbyLog;

/**
 * Encapsulates data about a chumby-like device
 * @author Duane Maxwell
 *
 */
public class Chumby {
	private static Chumby _instance;
	
	public String hardwareVersion;
	public String softwareVersion;
	public String firmwareVersion;
	public String modelNumber;
	public String serialNumber;
	public String guid;
	public String platform;
	
	public final static String DEFAULT_BASE_DOMAIN = "xml.chumby.com";
	public final static String DEFAULT_BASE_URL = "http://"+DEFAULT_BASE_DOMAIN;
	//public final static String DEFAULT_BASE_URL = "http://www-test.chumby.com";
	public final static String DEFAULT_WIDGETS_URL = "http://widgets.chumby.com";
	
	public String baseURL;
	public String widgetsURL;
	
	/**
	 * Constructor
	 */
	private Chumby() {
		Chumby._instance = this;
		baseURL = DEFAULT_BASE_URL;
		widgetsURL = DEFAULT_WIDGETS_URL;
		hardwareVersion = "4.0";
		softwareVersion = "1.0";
		firmwareVersion = "1.0";
		serialNumber = "none";
		modelNumber = "Android "+Build.VERSION.RELEASE;
		guid = "61261AF9-7986-9A25-59BF-AE830254C59B"; // will be replaced...
		platform = "android";
		//dumpModelInfo();
	}
	
	/**
	 * Returns the singleton instance of this class, creating it if necessary
	 * @return {@link Chumby} the singleton instance
	 */
	public static Chumby getInstance() {
		if (Chumby._instance==null) {
			new Chumby();
		}
		return Chumby._instance;
	}
	
	/**
	 * Converts a partial URL to a full URL to the default XML server
	 * @param urlFragment {@link String} a partial URL
	 * @return {@link String} a full URL
	 */
	public String makeURL(String urlFragment) {
		if (urlFragment.startsWith("/")) {
			return baseURL+urlFragment;
		}
		return urlFragment;
	}
	
	/**
	 * Converts a partial URL to a full URL to the default XML server, using HTTPS
	 * 
	 * @param urlFragment {@link String} a partial URL
	 * @return {@link String} a full URL
	 */
	public String makeSecureURL(String urlFragment) {
		String url = makeURL(urlFragment);
		if (url.startsWith("http://")) {
			url = url.replace("http://","https://");
		}
		return url;
	}

	/**
	 * Converts a partial URL to a full URL to the default widget/app server
	 * @param urlFragment {@link String} a partial URL
	 * @return
	 */
	public String makeWidgetsURL(String urlFragment) {
		if (urlFragment.startsWith("/")) {
			return widgetsURL+urlFragment;
		}
		return urlFragment;
	}

	/**
	 * Returns a string with device-specific parameters, typically version numbers
	 * @return {@link String} device-specific parameters
	 */
	public static String getArgs() {
		Chumby c = Chumby.getInstance();
		return "&hw="+c.hardwareVersion+"&sw="+c.softwareVersion+"&fw="+c.firmwareVersion;
	}
	
	/**
	 * Dumps information about the device to the log
	 */
	public static void dumpModelInfo() {
		ChumbyLog.i("VERSION.CODENAME: "+Build.VERSION.CODENAME);
		ChumbyLog.i("VERSION.INCREMENTAL: "+Build.VERSION.INCREMENTAL);
		ChumbyLog.i("VERSION.RELEASE: "+Build.VERSION.RELEASE);
		ChumbyLog.i("VERSION.SDK_INT: "+Integer.toString(Build.VERSION.SDK_INT));
		ChumbyLog.i("BOARD: "+Build.BOARD);
		ChumbyLog.i("BOOTLOADER: "+Build.BOOTLOADER);
		ChumbyLog.i("BRAND: "+Build.BRAND);
		ChumbyLog.i("CPU_ABI: "+Build.CPU_ABI);
		ChumbyLog.i("CPU_ABI2: "+Build.CPU_ABI2);
		ChumbyLog.i("DEVICE: "+Build.DEVICE);
		ChumbyLog.i("DISPLAY: "+Build.DISPLAY);
		ChumbyLog.i("FINGERPRINT: "+Build.FINGERPRINT);
		ChumbyLog.i("HARDWARE: "+Build.HARDWARE);
		ChumbyLog.i("HOST: "+Build.HOST);
		ChumbyLog.i("ID: "+Build.ID);
		ChumbyLog.i("MANUFACTURER: "+Build.MANUFACTURER);
		ChumbyLog.i("MODEL: "+Build.MODEL);
		ChumbyLog.i("PRODUCT: "+Build.PRODUCT);
		ChumbyLog.i("RADIO: "+Build.RADIO);
		ChumbyLog.i("TAGS: "+Build.TAGS);
		ChumbyLog.i("TIME: "+Long.toString(Build.TIME));
		ChumbyLog.i("TYPE: "+Build.TYPE);
		ChumbyLog.i("USER: "+Build.USER);
	}
}

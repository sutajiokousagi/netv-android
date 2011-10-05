package com.chumby.NeTV;

public class ConsoleListItem
{
	 private String ssid = "";
	 private String type = "";
	 private int level = 0;
	 private boolean encryption = false;
	 //private String icon = "";
	 
	 public ConsoleListItem(String title, String type)
	 {
		 setTitle( title );
		 setType( type );
	 }

	 public void setTitle(String ssid)
	 {
		 this.ssid = ssid;
	 }

	 public String getTitle()
	 {
		 return ssid;
	 }
	 
	 public void setType(String type)
	 {
		 this.type = type;
	 }

	 public String getType()
	 {
		 return type;
	 }
	 
	 // Reserve for later
	 //----------------------------------------------
	 
	 public void setLevel(int level)
	 {
		 this.level = level;
	 }

	 public int getLevel()
	 {
		 return level;
	 }
	 
	 public void setEncryption(String encryption)
	 {
		 this.encryption = (encryption.length() > 2) ? true : false;
	 }

	 public boolean getEncryption()
	 {
		 return encryption;
	 }
}
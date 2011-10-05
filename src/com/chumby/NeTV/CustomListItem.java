package com.chumby.NeTV;

public class CustomListItem
{
	 private String title = "";
	 private String description = "";
	 private String icon = "";
	 private String tag = "";
	 //private int level = 0;
	 //private boolean encryption = false;

	 public void setTitle(String title)
	 {
		 this.title = title;
	 }

	 public String getTitle()
	 {
		 return title;
	 }
	 
	 public void setDescription(String description)
	 {
		 this.description = description;
	 }

	 public String getDescription()
	 {
		 return description;
	 }
	 
	 public void setIcon(String icon)
	 {
		 this.icon = icon;
	 }

	 public String getIcon()
	 {
		 return icon;
	 }
	 
	 public void setTag(String tag)
	 {
		 this.tag = tag;
	 }

	 public String getTag()
	 {
		 return tag;
	 }
	 
	 //More fancy properties below
	 
	 /*
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
	 */

}
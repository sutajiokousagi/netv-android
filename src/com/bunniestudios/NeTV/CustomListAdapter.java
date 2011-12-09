package com.bunniestudios.NeTV;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomListAdapter extends BaseAdapter
{
	 private static ArrayList<CustomListItem> searchArrayList;
	 
	 private LayoutInflater mInflater;

	 public CustomListAdapter(Context context, ArrayList<CustomListItem> results)
	 {
		 searchArrayList = results;
		 mInflater = LayoutInflater.from(context);
	 }

	 public int getCount()
	 {
		 return searchArrayList.size();
	 }

	 public Object getItem(int position)
	 {
		 return searchArrayList.get(position);
	 }

	 public long getItemId(int position)
	 {
		 return position;
	 }

	 public View getView(int position, View convertView, ViewGroup parent)
	 {
		  ViewHolder holder;
		  if (convertView == null)
		  {
			  convertView = mInflater.inflate(R.layout.custom_list_item, null);
			  holder = new ViewHolder();
			  holder.imgIcon = (ImageView) convertView.findViewById(R.id.custom_list_item_icon);
			  holder.txtTitle = (TextView) convertView.findViewById(R.id.custom_list_item_line1);
			  holder.txtDescription = (TextView) convertView.findViewById(R.id.custom_list_item_line2);
			  convertView.setTag(holder);
		  }
		  else
		  {
			  holder = (ViewHolder) convertView.getTag();
		  }
		  
		  //Title
		  holder.txtTitle.setText(searchArrayList.get(position).getTitle());
		  
		  //Description
		  String descp = searchArrayList.get(position).getDescription();
		  holder.txtDescription.setText(descp);
		  if (descp.length() > 1)		holder.txtDescription.setVisibility(View.VISIBLE);
		  else						  	holder.txtDescription.setVisibility(View.INVISIBLE);
		  
		  //Icon
		  
		  return convertView;
	 }

	 static class ViewHolder
	 {
		 ImageView imgIcon;
		 TextView txtTitle;
		 TextView txtDescription;
	 }
}

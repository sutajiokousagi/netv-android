package com.chumby.NeTV;

import java.util.ArrayList;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ConsoleListAdapter extends BaseAdapter
{
	 private static ArrayList<ConsoleListItem> searchArrayList;
	 
	 private LayoutInflater mInflater;

	 public ConsoleListAdapter(Context context, ArrayList<ConsoleListItem> results)
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
			  convertView = mInflater.inflate(R.layout.console_list_item, null);
			  holder = new ViewHolder();
			  holder.txtSSID = (TextView) convertView.findViewById(R.id.line1);
			  holder.txtDescription = (TextView) convertView.findViewById(R.id.line2);
			  holder.imgIcon = (ImageView) convertView.findViewById(R.id.icon);
	
			  convertView.setTag(holder);
		  }
		  else
		  {
			  holder = (ViewHolder) convertView.getTag();
		  }
		  
		  holder.txtSSID.setText( Html.fromHtml( searchArrayList.get(position).getTitle() ) );
		  holder.txtDescription.setText( Html.fromHtml( searchArrayList.get(position).getType() ) );
		  //holder.imgIcon.setText(searchArrayList.get(position).getIcon());
	
		  return convertView;
	 }

	 static class ViewHolder
	 {
		 TextView txtSSID;
		 TextView txtDescription;
		 ImageView imgIcon;
	 }
}

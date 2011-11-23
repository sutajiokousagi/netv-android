package com.chumby.NeTV;

import java.util.ArrayList;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class CustomDialog extends Dialog
{
    public CustomDialog(Context context, int theme) {
        super(context, theme);
    }

    public CustomDialog(Context context) {
        super(context);
    }

    /**
     * Helper class for creating a custom dialog
     */
    public static class Builder {

        private Context 					context;
        private String 						title;
        private String						message;
        private ArrayList<CustomListItem> 	listViewData;
        private View 						contentView;
        private AdapterView.OnItemClickListener listViewItemClickListener;
        private View.OnClickListener		clickListener;

        public Builder(Context context) {
            this.context = context;
        }

        /**
         * Set the Dialog message from String
         * @param title
         * @return
         */
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        /**
         * Set the Dialog message from resource
         * @param title
         * @return
         */
        public Builder setMessage(int message) {
            this.message = (String) context.getText(message);
            return this;
        }

        /**
         * Set the Dialog title from resource
         * @param title
         * @return
         */
        public Builder setTitle(int title) {
            this.title = (String) context.getText(title);
            return this;
        }

        /**
         * Set the Dialog title from String
         * @param title
         * @return
         */
        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }
        
        /**
         * Set click handler for the buttons
         * @param title
         * @return
         */
        public Builder setOnClickListener(View.OnClickListener listener) {
            this.clickListener = listener;
            return this;
        }       
        
        /**
         * Set the data for list view
         * @param listViewData
         * @return
         */
        public Builder setListViewData(ArrayList<CustomListItem> listViewData)
        {
            this.listViewData = listViewData;
            return this;
        }
        
        /**
         * Set the data for list view and it's listener
         * @param positiveButtonText
         * @param listener
         * @return
         */
        public Builder setListViewData(ArrayList<CustomListItem> listViewData,
                AdapterView.OnItemClickListener listener)
        {
        	this.listViewData = listViewData;
            this.listViewItemClickListener = listener;
            return this;
        }
        
        /**
         * Set a custom content view for the Dialog.
         * If a message is set, the contentView is not
         * added to the Dialog...
         * @param v
         * @return
         */
        public Builder setContentView(View v) {
            this.contentView = v;
            return this;
        }
        
        /**
         * Create the custom dialog
         */
        public CustomDialog create()
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            
            // instantiate the dialog with the custom Theme
            final CustomDialog dialog = new CustomDialog(context, R.style.Dialog);
            View layout = inflater.inflate(R.layout.custom_dialog, null);
            dialog.addContentView(layout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
            
            // set the dialog title
            ((TextView)layout.findViewById(R.id.title)).setText(title);
            
            // setup the custom list view
            ListView lst = (ListView)layout.findViewById(R.id.listview);
            lst.setAdapter(new CustomListAdapter(context, listViewData));

            if (listViewItemClickListener != null)
            {
            	lst.setOnItemClickListener(new OnItemClickListener()
	            {
	                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
	                {
	                	listViewItemClickListener.onItemClick(arg0,arg1,position,id); 
	                }
	            });
            }
            
            if (clickListener != null)
            {
            	((Button)layout.findViewById(R.id.btn_refresh)).setOnClickListener(new View.OnClickListener()
            	{
					public void onClick(View v)
					{
						clickListener.onClick(v);
					}
				});
            }
            
            // set the content message
            if (message != null)
            {
                ((TextView) layout.findViewById(R.id.message)).setText(message);
            }
            else if (contentView != null)
            {
                // if no message set
                // add the contentView to the dialog body
                ((LinearLayout) layout.findViewById(R.id.content)).removeAllViews();
                ((LinearLayout) layout.findViewById(R.id.content)).addView(contentView, 
                                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            }
            dialog.setContentView(layout);
            return dialog;
        }

    }

}
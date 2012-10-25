package com.shreyaschand.czshopper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshScrollView;

public class Home extends Activity implements OnClickListener {

	LinearLayout itemsListView;
	LayoutInflater inflater;
	PullToRefreshScrollView pullToRefreshView; 
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViewById(R.id.add_button).setOnClickListener(this);
        itemsListView = (LinearLayout) findViewById(R.id.list);
        inflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        
        pullToRefreshView = (PullToRefreshScrollView) findViewById(R.id.list_pull__scroller);
        pullToRefreshView.setOnRefreshListener(new OnRefreshListener<ScrollView>() {
            public void onRefresh(PullToRefreshBase<ScrollView> refreshView) {
                new GetDataTask().execute();
            }
        });     
    }

    private class GetDataTask extends AsyncTask<Void, Void, HashMap<String, ArrayList<String>>> {
        
        @Override
        protected void onPostExecute(HashMap<String, ArrayList<String>> result) {
            // Call onRefreshComplete when the list has been refreshed.
            pullToRefreshView.onRefreshComplete();
            super.onPostExecute(result);
            for(Map.Entry<String, ArrayList<String>> entry : result.entrySet()){
            	String category = entry.getKey();
            	ArrayList<String> items = entry.getValue();
            	
            	LinearLayout listItem = (LinearLayout) inflater.inflate(R.layout.list_item, null);
        		((TextView)listItem.getChildAt(1)).setText(category);
        		listItem.getChildAt(0).setVisibility(View.GONE);
        		itemsListView.addView(listItem);
        		
        		for(String item : items){
        			listItem = (LinearLayout) inflater.inflate(R.layout.list_item, null);
            		((TextView)listItem.getChildAt(1)).setText(item);
            		itemsListView.addView(listItem);
        		}
            }
            
        }

		@Override
		protected HashMap<String, ArrayList<String>> doInBackground(Void... params) {
			HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
			ArrayList<String> items = new ArrayList<String>();
			items.add("Cherry Coke");
			items.add("Apple Juice");
			items.add("Sprite");
			result.put("Beverages", items);
			return result;
		}
    }
    
   	public void onClick(View v) {
		
		Toast.makeText(this, "Add New Item", Toast.LENGTH_LONG).show();
		//startActivity(new Intent(this, AddListItem.class));
		
	}
}

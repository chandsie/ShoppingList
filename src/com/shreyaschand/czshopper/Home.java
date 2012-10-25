package com.shreyaschand.czshopper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
	ConnectivityManager connMgr;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViewById(R.id.add_button).setOnClickListener(this);
        itemsListView = (LinearLayout) findViewById(R.id.list);
        
        inflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        
        pullToRefreshView = (PullToRefreshScrollView) findViewById(R.id.list_pull__scroller);
        pullToRefreshView.setOnRefreshListener(new OnRefreshListener<ScrollView>() {
            public void onRefresh(PullToRefreshBase<ScrollView> refreshView) {
                new GetDataTask().execute(true);
            }
        });
        
        new GetDataTask().execute(false);
    }

    private class GetDataTask extends AsyncTask<Boolean, Void, HashMap<String, ArrayList<String>>> {
        
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
		protected HashMap<String, ArrayList<String>> doInBackground(Boolean... networkRefresh) {
			    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			    JSONArray jsonData = null;
			    if (networkRefresh[0] && networkInfo != null && networkInfo.isConnected()) {
			        // Fetch updates from server if asked to do so and if network is available
			    	try {
				    	URLConnection conn = new URL("http://czshopper.herokuapp.com/items.json").openConnection();
						conn.setRequestProperty("Accept", "application/json");
						conn.setRequestProperty("X-CZ-Authorization", "quqSxtRqyBowMcz46qKr");
				    	conn.connect();
						BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream())); 
						String response = br.readLine();
						br.close();
						jsonData = new JSONArray(response);
			    	} catch (IOException ioe){
			    		// Network Error. Deal with it.
			    	} catch (JSONException e) {
						// Bad stuff happened. Probably not something I did.
					}
			    } else {
			        // Use saved values from db
			    }
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

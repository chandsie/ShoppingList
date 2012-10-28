package com.shreyaschand.czshopper;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshScrollView;

public class Home extends Activity {

	public static final String FILENAME = "items.json";
	private static final String URL = "http://czshopper.herokuapp.com/items.json";
	private LinearLayout itemsListView;
	private LayoutInflater inflater;
	private PullToRefreshScrollView pullToRefreshView;
	private ConnectivityManager connManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		findViewById(R.id.add_button).setOnClickListener(new AddItemListener());

		itemsListView = (LinearLayout) findViewById(R.id.list);

		// Pre-fetch and save the inflater; used to create layout for new list elements
		inflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		// Pre-fetch and save the connection manager; used to check connectivity when refreshing
		connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		pullToRefreshView = (PullToRefreshScrollView) findViewById(R.id.list_pull__scroller);
		pullToRefreshView.setOnRefreshListener(new OnRefreshListener<ScrollView>() {
			public void onRefresh(PullToRefreshBase<ScrollView> refreshView) {
				new GetDataTask().execute(true);
			}
		});

		new GetDataTask().execute(false);
	}

	private class GetDataTask extends AsyncTask<Boolean, Void, ArrayList<HashMap<String, String>>> {

		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(Boolean... networkRefresh) {

			ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
			NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
			String response = null;

			if (networkRefresh[0] && networkInfo != null && networkInfo.isConnected()) {
				// Fetch updates from server if asked to do so and if network is available
				try {
					HttpURLConnection conn = (HttpURLConnection) new URL(URL).openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Accept", "application/json");
					conn.setRequestProperty("X-CZ-Authorization", "quqSxtRqyBowMcz46qKr");
					conn.connect();
					BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					response = br.readLine();
					br.close();
					// Save the JSON for future offline access
					FileOutputStream outFile = openFileOutput(FILENAME, Context.MODE_PRIVATE);
					outFile.write(response.getBytes());
					outFile.close();
				} catch (IOException ioe) {
					Toast.makeText(Home.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
				}
			} else {
				// Use saved values from db
				try {
					BufferedReader inFile = new BufferedReader(new InputStreamReader(openFileInput(FILENAME)));
					response = inFile.readLine();
					inFile.close();
				} catch (IOException e) {
					// Error finding, reading or closing the file.
				}
			}

			if(response != null){
				// Convert the raw string response into an array of JSON objects
				JSONArray jsonData = null;
				try {
					jsonData = new JSONArray(response);
				}catch (JSONException e) {
					// Bad stuff happened. Probably not something I did.
					Toast.makeText(Home.this, getString(R.string.json_error), Toast.LENGTH_SHORT).show();
				}
				// Convert the JSONArray into a regular ArrayList of HashMaps
				// to allow get method (web vs. db) independent processing
				for (int i = 0; i != jsonData.length(); i++) {
					HashMap<String, String> item = new HashMap<String, String>();
					try {
						JSONObject jsonObject = jsonData.getJSONObject(i);
						item.put("id", jsonObject.getString("id"));
						item.put("category", jsonObject.getString("category"));
						item.put("name", jsonObject.getString("name"));
					} catch (JSONException e) {
						// bad stuff happened. don't make this happen.
						Toast.makeText(Home.this, getString(R.string.json_error), Toast.LENGTH_SHORT).show();
					}
					result.add(item);
				}
			}

			return result;
		}

		@Override
		protected void onPostExecute(ArrayList<HashMap<String, String>> items) {
			// Call onRefreshComplete when the list has been refreshed.
			pullToRefreshView.onRefreshComplete();
			super.onPostExecute(items);

			// Clear all entries in the view
			itemsListView.removeAllViews();

			for(HashMap<String, String> item : items) {
				String category = item.get("category");
				LinearLayout categoryLayout = (LinearLayout) itemsListView.findViewWithTag(category);
				// if not found, create a new layout for the category
				if (categoryLayout == null) {
					categoryLayout = (LinearLayout) inflater.inflate(R.layout.category_group, null);
					categoryLayout.setTag(category);

					// inflate the item view, remove the checkbox and button and then set the category text
					LinearLayout categoryHeader = (LinearLayout) inflater.inflate(R.layout.list_item, null);
					categoryHeader.removeViewAt(0);
					categoryHeader.removeViewAt(1);
					// since the checkbox was removed, the textview is the first element in the layout
					((TextView)categoryHeader.getChildAt(0)).setText(category);

					categoryLayout.addView(categoryHeader);
					itemsListView.addView(categoryLayout);
				}


				String id = item.get("id");
				LinearLayout itemLayout =  (LinearLayout) inflater.inflate(R.layout.list_item, null);
				itemLayout.setTag(id);

				itemLayout.getChildAt(0).setOnClickListener(new CheckItemListener());

				TextView tView = ((TextView)itemLayout.getChildAt(1));
				tView.setText(item.get("name"));
				tView.setOnClickListener(new EditItemListener());
				
				itemLayout.getChildAt(2).setOnClickListener(new DeleteItemListener());

				((LinearLayout) categoryLayout).addView(itemLayout);


			}

		}
	}

	private class AddItemListener implements OnClickListener {
		public void onClick(View v) {
			Toast.makeText(Home.this, "Add New Item", Toast.LENGTH_SHORT).show();
			// startActivity(new Intent(this, AddListItem.class));
		}
	}

	private class EditItemListener implements OnClickListener{
		public void onClick(View v) {
			Toast.makeText(Home.this, "Editing Item " + ((View) v.getParent()).getTag(), Toast.LENGTH_SHORT).show();
		}
	}

	private class CheckItemListener implements OnClickListener{
		public void onClick(View v) {
			LinearLayout itemLayout = (LinearLayout) v.getParent();
			TextView textView = (TextView) itemLayout.getChildAt(1);

			if (((CheckBox) v).isChecked()){
				// Apply the strike_thru styling bit mask to strike off checked items in the list
				textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
				itemLayout.getChildAt(2).setVisibility(View.VISIBLE);
				textView.setClickable(false);
			} else {
				// Apply the inverse of the strike_thru styling bit mask to unstrike an item
				textView.setPaintFlags(textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
				itemLayout.getChildAt(2).setVisibility(View.GONE);
				textView.setClickable(true);
			}
			Toast.makeText(Home.this, "Checked off Item " + ((View) v.getParent()).getTag(), Toast.LENGTH_SHORT).show();
		}
	}
	
	private class DeleteItemListener implements OnClickListener{
		public void onClick(View v) {
			LinearLayout itemLayout = (LinearLayout) v.getParent();
			Toast.makeText(Home.this, "Deleting Item " + ((View) v.getParent()).getTag(), Toast.LENGTH_SHORT).show();
		}
	}
}

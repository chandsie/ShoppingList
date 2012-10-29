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
import android.content.Intent;
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

	protected static final String FILENAME = "items.json";
	protected static final String URL = "http://czshopper.herokuapp.com/items";
	protected static final String ITEM_MESSAGE = "com.shreyaschand.czshopper.UPDATE_ITEM_MESSAGE";
	protected static final int HTTP_OK = 200;
	private static final int ADD_ITEM_REQUEST = 41;
	private static final int UPDATE_ITEM_REQUEST = 42;

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

	private class GetDataTask extends AsyncTask<Boolean, Integer, ArrayList<HashMap<String, String>>> {

		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(Boolean... networkRefresh) {
			ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
			NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
			String response = null;

			if (networkRefresh[0] && networkInfo != null && networkInfo.isConnected()) {
				// Fetch updates from server if asked to do so and if network is available
				try {
					HttpURLConnection conn = (HttpURLConnection) new URL(URL+".json").openConnection();
					conn.setConnectTimeout(5 * 1000);
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
					publishProgress(R.string.network_error);
					return null;
				}
			} else {
				// Use saved values from db
				try {
					BufferedReader inFile = new BufferedReader(new InputStreamReader(openFileInput(FILENAME)));
					response = inFile.readLine();
					inFile.close();
				} catch (IOException e) {
					// Error finding, reading or closing the file.
					//publishProgress(R.string.file?_error);
					//return null;
				}
			}

			if(response != null){
				// Convert the raw string response into an array of JSON objects
				JSONArray jsonData = null;
				try {
					jsonData = new JSONArray(response);
				}catch (JSONException e) {
					// Bad stuff happened. Probably not something I did.
					publishProgress(R.string.json_error);
					return null;
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
						publishProgress(R.string.json_error);
						// keep going, maybe other items won't throw an exception
					}
					result.add(item);
				}
			}

			return result;
		}

		protected void onProgressUpdate(Integer... errorID){
			Toast.makeText(Home.this, getString(errorID[0]), Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(ArrayList<HashMap<String, String>> items) {
			// Call onRefreshComplete when the list has been refreshed.
			pullToRefreshView.onRefreshComplete();
			super.onPostExecute(items);

			if(items == null || items.size() == 0) {
				return;
			}
			// Clear all entries in the view
			itemsListView.removeAllViews();

			for(HashMap<String, String> item : items) {
				String category = item.get("category");
				LinearLayout categoryLayout = (LinearLayout) itemsListView.findViewWithTag(category);
				// if not found, create a new layout for the category
				if (categoryLayout == null) {
					categoryLayout = (LinearLayout) inflater.inflate(R.layout.category_group, null);
					categoryLayout.setTag(category);

					// inflate an item view,
					LinearLayout categoryHeader = (LinearLayout) inflater.inflate(R.layout.list_item, null);
					// remove the checkbox 
					categoryHeader.removeViewAt(0);
					// remove the button
					categoryHeader.removeViewAt(1);
					// set the category text
					// the textview is the only element in the layout, thus at index 0
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
			Intent intent = new Intent(Home.this, UpdateList.class);
			intent.putExtra(Home.ITEM_MESSAGE, (String) null);
			startActivityForResult(intent, ADD_ITEM_REQUEST);
		}
	}

	private class EditItemListener implements OnClickListener{
		public void onClick(View v) {
			LinearLayout itemLayout = (LinearLayout) v.getParent();
			String itemID = (String) itemLayout.getTag();
			String itemName = (String) ((TextView)itemLayout.getChildAt(1)).getText();
			String itemCategory = (String) ((LinearLayout)itemLayout.getParent()).getTag();
			String[] item = {itemID, itemName, itemCategory};

			Intent intent = new Intent(Home.this, UpdateList.class);
			intent.putExtra(Home.ITEM_MESSAGE, item);
			startActivityForResult(intent, UPDATE_ITEM_REQUEST);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == ADD_ITEM_REQUEST) {
			if (resultCode == UpdateList.ACTION_CHANGE) {
				new GetDataTask().execute(false);
			}
		} else if(resultCode == UPDATE_ITEM_REQUEST){
			if (resultCode == UpdateList.ACTION_DELETE){
				String[] item = data.getExtras().getStringArray(Home.ITEM_MESSAGE);
				View view = itemsListView.findViewWithTag(item[2]).findViewWithTag(item[0]);
				new DeleteItemTask().execute(view);
			} else if (resultCode == UpdateList.ACTION_CHANGE) {
				new GetDataTask().execute(false);
			}
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
		}
	}

	private class DeleteItemListener implements OnClickListener{
		public void onClick(View v) {
			new DeleteItemTask().execute((View) v.getParent());
		}
	}

	private class DeleteItemTask extends AsyncTask<View, Integer, Boolean>{

		View item;

		@Override
		protected Boolean doInBackground(View... view) {
			item = view[0];
			NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
			if (networkInfo != null && networkInfo.isConnected()) {
				// Fetch updates from server if asked to do so and if network is available
				try {
					HttpURLConnection conn = (HttpURLConnection) new URL(URL+"/" + item.getTag() + ".json").openConnection();
					conn.setConnectTimeout(5 * 1000);
					conn.setRequestMethod("DELETE");
					conn.setRequestProperty("X-CZ-Authorization", "quqSxtRqyBowMcz46qKr");
					conn.connect();
					if(conn.getResponseCode() == HTTP_OK){
						return true;
					} else {
						publishProgress(R.string.delete_error);
					}
				} catch (IOException ioe) {
					publishProgress(R.string.network_error);
				}
			} else {
				publishProgress(R.string.network_error);
			}
			return false;
		}

		protected void onProgressUpdate(Integer... errorID){
			Toast.makeText(Home.this, getString(errorID[0]), Toast.LENGTH_SHORT).show();
		}

		protected void onPostExecute(Boolean result){
			if(result){
				LinearLayout category = (LinearLayout)item.getParent();
				category.removeView(item);
				// if the category layout now only contains it's title, delete it
				if(category.getChildCount() == 1){
					((LinearLayout)category.getParent()).removeView(category);
				}
			}
		}

	}
}

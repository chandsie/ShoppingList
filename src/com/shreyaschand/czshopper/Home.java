package com.shreyaschand.czshopper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

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

	// Constant fields for 
	protected static final String FILENAME = "items.json";
	protected static final String URL = "https://czshopper.herokuapp.com/items";

	// Constants for message passing between activities
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

		// Save a reference to the layout where the category layouts will be inserted
		itemsListView = (LinearLayout) findViewById(R.id.list);

		// Pre-fetch and save the inflater; used to create layout for new list elements
		inflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		// Pre-fetch and save the connection manager; used to check connectivity when refreshing
		connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		// Set up the "pull-to-refresh" functionality
		pullToRefreshView = (PullToRefreshScrollView) findViewById(R.id.list_pull__scroller);
		pullToRefreshView.setOnRefreshListener(new OnRefreshListener<ScrollView>() {
			public void onRefresh(PullToRefreshBase<ScrollView> refreshView) {
				new GetDataTask().execute(true); // Get data from the network
			}
		});

		new GetDataTask().execute(false); // Get data, not from the network, but from the local file
	}

	private class GetDataTask extends AsyncTask<Boolean, Integer, JSONArray> {

		/**
		 * Get data for the list, either from the network or local file as specified by {@code networkRefresh}
		 * 
		 * @param	networkRefresh	The first element specifies whether or not an attempt
		 * should be made to retrieve data from the network. If it is {@code true}, and an
		 * active connection is available data will be fetched from the online server, otherwise
		 * the local file is used.
		 */
		protected JSONArray doInBackground(Boolean... networkRefresh) {
			NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
			String response = null;

			if (networkRefresh[0] && networkInfo != null && networkInfo.isConnected()) {
				// Fetch updates from server if asked to do so and if network is available
				try {
					HttpsURLConnection conn = (HttpsURLConnection) new URL(URL+".json").openConnection();
					conn.setConnectTimeout(5 * 1000);
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Accept", "application/json");
					conn.setRequestProperty("X-CZ-Authorization", getString(R.string.authToken));
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
				// Use saved values from local file
				try {
					BufferedReader inFile = new BufferedReader(new InputStreamReader(openFileInput(FILENAME)));
					response = inFile.readLine();
					inFile.close();
				} catch (IOException e) {
					// Error finding, reading or closing the file.
					publishProgress(R.string.file_error);
					return null;
				}
			}

			//			ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
			JSONArray data = null;

			if(response != null){
				// Convert the raw string response into an array of JSON objects
				try {
					data = new JSONArray(response);
				}catch (JSONException e) {
					// Bad stuff happened. Probably not something I did.
					publishProgress(R.string.json_error);
					return null;
				}
			}

			return data;
		}

		/**
		 * Display any error messages propagated while executing doInBackground
		 */
		protected void onProgressUpdate(Integer... errorID){
			Toast.makeText(Home.this, getString(errorID[0]), Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(JSONArray items) {
			super.onPostExecute(items);

			// Clear all entries in the view
			itemsListView.removeAllViews();

			if(items == null || items.length() == 0) {
				// There was an error, or there no items in the list.
				// In either case, nothing needs to be done.
				pullToRefreshView.onRefreshComplete();
				return;
			}

			for(int i = 0; i != items.length(); i++) {
				try {
					JSONObject item = items.getJSONObject(i);

					LinearLayout categoryLayout = getOrCreateCategory(item.getString("category"));

					String id = item.getString("id");
					LinearLayout itemLayout =  (LinearLayout) inflater.inflate(R.layout.list_item, null);
					itemLayout.setTag(id);

					itemLayout.getChildAt(0).setOnClickListener(new CheckItemListener());

					TextView tView = ((TextView)itemLayout.getChildAt(1));
					tView.setText(item.getString("name"));
					tView.setOnClickListener(new EditItemListener());

					itemLayout.getChildAt(2).setOnClickListener(new DeleteItemListener());

					((LinearLayout) categoryLayout).addView(itemLayout);
				} catch (JSONException e){
					Toast.makeText(Home.this, getString(R.string.json_error), Toast.LENGTH_SHORT).show();
					// keep going, maybe other items won't throw an exception
				}
			}

			pullToRefreshView.onRefreshComplete();

		}

		private LinearLayout getOrCreateCategory(String category) {
			LinearLayout categoryLayout = (LinearLayout) itemsListView.findViewWithTag(category);
			// If there's already a layout for the category use it, otherwise create a new one
			if (categoryLayout == null) {
				categoryLayout = (LinearLayout) inflater.inflate(R.layout.category_group, null);
				categoryLayout.setTag(category);

				// Inflate an item view and remove all but the textview
				LinearLayout categoryHeader = (LinearLayout) inflater.inflate(R.layout.list_item, null);
				categoryHeader.removeViewAt(0);
				categoryHeader.removeViewAt(1);
				// The textview is the only element in the layout, thus at index 0
				((TextView)categoryHeader.getChildAt(0)).setText(category);

				categoryLayout.addView(categoryHeader);
				itemsListView.addView(categoryLayout);
			}
			return categoryLayout;
		}
	}

	private class AddItemListener implements OnClickListener {
		public void onClick(View v) {
			Intent intent = new Intent(Home.this, UpdateList.class);
			// Null item signals a request to add an item rather than updating an existing one
			intent.putExtra(Home.ITEM_MESSAGE, (String[]) null);
			startActivityForResult(intent, ADD_ITEM_REQUEST);
		}
	}

	private class EditItemListener implements OnClickListener{
		public void onClick(View v) {
			// Create the item descriptor string array
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
		} else if(requestCode == UPDATE_ITEM_REQUEST){
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
					HttpsURLConnection conn = (HttpsURLConnection) new URL(URL+"/" + item.getTag() + ".json").openConnection();
					conn.setConnectTimeout(5 * 1000);
					conn.setRequestMethod("DELETE");
					conn.setRequestProperty("X-CZ-Authorization", getString(R.string.authToken));
					conn.connect();
					if(conn.getResponseCode() == HTTP_OK){
						// Get the saved JSON and delete the item
						try {
							JSONArray list = null;
							try {
								BufferedReader inFile = new BufferedReader(new InputStreamReader(openFileInput(Home.FILENAME)));
								list = new JSONArray(inFile.readLine());
								inFile.close();
							} catch (FileNotFoundException e) {
								publishProgress(R.string.file_error);
							} catch (JSONException e) {
								publishProgress(R.string.json_error);
							}

							for(int i = 0; i < list.length(); i++){
								// Search through the list and find the item
								if (list.getJSONObject(i).getString("id").equals(item.getTag())) {
									// Once found, replace it with the new version
									list.put(i, null);
									break;
								}
							}

							FileOutputStream outFile = openFileOutput(Home.FILENAME, Context.MODE_PRIVATE);
							outFile.write(list.toString().getBytes());
							outFile.close();
						} catch (JSONException e) {
							// Despite the error, the deletion process was a success and we can still return true
							// This error only means that the offline cache is out of sync with the server. This
							// will be fixed at the next refresh. The user doesn't need to know about this error.
						}
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

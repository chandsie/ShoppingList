package com.shreyaschand.czshopper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class UpdateList extends Activity {

	// Constants for message passing between activities
	public static final String DELETE_MESSAGE = "com.shreyaschand.czshopper.DELETE_ITEM_MESSAGE";
	public static final int ACTION_DELETE = 43;
	public static final int ACTION_CHANGE = 44;
	
	private ConnectivityManager connManager;

	private String[] item;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up the layout, remove the titlebar, set the width to fill the parent
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_update_list);

		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		getWindow().setAttributes((LayoutParams) params);

		// Pre-fetch and save the connection manager; used to check connectivity when refreshing
		connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		
		Button deleteButton = (Button) findViewById(R.id.delete_button);
		Button addButton = (Button) findViewById(R.id.add_button);
		TextView title = (TextView) findViewById(R.id.dialog_title);

		Intent intent = getIntent();
		item = intent.getExtras().getStringArray(Home.ITEM_MESSAGE);

		if(item == null){
			// If request was to add an item, remove the delete button and set the appropriate title
			((LinearLayout)deleteButton.getParent()).removeView(deleteButton);
			title.setText(getString(R.string.add_item_title));
			// Initialize the item descriptor array appropriately
			item = new String[3];
			item[0] = null;
		} else {
			// If request was to update a current item, set up the delete button...
			deleteButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// return extra message to delete this item
					Intent intent = new Intent();
					intent.putExtra(Home.ITEM_MESSAGE, item);
					setResult(ACTION_DELETE, intent);
					finish();
				}
			});
			// ...initialize the texboxes...
			((EditText)findViewById(R.id.category_textbox)).setText(item[2]);
			((EditText)findViewById(R.id.name_textbox)).setText(item[1]);
			// ...and set the appropriate button text and title
			addButton.setText(getString(R.string.update));
			title.setText(getString(R.string.update_item_title));
		}
		
		addButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Extract data from the textboxes and fire the AsyncTask to process it
				item[1] = ((EditText)findViewById(R.id.name_textbox)).getText().toString();
				item[2] = ((EditText)findViewById(R.id.category_textbox)).getText().toString();
				new AddItemTask().execute();
			}
		});
		
		findViewById(R.id.cancel_button).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

	}

	private class AddItemTask extends AsyncTask<Void, Integer, Boolean> {

		/**
		 * Add a new, or update an existing item on the list
		 */
		protected Boolean doInBackground(Void... params) {
			NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
			
			// Fetch updates from server if asked to do so and if network is available
			if (networkInfo != null && networkInfo.isConnected()) {
				try {
					// Craft the url based on whether adding or updating
					String url = Home.URL;
					if(item[0] != null) {
						// updating an existing item
						url += "/" + item[0];
					}
					url += ".json";
					
					// Setup the connection to the server and connect to it
					HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
					conn.setConnectTimeout(5 * 1000);
					conn.setDoOutput(true);
					if(item[0] == null){
						// Creating a new item
						conn.setRequestMethod("POST");
					} else {
						// Updating an existing item
						conn.setRequestMethod("PUT");
					}
					conn.setRequestProperty("Accept", "application/json");
					conn.setRequestProperty("Content-type", "application/json");
					conn.setRequestProperty("X-CZ-Authorization", getString(R.string.authToken));
					conn.connect();
					
					OutputStreamWriter outStream = new OutputStreamWriter(conn.getOutputStream());
					
					// Create the JSON to send to the server
					JSONObject innerObj = new JSONObject();
					innerObj.put("name", item[1]);
					innerObj.put("category", item[2]);
					JSONObject outerObj = new JSONObject();
					outerObj.put("item", innerObj);
					
					//Send the JSON and close the stream
					outStream.write(outerObj.toString());
					outStream.close();
					
					// Get the response from the server and convert it to a JSON object
					BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					JSONObject response = new JSONObject(br.readLine());
					br.close();
					
					// Get the saved JSON and update it with the new item
					JSONArray list = null;
					try {
						BufferedReader inFile = new BufferedReader(new InputStreamReader(openFileInput(Home.FILENAME)));
						list = new JSONArray(inFile.readLine());
						inFile.close();
					} catch (FileNotFoundException e) {
						// File doesn't already exist, so just write the current item into the new file
						list = new JSONArray();
					}
					
					
					for(int i = 0; item[0] != null && i < list.length(); i++){
						// Search through the list and find the item
						if (list.getJSONObject(i).getInt("id") == response.getInt("id")) {
							// Once found, replace it with the new version
							list.put(i, response);
							break;
						}
					}
					
					if(item[0] == null || list.length() == 0){
						// If it's a new item or the file doesn't exist, just insert it into the (new) file
						list.put(response);
					}
					
					FileOutputStream outFile = openFileOutput(Home.FILENAME, Context.MODE_PRIVATE);
					outFile.write(list.toString().getBytes());
					outFile.close();
					
				} catch (IOException ioe) {
					publishProgress(R.string.network_error);
					return false;
				} catch (JSONException e) {
					publishProgress(R.string.json_error);
					return false;
				}
				
			} else {
				publishProgress(R.string.network_error);
				return false;
			}
			return true;
		}

		/**
		 * Display any error messages propagated while executing doInBackground
		 */
		protected void onProgressUpdate(Integer... errorID){
			Toast.makeText(UpdateList.this, getString(errorID[0]), Toast.LENGTH_SHORT).show();
		}

		/**
		 * If the task was successful, return to the list and tell it to update itself with the new contents
		 */
		protected void onPostExecute(Boolean result){
			if(result) {
				Intent intent = new Intent();
				setResult(ACTION_CHANGE, intent);
				finish();
			}
		}
	}

}

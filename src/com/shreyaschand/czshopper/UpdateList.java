package com.shreyaschand.czshopper;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

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

	public static final String DELETE_MESSAGE = "com.shreyaschand.czshopper.DELETE_ITEM_MESSAGE";
	public static final int ACTION_DELETE = 43;
	public static final int ACTION_CHANGE = 44;
	private ConnectivityManager connManager;

	private String[] item;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_update_list);

		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		getWindow().setAttributes((LayoutParams) params);

		connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		
		Button deleteButton = (Button) findViewById(R.id.delete_button);
		Button addButton = (Button) findViewById(R.id.add_button);
		TextView title = (TextView) findViewById(R.id.dialog_title);

		Intent intent = getIntent();
		item = intent.getExtras().getStringArray(Home.ITEM_MESSAGE);

		if(item == null){
			// adding item
			((LinearLayout)deleteButton.getParent()).removeView(deleteButton);
			title.setText("Add New Item");
			item = new String[3];
			item[0] = null;
		} else {
			deleteButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// return extra message to delete this item
					Intent intent = new Intent();
					intent.putExtra(Home.ITEM_MESSAGE, item);
					setResult(ACTION_DELETE, intent);
					finish();
				}
			});
			((EditText)findViewById(R.id.category_textbox)).setText(item[2]);
			((EditText)findViewById(R.id.name_textbox)).setText(item[1]);
			addButton.setText(getString(R.string.update));
			title.setText("Update Item");
		}
		
		addButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
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

		@Override
		protected Boolean doInBackground(Void... params) {
			NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
			
			if (networkInfo != null && networkInfo.isConnected()) {
				// Fetch updates from server if asked to do so and if network is available
				try {
					String url = Home.URL;
					if(item[0] != null) {
						url += "/" + item[0];
					}
					url += ".json";
					
					HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
					conn.setConnectTimeout(5 * 1000);
					conn.setDoOutput(true);
					if(item[0] == null){
						conn.setRequestMethod("POST");
					} else {
						conn.setRequestMethod("PUT");
					}
					conn.setRequestProperty("Accept", "application/json");
					conn.setRequestProperty("Content-type", "application/json");
					conn.setRequestProperty("X-CZ-Authorization", "quqSxtRqyBowMcz46qKr");
					conn.connect();
					
					OutputStreamWriter outStream = new OutputStreamWriter(conn.getOutputStream());
					
					JSONObject innerObj = new JSONObject();
					innerObj.put("name", item[1]);
					innerObj.put("category", item[2]);
					JSONObject outerObj = new JSONObject();
					outerObj.put("item", innerObj);
					
					outStream.write(outerObj.toString());
					outStream.close();
					
					
					BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					JSONObject response = new JSONObject(br.readLine());
					br.close();
					
					BufferedReader inFile = new BufferedReader(new InputStreamReader(openFileInput(Home.FILENAME)));
					JSONArray list = new JSONArray(inFile.readLine());
					inFile.close();

					list.put(response);
					
					// Save the JSON for future offline access
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

		protected void onProgressUpdate(Integer... errorID){
			Toast.makeText(UpdateList.this, getString(errorID[0]), Toast.LENGTH_SHORT).show();
		}

		protected void onPostExecute(Boolean result){
			if(result) {
				Intent intent = new Intent();
				setResult(ACTION_CHANGE, intent);
				finish();
			}
		}
	}

}

package com.shreyaschand.czshopper;

import android.app.Activity;
import android.content.Intent;
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
	public static final int ACTION_ADD = 44;
	public static final int ACTION_UPDATE = 45;

	private String[] item;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_update_list);

		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		getWindow().setAttributes((LayoutParams) params);

		Button deleteButton = (Button) findViewById(R.id.delete_button);
		Button addButton = (Button) findViewById(R.id.add_button);
		TextView title = (TextView) findViewById(R.id.dialog_title);

		Intent intent = getIntent();
		item = intent.getExtras().getStringArray(Home.ITEM_MESSAGE);

		if(item == null){
			// adding item
			((LinearLayout)deleteButton.getParent()).removeView(deleteButton);
			title.setText("Add New Item");
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

	private class AddItemTask extends AsyncTask<String, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			// TODO Auto-generated method stub
			return null;
		}

		protected void onProgressUpdate(Integer... errorID){
			Toast.makeText(UpdateList.this, getString(errorID[0]), Toast.LENGTH_SHORT).show();
		}

		protected void onPostExecute(Boolean result){

		}
	}

}

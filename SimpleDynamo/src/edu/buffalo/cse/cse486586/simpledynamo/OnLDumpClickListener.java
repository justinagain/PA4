package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;


public class OnLDumpClickListener implements OnClickListener {


	private static int ldumpClicks = 0;
	private static final String TAG = OnLDumpClickListener.class.getName();
	private static final int TEST_CNT = 50;
	public static final String KEY_FIELD = "key";
	public static final String VALUE_FIELD = "value";

	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;

	public OnLDumpClickListener(TextView _tv, ContentResolver _cr) {
		mTextView = _tv;
		mContentResolver = _cr;
		mUri = Util.getProviderUri();
	}
		
	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}
	
	@Override
	public void onClick(View arg0) {
		new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	private class Task extends AsyncTask<Void, String, Void> {

		private boolean firstPass = true;
		
		@Override
		protected Void doInBackground(Void... params) {
			ldumpClicks++;
			publishProgress("New LDump Request - Click: " + ldumpClicks + "\n");
	    	Uri selectAllUri = Util.getProviderUri();
	    	Log.v(TAG, "About to query from LDump");
	    	Cursor resultCursor = mContentResolver.query(selectAllUri, null, SimpleDynamoProvider.ALL_SELECTION_LOCAL, null, "");
			int keyIndex = resultCursor.getColumnIndex(OnLDumpClickListener.KEY_FIELD);
			int valueIndex = resultCursor.getColumnIndex(OnLDumpClickListener.VALUE_FIELD);
			Log.v(TAG, "About LDump the results");
			for (boolean hasItem = resultCursor.moveToFirst(); hasItem; hasItem = resultCursor.moveToNext()) {
				String key = resultCursor.getString(keyIndex);
				String value = resultCursor.getString(valueIndex);
				Log.v(TAG, "Key and value are: " + key + " : " + value);
				publishProgress(key + ":" + value + "\n");
			}
			return null;
		}
		
		protected void onProgressUpdate(String...strings) {
			if(firstPass){
				mTextView.setText("");
				firstPass = false;
			}
			mTextView.append(strings[0]);
			return;
		}

	}

}

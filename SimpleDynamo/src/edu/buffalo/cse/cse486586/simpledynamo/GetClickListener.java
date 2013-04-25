package edu.buffalo.cse.cse486586.simpledynamo;
import java.security.NoSuchAlgorithmException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;


public class GetClickListener implements OnClickListener {

	private static int getClicks = 0;
	private static final String TAG = GetClickListener.class.getName();
	private static final int TEST_CNT = 20;
	public static final String KEY_FIELD = "key";
	public static final String VALUE_FIELD = "value";

	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;

	public GetClickListener(TextView _tv, ContentResolver _cr) {
		mTextView = _tv;
		mContentResolver = _cr;
		mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
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
			getClicks++;
			publishProgress("New Get Request - Click: " + getClicks + "\n");
			for (int i = 0; i < TEST_CNT; i++) {
				Cursor resultCursor = mContentResolver.query(Util.getProviderUri(), null, i+"", null, "");
				int keyIndex = resultCursor.getColumnIndex(OnLDumpClickListener.KEY_FIELD);
				int valueIndex = resultCursor.getColumnIndex(OnLDumpClickListener.VALUE_FIELD);
				for (boolean hasItem = resultCursor.moveToFirst(); hasItem; hasItem = resultCursor.moveToNext()) {
					String key = resultCursor.getString(keyIndex);
					String value = resultCursor.getString(valueIndex);
					Log.v(TAG, "Key and value are: " + key + " : " + value);
					publishProgress(key + ":" + value + "\n");
				}

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

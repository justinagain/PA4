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


public class PutClickListener implements OnClickListener {

	// For 1.1.e.ii
	public static String[] VALUES = new String[]{"Put1", "Put2", "Put3"};
	
	private static int putClicks = 0;
	private static final String TAG = PutClickListener.class.getName();
	private static final int TEST_CNT = 20;
	public static final String KEY_FIELD = "key";
	public static final String VALUE_FIELD = "value";

	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private String buttonName;
	private final ContentValues[] mContentValues;

	public PutClickListener(TextView _tv, ContentResolver _cr, String newButtonName) {
		mTextView = _tv;
		mContentResolver = _cr;
		this.buttonName = newButtonName;
		mContentValues = initTestValues();
	}
		
	@Override
	public void onClick(View arg0) {
		//On click reset the count to zero: 1.1.d
		putClicks = 0;
		new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	
	private ContentValues[] initTestValues() {
		// From 1.1.e.ii
		ContentValues[] cv = new ContentValues[TEST_CNT];
		for (int i = 0; i < TEST_CNT; i++) {
			cv[i] = new ContentValues();
			cv[i].put(KEY_FIELD, i + "");
			cv[i].put(VALUE_FIELD, buttonName + i);
		}

		return cv;
	}
	
	private class Task extends AsyncTask<Void, String, Void> {

		private boolean firstPass = true;
		
		@Override
		protected Void doInBackground(Void... params) {
			putClicks++;
			publishProgress("New " + buttonName + " Request - Click: " + putClicks + "\n");
			
			if (testInsert()) {
				publishProgress("Insert success\n");
			} else {
				publishProgress("Insert fail\n");
				return null;
			}

//			if (testQuery()) {
//				publishProgress("Query success\n");
//			} else {
//				publishProgress("Query fail\n");
//			}
			
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
		
		private boolean testInsert() {
			try {
				for (int i = 0; i < TEST_CNT; i++) {
					mContentResolver.insert(Util.getProviderUri(), mContentValues[i]);
					// 1.1.c
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				Log.e(TAG, e.toString());
				return false;
			}

			return true;
		}

		private boolean testQuery() {
			try {
				for (int i = 0; i < TEST_CNT; i++) {
					String key = (String) mContentValues[i].get(KEY_FIELD);
					String val = (String) mContentValues[i].get(VALUE_FIELD);

					Log.v(TAG, "About to query for: " + key);
					Cursor resultCursor = mContentResolver.query(Util.getProviderUri(), null,
							key, null, null);
					if (resultCursor == null) {
						Log.e(TAG, "Result null");
						throw new Exception();
					}

					int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
					int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
					if (keyIndex == -1 || valueIndex == -1) {
						Log.e(TAG, "Wrong columns");
						resultCursor.close();
						throw new Exception();
					}

					resultCursor.moveToFirst();

					if (!(resultCursor.isFirst() && resultCursor.isLast())) {
						Log.e(TAG, "Wrong number of rows");
						resultCursor.close();
						throw new Exception();
					}

					String returnKey = resultCursor.getString(keyIndex);
					String returnValue = resultCursor.getString(valueIndex);
					if (!(returnKey.equals(key) && returnValue.equals(val))) {
						Log.e(TAG, "(key, value) pairs don't match\n");
						resultCursor.close();
						throw new Exception();
					}

					resultCursor.close();
				}
			} catch (Exception e) {
				return false;
			}

			return true;
		}


	}

}

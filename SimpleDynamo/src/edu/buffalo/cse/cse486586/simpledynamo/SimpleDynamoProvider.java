package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {
	
	public static final String TAG = SimpleDynamoProvider.class.getName();
	private String currentNode;
	
    @Override
    public Uri insert(Uri simpleDhtUri, ContentValues contentValues) {
		Log.v(TAG, "About to insert into content provider with URI: " + simpleDhtUri.toString());
        writeToInternalStorage(simpleDhtUri, contentValues);
        getContext().getContentResolver().notifyChange(simpleDhtUri, null);
		return simpleDhtUri;
    }

	private boolean writeToInternalStorage(Uri uri, ContentValues contentValues){
		boolean success = false;
		FileOutputStream fos;
		try {
			String keyValue = contentValues.get(PutClickListener.KEY_FIELD).toString();
			String contentValue = contentValues.get(PutClickListener.VALUE_FIELD).toString();
			String fileName = uri.toString().replace("content://", "");
			fileName = fileName + "_" + keyValue;
			Log.v(TAG, "filename is: " + fileName);
			fos = this.getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
			fos.write(contentValue.getBytes());				
			fos.close();
			success = true;
			Log.v(TAG, "Wrote ContentValues successfully.");								
		} catch (FileNotFoundException e) {
			Log.v(TAG, "File not found when writing ContentValues");
			e.printStackTrace();
		} catch (IOException e) {
			Log.v(TAG, "Some IO Exception when writing ContentValues");
			e.printStackTrace();
		}
		return success;
	}

    @Override
    public Cursor query(Uri providedUri, String[] arg1, String keyValue, String[] arg3,
			String arg4) {
    	MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key", "value"});
		getLocalKeyValue(matrixCursor, keyValue);			
		return matrixCursor;    
	}
    
	private void getLocalKeyValue(MatrixCursor matrixCursor, String keyValue) {
		File[] files = this.getContext().getFilesDir().listFiles();
		String fileName = Util.getProviderUri().toString();
		fileName = fileName.replace("content://", "");
		for (File file : files) {
			if(file.getName().startsWith(fileName) && file.getName().endsWith("_" + keyValue)){
				Log.v(TAG, "A match is found: " + keyValue);
				try {
					FileInputStream fis = this.getContext().openFileInput(file.getName());
					int characterIntValue;
					String value = "";
					while ((characterIntValue= fis.read()) != -1) {
						value = value + (char)characterIntValue;
					}		
					String[] cursorRow = new String[]{keyValue, value};
					matrixCursor.addRow(cursorRow);
				} catch (FileNotFoundException e) {
					Log.v(TAG, "File not found when reading ContentValues: " + file.getName());
					e.printStackTrace();
				} catch (IOException e) {
					Log.v(TAG, "Some IO Exception when reading ContentValues");
					e.printStackTrace();
				}			
			}
		}
		Log.v(TAG, "Matrix size is: " + matrixCursor.getCount());
	}


	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean onCreate() {
    	Log.v(TAG, "Cleaning provider....");
    	cleanProvider();
    	setNode();
    	return false;
	}
	
	private void setNode() {
		Application application = (Application)getContext();
		TelephonyManager tel = (TelephonyManager)application.getSystemService(Context.TELEPHONY_SERVICE);
		currentNode = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		createServer();
	}

	
	private void cleanProvider() {
		File[] files = this.getContext().getFilesDir().listFiles();
		String fileName = Util.getProviderUri().toString();            
		fileName = fileName.replace("content://", "");
		for (File file : files) {
			Log.v(TAG, "Base file is: " + fileName + " and compare name is: " + file.getName());
			if(file.getName().startsWith(fileName)){
				Log.v(TAG, "We have a match and must delete - it is old content.");
				file.delete();				
			}
		}		
	}

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    
	private void createServer() {
		try{
			ServerSocket serverSocket = new ServerSocket(10000);
			new ServerTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		}
		catch(IOException e){
			Log.v(TAG, "Exception creating ServerSocket");
		}
	}

}

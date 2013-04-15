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
	public static final String PREDECESSOR_NODE = "pred";
	public static final String CURRENT_NODE = "curr";
	public static final String SUCCESSOR_NODE = "succ";
	private String currentNode;
	private String predecessorNode;
	private String successorNode;
	private boolean waitForResponse;
	private String[] singleResponseCursorRow;

	public static final String ALL_SELECTION_LOCAL = "all_local_select";

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
			Log.v(TAG, "About to insert into a specific file");
			String keyValue = contentValues.get(PutClickListener.KEY_FIELD).toString();
			String contentValue = contentValues.get(PutClickListener.VALUE_FIELD).toString();
			// Satisfies 2.1.a
			String type = findPartition(keyValue);
			Log.v(TAG, "Determined should be sent to: " + type);

			if(type.equals(PREDECESSOR_NODE)){
				Log.v(TAG, "I have a message that must be sent to the predecessor node " + predecessorNode);
				SimpleDynamoMessage message = SimpleDynamoMessage.getInsertMessage(predecessorNode, keyValue, contentValue);
		    	new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);				
			}
			else if(type.equals(CURRENT_NODE)){
				Log.v(TAG, "I have a message that belongs to the current node " + currentNode);				
				String fileName = uri.toString().replace("content://", "");
				fileName = fileName + "_" + keyValue;
				Log.v(TAG, "filename is: " + fileName);
				fos = this.getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
				fos.write(contentValue.getBytes());				
				fos.close();
				success = true;
				Log.v(TAG, "Wrote ContentValues successfully.");								
			}
			else if(type.equals(SUCCESSOR_NODE)){
				Log.v(TAG, "I have a message that must be sent to the successor node " + successorNode);				
				SimpleDynamoMessage message = SimpleDynamoMessage.getInsertMessage(successorNode, keyValue, contentValue);
		    	new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);				
			}
			else{
				Log.v(TAG, "Bad mojo - no node found!");
			}
		} catch (FileNotFoundException e) {
			Log.v(TAG, "File not found when writing ContentValues");
			e.printStackTrace();
		} catch (IOException e) {
			Log.v(TAG, "Some IO Exception when writing ContentValues");
			e.printStackTrace();
		}
		return success;
	}
	
	private String findPartition(String keyToInsert){
		Log.v(TAG, "Evaluation where key should be inserted for key: " + keyToInsert);
		String type = "";
		String keyHash;
		try {
			keyHash = genHash(keyToInsert);
			String predecessorHash = genHash(predecessorNode);
			String currentNodeHash = genHash(currentNode);
			String successorHash = genHash(successorNode);
			
			// THERE ARE THREE OR MORE NODES
			if(predecessorHash.compareTo(currentNodeHash) < 0 &&
			   currentNodeHash.compareTo(successorHash) < 0){
				
				if(keyHash.compareTo(predecessorHash) > 0 &&
					keyHash.compareTo(currentNodeHash) < 0){
					type = PREDECESSOR_NODE;
				}
				else if(keyHash.compareTo(currentNodeHash) > 0 &&
					keyHash.compareTo(successorHash) < 0){
					type = CURRENT_NODE;
				}
				else{
					type = SUCCESSOR_NODE;
				}
			}
			else if(successorHash.compareTo(predecessorHash) < 0 &&
					   predecessorHash.compareTo(currentNodeHash) < 0){
						
				if(keyHash.compareTo(predecessorHash) > 0 &&
					keyHash.compareTo(currentNodeHash) < 0){
					type = PREDECESSOR_NODE;
				}
				else if(keyHash.compareTo(successorHash) > 0 &&
					keyHash.compareTo(predecessorHash) < 0){
					type = SUCCESSOR_NODE;
				}
				else{
					type = CURRENT_NODE;
				}
			}
			else if(currentNodeHash.compareTo(successorHash) < 0 &&
					   successorHash.compareTo(predecessorHash) < 0){
						
				if(keyHash.compareTo(currentNodeHash) > 0 &&
					keyHash.compareTo(successorHash) < 0){
					type = CURRENT_NODE;
				}
				else if(keyHash.compareTo(successorHash) > 0 &&
					keyHash.compareTo(predecessorHash) < 0){
					type = SUCCESSOR_NODE;
				}
				else{
					type = PREDECESSOR_NODE;
				}
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return type;
	}

    @Override
    public Cursor query(Uri providedUri, String[] arg1, String keyValue, String[] arg3,
			String arg4) {
		Log.v(TAG, "Entering SimpleDynamoProvider query");
    	MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key", "value"});
		if(keyValue.equals(ALL_SELECTION_LOCAL)){
			getAllLocalKeyValue(matrixCursor);
		}
		else{
			getLocalKeyValue(matrixCursor, keyValue);
			// We need to go elsewhere to find the key
			if(matrixCursor.getCount() == 0){
				String nodeToSendQuery = "";
				String type = findPartition(keyValue);
				if(type.equals(PREDECESSOR_NODE)){
					nodeToSendQuery = predecessorNode;
				}
				else if(type.equals(SUCCESSOR_NODE)){
					nodeToSendQuery = successorNode;					
				}
				new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, SimpleDynamoMessage.getQueryMessage(nodeToSendQuery, currentNode, keyValue));
				waitForResponse = true;
				while(waitForResponse){	
				}
				matrixCursor.addRow(singleResponseCursorRow);
			}
		}
		return matrixCursor;    
	}
    
	private void getAllLocalKeyValue(MatrixCursor matrixCursor) {
		for (int i = 0; i < 20; i++) {
			getLocalKeyValue(matrixCursor, i + "");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
    	// Could be problematic ... 
    	cleanProvider();
    	setNode();
    	setConnections();
    	return false;
	}
	
	private void setConnections() {
		predecessorNode = Util.getPredecessor(currentNode);
		successorNode = Util.getSuccessor(currentNode);
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
	
	public void processInsertRequest(SimpleDynamoMessage sdm) {
		ContentValues cv = new ContentValues();
		cv.put(PutClickListener.KEY_FIELD, sdm.getKey());
		cv.put(PutClickListener.VALUE_FIELD, sdm.getValue());
		insert(Util.getProviderUri(), cv);		
	}

	public void processQueryResponse(SimpleDynamoMessage sdm) {
		singleResponseCursorRow = new String[]{sdm.getKey(), sdm.getValue()};
		waitForResponse = false;
		
	}
		
	public void processQueryRequest(SimpleDynamoMessage sdm) {
		String key = sdm.getKeyForSingle();
		Cursor cursor = query(Util.getProviderUri(), null, key, null, null);
		int keyIndex = cursor.getColumnIndex(PutClickListener.KEY_FIELD);
		int valueIndex = cursor.getColumnIndex(PutClickListener.VALUE_FIELD);
		cursor.moveToFirst();
		String returnKey = cursor.getString(keyIndex);
		String returnValue = cursor.getString(valueIndex);
		SimpleDynamoMessage dmResponse = SimpleDynamoMessage.getQueryResponsMessage(sdm.getAvdTwo(), key, returnValue);
		new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dmResponse);
	}

	
}

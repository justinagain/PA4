package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;

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
	
	// https://github.com/justinagain/PA4.git
	
	public static final String TAG = SimpleDynamoProvider.class.getName();
	public static final String PREDECESSOR_NODE = "pred";
	public static final String CURRENT_NODE = "curr";
	public static final String SUCCESSOR_NODE = "succ";
	private String currentNode;
	private String predecessorNode;
	private String successorNode;
	private volatile String failedNode;
	private volatile boolean waitForResponse;
	private volatile String[] singleResponseCursorRow;
	private volatile boolean quorum;
	private String[] ports = new String[]{Constants.AVD0_PORT, Constants.AVD1_PORT, Constants.AVD2_PORT};
	private HashMap<String, String> keyVersions = new HashMap<String, String>();
	private int globalId = 0;

	
	public static final String ALL_SELECTION_LOCAL = "all_local_select";

    @Override
    public Uri insert(Uri simpleDhtUri, ContentValues contentValues) {
		String keyValue = contentValues.get(PutClickListener.KEY_FIELD).toString();
		String contentValue = contentValues.get(PutClickListener.VALUE_FIELD).toString();
		determineQuorum();
		String port = "";
		if(failedNode.length() == 0){
			port = findPartitionForThreeNodes(keyValue);			
		}
		else{
			port = findPartitionForTwoNodes(keyValue, failedNode);						
		}
		Log.v(TAG, "Partition is: " + port);		
    	if(port.equals(currentNode)){
    		globalId++;
    		keyVersions.put(keyValue, globalId + "");
	        writeToInternalStorage(Util.getProviderUri(), contentValues);
	        getContext().getContentResolver().notifyChange(Util.getProviderUri(), null);   
	        sendToSuccessors(keyValue, contentValue, failedNode);
    	}
    	//send to Coordinator if you get an insert and you are not the coordinator
    	else{
			SimpleDynamoMessage message = SimpleDynamoMessage.getInsertMessage(port, keyValue, contentValue);
	    	new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);				    		
    	}
		return simpleDhtUri;
    }

	private void determineQuorum() {
		failedNode ="";
		quorum = false;
		for (int i = 0; i < ports.length; i++) {
			if(! ports[i].equals(currentNode)){
				SimpleDynamoMessage message = SimpleDynamoMessage.getQuorumRequest(ports[i], currentNode);
		    	new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);				    		
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(quorum == true){
					Log.d(TAG, "This node is active: " + ports[i]);
					quorum = false;
				}
				else{
					Log.d(TAG, "This node is inactive: " + ports[i]);
					failedNode = ports[i];
					break;
				}
			}
		}
	}

	private void sendToSuccessors(String keyValue, String contentValue, String failedNode) {
		for (int i = 0; i < ports.length; i++) {
			if(! ports[i].equals(currentNode) || ! ports[i].equals(failedNode)){
				SimpleDynamoMessage message = SimpleDynamoMessage.getInsertReplicaMessage(ports[i], currentNode, keyValue, contentValue, globalId);
		    	new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);
			}
		}
	}

	private boolean writeToInternalStorage(Uri uri, ContentValues contentValues){
		boolean success = false;
		FileOutputStream fos;
		try {
			Log.v(TAG, "About to insert into a specific file");
			String keyValue = contentValues.get(PutClickListener.KEY_FIELD).toString();
			String contentValue = contentValues.get(PutClickListener.VALUE_FIELD).toString();
			// Satisfies 2.1.a
			Log.v(TAG, "I have a message that belongs to the current node " + currentNode);				
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
	
	private String findPartitionForTwoNodes(String keyToInsert, String failedNode){
		Log.v(TAG, "***** I need to find a partition based on this failed node: " + failedNode);								
		String port = findPartitionForThreeNodes(keyToInsert);
		// if the partition is the bad one, we need to send it to the predecessor
		if(port.equals(failedNode)){
			if(port.equals(Constants.AVD0_PORT)){
				port = Constants.AVD1_PORT;
			}
			else if(port.equals(Constants.AVD1_PORT)){
				port = Constants.AVD2_PORT;
			}
			else if(port.equals(Constants.AVD2_PORT)){
				port = Constants.AVD0_PORT;				
			}
		}
		Log.v(TAG, "***** I decided to send to this node: " + port+ " for value " + keyToInsert);								
		return port;
	}

	
	private String findPartitionForThreeNodes(String keyToInsert){
		String port = "";
		String keyHash;
		try {
			keyHash = genHash(keyToInsert);
			String avd2Hash = genHash(Constants.AVD2_PORT);
			String avd1Hash = genHash(Constants.AVD1_PORT);
			String avd0Hash = genHash(Constants.AVD0_PORT);
			
			// THERE ARE THREE OR MORE NODES
			if(keyHash.compareTo(avd2Hash) < 0){				
					// current node
					port = Constants.AVD0_PORT;
			}
			else if(keyHash.compareTo(avd2Hash) > 0 &&
					   keyHash.compareTo(avd1Hash) < 0){		
					port = Constants.AVD2_PORT;
			}
			else if(keyHash.compareTo(avd1Hash) > 0 ||
					   keyHash.compareTo(avd0Hash) < 0){						
					port = Constants.AVD1_PORT;
			}
			else{
				port = Constants.AVD0_PORT;
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return port;
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
			determineQuorum();
			String port = "";
			if(failedNode.length() == 0){
				port = findPartitionForThreeNodes(keyValue);			
			}
			else{
				port = findPartitionForTwoNodes(keyValue, failedNode);						
			}
			if(port.equals(currentNode)){
				getLocalKeyValue(matrixCursor, keyValue);				
			}
			else{
				waitForResponse = true;
				boolean firstPass = true;
				while(waitForResponse){	
					if(firstPass){
						new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, SimpleDynamoMessage.getQueryMessage(port, currentNode, keyValue));						
					}
					firstPass = false;
				}
				matrixCursor.addRow(singleResponseCursorRow);				
			}
		}
		return matrixCursor;    
	}
    
	private void getAllLocalKeyValue(MatrixCursor matrixCursor) {
		for (int i = 0; i < 20; i++) {
			getLocalKeyValue(matrixCursor, i + "");
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
    	requestSync();
    	return false;
	}
	
	private void requestSync() {
		SimpleDynamoMessage message = SimpleDynamoMessage.geRequestSyncMessage(successorNode, currentNode);
    	new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);				    						
		
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
			file.delete();				
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
	
	public void processQueryResponse(SimpleDynamoMessage sdm) {
		singleResponseCursorRow = new String[]{sdm.getKey(), sdm.getValue()};
		waitForResponse = false;		
	}
		
	public void processQueryRequest(SimpleDynamoMessage sdm) {
		String key = sdm.getKeyForSingle();
		
    	MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
		getLocalKeyValue(cursor, key);				


		
		//Cursor cursor = query(Util.getProviderUri(), null, key, null, null);
		int keyIndex = cursor.getColumnIndex(PutClickListener.KEY_FIELD);
		int valueIndex = cursor.getColumnIndex(PutClickListener.VALUE_FIELD);
		cursor.moveToFirst();
		String returnKey = cursor.getString(keyIndex);
		String returnValue = cursor.getString(valueIndex);
		SimpleDynamoMessage dmResponse = SimpleDynamoMessage.getQueryResponsMessage(sdm.getAvdTwo(), key, returnValue);
		new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dmResponse);
	}

	public void processInsertMessage(SimpleDynamoMessage sdm) {
		Log.v(TAG, "In processInserMessage for " + currentNode);
		ContentValues cv = new ContentValues();
		cv.put(PutClickListener.KEY_FIELD, sdm.getKey());
		cv.put(PutClickListener.VALUE_FIELD, sdm.getValue());
		insert(Util.getProviderUri(), cv);
	}

	public void processInsertReplicaMessage(SimpleDynamoMessage sdm) {
		Log.v(TAG, "In processInsertReplicaMessage for " + currentNode);
		ContentValues cv = new ContentValues();
		cv.put(PutClickListener.KEY_FIELD, sdm.getKey());
		cv.put(PutClickListener.VALUE_FIELD, sdm.getValue());
		globalId = Integer.parseInt(sdm.getGlobalId());
		keyVersions.put(sdm.getKey(), globalId + "");
        writeToInternalStorage(Util.getProviderUri(), cv);
        getContext().getContentResolver().notifyChange(Util.getProviderUri(), null);
	}

	public void processQuorumRequestMessage(SimpleDynamoMessage sdm) {
		SimpleDynamoMessage message = SimpleDynamoMessage.getQuorumResponse(sdm.getAvdTwo(), sdm.getAvdOne(), globalId + "");
    	new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);				    		
	}

	public void processQuorumResponseMessage(SimpleDynamoMessage sdm) {
		Log.v(TAG, "Processing a message.  My global ID is stated to be: " + sdm.getGlobalId());
		quorum = true;
	}
	
	public boolean isQuorumLocked(){
		return quorum;
	}

	public void processSycnRequestMessage(SimpleDynamoMessage sdm) {
    	Uri selectAllUri = Util.getProviderUri();
    	Cursor resultCursor = query(selectAllUri, null, SimpleDynamoProvider.ALL_SELECTION_LOCAL, null, "");
		int keyIndex = resultCursor.getColumnIndex(OnLDumpClickListener.KEY_FIELD);
		int valueIndex = resultCursor.getColumnIndex(OnLDumpClickListener.VALUE_FIELD);
		Log.v(TAG, "About LDump the results");
		for (boolean hasItem = resultCursor.moveToFirst(); hasItem; hasItem = resultCursor.moveToNext()) {
			String key = resultCursor.getString(keyIndex);
			String value = resultCursor.getString(valueIndex);

			String port = findPartitionForThreeNodes(key);
			if(port.equals(sdm.getAvdTwo())){
				SimpleDynamoMessage message = SimpleDynamoMessage.getInsertMessage(sdm.getAvdTwo(), key, value);
		    	new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);				    						
			}
			else{
				SimpleDynamoMessage message = SimpleDynamoMessage.getInsertReplicaMessage(sdm.getAvdTwo(), currentNode, key, value, 0);
		    	new SimpleDynamoClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);				    										
			}
		
		}
		
	}

}

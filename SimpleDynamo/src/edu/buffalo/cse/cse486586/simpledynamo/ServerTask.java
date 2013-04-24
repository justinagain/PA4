package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

public class ServerTask extends AsyncTask<ServerSocket, String, Void>{

	private static final String TAG = ServerTask.class.getName();
	private SimpleDynamoProvider sdp;
	
	public ServerTask(SimpleDynamoProvider newSdp){
		sdp = newSdp;
	}
	
	@Override
	protected Void doInBackground(ServerSocket... sockets) {
		Log.v(TAG, "Create a socket");
		String msg = null;
		ServerSocket serverSocket = sockets[0];
		Socket socket;
		
		try{
			while(true){					
				Log.v(TAG, "Socket awaits accept ... ");
				socket = serverSocket.accept();
				Log.v(TAG, "A message is coming in ... ");
				InputStream stream = socket.getInputStream();
				byte[] data = new byte[SimpleDynamoMessage.MSG_SIZE];
				int count = stream.read(data);				
				Log.v(TAG, "Message recieved with bytes: " + count);
				SimpleDynamoMessage sdm = SimpleDynamoMessage.createMessageFromByteArray(data);
				if(sdm.isInsertMessage()) {
					Log.v(TAG, "An insert message has been received.");
					Log.v(TAG, "Recevied from " + sdm.getAvdOne());
					sdp.processInsertMessage(sdm);					
				} else if(sdm.isQueryRequest()){
					Log.v(TAG, "A query request has been received.");
					Log.v(TAG, "Recevied from " + sdm.getAvdOne());
					sdp.processQueryRequest(sdm);					
				} else if(sdm.isQueryResponse()){
					Log.v(TAG, "A query response has been received.");
					Log.v(TAG, "Recevied from " + sdm.getAvdOne());
					sdp.processQueryResponse(sdm);
				} else if(sdm.isInsertReplica()){
					Log.v(TAG, "An insert replica message has been received.");
					Log.v(TAG, "Recevied from " + sdm.getAvdOne());
					sdp.processInsertReplicaMessage(sdm);					
				} else if(sdm.isQuorumRequest()){
					Log.v(TAG, "A quorum request has been received.");
					Log.v(TAG, "Recevied from " + sdm.getAvdTwo());
					sdp.processQuorumRequestMessage(sdm);										
				} else if(sdm.isQuorumResponse()){
					Log.v(TAG, "A quorum response has been received.");
					Log.v(TAG, "Recevied from " + sdm.getAvdTwo());
					sdp.processQuorumResponseMessage(sdm);										
				}
				socket.close();
			}
		}
		catch (IOException e){
			Log.v(TAG, "IOException creating ServerSocket");
		}
		return null;
	}

	
}
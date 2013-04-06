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
				byte[] data = new byte[DynamoMessage.MSG_SIZE];
				int count = stream.read(data);				
				Log.v(TAG, "Message recieved with bytes: " + count);
				DynamoMessage dm = DynamoMessage.createMessageFromByteArray(data);
				// process message!
				socket.close();
			}
		}
		catch (IOException e){
			Log.v(TAG, "IOException creating ServerSocket");
		}
		return null;
	}

	
}
package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

public class SimpleDynamoClientTask extends AsyncTask<SimpleDynamoMessage, Void, Void>{

		private static final String TAG = SimpleDynamoClientTask.class.getName();
		
		protected Void doInBackground(SimpleDynamoMessage... msgs){
			try {
				Log.v(TAG, "In SimpleDynamoClientTask and about to push message to: " + Util.getPortNumber(msgs[0].getAvdOne()));
				Socket writeSocket = new Socket(Constants.IP_ADDRESS, Util.getPortNumber(msgs[0].getAvdOne()));
				writeSocket.getOutputStream().write(msgs[0].getPayload());
				writeSocket.getOutputStream().flush();
				writeSocket.close();
				Log.v(TAG, "Pushed!");
			} catch (UnknownHostException e) {
				e.printStackTrace();
				Log.v(TAG, "Error creating Inet Address");
			} catch (IOException e) {
				Log.v(TAG, "Error creating Socket");
				e.printStackTrace();
			}
			return null;
		}
	
}

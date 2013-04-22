package edu.buffalo.cse.cse486586.simpledynamo;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Util {
	
	public static Uri getProviderUri() {
		String scheme = "content"; 
		String authority = "edu.buffalo.cse.cse486586.simpledynamo.provider";
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}	

	public static int getPortNumber(String avd) {
		int port = 0;
		if(avd.equals(Constants.AVD0_PORT)){
			port = Integer.parseInt(Constants.AVD0_REDIRECT_PORT);
		}
		else if(avd.equals(Constants.AVD1_PORT)){
			port = Integer.parseInt(Constants.AVD1_REDIRECT_PORT);			
		}
		else if(avd.equals(Constants.AVD2_PORT)){
			port = Integer.parseInt(Constants.AVD2_REDIRECT_PORT);						
		}
		return port;
	}
	
	public static String getPredecessor(String currentAvd){
		String predecessor = "DEFAULT";
		if(currentAvd.equals(Constants.AVD0_PORT)){
			predecessor = Constants.AVD1_PORT;			
		} else if(currentAvd.equals(Constants.AVD1_PORT)){
			predecessor = Constants.AVD2_PORT;			
		} else if(currentAvd.equals(Constants.AVD2_PORT)){
			predecessor = Constants.AVD0_PORT;
		}
		return predecessor;
	}

	public static String getSuccessor(String currentAvd){
		String successor = "DEFAULT";
		if(currentAvd.equals(Constants.AVD0_PORT)){
			successor = Constants.AVD2_PORT;			
		} else if(currentAvd.equals(Constants.AVD1_PORT)){
			successor = Constants.AVD0_PORT;			
		} else if(currentAvd.equals(Constants.AVD2_PORT)){
			successor = Constants.AVD1_PORT;
		}
		return successor;		
	}
}

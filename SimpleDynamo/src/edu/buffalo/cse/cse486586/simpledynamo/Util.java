package edu.buffalo.cse.cse486586.simpledynamo;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Util {
	
	public static Uri providerUri;
	

	public static Uri getProviderUri() {
		String scheme = "content"; 
		String authority = "edu.buffalo.cse.cse486586.simpledynamo.provider";
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}
	public static String getPortNumber(Activity activity){
		TelephonyManager tel = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
		String port = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		String avdIdentifier = "unspecified";
		if(port.equals(Constants.AVD0_PORT)){
			avdIdentifier = Constants.AVD0;
		}
		else if(port.equals(Constants.AVD1_PORT)){
			avdIdentifier = Constants.AVD1;			
		}
		else if(port.equals(Constants.AVD2_PORT)){
			avdIdentifier = Constants.AVD2;						
		}
		
		return avdIdentifier;		
	}
	
	public static String[] getRemoteClientPorts(String portString){
		String[] remoteClientPorts = null;
		if(portString.equals(Constants.AVD0_PORT)){
			remoteClientPorts = Constants.AVD0_REMOTE_CLIENTS;
		}
		else if(portString.equals(Constants.AVD1_PORT)){
			remoteClientPorts = Constants.AVD1_REMOTE_CLIENTS;
		}
		else if(portString.equals(Constants.AVD2_PORT)){
			remoteClientPorts = Constants.AVD2_REMOTE_CLIENTS;
		}
		else{
		}
		return remoteClientPorts;
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
			predecessor = Constants.AVD2_PORT;			
		} else if(currentAvd.equals(Constants.AVD1_PORT)){
			predecessor = Constants.AVD0_PORT;			
		} else if(currentAvd.equals(Constants.AVD2_PORT)){
			predecessor = Constants.AVD1_PORT;
		}
		return predecessor;
	}

	public static String getSuccessor(String currentAvd){
		String successor = "DEFAULT";
		if(currentAvd.equals(Constants.AVD0_PORT)){
			successor = Constants.AVD1_PORT;			
		} else if(currentAvd.equals(Constants.AVD1_PORT)){
			successor = Constants.AVD2_PORT;			
		} else if(currentAvd.equals(Constants.AVD2_PORT)){
			successor = Constants.AVD0_PORT;
		}
		return successor;		
	}
	public static boolean isCoordinator(String avd) {
		boolean coordinator = false;
		if(avd.equals(Constants.AVD0_PORT)){
			coordinator = true;
		}
		return coordinator;
	}
}

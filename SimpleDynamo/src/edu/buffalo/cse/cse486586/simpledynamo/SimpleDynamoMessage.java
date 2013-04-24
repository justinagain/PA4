package edu.buffalo.cse.cse486586.simpledynamo;

public class SimpleDynamoMessage {
	

	public static final String INSERT = "i";
	public static final String INSERT_REPLICA = "r";
	private static final String QUORUM_REQUEST = "q";
	private static final String QUORUM_RESPONSE = "u";
	
	
	public static final String GLOBAL_QUERY_RESPONSE = "a";
	public static final String GLOBAL_QUERY = "g";
	public static final String REQUEST_JOIN = "j";
	public static final String NEW_JOIN_RESPONSE = "r";
	public static final String NEW_PREDECESSOR_RESPONSE = "p";
	public static final String NEW_SUCCESSOR_RESPONSE = "s";
	public static final String QUERY = "q";
	public static final String SINGLE_QUERY_REQUEST = "z";
	public static final String SINGLE_QUERY_RESPONSE = "b";
	public static final int AVD_INSERT_PT_ONE = 1;
	public static final int AVD_INSERT_PT_TWO = 5;
	public static final int AVD_INSERT_PT_THREE = 9;
	public static final int KEY_INSERT_PT = 5;
	public static final int VALUE_INSERT_PT = 11;
	public static final int COUNT_INSERT_PT = 17;
	public static final int KEY_FOR_SINGLE_INSERT_PT = 17;
	private static final byte ARRAY_INITIALIZER = "z".getBytes()[0];
	public static final int MSG_SIZE = 142;
	private byte[] payload;
	
	private SimpleDynamoMessage(String type) {
		payload = new byte[142];
		initializeArray();
		payload[0] = type.getBytes()[0];
	}

	public static SimpleDynamoMessage createMessageFromByteArray(byte[] data) {
		return new SimpleDynamoMessage(data);
	}

	private SimpleDynamoMessage(byte[] newPayload) {
		payload = newPayload;
	}

	private void initializeArray() {
		for (int i = 0; i < payload.length; i++) {
			payload[i] = ARRAY_INITIALIZER;
		}
	}
	
	private void reinitializeArray(int startIndex, int length){
		for (int i = 0; i < length; i++) {
			payload[startIndex] = ARRAY_INITIALIZER;
			startIndex++;
		}		
	}
	
	/** Factory methods to create specific message types */
	public static SimpleDynamoMessage getTestTwoRequestBroadcastMessage() {
		SimpleDynamoMessage dhtMessage = new SimpleDynamoMessage(INSERT);
		return dhtMessage;		
	}
	
	public void setAvd(String avdNumber, int insertionPoint){ 
		reinitializeArray(insertionPoint, 4);
		insertTextPayloadContent(avdNumber, insertionPoint);
	}

	public String getAvdOne(){ return new String(getPayloadAsString(4, AVD_INSERT_PT_ONE));}
	public byte[] getPayload(){ return payload;}

	
	public void setKey(String key){ 
		reinitializeArray(KEY_INSERT_PT, 6);
		insertTextPayloadContent(key, KEY_INSERT_PT);
	}

	public void setValue(String value){ 
		reinitializeArray(VALUE_INSERT_PT, 6);
		insertTextPayloadContent(value, VALUE_INSERT_PT);
	}

	public String getKey(){ 
		String keyValue = new String(getPayloadAsString(6, KEY_INSERT_PT));
		keyValue = keyValue.replaceAll("z", "");
		return keyValue;
	}
	
	public String getValue(){ 
		String contentValue = new String(getPayloadAsString(6, VALUE_INSERT_PT));
		contentValue = contentValue.replaceAll("z", "");
		return contentValue;
	}

	private byte[] getPayloadAsString(int size, int startPoint) {
		byte[] avdBytes = new byte[size];
		for (int i = 0; i < avdBytes.length; i++) {
			avdBytes[i] = payload[startPoint];
			startPoint++;
		}
		return avdBytes;
	}
	
	private void insertTextPayloadContent(String value, int insertPoint) {
		byte[] stringBytes = value.getBytes();
		for (int i = 0; i < value.length(); i++) {
			payload[insertPoint] = stringBytes[i];
			insertPoint = insertPoint + 1;
		}
	}

	public boolean isInsertMessage(){return determineType(INSERT);}
	public boolean isInsertReplica(){return determineType(INSERT_REPLICA);}

	private boolean determineType(String type) {
		String byteValue = new String(new byte[]{payload[0]});
		boolean isRequestType = false;
		if(byteValue.equals(type)){
			isRequestType = true;
		}
		return isRequestType;
	}

	
	public static SimpleDynamoMessage getQueryMessage(String nodeToSendQuery, String currentNode, String key) {
		SimpleDynamoMessage dhtMessage = new SimpleDynamoMessage(SINGLE_QUERY_REQUEST);
		dhtMessage.setAvd(nodeToSendQuery, SimpleDynamoMessage.AVD_INSERT_PT_ONE);
		dhtMessage.setAvd(currentNode, SimpleDynamoMessage.AVD_INSERT_PT_TWO);
		dhtMessage.setKeyForSingle(key);
		return dhtMessage;
	}
	
	public static SimpleDynamoMessage getInsertMessage(String avd, String key, String value) {
		SimpleDynamoMessage dhtMessage = new SimpleDynamoMessage(INSERT);
		dhtMessage.setAvd(avd, AVD_INSERT_PT_ONE);
		dhtMessage.setKey(key);
		dhtMessage.setValue(value);
		return dhtMessage;
	}

	
	public static SimpleDynamoMessage getQueryResponsMessage(String avdTwo, String key, String returnValue) {
		SimpleDynamoMessage dm = new SimpleDynamoMessage(SINGLE_QUERY_RESPONSE);
		dm.setAvd(avdTwo, AVD_INSERT_PT_ONE);
		dm.setKey(key);
		dm.setValue(returnValue);
		return dm;
	}

	public boolean isQueryRequest() {return determineType(SINGLE_QUERY_REQUEST);}
	public boolean isQueryResponse() {return determineType(SINGLE_QUERY_RESPONSE);}
	public boolean isQuorumRequest() {return determineType(QUORUM_REQUEST);}
	public boolean isQuorumResponse() {return determineType(QUORUM_RESPONSE);}

	private void setKeyForSingle(String key) {
		reinitializeArray(KEY_FOR_SINGLE_INSERT_PT, 6);
		insertTextPayloadContent(key, KEY_FOR_SINGLE_INSERT_PT);
	}

	public String getKeyForSingle() {
		String value = new String(getPayloadAsString(6, KEY_FOR_SINGLE_INSERT_PT));
		value = value.replace("z", "");
		return value;
	}

	public String getAvdTwo(){ return new String(getPayloadAsString(4, AVD_INSERT_PT_TWO));}

	public static SimpleDynamoMessage getInsertReplicaMessage(String avd, String keyValue, String contentValue) {
		SimpleDynamoMessage dhtMessage = new SimpleDynamoMessage(INSERT_REPLICA);
		dhtMessage.setAvd(avd, AVD_INSERT_PT_ONE);
		dhtMessage.setKey(keyValue);
		dhtMessage.setValue(contentValue);
		return dhtMessage;
	}

	public static SimpleDynamoMessage getQuorumRequest(String requestee, String requestor) {
		SimpleDynamoMessage dhtMessage = new SimpleDynamoMessage(QUORUM_REQUEST);
		dhtMessage.setAvd(requestee, AVD_INSERT_PT_ONE);
		dhtMessage.setAvd(requestor, AVD_INSERT_PT_TWO);
		return dhtMessage;
	}

	public static SimpleDynamoMessage getQuorumResponse(String requestor, String requestee) {
		SimpleDynamoMessage dhtMessage = new SimpleDynamoMessage(QUORUM_RESPONSE);
		dhtMessage.setAvd(requestor, AVD_INSERT_PT_ONE);
		dhtMessage.setAvd(requestee, AVD_INSERT_PT_TWO);
		return dhtMessage;
	}
	
}

package org.alexd.jsonrpc;

import java.util.UUID;

import org.alexd.jsonrpc.JSONRPCParams.Versions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;

public abstract class JSONRPCThreadedClient {

	protected enum Description{
		NORMAL_RESPONSE,
		ERROR
	};
	
	protected static final String JSON_RESULT = "result"; 

	protected class MessageObject{
		public Description description;
		public Object content;
		
		public MessageObject(Description description, Object content){
			this.description = description;
			this.content = content;
		}
	}
	
	public static interface OnObjectResultListener{
		void manageResult(Object result);

		void sendErrorMessageNull();

		void sendError(Exception content); 
	}
	
	public interface OnBooleanResultListener extends OnObjectResultListener{
		void manageResult(boolean result);
	}

	public interface OnDoubleResultListener extends OnObjectResultListener{
		void manageResult(double result);
	}

	public interface OnIntResultListener extends OnObjectResultListener{
		void manageResult(int result);
	}

	public interface OnJSONArrayResultListener extends OnObjectResultListener{
		void manageResult(JSONArray result);
	}

	public interface OnJSONObjectResultListener extends OnObjectResultListener{
		void manageResult(JSONObject result);
	}

	public interface OnLongResultListener extends OnObjectResultListener{
		void manageResult(long result);
	}

	public interface OnStringResultListener extends OnObjectResultListener{
		void manageResult(String result);
	}

	
	protected Versions version;
	
	/**
	 * Create a JSONRPCClient from a given uri 
	 * @param uri The URI of the JSON-RPC service
	 * @return a JSONRPCClient instance acting as a proxy for the web service
	 */
	public static JSONRPCClient create(String uri, Versions version) {
		JSONRPCClient client = new JSONRPCHttpClient(uri);
		client.version = version;
		return client;
	}
	
	protected boolean _debug = false;
	
	/**
	 * Setting the _debugging mode (ON / OFF  -> FALSE / TRUE
	 */
	public void setDebug(boolean _debug) {
		this._debug = _debug;
	}
	
	/**
	 * Get the debugging mode
	 */
	public boolean isDebug(){
		return _debug;
	}
	
	protected abstract JSONObject doJSONRequest(JSONObject request) throws JSONRPCException;
	
	protected static JSONArray getJSONArray(Object[] array){
		JSONArray arr = new JSONArray();
		for (Object item : array) {
			if(item.getClass().isArray()){
				arr.put(getJSONArray((Object[])item));
			}
			else {
				arr.put(item);
			}
		}
		return arr;
	}
	
	protected JSONObject doRequest(String method, Object[] params) throws JSONRPCException
	{
		//Copy method arguments in a json array
		JSONArray jsonParams = new JSONArray();
		for (int i=0; i<params.length; i++)
		{
			if(params[i].getClass().isArray()){
				jsonParams.put(getJSONArray((Object[])params[i]));
			}
			jsonParams.put(params[i]);
		}
		
		//Create the json request object
		JSONObject jsonRequest = new JSONObject();
		try 
		{
			jsonRequest.put("id", UUID.randomUUID().hashCode());
			jsonRequest.put("method", method);
			jsonRequest.put("params", jsonParams);
		}
		catch (JSONException e1)
		{
			throw new JSONRPCException("Invalid JSON request", e1);
		}
		return doJSONRequest(jsonRequest);
	}
	
	protected JSONObject doRequest(String method, JSONObject params) throws JSONRPCException, JSONException {
		
		JSONObject jsonRequest = new JSONObject();
		try{
			jsonRequest.put("id", UUID.randomUUID().hashCode());
			jsonRequest.put("method", method);
			jsonRequest.put("params", params);
			jsonRequest.put("jsonrpc", "2.0");
		} catch (JSONException e1) {
			throw new JSONRPCException("Invalid JSON request", e1);
		}
		return doJSONRequest(jsonRequest);
	}
	
	protected int soTimeout = 0, connectionTimeout = 0;
	
	/**
	 * Get the socket operation timeout in milliseconds
	 */
	public int getSoTimeout()
	{
		return soTimeout;
	}

	/**
	 * Set the socket operation timeout
	 * @param soTimeout timeout in milliseconds
	 */
	public void setSoTimeout(int soTimeout)
	{
		this.soTimeout = soTimeout;
	}

	/**
	 * Get the connection timeout in milliseconds
	 */
	public int getConnectionTimeout()
	{
		return connectionTimeout;
	}

	/**
	 * Set the connection timeout
	 * @param connectionTimeout timeout in milliseconds
	 */
	public void setConnectionTimeout(int connectionTimeout)
	{
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void call(final String method, final OnObjectResultListener listener, final Object ... params) 
	{
		final Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				if(msg.obj == null) listener.sendErrorMessageNull();
				else{
					MessageObject object = (MessageObject)msg.obj;
					if(object.description == Description.ERROR){
						listener.sendError((Exception) object.content);
					}
					else{
						listener.manageResult(object.content);
					}
				}
			};
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				MessageObject mo = null;
				try {
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							doRequest(method, params).get(JSON_RESULT));
				} catch (JSONException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	}
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */	
	public void call(final String method, final OnObjectResultListener onResultListener, final JSONObject params) {
		final Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				if(msg.obj == null) onResultListener.sendErrorMessageNull();
				else{
					MessageObject object = (MessageObject)msg.obj;
					if(object.description == Description.ERROR){
						onResultListener.sendError((Exception) object.content);
					}
					else{
						onResultListener.manageResult(object.content);
					}
				}
			};
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				MessageObject mo = null;
				try {
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							doRequest(method, params).get(JSON_RESULT));
				} catch (JSONException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	}
	 
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callString(final String method, final OnStringResultListener listener, final Object ... params) throws JSONRPCException
	{
		final Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				if(msg.obj == null) listener.sendErrorMessageNull();
				else{
					MessageObject object = (MessageObject)msg.obj;
					if(object.description == Description.ERROR){
						listener.sendError((Exception) object.content);
					}
					else{
						listener.manageResult((String)object.content);
					}
				}
			};
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				MessageObject mo = null;
				try {
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							doRequest(method, params).getString(JSON_RESULT));
				} catch (JSONException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	}
	
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */	
	public void callString(final String method, final OnStringResultListener listener, final JSONObject params) throws JSONRPCException {
		final Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				if(msg.obj == null) listener.sendErrorMessageNull();
				else{
					MessageObject object = (MessageObject)msg.obj;
					if(object.description == Description.ERROR){
						listener.sendError((Exception) object.content);
					}
					else{
						listener.manageResult((String)object.content);
					}
				}
			};
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				MessageObject mo = null;
				try {
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
						doRequest(method, params).getString(JSON_RESULT));
				} catch (JSONException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo)); 
			}
		};
		thread.start();
	}
	 
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callInt(final String method, final OnIntResultListener listener, final Object ... params) 
	{
		final Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				if(msg.obj == null) listener.sendErrorMessageNull();
				else{
					MessageObject object = (MessageObject)msg.obj;
					if(object.description == Description.ERROR){
						listener.sendError((Exception) object.content);
					}
					else{
						listener.manageResult(((Integer)object.content).intValue());
					}
				}
			};
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				JSONObject response = null;
				MessageObject mo = null;
				try {
					response = doRequest(method, params);
					if(response == null){
						mo = new MessageObject(Description.ERROR, 
								new JSONRPCException("Cannot call method: " + method));
					}
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							new Integer(response.getInt(JSON_RESULT)));
				} catch (JSONException e) {
					try{
						mo = new MessageObject(Description.NORMAL_RESPONSE, 
								new Integer(response.getString(JSON_RESULT)));
					} catch(NumberFormatException e1){
						mo = new MessageObject(Description.ERROR, e);
					} catch (JSONException e1){ 
						mo = new MessageObject(Description.ERROR, e);
					}
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	}
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */	
	public void callInt(final String method, final OnIntResultListener listener, final JSONObject params) {
		
		final Handler handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg.obj == null){
					listener.sendErrorMessageNull();
				}
				else{
					MessageObject mo = (MessageObject) msg.obj;
					if(mo.description == Description.ERROR){
						listener.sendError((Exception)mo.content);
					}
					else{
						listener.manageResult(((Integer)mo.content).intValue());
					}
				}
			}
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				JSONObject response = null;
				MessageObject mo;
				try{
					response = doRequest(method, params);
					if(response == null){
						mo = new MessageObject(Description.ERROR, new JSONRPCException("Cannot call method: " + method));
					}
					else{
						mo = new MessageObject(Description.NORMAL_RESPONSE, 
								new Integer(response.getInt(JSON_RESULT)));
					}
				}
				catch (JSONException e) {
					try {
						mo = new MessageObject(Description.NORMAL_RESPONSE, 
								new Integer(response.getString(JSON_RESULT)));
					} catch (NumberFormatException e1) {
						mo = new MessageObject(Description.ERROR, e1);
					} catch (JSONException e1) {
						mo = new MessageObject(Description.ERROR, e1);
					}
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	 }
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callLong(final String method, final OnLongResultListener listener, final Object ... params) 
	{
		
		final Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				if(msg.obj == null){
					listener.sendErrorMessageNull();
				}
				else{
					MessageObject mo = (MessageObject) msg.obj;
					if(mo.description == Description.ERROR){
						listener.sendError((Exception)mo.content);
					}
					else{
						listener.manageResult(((Long)mo.content).longValue());
					}
				}
			};
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				JSONObject response = null;
				MessageObject mo = null;
				try{
					response = doRequest(method, params);
					if(response == null){
						mo = new MessageObject(Description.ERROR, 
								new JSONRPCException("Cannot call method: " + method));
					}
					else{
						mo = new MessageObject(Description.NORMAL_RESPONSE, 
								new Long(response.getLong(JSON_RESULT)));
					}
				} catch (JSONException e) {
					try{
						mo = new MessageObject(Description.NORMAL_RESPONSE,
								Long.parseLong(response.getString(JSON_RESULT)));
					} catch (NumberFormatException e1) {
						mo = new MessageObject(Description.ERROR, e1);
					} catch (JSONException e2) {
						mo = new MessageObject(Description.ERROR, e2);
					}
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	}
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callLong(final String method, 
			final OnLongResultListener listener, final JSONObject params)
	{
		final Handler handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg == null || msg.obj == null){
					listener.sendErrorMessageNull();
				}
				else{
					MessageObject mo = (MessageObject)msg.obj;
					if(mo.description == Description.ERROR){
						listener.sendError((Exception)mo.content);
					}
					else{
						listener.manageResult(((Long)mo.content).longValue());
					}
				}
			}
		};
		
		Thread thread = new Thread(){
			public void run() {
				JSONObject response = null;
				MessageObject mo = null;
				try {
					response = doRequest(method, params);
					mo = new MessageObject(Description.NORMAL_RESPONSE,
							new Long(response.getLong(JSON_RESULT)));
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONException e) {
					try {
						mo = new MessageObject(Description.NORMAL_RESPONSE, 
								new Long(response.getString(JSON_RESULT)));
					} catch (NumberFormatException e1) {
						mo = new MessageObject(Description.ERROR, e1);
					} catch (JSONException e1) {
						mo = new MessageObject(Description.ERROR, e1);
					}
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			};
		};
		thread.start();
	}	
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callBoolean(final String method, 
			final OnBooleanResultListener listener, final Object ... params) 
	{
		final Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				if(msg == null || msg.obj == null){
					listener.sendErrorMessageNull();
				}
				else{
					MessageObject mo = (MessageObject)msg.obj;
					if(mo.description == Description.ERROR){
						Exception e = (Exception)mo.content;
						listener.sendError(e);
					}
					else{
						listener.manageResult(((Boolean)mo.content).booleanValue());
					}
				}
			};
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				JSONObject response = null;
				MessageObject mo = null;
				try{
					response = doRequest(method, params);
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							new Boolean(response.getBoolean(JSON_RESULT)));
				} catch (JSONRPCException e){
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONException e) {
					try {
						mo = new MessageObject(Description.NORMAL_RESPONSE, 
								new Boolean(response.getString(JSON_RESULT)));
					} catch (JSONException e1) {
						mo = new MessageObject(Description.ERROR, e1);
					}
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			};
		};
		thread.start();
	}
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callBoolean(final String method, 
			final OnBooleanResultListener listener, final JSONObject params){
		final Handler handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg == null || msg.obj == null){
					listener.sendErrorMessageNull();
				}
				else{
					MessageObject mo = (MessageObject)msg.obj;
					if(mo.description == Description.ERROR){
						listener.sendError((Exception)mo.content);
					}
					else{
						listener.manageResult(((Boolean)mo.content).booleanValue());
					}
				}
			}
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				MessageObject mo = null;
				JSONObject response = null;
				try{
					response = doRequest(method, params);
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							new Boolean(response.getBoolean(JSON_RESULT)));
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONException e) {
					try {
						mo = new MessageObject(Description.NORMAL_RESPONSE, 
								new Boolean(response.getString(JSON_RESULT)));
					} catch (JSONException e1) {
						mo = new MessageObject(Description.ERROR, e1);
					}
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	}

	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callDouble(final String method, 
			final OnDoubleResultListener listener, final Object ... params) 
	{
		final Handler handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg == null || msg.obj == null){
					listener.sendErrorMessageNull();
				}
				else{
					MessageObject mo = (MessageObject) msg.obj;
					if(mo.description == Description.ERROR){
						listener.sendError((Exception)mo.content);
					}
					else{
						listener.manageResult(((Double)mo.content).doubleValue());
					}
				}
			}
		};
		
		Thread thread = new Thread(){
			public void run() {
				MessageObject mo = null;
				JSONObject response = null;
				try {
					response = doRequest(method, params);
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							new Double(response.getDouble(JSON_RESULT)));
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONException e) {
					try {
						mo = new MessageObject(Description.NORMAL_RESPONSE, 
								new Double(response.getString(JSON_RESULT)));
					} catch (NumberFormatException e1) {
						mo = new MessageObject(Description.ERROR, e1);
					} catch (JSONException e1) {
						mo = new MessageObject(Description.ERROR, e1);
					} 
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			};
		};
		thread.start();
	}
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callDouble(final String method, 
			final OnDoubleResultListener listener, final JSONObject params) {
		
		final Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				if(msg == null || msg.obj == null){
					listener.sendErrorMessageNull();
				}
				else{
					MessageObject mo = (MessageObject)msg.obj;
					if(mo.description == Description.ERROR){
						listener.sendError((Exception)mo.content);
					}
					else{
						listener.manageResult(((Double)mo.content).doubleValue());
					}
				}
			};
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				MessageObject mo = null;
				JSONObject response = null;
				try {
					response = doRequest(method, params);
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							new Double(response.getDouble(JSON_RESULT)));
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONException e) {
					try {
						mo = new MessageObject(Description.NORMAL_RESPONSE, 
								new Double(response.getString(JSON_RESULT)));
					} catch (NumberFormatException e1) {
						mo = new MessageObject(Description.ERROR, e1);
					} catch (JSONException e1) {
						mo = new MessageObject(Description.ERROR, e1);
					}
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	}
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callJSONObject(final String method, 
			final OnJSONObjectResultListener listener, final JSONObject params) {
		final Handler handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg == null || msg.obj == null){
					listener.sendErrorMessageNull();
				}
				else{
					MessageObject mo = (MessageObject)msg.obj;
					if(mo.description == Description.ERROR){
						listener.sendError((Exception)mo.content);
					}
					else {
						listener.manageResult((JSONObject)mo.content);
					}
				}
			}
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				MessageObject mo = null;
				JSONObject response = null;
				try {
					response = doRequest(method, params);
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							response.getJSONObject(JSON_RESULT));
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONException e) {
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	}
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callJSONObject(final String method, 
			final OnJSONObjectResultListener listener, final Object ... params)
	{
		final Handler handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg == null || msg.obj == null){
					listener.sendErrorMessageNull();
				}
				else{
					MessageObject mo = (MessageObject)msg.obj;
					if(mo.description == Description.ERROR){
						listener.sendError((Exception)mo.content);
					}
					else{
						listener.manageResult((JSONObject)mo.content);
					}
				}
			}
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				JSONObject response = null;
				MessageObject mo = null;
				try {
					response = doRequest(method, params);
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							response.getJSONObject(JSON_RESULT));
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONException e) {
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	}
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callJSONArray(final String method, 
			final OnJSONArrayResultListener listener, final Object ... params)
	{
		final Handler handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg == null || msg.obj == null){
					listener.sendErrorMessageNull();
				}
				else{
					MessageObject mo = (MessageObject)msg.obj;
					if(mo.description == Description.ERROR){
						listener.sendError((Exception)mo.content);
					}
					else{
						listener.manageResult((JSONArray)mo.content);
					}
				}
			}
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				MessageObject mo = null;
				JSONObject response = null;
				try {
					response = doRequest(method, params);
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							response.getJSONArray(JSON_RESULT));
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONException e) {
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	}
	
	/**
	 * Perform a remote JSON-RPC method call
	 * @param method The name of the method to invoke
	 * @param params Arguments of the method
	 * @throws JSONRPCException if an error is encountered during JSON-RPC method call
	 */
	public void callJSONArray(final String method, 
			final OnJSONArrayResultListener listener, final JSONObject params) 
	{
		final Handler handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg == null || msg.obj == null){
					listener.sendErrorMessageNull();
				}
				else{
					MessageObject mo = (MessageObject)msg.obj;
					if(mo.description == Description.ERROR){
						listener.sendError((Exception)mo.content);
					}
					else{
						listener.manageResult((JSONArray)mo.content);
					}
				}
			}
		};
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				MessageObject mo = null;
				JSONObject response =  null;
				try {
					response = doRequest(method, params);
					mo = new MessageObject(Description.NORMAL_RESPONSE, 
							response.getJSONArray(JSON_RESULT));
				} catch (JSONRPCException e) {
					mo = new MessageObject(Description.ERROR, e);
				} catch (JSONException e) {
					mo = new MessageObject(Description.ERROR, e);
				}
				handler.sendMessage(handler.obtainMessage(NORM_PRIORITY, mo));
			}
		};
		thread.start();
	}
}

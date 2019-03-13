package com.m4bce.jsonrpc;

import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class JsonRpcService {
	
	public interface Listener<E>{
		public void onResult(E response);
	}
	
	private static final String TAG = JsonRpcService.class.toString();
	private static final int METHODE_TYPE = Request.Method.GET;
	
	private static JsonRpcService service;
	
	public static JsonRpcService getService(Context context, String serviceUrl){
		if(service == null){
			service = new JsonRpcService(context, serviceUrl);
		}
		return service;
	}
	
	private String serviceUrl;
	private RequestQueue queue;
	
	private JsonRpcService(Context context, String serviceUrl) {
		this.serviceUrl = serviceUrl;
		queue = Volley.newRequestQueue(context);
	}
	
	private JSONObject getJsonRequest(String method, JSONObject parameters) throws JSONException{
		JSONObject jsonRequest = new JSONObject();
		jsonRequest.put("jsonrpc", "2.0");
		jsonRequest.put("method", method);
		jsonRequest.put("id", UUID.randomUUID().hashCode());
		jsonRequest.put("params", parameters);
		return jsonRequest;
	}
	
	private JSONObject getJsonRequest(String method, Object ... params) throws JSONException{
		JSONObject jsonRequest = new JSONObject();
		jsonRequest.put("jsonrpc", "2.0");
		jsonRequest.put("method", method);
		jsonRequest.put("id", UUID.randomUUID().hashCode());
//		jsonRequest.put("params", parameters);
		JSONArray array = new JSONArray();
		for (Object object : params) {
			array.put(object);
		}
		jsonRequest.put("params", array);
		return jsonRequest;
	}

	public void call(Listener<Object> listener, String method, JSONObject parameters){
		try{
			JsonObjectRequest request = new JsonObjectRequest(METHODE_TYPE, serviceUrl,
					getJsonRequest(method, parameters),
					new Response.Listener<JSONObject>() {

						@Override
						public void onResponse(JSONObject response) {
							Log.d(TAG, response.toString());
						}
					},
					new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							Log.e(TAG, error.toString());
						}
					});
			queue.add(request);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void call(Listener<Object> listener, String method, Object ... parameters){
		try{
			JsonObjectRequest request = new JsonObjectRequest(METHODE_TYPE, serviceUrl,
					getJsonRequest(method, parameters),
					new Response.Listener<JSONObject>() {

						@Override
						public void onResponse(JSONObject response) {
							Log.d(TAG, response.toString());
						}
					},
					new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							Log.e(TAG, error.toString());
						}
					});
			queue.add(request);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void callString(final Listener<String> listener, String method, JSONObject parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((String)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callString(final Listener<String> listener, String method, Object ... parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((String)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callInt(final Listener<Integer> listener, String method, JSONObject parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((Integer)response);
			}
		};
		call(intListener, method, parameters);
	}

	public void callInt(final Listener<Integer> listener, String method, Object ... parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((Integer)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callLong(final Listener<Long> listener, String method, JSONObject parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((Long)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callLong(final Listener<Long> listener, String method, Object ... parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((Long)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callBoolean(final Listener<Boolean> listener, String method, JSONObject parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((Boolean)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callBoolean(final Listener<Boolean> listener, String method, Object ... parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((Boolean)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callDouble(final Listener<Double> listener, String method, JSONObject parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((Double)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callDouble(final Listener<Double> listener, String method, Object ... parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((Double)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callJsonObject(final Listener<JSONObject> listener, String method, JSONObject parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((JSONObject)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callJsonObject(final Listener<JSONObject> listener, String method, Object ... parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((JSONObject)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callJsonArray(final Listener<JSONArray> listener, String method, JSONObject parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((JSONArray)response);
			}
		};
		call(intListener, method, parameters);
	}
	
	public void callJsonArray(final Listener<JSONArray> listener, String method, Object ... parameters){
		Listener<Object> intListener = new Listener<Object>() {
			@Override
			public void onResult(Object response) {
				listener.onResult((JSONArray)response);
			}
		};
		call(intListener, method, parameters);
	}
	
}

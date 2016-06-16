package org.transdroid.search.WhatCdAdapter;

import java.io.InputStream;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.gui.SettingsHelper;
import org.transdroid.util.HttpHelper;

import android.content.Context;
import android.util.Log;   

/*
 api doc @ https://github.com/WhatCD/Gazelle/wiki/JSON-API-Documentation#torrent-search
*/
public class WhatCdAdapter implements ISearchAdapter {
    private static final String LOG_TAG = WhatCdAdapter.class.getName();

    private static final String LOGIN_URL = "https://what.cd/login.php";

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String LOGIN = "Log In";

	/**
	 * Implementing search providers should synchronously perform the search for torrents matching the given query
	 * string.
	 * @param context The Android activity/provider context from which the shared preferences can be accessed
	 * @param query The raw (non-urlencoded) query to search for
	 * @param order The preferred order in which results are sorted
	 * @param maxResults Maximum number of results to return
	 * @return The list of found torrents on the site matching the search query
	 * @throws Exception When an exception occurred during the loading or parsing of the search results
	 */
	List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception;

	/**
	 * Implementing search providers should provide the URL of an RSS feed matching the search a specific query.
	 * @param query The raw (non-urlencoded) query for which the RSS feed should provide torrents
	 * @param order The preferred order in which the RSS items are sorted
	 * @return The RSS feed URL, or null if this is not supported by the site
	 */
	String buildRssFeedUrlFromSearch(String query, SortOrder order);

	/**
	 * Implementing search providers should return the real name of the site they work on.
	 * @return The name of the torrent site
	 */
	String getSiteName();

	/**
	 * Implementing search providers should return whether this is a private site, that is, whether this site requires
	 * user credentials before it can be searched.
	 * @return True if this is an adapter to a private site, false otherwise.
	 */
	boolean isPrivateSite() {
	  return true;
	}

	/**
	 * Implementing search providers should return whether the site uses a token authentication system.
	 * @return True is a session token is used in lieu of a username/password login combination
	 */
	boolean usesToken();

	/**
	 * Implement search providers should set up an HTTP request for the specified torrent file uri and, possibly after
	 * setting authentication credentials, return a handle to the file content stream.
	 * @param context The Android activity/provider context from which the shared preferences can be accessed
	 * @param url The full url of the torrent file to download
	 * @return An InputStream handle to the requested file so it can be further downloaded, or null if no connection is
	 *         possible (like when the device is offline or when the user is not authorized)
	 * @throws Exception When an exception occurred during the retrieval of the request url
	 */
	InputStream getTorrentFile(Context context, String url) throws Exception;
   
  private void login(DefaultHttpClient client, String username, String password) throws Exception {
        Log.d(LOG_TAG, "Attempting to login.");

        HttpPost request = new HttpPost(LOGIN_URL);
        request.setEntity(new UrlEncodedFormEntity(Arrays
                .asList(new BasicNameValuePair[] {
                        new BasicNameValuePair(USERNAME, username),
                        new BasicNameValuePair(PASSWORD, password),
                        new BasicNameValuePair(LOGIN, LOGIN)})));

        client.execute(request);

        // verify we have the cookies needed to log in
        boolean success = false;
        for (Cookie cookie : client.getCookieStore().getCookies()) {
            if ("session".equals(cookie.getName()))
              success = true;
        }
        
        // if we don't have the correct cookies, login failed. notify user with a toast and toss an exception.
        if (!success) {
        	Log.e(LOG_TAG, "Failed to log into What.cd as '" + username + "'. Did not receive expected login cookies!");
            throw new LoginException("Failed to log into What.cd as '" + username + "'. Did not receive expected login cookies!");
        }
        
        Log.d(LOG_TAG, "Successfully logged in to What.cd");
    }
}

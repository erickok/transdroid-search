/*
 *	This file is part of Transdroid Torrent Search 
 *	<http://code.google.com/p/transdroid-search/>
 *	
 *	Transdroid Torrent Search is free software: you can redistribute 
 *	it and/or modify it under the terms of the GNU Lesser General 
 *	Public License as published by the Free Software Foundation, 
 *	either version 3 of the License, or (at your option) any later 
 *	version.
 *	
 *	Transdroid Torrent Search is distributed in the hope that it will 
 *	be useful, but WITHOUT ANY WARRANTY; without even the implied 
 *	warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 *	See the GNU Lesser General Public License for more details.
 *	
 *	You should have received a copy of the GNU Lesser General Public 
 *	License along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.search.TorrentDay;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.security.auth.login.LoginException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.gui.SettingsHelper;
import org.transdroid.util.HttpHelper;

import android.content.Context;

/**
 * An adapter that provides access to TorrentDay searches by parsing their AJAX JSON API.
 */
public class TorrentDayAdapter implements ISearchAdapter {

	private static final String LOGINURL = "https://torrentday.eu/torrents/";
	private static final String QUERYURL = "https://torrentday.eu/V3/API/API.php";
	private static final int CONNECTION_TIMEOUT = 8000;

	private DefaultHttpClient prepareRequest(Context context) throws Exception {

		String username = SettingsHelper.getSiteUser(context, TorrentSite.TorrentDay);
		String password = SettingsHelper.getSitePass(context, TorrentSite.TorrentDay);
		if (username == null || password == null) {
			throw new InvalidParameterException(
					"No username or password was provided, while this is required for this private site.");
		}

		// Setup http client
		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT);
		DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);

		// First log in
		HttpPost loginPost = new HttpPost(LOGINURL);
		loginPost.setEntity(new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair("username", username),
				new BasicNameValuePair("password", password))));
		HttpResponse loginResult = httpclient.execute(loginPost);
		if (loginResult.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			// Failed to sign in
			throw new LoginException("Login failure for TorrentDay with user " + username);
		}

		return httpclient;

	}

	@Override
	public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {

		DefaultHttpClient httpclient = prepareRequest(context);

		// Start synchronous search via the JSON API
		HttpPost queryPost = new HttpPost(QUERYURL);
		List<BasicNameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("search", URLEncoder.encode(query, "UTF-8")));
		params.add(new BasicNameValuePair("jxt", "8")); // ???
		params.add(new BasicNameValuePair("jxw", "b")); // ???
		params.add(new BasicNameValuePair("cata", "yes")); // ???
		if (order == SortOrder.BySeeders) {
			params.add(new BasicNameValuePair("s", "4")); // Seeders sort parameter
			params.add(new BasicNameValuePair("t", "2")); // Sort order (2 is descending, 1 is ascending)
		}
		queryPost.setEntity(new UrlEncodedFormEntity(params));
		HttpResponse queryResult = httpclient.execute(queryPost);
		if (queryResult.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			throw new Exception("Unsuccessful query to the TorrentDay JSON API (after a successful login)");
		}

		// Read JSON response
		InputStream instream = queryResult.getEntity().getContent();
		String json = HttpHelper.convertStreamToString(instream);
		instream.close();
		JSONObject structure = new JSONObject(json);
		
		// Construct the list of search results
		List<SearchResult> results = new ArrayList<>();
		JSONArray torrents = structure.getJSONArray("Fs").getJSONObject(0).getJSONObject("Cn").getJSONArray("torrents");
		String detailsLink = "https://torrentday.eu/details.php?id=%1$s";
		String torrentLink = "https://torrentday.eu/download.php/%1$s/%2$s";
		SimpleDateFormat addedFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		for (int i = 0; i < torrents.length(); i++) {
			JSONObject torrent = torrents.getJSONObject(i);
			results.add(new SearchResult(
					torrent.getString("name"), 
					String.format(torrentLink, torrent.getString("id"), torrent.getString("fname")), 
					String.format(detailsLink, torrent.getString("id")), 
					torrent.getString("size"), 
					addedFormat.parse(torrent.getString("added")), 
					torrent.getInt("seed"), 
					torrent.getInt("leech")));
		}
		return results;

	}

	@Override
	public InputStream getTorrentFile(Context context, String url) throws Exception {

		// Provide an authenticated file handle to the requested url
		DefaultHttpClient httpclient = prepareRequest(context);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		return response.getEntity().getContent();

	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// TorrentDay doesn't support RSS feed-based searches
		return null;
	}

	@Override
	public String getSiteName() {
		return "TorrentDay";
	}

	@Override
	public boolean isPrivateSite() {
		return true;
	}

	@Override
	public boolean usesToken() {
		return false;
	}

}

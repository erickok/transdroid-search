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
package org.transdroid.search.Isohunt;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.util.HttpHelper;

/**
 * An adapter that provides easy access to isoHunt torrent searches. Communication
 * is handled via the isoHunt JSON REST API.
 * 
 * @author Eric Kok
 */
public class IsohuntAdapter implements ISearchAdapter {

	private static final String RPC_QUERYURL = "http://isohunt.to/js/json.php?ihq=%s&start=%s&rows=%s";
	private static final String RPC_SORT_COMPOSITE = ""; // Empty because it shouldn't append a sort string to the URL
	private static final String RPC_SORT_SEEDS = "&sort=seeds"; // Will be appended to URL
	private static final int CONNECTION_TIMEOUT = 10000;

	private static final String RPC_RESULTS = "total_results";
	private static final String RPC_ITEMS = "items";
	private static final String RPC_ITEMS_LIST = "list";
	private static final String RPC_ITEM_TITLE = "title";
	private static final String RPC_ITEM_LINK = "link";
	private static final String RPC_ITEM_URL = "enclosure_url";
	private static final String RPC_ITEM_SIZE = "size";
	private static final String RPC_ITEM_SEEDS = "Seeds";
	private static final String RPC_ITEM_LEECHERS = "leechers";
	private static final String RPC_ITEM_PUBDATE = "pubDate";

	@Override
	public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
		
		if (query == null) {
			return null;
		}
		
		// Build a search request parameters
		String encodedQuery = "";
		try {
			encodedQuery = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw e;
		}

		// Build full URL string
		final int startAt = 0; // Provides future support for paged results
		String url = String.format(RPC_QUERYURL, encodedQuery, String.valueOf(startAt), String.valueOf(maxResults));
		if (order == SortOrder.BySeeders) {
			url += RPC_SORT_SEEDS;
		} else {
			url += RPC_SORT_COMPOSITE;
		}

        // Setup request using GET
        HttpParams httpparams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT); 
        DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);
        HttpGet httpget = new HttpGet(url);
        
        // Make request
        HttpResponse response = httpclient.execute(httpget);

        // Read JSON response
        InputStream instream = response.getEntity().getContent();
        JSONObject json = new JSONObject(HttpHelper.ConvertStreamToString(instream));
        instream.close();
        
		// Empty result set?
		if (json.getInt(RPC_RESULTS) == 0) {
			return new ArrayList<SearchResult>();
		}
		
		// Retrieve the items list
		JSONObject list = json.getJSONObject(RPC_ITEMS);
		JSONArray items = list.getJSONArray(RPC_ITEMS_LIST);

		// Add search results
		List<SearchResult> results = new ArrayList<SearchResult>();
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			results.add(new SearchResult(
					item.getString(RPC_ITEM_TITLE).replaceAll("\\<.*?>",""),
					item.getString(RPC_ITEM_URL),
					item.getString(RPC_ITEM_LINK),
					item.getString(RPC_ITEM_SIZE), 
					new Date(Date.parse(item.getString(RPC_ITEM_PUBDATE))),
					item.getInt(RPC_ITEM_SEEDS),
					item.getInt(RPC_ITEM_LEECHERS)));
		}
		
		// Return the results list
		return results;
		
	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {

		// Build a search request parameters
		String encodedQuery = "";
		try {
			encodedQuery = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return "http://isohunt.com/js/rss/" + encodedQuery;
	}

	@Override
	public String getSiteName() {
		return "isoHunt";
	}
	
}

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
package org.transdroid.search.Fenopy;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
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
import org.transdroid.util.FileSizeConverter;
import org.transdroid.util.HttpHelper;

/**
 * An adapter that provides easy access to Fenopy torrent searches. Communication is handled via the Fenopy JSON REST
 * API.
 * @author Eric Kok
 */
public class FenopyAdapter implements ISearchAdapter {

	private static final String RPC_QUERYURL = "http://fenopy.se/module/search/api.php?keyword=%1$s&sort=%2$s&limit=%3$s&category=0&format=json";
	private static final String RPC_SORT_COMPOSITE = "relevancy";
	private static final String RPC_SORT_SEEDS = "peer";
	private static final int CONNECTION_TIMEOUT = 5000;

	@Override
	public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {

		if (query == null) {
			return null;
		}

		// Build search URL
		String url = String.format(RPC_QUERYURL, URLEncoder.encode(query),
				order == SortOrder.BySeeders ? RPC_SORT_SEEDS : RPC_SORT_COMPOSITE, String.valueOf(maxResults));

		// Setup HTTP client
		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT);
		DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);

		// Make request
		HttpResponse response = httpclient.execute(new HttpGet(url));

		// Read JSON response
		InputStream instream = response.getEntity().getContent();
		JSONArray json = new JSONArray(HttpHelper.ConvertStreamToString(instream));
		instream.close();

		// Add search results
		List<SearchResult> results = new ArrayList<SearchResult>();
		for (int i = 0; i < json.length(); i++) {
			JSONObject item = json.getJSONObject(i);
			results.add(new SearchResult(item.getString("name"), item.getString("torrent"), item.getString("page"),
					FileSizeConverter.getSize(item.getLong("size")), null, item.getInt("seeder"), item
							.getInt("leecher")));
		}

		// Return the results list
		return results;

	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		return "http://fenopy.se/rss.xml?keyword=test" + URLEncoder.encode(query);
	}

	@Override
	public String getSiteName() {
		return "Fenopy";
	}

}

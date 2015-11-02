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
package org.transdroid.search.ScambioEtico;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
import android.text.Html;
import android.util.Log;

/**
 * An adapter that provides access to Scambio Etico searches by parsing the raw HTML output.
 */
public class ScambioEtico implements ISearchAdapter {

	private static final String LOGIN_USER = "UserName";
	private static final String LOGIN_PASS = "PassWord";
	private static final String LOGINURL =
			"http://forum.tntvillage.scambioetico.org/index.php?act=Login&CODE=01";
	private static final String QUERYURL =
			"http://forum.tntvillage.scambioetico.org/index.php?act=allreleases&filter=%s%s";
	private static final String TORRENT_URL =
			"http://forum.tntvillage.scambioetico.org/index.php?act=Attach&type=post&id=%s";
	private static final String SORT_COMPOSITE = "";
	private static final String SORT_SEEDS = "&sb=4";
	private static final int CONNECTION_TIMEOUT = 8000;
	private static final String ENCODING = "ISO-8859-1";
	
	private static final String WRONG_PASSWORD = "Password errata. Inserisci la password rispettando "
			+ "i caratteri MAIUSCOLI e minuscoli.";
	private static final String WRONG_LOGIN = "Non &#232; possibile trovare un utente del Forum "
			+ "chiamato <b>";
	
	// Texts to find subsequently (Used in parseHtml)
	private static final String START_RESULTS = "<!--TORRENT TABLE-->";
	private static final String END_RESULTS = "<!--END TORRENT TABLE-->";
	private static final String ITEM_PREFIX = "<tr class=\"row4\">";
	
	// Regexps for extracting the informations that we care (Used in parseHtmlItem)
	private static final String MAIN_EXTRACTOR =
			"<a [^>]*href='(http://forum.tntvillage.scambioetico.org/"
			+ "index.php[?]showtopic=[0-9]+)'[^>]*>([^<]+)</a>";
	private static final String TORRENT_ID_EXTRACTOR =
			"<a [^>]*href='http://forum.tntvillage.scambioetico.org/"
			+ "index.php[?]act=peers&pid=([0-9]+)'[^>]*>PeerLists</a>";
	private static final String SIZE_EXTRACTOR =
			"\\[<span [^>]*style='color:blue'[^>]*>[^0-9\\.>]*([0-9\\.]+)[^0-9\\.>]*</span>\\]";
	private static final String SEEDS_EXTRACTOR =
			"\\[<span [^>]*style='color:red'[^>]*>[^0-9\\.>]*([0-9]+)[^0-9\\.>]*</span>\\]";
	private static final String LEECHERS_EXTRACTOR =
			"\\[<span [^>]*style='color:green'[^>]*>[^0-9\\.>]*([0-9]+)[^0-9\\.>]*</span>\\]";	

	private DefaultHttpClient prepareRequest(Context context) throws Exception {

		String username = SettingsHelper.getSiteUser(context, TorrentSite.ScambioEtico);
		String password = SettingsHelper.getSitePass(context, TorrentSite.ScambioEtico);
		if (username == null || password == null) {
			throw new InvalidParameterException("No username or password was provided, while "
					+ "this is required for Scambio Etico.");
		}

		// Setup request using GET
		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT);
		DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);

		// First log in
		Log.d("ScambioEtico", "Starting Authentication");
		HttpPost loginPost = new HttpPost(LOGINURL);
		loginPost.setEntity(new UrlEncodedFormEntity(Arrays.asList(
				new BasicNameValuePair[] {
						new BasicNameValuePair(LOGIN_USER, username),
						new BasicNameValuePair(LOGIN_PASS, password)
				})));
		HttpResponse loginResult = httpclient.execute(loginPost);
		String loginHtml = HttpHelper.convertStreamToString(loginResult.getEntity().getContent());
		if (loginHtml.indexOf(WRONG_LOGIN) >= 0) {
			throw new LoginException("Login failure for Scambio Etico wrong username " + username);
		} else if (loginHtml.indexOf(WRONG_PASSWORD) >= 0) {
			throw new LoginException("Login failure for Scambio Etico: bad password for user " + username);
		}
		Log.d("ScambioEtico", "Authentication successful");

		return httpclient;
	}

	@Override
	public List<SearchResult> search(Context context, String query, SortOrder order,
			int maxResults) throws Exception {

		DefaultHttpClient httpclient = prepareRequest(context);

		// Build a search request parameters
		String encodedQuery = "";
		try {
			encodedQuery = URLEncoder.encode(query, ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw e;
		}

		String urlPostfix = (order == SortOrder.BySeeders) ? SORT_SEEDS	: SORT_COMPOSITE;
		String url = String.format(QUERYURL, encodedQuery, urlPostfix);

		// Start synchronous search

		// Make request
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = httpclient.execute(httpget);

		// Read HTML response
		InputStream instream = response.getEntity().getContent();
		String html = HttpHelper.convertStreamToString(instream);
		instream.close();
		return parseHtml(html, maxResults);

	}

	@Override
	public InputStream getTorrentFile(Context context, String url) throws Exception {
		// Provide an authenticated file handle to the requested url
		DefaultHttpClient httpclient = prepareRequest(context);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		return response.getEntity().getContent();

	}

	// Parse the search results from HTML by looking for the identifying texts
	protected List<SearchResult> parseHtml(String html, int maxResults) throws Exception {
		int startResults = html.indexOf(START_RESULTS);
		int endResults = html.indexOf(END_RESULTS);
		
		List<SearchResult> results = new ArrayList<SearchResult>();

		if (startResults < 0) {
			return results; // No results for this query
		} else if (endResults <= 0) {
			// This should not happen but, if the site administrator will change the END_RESULTS
			// string we may still try to keep in working (endResults is mainly for safety).
			// This is still adaptive programming that it chills me to the bone but let us keep a
			// pragmatic approach... and parsing html without a clear interface is fragile anyway.
			endResults = html.length();
		}
		
		startResults += START_RESULTS.length();
		String resultsTable = html.substring(startResults, endResults);

		int itemsPointer = 0;
		do {
			itemsPointer += ITEM_PREFIX.length();
			int nextItem = resultsTable.indexOf(ITEM_PREFIX, itemsPointer);
			nextItem = (nextItem >= 0) ? nextItem : resultsTable.length();
			
			results.add(parseHtmlItem(resultsTable.substring(itemsPointer, nextItem)));
			
			itemsPointer = nextItem;
		} while (itemsPointer < resultsTable.length() && results.size() < maxResults);
		return results;
	}

	private String extractData(String regexp, String htmlItem) {
		return extractData(regexp, htmlItem, 1);
	}
	
	private String extractData(String regexp, String htmlItem, int group) {
		Pattern pattern = Pattern.compile(regexp);
		Matcher matcher = pattern.matcher(htmlItem);
		if (matcher == null || !matcher.find()) {
			throw new IllegalStateException(
					"Impossible to parse results. Probably Scambio Etico refactored its html and this application must be updated.");
		}
		
		return matcher.group(group);
	}
	
	private SearchResult parseHtmlItem(String htmlItem) throws UnsupportedEncodingException {
		String title = Html.fromHtml(extractData(MAIN_EXTRACTOR, htmlItem, 2)).toString();
		String detailsUrl = Html.fromHtml(extractData(MAIN_EXTRACTOR, htmlItem, 1)).toString();
		String torrentId = extractData(TORRENT_ID_EXTRACTOR, htmlItem);
		String size = extractData(SIZE_EXTRACTOR, htmlItem) + " GiB";
		int seeds = Integer.parseInt(extractData(SEEDS_EXTRACTOR, htmlItem));
		int leechers = Integer.parseInt(extractData(LEECHERS_EXTRACTOR, htmlItem));
		
		
		return new SearchResult(title, String.format(TORRENT_URL, torrentId), detailsUrl, size, null, seeds, leechers);

	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// Scambio Etico doesn't support RSS feed-based searches
		return null;
	}

	@Override
	public String getSiteName() {
		return "Scambio Etico";
	}

	@Override
	public boolean isPrivateSite() {
		// Not really private, still it requires registration.
		return true;
	}

	@Override
	public boolean usesToken() {
		return false;
	}

}

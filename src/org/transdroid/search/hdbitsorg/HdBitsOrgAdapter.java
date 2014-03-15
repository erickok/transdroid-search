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
package org.transdroid.search.hdbitsorg;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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

/**
 * An adapter that provides access to hdbits.org searches by parsing the raw
 * HTML output.
 * 
 * @author John Conrad
 */
public class HdBitsOrgAdapter implements ISearchAdapter {
	private static final String LOG_TAG = HdBitsOrgAdapter.class.getName();

	private static final String LOGIN_FORM_URL = "https://hdbits.org/login";
	private static final String LOGIN_URL = "https://hdbits.org/login/doLogin";

	// note java escaped quotes in regex string
	private static final String TOKEN_REGEX = "<input[^>]*name=\"lol\"[^>]*value=\"([^\"]+)\"[^>]*>"; 

	private static final String POST_USERNAME = "uname";
	private static final String POST_PASSWORD = "password";
	private static final String POST_TOKEN = "lol";

	// bleh
	private static final String QUERYURL = "http://www.iptorrents.com/torrents/?q=%1$s%2$s";
	private static final String SORT_COMPOSITE = "";
	private static final String SORT_SEEDS = ";o=seeders";
	private static final int CONNECTION_TIMEOUT = 8000;

	// =========================================================
	// ISearchAdapter   rewrite all this
	// =========================================================

	@Override
	public String getSiteName() {
		return "hdbits.org";
	}

	@Override
	public boolean isPrivateSite() {
		return true;
	}

	@Override
	public List<SearchResult> search(Context context, String query,
			SortOrder order, int maxResults) throws Exception {
		DefaultHttpClient httpclient = prepareRequest(context);

		// Build a search request parameters
		String encodedQuery = "";
		try {
			encodedQuery = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw e;
		}

		final String url = String.format(QUERYURL, encodedQuery,
				(order == SortOrder.BySeeders ? SORT_SEEDS : SORT_COMPOSITE));

		// Start synchronous search

		// Make request
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = httpclient.execute(httpget);

		// Read HTML response
		InputStream instream = response.getEntity().getContent();
		String html = HttpHelper.ConvertStreamToString(instream);
		instream.close();
		return parseHtml(html, maxResults);

	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// no rss based search for hdbits. there is a live RSS feed for all
		// torrents
		// on the site but it does not provide search capability
		return null;
	}

	@Override
	public InputStream getTorrentFile(Context context, String url)
			throws Exception {

		// Provide an authenticated file handle to the requested url
		DefaultHttpClient httpclient = prepareRequest(context);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		return response.getEntity().getContent();

	}

	// =========================================================
	// LOGIN LOGIC     done
	// =========================================================

	private DefaultHttpClient prepareRequest(Context context) throws Exception {
		Log.i(LOG_TAG, "preparing login attempt");

		// retrieve stored login info
		String username = SettingsHelper.getSiteUser(context,
				TorrentSite.HdBitsOrg);
		String password = SettingsHelper.getSitePass(context,
				TorrentSite.HdBitsOrg);

		// verify we have login credentials. does this ever get hit?
		if (username == null || password == null) {
			throw new InvalidParameterException(
					"No username or password was provided, while this is required for this private site.");
		}

		// setup our http client
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT);
		DefaultHttpClient client = new DefaultHttpClient(params);

		// grab our unique login token
		String token = grabToken(client);

		// login to hdbits populating the HttpClient with the required cookies
		login(client, username, password, token);
		Log.i(LOG_TAG, "login success?");
		return client;
	}

	/**
	 * Retrieves a hidden token from the hdbits.org login form.
	 */
	private String grabToken(DefaultHttpClient client) throws Exception {
		Log.i(LOG_TAG, "attempting to grab token");

		// grab html
		HttpGet post = new HttpGet(LOGIN_FORM_URL);
		HttpResponse response = client.execute(post);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			throw new Exception("Failed to retrieve hdbits.org login form.");

		// try to find the hidden parameter on the login form
		String html = HttpHelper.ConvertStreamToString(response.getEntity().getContent());
		Pattern tokenRegexParser = Pattern.compile(TOKEN_REGEX);
		Matcher match = tokenRegexParser.matcher(html);
		boolean success = match.find();
		if (!success)
			throw new Exception(
					"Unable to find hdbits.org login token. Has website HTML changed?");

		// success!
		String token = match.group(1);
		Log.i(LOG_TAG, "login token: " + token);
		return match.group(1);
	}

	/**
	 * Attempts to log in to hdbits.org with the given credentials. On success
	 * the given DefaultHttpClient should hold all required cookies to access
	 * the site.
	 */
	private void login(DefaultHttpClient client, String username,
			String password, String token) throws Exception {
		Log.i(LOG_TAG, "attempting to login");

		HttpPost request = new HttpPost(LOGIN_URL);
		request.setEntity(new UrlEncodedFormEntity(Arrays
				.asList(new BasicNameValuePair[] {
						new BasicNameValuePair(POST_USERNAME, username),
						new BasicNameValuePair(POST_PASSWORD, password),
						new BasicNameValuePair(POST_TOKEN, token),
						new BasicNameValuePair("returnto", "%2F") })));

		HttpResponse response = client.execute(request);

		Log.e(LOG_TAG, "login status: ");
		for (Header line : response.getAllHeaders()) {
			Log.i(LOG_TAG, line.toString());
		}

		Log.e(LOG_TAG, "client cookies: ");
		for (Cookie line : client.getCookieStore().getCookies()) {
			Log.e(LOG_TAG, line.toString());
		}

		/*
		 * // returns a 302 MOVED TEMPORARILY on success and a 200 OK with error
		 * text on failure if (response.getStatusLine().getStatusCode() !=
		 * HttpStatus.SC_MOVED_TEMPORARILY) { // login failure Log.e(LOG_TAG,
		 * "login failure: "); for(Cookie line:
		 * client.getCookieStore().getCookies()) { Log.e(LOG_TAG,
		 * line.toString()); }
		 * 
		 * throw new Exception("Login failure for " + getSiteName() +
		 * " with user " + username); }
		 */
	}

	// =========================================================
	// SEARCH LOGIC
	// =========================================================

	
	
	
	
	

	// =========================================================
	// OLD JUNK
	// =========================================================

	protected List<SearchResult> parseHtml(String html, int maxResults)
			throws Exception {

		try {

			// Texts to find subsequently
			final String RESULTS = "<table class=torrents align=center border=1>";
			final String NOTORRENTS = "No Torrents Found";
			final String TORRENT = "<tr><td class=t_label>";

			// Parse the search results from HTML by looking for the identifying
			// texts
			List<SearchResult> results = new ArrayList<SearchResult>();
			int resultsStart = html.indexOf(RESULTS) + RESULTS.length();
			if (html.indexOf(NOTORRENTS) >= 0)
				return results; // Success, but no results for this query

			int torStart = html.indexOf(TORRENT, resultsStart);
			while (torStart >= 0 && results.size() < maxResults) {
				int nextTorrentIndex = html.indexOf(TORRENT,
						torStart + TORRENT.length());
				if (nextTorrentIndex >= 0) {
					results.add(parseHtmlItem(html.substring(
							torStart + TORRENT.length(), nextTorrentIndex)));
				} else {
					results.add(parseHtmlItem(html.substring(torStart
							+ TORRENT.length())));
				}
				torStart = nextTorrentIndex;
			}
			return results;

		} catch (OutOfMemoryError e) {
			throw new Exception(e);
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	private SearchResult parseHtmlItem(String htmlItem) {

		// Texts to find subsequently
		final String DETAILS = "<a class=\"t_title\" href=\"";
		final String DETAILS_END = "\">";
		final String NAME_END = "</a>";
		final String LINK = "Bookmark it!\"></a></td><td class=ac><a href=\"";
		final String LINK_END = "\">";
		final String COMMENTS = "#startcomments";
		final String SIZE = "</a></td><td class=ac>";
		final String SIZE_END = "</td>";
		final String SEEDERS = "t_seeders\">";
		final String SEEDERS_END = "</td>";
		final String LEECHERS = "t_leechers\">";
		final String LEECHERS_END = "</td>";
		String prefix = "http://www.iptorrents.com";

		int detailsStart = htmlItem.indexOf(DETAILS) + DETAILS.length();
		String details = htmlItem.substring(detailsStart,
				htmlItem.indexOf(DETAILS_END, detailsStart));
		details = prefix + details;

		// Name starts right after the link of an item
		int nameStart = htmlItem.indexOf(DETAILS_END, detailsStart)
				+ DETAILS_END.length();
		String name = htmlItem.substring(nameStart,
				htmlItem.indexOf(NAME_END, nameStart));

		int linkStart = htmlItem.indexOf(LINK, nameStart) + LINK.length();
		String link = htmlItem.substring(linkStart,
				htmlItem.indexOf(LINK_END, linkStart));
		link = prefix + link;

		int commentsStart = htmlItem.indexOf(COMMENTS, linkStart)
				+ COMMENTS.length();

		int sizeStart = htmlItem.indexOf(SIZE, commentsStart) + SIZE.length();
		String size = htmlItem.substring(sizeStart,
				htmlItem.indexOf(SIZE_END, sizeStart));

		int seedersStart = htmlItem.indexOf(SEEDERS, sizeStart)
				+ SEEDERS.length();
		int seeders = 0;
		if (seedersStart >= 0) {
			try {
				String seedersText = htmlItem.substring(seedersStart,
						htmlItem.indexOf(SEEDERS_END, seedersStart));
				seeders = Integer.parseInt(seedersText);
			} catch (Exception e) {
				// Number of seeders not found; ignore
			}
		}

		int leechersStart = htmlItem.indexOf(LEECHERS, seedersStart)
				+ LEECHERS.length();
		int leechers = 0;
		if (leechersStart >= 0) {
			try {
				String leechersText = htmlItem.substring(leechersStart,
						htmlItem.indexOf(LEECHERS_END, leechersStart));
				leechers = Integer.parseInt(leechersText);
			} catch (Exception e) {
				// Number of seeders not found; ignore
			}
		}

		return new SearchResult(name, link, details, size, null, seeders,
				leechers);

	}
}

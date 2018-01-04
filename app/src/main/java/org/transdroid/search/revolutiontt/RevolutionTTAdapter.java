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
package org.transdroid.search.revolutiontt;

import android.content.SharedPreferences;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.gui.SettingsHelper;
import org.transdroid.util.HttpHelper;

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

/**
 * An adapter that provides access to RevolutionTT searches by parsing the raw HTML output of their mobile website.
 */
public class RevolutionTTAdapter implements ISearchAdapter {

	private static final String LOGINURL = "https://revolutiontt.me/takelogin.php";
	private static final String QUERYURL = "https://revolutiontt.me/browse.php?search=%1$s&cat=0&titleonly=1%2$s";
	private static final String SORT_COMPOSITE = "";
	private static final String SORT_SEEDS = "&sort=7&type=desc";

	private HttpClient prepareRequest(SharedPreferences prefs) throws Exception {

		String username = SettingsHelper.getSiteUser(prefs, TorrentSite.RevolutionTT);
		String password = SettingsHelper.getSitePass(prefs, TorrentSite.RevolutionTT);
		if (username == null || password == null) {
			throw new InvalidParameterException("No username or password was provided, while this is required for this private site.");
		}

		// Setup http client
		HttpClient httpclient = HttpHelper.buildDefaultSearchHttpClient(false);

		httpclient.execute(new HttpGet("https://revolutiontt.me/login.php"));

		// First log in
		HttpPost loginPost = new HttpPost(LOGINURL);
		loginPost.setEntity(new UrlEncodedFormEntity(
				Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("username", username), new BasicNameValuePair("password", password)})));
		HttpResponse loginResult = httpclient.execute(loginPost);
		String loginHtml = HttpHelper.convertStreamToString(loginResult.getEntity().getContent());
		final String LOGIN_ERROR = "Login failed!";
		if (loginResult.getStatusLine().getStatusCode() != HttpStatus.SC_OK || loginHtml.indexOf(LOGIN_ERROR) >= 0) {
			// Failed to sign in
			throw new LoginException("Login failure for RevolutionTT with user " + username);
		}

		return httpclient;

	}

	@Override
	public List<SearchResult> search(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws Exception {

		HttpClient httpclient = prepareRequest(prefs);

		// Build a search request parameters
		final String url = String.format(QUERYURL, URLEncoder.encode(query, "UTF-8"), (order == SortOrder.BySeeders ? SORT_SEEDS : SORT_COMPOSITE));

		// Start synchronous search
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = httpclient.execute(httpget);

		// Read HTML response
		InputStream instream = response.getEntity().getContent();
		String html = HttpHelper.convertStreamToString(instream);
		instream.close();
		return parseHtml(html, maxResults);

	}

	@Override
	public InputStream getTorrentFile(SharedPreferences prefs, String url) throws Exception {

		// Provide an authenticated file handle to the requested url
		HttpClient httpclient = prepareRequest(prefs);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		return response.getEntity().getContent();

	}

	protected List<SearchResult> parseHtml(String html, int maxResults) throws Exception {

		// Texts to find subsequently
		final String NOTORRENTS = "Nothing found!";
		final String RESULTS = "&nbsp;&nbsp;Name";
		final String TORRENT = "<tr>\n<td align=center ";

		// Parse the search results from HTML by looking for the identifying texts
		List<SearchResult> results = new ArrayList<>();
		if (html.contains(NOTORRENTS)) {
			return results; // Success, but no results for this query
		}

		int resultsStart = html.indexOf(RESULTS) + RESULTS.length();
		int torStart = html.indexOf(TORRENT, resultsStart);
		while (torStart >= 0 && results.size() < maxResults) {
			int nextTorrentIndex = html.indexOf(TORRENT, torStart + TORRENT.length());
			if (nextTorrentIndex >= 0) {
				results.add(parseHtmlItem(html.substring(torStart + TORRENT.length(), nextTorrentIndex)));
			} else {
				results.add(parseHtmlItem(html.substring(torStart + TORRENT.length())));
			}
			torStart = nextTorrentIndex;
		}
		return results;

	}

	private SearchResult parseHtmlItem(String htmlItem) {

		// Texts to find subsequently
		final String DETAILS = "br_right><a href=\"";
		final String DETAILS_END = "\"><b>";
		final String NAME_END = "</b>";
		final String LINK = "br_left'><a href=\"";
		final String LINK_END = "\">";
		final String DATE = "<nobr>";
		final String DATE_END = "</nobr>";
		final String SIZE = "nowrap>";
		final String SIZE_END = "<br>";
		final String SEEDERS = "color=#000000>";
		final String SEEDERS_END = "</font>";
		final String LEECHERS = "todlers=1>";
		final String LEECHERS_END = "</b>";
		String prefix = "https://revolutiontt.me/";
		final DateFormat parseDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		int detailsStart = htmlItem.indexOf(DETAILS) + DETAILS.length();
		String details = htmlItem.substring(detailsStart, htmlItem.indexOf(DETAILS_END, detailsStart));
		details = prefix + details;

		// Name starts right after the link of an item
		int nameStart = htmlItem.indexOf(DETAILS_END, detailsStart) + DETAILS_END.length();
		String name = htmlItem.substring(nameStart, htmlItem.indexOf(NAME_END, nameStart));

		int linkStart = htmlItem.indexOf(LINK, nameStart) + LINK.length();
		String link = htmlItem.substring(linkStart, htmlItem.indexOf(LINK_END, linkStart));
		link = prefix + link;

		int dateStart = htmlItem.indexOf(DATE, linkStart) + DATE.length();
		String dateString = htmlItem.substring(dateStart, htmlItem.indexOf(DATE_END, dateStart));
		Date date = null;
		try {
			date = parseDateFormat.parse(dateString.replace("<br />", " "));
		} catch (Exception e) {
			// Ignore; just leave date null
		}

		int sizeStart = htmlItem.indexOf(SIZE, dateStart) + SIZE.length();
		String size = htmlItem.substring(sizeStart, htmlItem.indexOf(SIZE_END, sizeStart));

		int seedersStart = htmlItem.indexOf(SEEDERS, sizeStart) + SEEDERS.length();
		int seeders = 0;
		if (seedersStart >= 0) {
			try {
				String seedersText = htmlItem.substring(seedersStart, htmlItem.indexOf(SEEDERS_END, seedersStart));
				seeders = Integer.parseInt(seedersText);
			} catch (Exception e) {
				// Number of seeders not found; ignore
			}
		}

		int leechersStart = htmlItem.indexOf(LEECHERS, seedersStart) + LEECHERS.length();
		int leechers = 0;
		if (leechersStart >= 0) {
			try {
				String leechersText = htmlItem.substring(leechersStart, htmlItem.indexOf(LEECHERS_END, leechersStart));
				leechers = Integer.parseInt(leechersText);
			} catch (Exception e) {
				// Number of seeders not found; ignore
			}
		}

		return new SearchResult(name, link, details, size, date, seeders, leechers);

	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// RevolutionTT doesn't support RSS feed-based searches
		return null;
	}

	@Override
	public String getSiteName() {
		return "RevolutionTT";
	}

	public AuthType getAuthType() {
		return AuthType.USERNAME;
	}

	public String[] getRequiredCookies() {
		return null;
	}

}

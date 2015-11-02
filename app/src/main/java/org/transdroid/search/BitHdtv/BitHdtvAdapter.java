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
package org.transdroid.search.BitHdtv;

import android.content.Context;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.security.auth.login.LoginException;

/**
 * An adapter that provides access to BitHDTV searches by parsing the raw HTML output.
 */
public class BitHdtvAdapter implements ISearchAdapter {

	private static final String LOGIN_USER = "username";
	private static final String LOGIN_PASS = "password";
	private static final String LOGINURL = "https://www.bit-hdtv.com/takelogin.php";
	private static final String QUERYURL = "https://www.bit-hdtv.com/torrents.php?search=%1$s&cat=0%2$s";
	private static final String SORT_COMPOSITE = "";
	private static final String SORT_SEEDS = "&sort=7&type=desc";
	private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

	private HttpClient prepareRequest(Context context) throws Exception {

		String username = SettingsHelper.getSiteUser(context, TorrentSite.BitHdtv);
		String password = SettingsHelper.getSitePass(context, TorrentSite.BitHdtv);
		if (username == null || password == null) {
			throw new InvalidParameterException("No username or password was provided, while this is required for this private site.");
		}

		// First log in
		HttpClient httpclient = HttpHelper.buildDefaultSearchHttpClient(false);
		HttpPost loginPost = new HttpPost(LOGINURL);
		loginPost.setEntity(new UrlEncodedFormEntity(
				Arrays.asList(new BasicNameValuePair(LOGIN_USER, username), new BasicNameValuePair(LOGIN_PASS, password))));
		HttpResponse loginResult = httpclient.execute(loginPost);
		if (loginResult.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			// Failed to sign in
			throw new LoginException("Login failure for BitHdTv with user " + username);
		}
		String loginHtml = HttpHelper.convertStreamToString(loginResult.getEntity().getContent());
		final String LOGIN_ERROR = "Login failed!";
		if (loginHtml == null || loginHtml.contains(LOGIN_ERROR)) {
			// Failed to sign in
			throw new LoginException("Login failure for BitHdTv with user " + username);
		}

		return httpclient;

	}

	@Override
	public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {

		HttpClient httpclient = prepareRequest(context);

		final String url = String.format(QUERYURL, URLEncoder.encode(query, "UTF-8"), (order == SortOrder.BySeeders ? SORT_SEEDS : SORT_COMPOSITE));

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
		HttpClient httpclient = prepareRequest(context);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		return response.getEntity().getContent();

	}

	protected List<SearchResult> parseHtml(String html, int maxResults) throws Exception {

		// Texts to find subsequently
		final String NOMATCH = "No match!";
		final String RESULTS = "<!-- uj rendezes kezdodik -->";
		final String TORRENT = "<td class=detail align=center><p><a href='";

		// Parse the search results from HTML by looking for the identifying texts
		List<SearchResult> results = new ArrayList<>();
		if (html.contains(NOMATCH)) {
			return results; // Success, but no result for this query
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
		final String LINK_END = "'><img src=/pic/dwnld.gif";
		final String NAME = "<a title=\"";
		final String NAME_END = "\" href=\"";
		final String DETAILS = "\" href=\"/";
		final String DETAILS_END = "\">";
		final String DATE = "<td class=detail align=center>";
		final String DATE_END = "</td>";
		final String SIZE = "<td class=detail align=center>";
		final String SIZE_END = "</td>";
		final String SEEDERS = "#seeders\">";
		final String SEEDERS_END = "</a>";
		final String LEECHERS = "#leechers\">";
		final String LEECHERS_END = "</a>";
		String prefix = "http://www.bit-hdtv.com/";

		// Link starts right at the beginning of an item
		String link = htmlItem.substring(0, htmlItem.indexOf(LINK_END));

		int nameStart = htmlItem.indexOf(NAME, 0) + NAME.length();
		String name = htmlItem.substring(nameStart, htmlItem.indexOf(NAME_END, nameStart));

		int detailsStart = htmlItem.indexOf(DETAILS, nameStart) + DETAILS.length();
		String details = htmlItem.substring(detailsStart, htmlItem.indexOf(DETAILS_END, detailsStart));
		details = prefix + details;

		int dateStart = htmlItem.indexOf(DATE, detailsStart) + DATE.length();
		String dateText = htmlItem.substring(dateStart, htmlItem.indexOf(DATE_END, dateStart));
		Date date = null;
		try {
			date = df.parse(dateText);
		} catch (ParseException e) {
			// Not parsable; just leave it at null
		}

		int sizeStart = htmlItem.indexOf(SIZE, dateStart) + SIZE.length();
		String size = htmlItem.substring(sizeStart, htmlItem.indexOf(SIZE_END, sizeStart));
		size = size.replace("<br>", "");

		int seedersStart = htmlItem.indexOf(SEEDERS, dateStart);
		int seeders = 0;
		if (seedersStart >= 0) {
			seedersStart += SEEDERS.length();
			String seedersText = htmlItem.substring(seedersStart, htmlItem.indexOf(SEEDERS_END, seedersStart));
			seeders = Integer.parseInt(seedersText);
		}

		int leechersStart = htmlItem.indexOf(LEECHERS, dateStart);
		int leechers = 0;
		if (leechersStart >= 0) {
			leechersStart += LEECHERS.length();
			String leechersText = htmlItem.substring(leechersStart, htmlItem.indexOf(LEECHERS_END, leechersStart));
			leechers = Integer.parseInt(leechersText);
		}

		return new SearchResult(name, link, details, size, date, seeders, leechers);

	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// BIT-HDTV doesn't support RSS feed-based searches
		return null;
	}

	@Override
	public String getSiteName() {
		return "BIT-HDTV";
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

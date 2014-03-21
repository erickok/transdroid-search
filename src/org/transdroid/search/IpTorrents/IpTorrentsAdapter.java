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
package org.transdroid.search.IpTorrents;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.gui.SettingsHelper;
import org.transdroid.util.HttpHelper;

import android.content.Context;

/**
 * An adapter that provides access to IPTorrents searches by parsing the raw HTML output.
 */
public class IpTorrentsAdapter implements ISearchAdapter {

	private static final String LOGIN_USER = "username";
	private static final String LOGIN_PASS = "password";
	private static final String LOGINURL = "http://www.iptorrents.com/torrents/";
	private static final String QUERYURL = "http://www.iptorrents.com/torrents/?q=%1$s%2$s";
	private static final String SORT_COMPOSITE = "";
	private static final String SORT_SEEDS = ";o=seeders";
	private static final int CONNECTION_TIMEOUT = 8000;

	private DefaultHttpClient prepareRequest(Context context) throws Exception {

		String username = SettingsHelper.getSiteUser(context, TorrentSite.IpTorrents);
		String password = SettingsHelper.getSitePass(context, TorrentSite.IpTorrents);
		if (username == null || password == null) {
			throw new InvalidParameterException(
					"No username or password was provided, while this is required for this private site.");
		}

		// Setup request using GET
		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT);
		DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);

		// First log in
		HttpPost loginPost = new HttpPost(LOGINURL);
		loginPost.setEntity(new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair[] {
				new BasicNameValuePair(LOGIN_USER, username), new BasicNameValuePair(LOGIN_PASS, password) })));
		HttpResponse loginResult = httpclient.execute(loginPost);
		if (loginResult.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			// Failed to sign in
			throw new LoginException("Login failure for IPTorrents with user " + username);
		}

		return httpclient;

	}

	@Override
	public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {

		DefaultHttpClient httpclient = prepareRequest(context);

		// Build a search request parameters
		String encodedQuery = "";
		try {
			encodedQuery = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw e;
		}

		final String url = String.format(QUERYURL, encodedQuery, (order == SortOrder.BySeeders ? SORT_SEEDS
				: SORT_COMPOSITE));

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
	public InputStream getTorrentFile(Context context, String url) throws Exception {

		// Provide an authenticated file handle to the requested url
		DefaultHttpClient httpclient = prepareRequest(context);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		return response.getEntity().getContent();

	}

	protected List<SearchResult> parseHtml(String html, int maxResults) throws Exception {

		try {

			// Texts to find subsequently
			final String RESULTS = "<table class=torrents align=center border=1>";
			final String NOTORRENTS = "No Torrents Found";
			final String TORRENT = "<tr><td class=t_label>";

			// Parse the search results from HTML by looking for the identifying texts
			List<SearchResult> results = new ArrayList<SearchResult>();
			int resultsStart = html.indexOf(RESULTS) + RESULTS.length();
			if (html.indexOf(NOTORRENTS) >= 0)
				return results; // Success, but no results for this query

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
		String details = htmlItem.substring(detailsStart, htmlItem.indexOf(DETAILS_END, detailsStart));
		details = prefix + details;

		// Name starts right after the link of an item
		int nameStart = htmlItem.indexOf(DETAILS_END, detailsStart) + DETAILS_END.length();
		String name = htmlItem.substring(nameStart, htmlItem.indexOf(NAME_END, nameStart));

		int linkStart = htmlItem.indexOf(LINK, nameStart) + LINK.length();
		String link = htmlItem.substring(linkStart, htmlItem.indexOf(LINK_END, linkStart));
		link = prefix + link;

		int commentsStart = htmlItem.indexOf(COMMENTS, linkStart) + COMMENTS.length();

		int sizeStart = htmlItem.indexOf(SIZE, commentsStart) + SIZE.length();
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

		return new SearchResult(name, link, details, size, null, seeders, leechers);

	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// IPTorrents doesn't support RSS feed-based searches
		return null;
	}

	@Override
	public String getSiteName() {
		return "IPTorrents";
	}

	@Override
	public boolean isPrivateSite() {
		return true;
	}

}

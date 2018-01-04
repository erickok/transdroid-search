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
package org.transdroid.search.ThePirateBay;

import java.io.InputStream;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.util.HttpHelper;

import android.content.SharedPreferences;

/**
 * An adapter that provides access to The Pirate Bay torrent searches by parsing
 * the raw HTML output.
 *
 * @author Eric Kok
 */
public class ThePirateBayAdapter implements ISearchAdapter {

	private static final String DOMAIN = "https://pirateproxy.cc";
	private static final String QUERYURL = DOMAIN + "/search/%1$s/0/%2$s/0";
	private static final String SORT_COMPOSITE = "3";
	private static final String SORT_SEEDS = "7";
	private static final int CONNECTION_TIMEOUT = 20000;

	@Override
	public List<SearchResult> search(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws Exception {

		if (query == null) {
			return null;
		}

		// Build full URL string
		final String url = String.format(QUERYURL, URLEncoder.encode(query, "UTF-8"), (order == SortOrder.BySeeders? SORT_SEEDS: SORT_COMPOSITE));

		// Start synchronous search

        // Setup request using GET
        HttpClient httpclient = HttpHelper.buildDefaultSearchHttpClient(false);
        // Spoof Firefox user agent to force a result from The Pirate Bay
        httpclient.getParams().setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        HttpGet httpget = new HttpGet(url);

        // Make request
        HttpResponse response = httpclient.execute(httpget);

        // Read HTML response
        InputStream instream = response.getEntity().getContent();
        String html = HttpHelper.convertStreamToString(instream);
        instream.close();
        return parseHtml(html);

	}

	@Override
	public InputStream getTorrentFile(SharedPreferences prefs, String url) throws Exception {

		// Provide a simple file handle to the requested url
		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT);
		DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		return response.getEntity().getContent();

	}

	protected List<SearchResult> parseHtml(String html) throws Exception {

		// Texts to find subsequently
		final String RESULTS = "<table id=\"searchResult\">";
		final String TORRENT = "<div class=\"detName\">";

		// Parse the search results from HTML by looking for the identifying texts
		List<SearchResult> results = new ArrayList<>();
		int resultsStart = html.indexOf(RESULTS)+ RESULTS.length();

		int torStart = html.indexOf(TORRENT,resultsStart);
		while (torStart >= 0) {
			int nextTorrentIndex = html.indexOf(TORRENT,torStart + TORRENT.length());
			if(nextTorrentIndex>=0) {
				results.add(parseHtmlItem(html.substring(torStart + TORRENT.length(), nextTorrentIndex)));
			} else {
				results.add(parseHtmlItem(html.substring(torStart + TORRENT.length())));
			}
			torStart = nextTorrentIndex;
		}
		return results;

	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// The Pirate Bay doesn't support RSS feeds
		return null;
	}

	@Override
	public String getSiteName() {
		return "The Pirate Bay (proxied)";
	}

	private SearchResult parseHtmlItem(String htmlItem) {

		// Texts to find subsequently
		final String DETAILS = "<a href=\"";
		final String DETAILS_END = "\" class=\"detLink\"";
		final String NAME = "\">";
		final String NAME_END = "</a>";
		final String MAGNET_LINK = "<a href=\"";
		final String MAGNET_LINK_END = "\" title=\"Download this torrent using magnet";
		final String DATE = "detDesc\">Uploaded ";
		final String DATE_END = ", Size ";
		final String SIZE = ", Size ";
		final String SIZE_END = ", ULed by";
		final String SEEDERS = "<td align=\"right\">";
		final String SEEDERS_END = "</td>";
		final String LEECHERS = "<td align=\"right\">";
		final String LEECHERS_END = "</td>";
		String prefixDetails = DOMAIN;
		String prefixYear = (new Date().getYear() + 1900) + " "; // Date.getYear() gives the current year - 1900
		SimpleDateFormat df1 = new SimpleDateFormat("yyyy MM-dd HH:mm", Locale.US);
		SimpleDateFormat df2 = new SimpleDateFormat("MM-dd yyyy", Locale.US);

		int detailsStart = htmlItem.indexOf(DETAILS) + DETAILS.length();
		String details = htmlItem.substring(detailsStart, htmlItem.indexOf(DETAILS_END, detailsStart));
		details = prefixDetails + details;
		int nameStart = htmlItem.indexOf(NAME, detailsStart) + NAME.length();
		String name = htmlItem.substring(nameStart, htmlItem.indexOf(NAME_END, nameStart));

		// Magnet link is first
		int magnetLinkStart = htmlItem.indexOf(MAGNET_LINK, nameStart) + MAGNET_LINK.length();
		String magnetLink = htmlItem.substring(magnetLinkStart, htmlItem.indexOf(MAGNET_LINK_END, magnetLinkStart));

		int dateStart = htmlItem.indexOf(DATE, magnetLinkStart) + DATE.length();
		String dateText = htmlItem.substring(dateStart, htmlItem.indexOf(DATE_END, dateStart));
		dateText = dateText.replace("&nbsp;", " ");
		Date date = null;
		if (dateText.startsWith("Today")) {
			date = new Date();
		} else if (dateText.startsWith("Y-day")) {
			date = new Date(new Date().getTime() - 86400000L);
		} else {
			try {
				date = df1.parse(prefixYear + dateText);
			} catch (ParseException e) {
				try {
					date = df2.parse(dateText);
				} catch (ParseException e1) {
					// Not parsable at all; just leave it at null
				}
			}
		}
		int sizeStart = htmlItem.indexOf(SIZE, dateStart) + SIZE.length();
		String size = htmlItem.substring(sizeStart, htmlItem.indexOf(SIZE_END, sizeStart));
		size = size.replace("&nbsp;", " ");
		int seedersStart = htmlItem.indexOf(SEEDERS, sizeStart) + SEEDERS.length();
		String seedersText = htmlItem.substring(seedersStart, htmlItem.indexOf(SEEDERS_END, seedersStart));
		int seeders = Integer.parseInt(seedersText);
		int leechersStart = htmlItem.indexOf(LEECHERS, seedersStart) + LEECHERS.length();
		String leechersText = htmlItem.substring(leechersStart, htmlItem.indexOf(LEECHERS_END, leechersStart));
		int leechers = Integer.parseInt(leechersText);

		return new SearchResult(name, magnetLink, details, size, date, seeders, leechers);
	}

	public AuthType getAuthType() {
		return AuthType.NONE;
	}

	public String[] getRequiredCookies() {
		return null;
	}

}

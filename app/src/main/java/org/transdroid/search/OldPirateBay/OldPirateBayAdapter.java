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
package org.transdroid.search.OldPirateBay;

import android.content.Context;

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

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * An adapter that provides access to Old Pirate Bay torrent searches by parsing the raw HTML output.
 * @author Eric Kok
 */
public class OldPirateBayAdapter implements ISearchAdapter {

	private static final String QUERYURL = "https://oldpiratebay.org/search.php?q=%1$s%2$s";
	private static final String SORT_COMPOSITE = "";
	private static final String SORT_SEEDS = "&Torrent_sort=seeders.desc";

	@Override
	public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {

		// Build full URL string
		final String url = String.format(QUERYURL, URLEncoder.encode(query, "UTF-8"), (order == SortOrder.BySeeders ? SORT_SEEDS : SORT_COMPOSITE));

		// Start synchronous search
		HttpClient httpclient = HttpHelper.buildDefaultSearchHttpClient(true); // Ignore SSL certificate to overcome Cloudfare protection
		HttpResponse response = httpclient.execute(new HttpGet(url));

		// Read HTML response
		InputStream instream = response.getEntity().getContent();
		String html = HttpHelper.convertStreamToString(instream);
		instream.close();
		return parseHtml(html);

	}

	@Override
	public InputStream getTorrentFile(Context context, String url) throws Exception {

		// Provide a simple file handle to the requested url
		HttpClient httpclient = HttpHelper.buildDefaultSearchHttpClient(true);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		return response.getEntity().getContent();

	}

	protected List<SearchResult> parseHtml(String html) throws Exception {

		// Texts to find subsequently
		final String RESULTS = "<table class=\"table-torrents";
		final String TORRENT = "<td class=\"title-row\">";

		// Parse the search results from HTML by looking for the identifying texts
		List<SearchResult> results = new ArrayList<>();
		int resultsStart = html.indexOf(RESULTS) + RESULTS.length();

		int torStart = html.indexOf(TORRENT, resultsStart);
		while (torStart >= 0) {
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

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// The Pirate Bay doesn't support RSS feeds
		return null;
	}

	@Override
	public String getSiteName() {
		return "Old Pirate Bay";
	}

	private SearchResult parseHtmlItem(String htmlItem) {

		// Texts to find subsequently
		final String MAGNET_LINK = "<a href='";
		final String MAGNET_LINK_END = "' title='MAGNET LINK'";
		final String DETAILS = "<a href=\"";
		final String DETAILS_END = "\">";
		final String NAME = "<span>";
		final String NAME_END = "</span>";
		final String SIZE = "size-row\">";
		final String SIZE_END = "</td>";
		final String SEEDERS = "seeders-row sy\">";
		final String SEEDERS_END = "</td>";
		final String LEECHERS = "leechers-row ly\">";
		final String LEECHERS_END = "</td>";
		String prefixDetails = "https://oldpiratebay.org";

		int magnetLinkStart = htmlItem.indexOf(MAGNET_LINK) + MAGNET_LINK.length();
		int magnetLinkEnd = htmlItem.indexOf(MAGNET_LINK_END, magnetLinkStart);
		String magnetLink = htmlItem.substring(magnetLinkStart, magnetLinkEnd);

		int detailsStart = htmlItem.indexOf(DETAILS, magnetLinkEnd) + DETAILS.length();
		int detailsEnd = htmlItem.indexOf(DETAILS_END, detailsStart);
		String details = htmlItem.substring(detailsStart, detailsEnd);
		details = prefixDetails + details;

		int nameStart = htmlItem.indexOf(NAME, detailsEnd) + NAME.length();
		int nameEnd = htmlItem.indexOf(NAME_END, nameStart);
		String name = htmlItem.substring(nameStart, nameEnd);

		int sizeStart = htmlItem.indexOf(SIZE, nameEnd) + SIZE.length();
		int sizeEnd = htmlItem.indexOf(SIZE_END, sizeStart);
		String size = htmlItem.substring(sizeStart, sizeEnd);

		int seedersStart = htmlItem.indexOf(SEEDERS, sizeStart) + SEEDERS.length();
		int seedersEnd = htmlItem.indexOf(SEEDERS_END, seedersStart);
		String seedersText = htmlItem.substring(seedersStart, seedersEnd);
		int seeders = Integer.parseInt(seedersText);

		int leechersStart = htmlItem.indexOf(LEECHERS, seedersStart) + LEECHERS.length();
		int leechersEnd = htmlItem.indexOf(LEECHERS_END, leechersStart);
		String leechersText = htmlItem.substring(leechersStart, leechersEnd);
		int leechers = Integer.parseInt(leechersText);

		return new SearchResult(name, magnetLink, details, size, null, seeders, leechers);
	}

	@Override
	public boolean isPrivateSite() {
		return false;
	}

}

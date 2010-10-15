/*
 *	This file is part of Transdroid Torrent Search 
 *	<http://code.google.com/p/transdroid-search/>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
package org.transdroid.search.ThePirateBay;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.util.HttpHelper;

/**
 * An adapter that provides access to The Pirate Bay torrent searches by parsing
 * the raw HTML output.
 * 
 * @author Eric Kok
 */
public class ThePirateBayAdapter implements ISearchAdapter {

	private static final String QUERYURL = "http://thepiratebay.org/search/%s/%s/%s/100,200,300,400,600/";
	private static final String SORT_COMPOSITE = "99";
	private static final String SORT_SEEDS = "7";
	private static final int CONNECTION_TIMEOUT = 10000;

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
		final int startAt = 0; // In the future, this would allow for paged results parsing
		final int pageNr = (startAt - 1) / 30; // 30 results per page, startAt is 1-based (not 0-based)
		final String url = String.format(QUERYURL, encodedQuery, String.valueOf(pageNr), (order == SortOrder.BySeeders? SORT_SEEDS: SORT_COMPOSITE));
		
		// Start synchronous search

        // Setup request using GET
        HttpParams httpparams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT); 
        DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);
        HttpGet httpget = new HttpGet(url);
        
        // Make request
        HttpResponse response = httpclient.execute(httpget);

        // Read HTML response
        InputStream instream = response.getEntity().getContent();
        String html = HttpHelper.ConvertStreamToString(instream);
        instream.close();
        return parseHtml(html);
        
	}

	protected List<SearchResult> parseHtml(String html) throws Exception {
		
		try {
			
			// Texts to find subsequently
			final String RESULTS = "<table id=\"searchResult\">";
			final String TORRENT = "<div class=\"detName\">";
			final String DETAILS = "<a href=\"";
			final String DETAILS_END = "\" class=\"detLink\"";
			final String NAME = "\">";
			final String NAME_END = "</a></div>";
			final String LINK = "<a href=\"";
			final String LINK_END = "\" title=\"";
			final String DATE = "detDesc\">Uploaded ";
			final String DATE_END = ", Size ";
			final String SIZE = ", Size ";
			final String SIZE_END = ", ULed by";
			final String SEEDERS = "<td align=\"right\">";
			final String SEEDERS_END = "</td>";
			final String LEECHERS = "<td align=\"right\">";
			final String LEECHERS_END = "</td>";
			String prefixDetails = "http://thepiratebay.org";
			String prefixYear = (new Date().getYear() + 1900) + " "; // Date.getYear() gives the current year - 1900
			SimpleDateFormat df1 = new SimpleDateFormat("yyyy MM-dd HH:mm");
			SimpleDateFormat df2 = new SimpleDateFormat("MM-dd yyyy");
			
			// Parse the search results from HTML by looking for the identifying texts
			List<SearchResult> results = new ArrayList<SearchResult>();
			int resultsStart = html.indexOf(RESULTS);
			int torStart = html.indexOf(TORRENT, resultsStart);
			while (torStart >= 0) {
	
				int detailsStart = html.indexOf(DETAILS, torStart) + DETAILS.length();
				String details = html.substring(detailsStart, html.indexOf(DETAILS_END, detailsStart));
				details = prefixDetails + details;
				int nameStart = html.indexOf(NAME, detailsStart) + NAME.length();
				String name = html.substring(nameStart, html.indexOf(NAME_END, nameStart));
				int linkStart = html.indexOf(LINK, nameStart) + LINK.length();
				String link = html.substring(linkStart, html.indexOf(LINK_END, linkStart));
				int dateStart = html.indexOf(DATE, linkStart) + DATE.length();
				String dateText = html.substring(dateStart, html.indexOf(DATE_END, dateStart));
				dateText = dateText.replace("&nbsp;", " ");
				Date date = null;
				try {
					date = df1.parse(prefixYear + dateText);
				} catch (ParseException e) {
					try {
						date = df2.parse(dateText);
					} catch (ParseException e1) {
						// Not parsable at all; just leave it at null
					}
				}
				int sizeStart = html.indexOf(SIZE, dateStart) + SIZE.length();
				String size = html.substring(sizeStart, html.indexOf(SIZE_END, sizeStart));
				size = size.replace("&nbsp;", " ");
				int seedersStart = html.indexOf(SEEDERS, sizeStart) + SEEDERS.length();
				String seedersText = html.substring(seedersStart, html.indexOf(SEEDERS_END, seedersStart));
				int seeders = Integer.parseInt(seedersText);
				int leechersStart = html.indexOf(LEECHERS, seedersStart) + LEECHERS.length();
				String leechersText = html.substring(leechersStart, html.indexOf(LEECHERS_END, leechersStart));
				int leechers = Integer.parseInt(leechersText);
				
				results.add(new SearchResult(name, link, details, size, date, seeders, leechers));
				
				// Find new torrent
				torStart = html.indexOf(TORRENT, leechersStart);
			}
			return results;
			
		} catch (OutOfMemoryError e) {
			throw new Exception(e);
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// The Pirate Bay doesn't support RSS feeds
		return null;
	}

}

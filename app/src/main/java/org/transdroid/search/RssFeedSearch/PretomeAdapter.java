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
package org.transdroid.search.RssFeedSearch;

import android.content.SharedPreferences;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.gui.SettingsHelper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Search adapter for the Pretome private torrent site (based on custom search RSS feeds). Requires user to enter their RSS feed token.
 * @author Eric Kok
 */
public class PretomeAdapter extends RssFeedSearchAdapter {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

	private SharedPreferences prefs;

	@Override
	public List<SearchResult> search(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws Exception {
		this.prefs = prefs;
		return super.search(prefs, query, order, maxResults);
	}

	@Override
	protected String getUrl(String query, SortOrder order) {
		String token = SettingsHelper.getSiteToken(prefs, TorrentSite.Pretome);
		if (token == null) {
			throw new InvalidParameterException("No RSS feed token was provided, while this is required for this private site.");
		}

		// NOTE: Torrent Reactor doesn't support sorting in the RSS feed
		try {
			return String.format(Locale.US, "https://pretome.info/rss.php?st=1&tf=all&search=%2$s&sort=%3$s&type=d&key=%1$s&full",
					token, URLEncoder.encode(query, "UTF-8"), order == SortOrder.BySeeders ? "7" : "0");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	protected SearchResult fromRssItemToSearchResult(Item item) {
		// Size
		String d = item.getDescription();
		int sizeStart = d.indexOf("Size: ") + "Size: ".length();
		String size = d.substring(sizeStart, d.indexOf("(", sizeStart));
		// Date
		int dateStart = d.indexOf("Added: ", sizeStart) + "Added: ".length();
		String dateString = d.substring(dateStart, d.indexOf(" (", dateStart));
		Date date = null;
		try {
			date = DATE_FORMAT.parse(dateString);
		} catch (ParseException e) {
			// Leave date null
		}
		// Torrent link
		int idStart = item.getLink().indexOf("?id=") + "?id=".length();
		String id = item.getLink().substring(idStart, item.getLink().indexOf("&", idStart));
		String torrentLink = String.format(Locale.US, "https://pretome.info/download.php/%1$s/%2$s.torrent", id, item.getTitle());

		// NOTE Pretome does not report seeders/leechers in RSS feeds
		return new SearchResult(
				item.getTitle(),
				torrentLink,
				item.getLink(),
				size,
				date,
				0,
				0);
	}

	@Override
	public String getSiteName() {
		return "Pretome";
	}

	@Override
	public boolean isPrivateSite() {
		return true;
	}

	@Override
	public boolean usesToken() {
		return true;
	}

	@Override
	protected RssParser getRssParser(final String url) {
		return new RssParser(url) {
			@Override
			public void parse() throws ParserConfigurationException, SAXException, IOException {
				HttpClient httpclient = initialise();
				HttpResponse result = httpclient.execute(new HttpGet(url));
				//FileInputStream urlInputStream = new FileInputStream("/sdcard/rsstest2.txt");
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				InputSource is = new InputSource();
				// Pretome supplies UTF-8 compatible character data yet incorrectly defined a windows-1251 encode: override
				is.setEncoding("UTF-8");
				is.setCharacterStream(new InputStreamReader(result.getEntity().getContent()));
				sp.parse(is, this);
			}
		};
	}

}

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
package org.transdroid.search.adapters.rss.privatetrackers;

import android.content.SharedPreferences;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.adapters.rss.RssFeedSearchAdapter;
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
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Search adapter for the Pretome private torrent site (based on custom search RSS feeds). Requires user to enter their RSS feed token.
 *
 * @author Eric Kok
 */
public class IPTorrentsAdapter extends RssFeedSearchAdapter {

    // String.format(FORMATTED_URI, uid, token, query);
    private static String FORMATTED_URI = "https://iptorrents.com/t.rss?u=%s;tp=%s;48;20;100;101;68;22;99;5;65;download;q=%s";

    // Fri, 20 Aug 2021 21:59:14 +0000
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    private SharedPreferences prefs;

    @Override
    public List<SearchResult> search(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws Exception {
        this.prefs = prefs;
        return super.search(prefs, query, order, maxResults);
    }

    @Override
    public String buildRssFeedUrlFromSearch(SharedPreferences prefs, String query, SortOrder order) {
        this.prefs = prefs;
        return super.buildRssFeedUrlFromSearch(prefs, query, order);
    }

    @Override
    protected String getUrl(String query, SortOrder order) {
        String token = SettingsHelper.getSiteToken(prefs, TorrentSite.IPTorrents);
        String uid = SettingsHelper.getSiteUserId(prefs, TorrentSite.IPTorrents);
        if (token == null || uid == null) {
            throw new InvalidParameterException("RSS feed token and user id is required for IPTorrents.");
        }

        try {
            return String.format(Locale.US, FORMATTED_URI, uid, token, URLEncoder.encode(query, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Override
    protected SearchResult fromRssItemToSearchResult(Item item) {

        /*
         *   <item>
         *   <title>full title</title>
         *   <link>https://...</link>
         *   <pubDate>Fri, 20 Aug 2021 21:59:14 +0000</pubDate>
         *   <description>291 MB; TV/x265</description>
         *   </item>
         */
        return new SearchResult(
                item.getTitle(),
                item.getLink(),
                item.getLink(),
                item.getDescription(),
                item.getPubdate(),
                0,
                0);
    }

    @Override
    public String getSiteName() {
        return "IPTorrents";
    }

    public AuthType getAuthType() {
        return AuthType.TOKEN_AND_UID;
    }

    @Override
    protected RssParser getRssParser(final String url) {
        return new RssParser(url);
    }

}

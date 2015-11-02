/*
 *    This file is part of Transdroid Torrent Search 
 *    <http://code.google.com/p/transdroid-search/>
 *    
 *    Transdroid Torrent Search is free software: you can redistribute 
 *    it and/or modify it under the terms of the GNU Lesser General 
 *    Public License as published by the Free Software Foundation, 
 *    either version 3 of the License, or (at your option) any later 
 *    version.
 *    
 *    Transdroid Torrent Search is distributed in the hope that it will 
 *    be useful, but WITHOUT ANY WARRANTY; without even the implied 
 *    warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 *    See the GNU Lesser General Public License for more details.
 *    
 *    You should have received a copy of the GNU Lesser General Public 
 *    License along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.search.hdtorrents;

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

import org.apache.http.HttpResponse;
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
 * An adapter that provides access to HD-Torrents.org searches by parsing the raw HTML output.
 */
public class HdTorrentsAdapter implements ISearchAdapter {
    private static final String LOG_TAG = HdTorrentsAdapter.class.getName();

    private static final String LOGIN_URL = "https://hd-torrents.org/login.php";

    private static final String LOGIN_POST_USERNAME = "uid";
    private static final String LOGIN_POST_PASSWORD = "pwd";

    private static final String SEARCH_URL = "https://hd-torrents.org/torrents.php?search=%s&active=1&options=0";
    private static final String SEARCH_SORT_BY_SEEDERS_SUFFIX = "&order=seeds&by=DESC";

    private static final String START_STRING = "nd();\">";
    private static final String END_STRING = "<a href=\"torrent_history.php?";
    private static final String TORRENT_STRING = ".torrent";
    private static final String DATE_START_SEARCH_STRING = "add_wishlist_star.png border=0  alt=\"torrent\"/></A></TD>";
    private static final String DATE_START_STRING = "<b";
    private static final String DATE_END_STRING = "><b";
    private static final String SIZE_START_STRING = "mainblockcontent\">";
    private static final String SIZE_END_STRING = "</td>";
    private static final String PEER_START_STRING = "Click here to view peers details\"><b>";
    private static final String PEER_END_STRING = "</b>";
    private static final String IMDB_START_STRING = "http://www.imdb.com/";
    private static final String IMDB_END_STRING = "\"";
    
    private static final String URL_PREFIX = "https://hd-torrents.org/";
    private static final int CONNECTION_TIMEOUT = 8000;

    // =========================================================
    // ISearchAdapter
    // =========================================================

    @Override
    public String getSiteName() {
        return "HD-Torrents";
    }

    @Override
    public boolean isPrivateSite() {
        return true;
    }

    @Override
    public boolean usesToken() {
        return false;
    }

    @Override
    public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {
        
        DefaultHttpClient client = prepareRequest(context);

        // build search query
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String url = String.format(SEARCH_URL, encodedQuery);
//        if (order == SortOrder.BySeeders) url += SEARCH_SORT_BY_SEEDERS_SUFFIX;

        // make request
        Log.d(LOG_TAG, "Executing search request from: " + url);
        HttpResponse response = client.execute(new HttpGet(url));

        // parse HTML response into a list of torrents
        String html = HttpHelper.convertStreamToString(response.getEntity().getContent());
        return parseHtml(html, maxResults);

    }

    @Override
    public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
        // no rss based search for HD-Torrents.org. there is a live RSS feed for all
        // torrents on the site but it does not provide search capability
        return null;
    }

    @Override
    public InputStream getTorrentFile(Context context, String url) throws Exception {
        // Provide an authenticated file handle to the requested url
        DefaultHttpClient client = prepareRequest(context);
        HttpResponse response = client.execute(new HttpGet(url));
        return response.getEntity().getContent();

    }

    // =========================================================
    // LOGIN LOGIC
    // =========================================================

    private DefaultHttpClient prepareRequest(Context context) throws Exception {
        Log.d(LOG_TAG, "Preparing login attempt.");

        // retrieve stored login info
        String username = SettingsHelper.getSiteUser(context, TorrentSite.HdTorrents);
        String password = SettingsHelper.getSitePass(context, TorrentSite.HdTorrents);

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

        // login to HD-Torrents.org populating the HttpClient with the required cookies
        login(client, username, password);
        return client;
    }

    /**
     * Attempts to log in to HD-Torrents.org with the given credentials. On success
     * the given DefaultHttpClient should hold all required cookies to access
     * the site.
     */
    private void login(DefaultHttpClient client, String username, String password) throws Exception {
        Log.d(LOG_TAG, "Attempting to login.");

        HttpPost request = new HttpPost(LOGIN_URL);
        request.setEntity(new UrlEncodedFormEntity(Arrays
                .asList(new BasicNameValuePair[] {
                        new BasicNameValuePair(LOGIN_POST_USERNAME, username),
                        new BasicNameValuePair(LOGIN_POST_PASSWORD, password) })));

        client.execute(request);

        // verify we have the cookies needed to log in
        boolean success = false, uid = false, pass = false, hash = false;
        for (Cookie cookie : client.getCookieStore().getCookies()) {
            if ("uid".equals(cookie.getName())) uid = true;
            if ("pass".equals(cookie.getName())) pass = true;
            if ("hashx".equals(cookie.getName())) hash = true;
        }
        
        // if we don't have the correct cookies, login failed. notify user with a toast and toss an exception.
        success = uid && pass && hash;
        if (!success) {
        	Log.e(LOG_TAG, "Failed to log into HD-Torrents as '" + username + "'. Did not receive expected login cookies!");
            throw new LoginException("Failed to log into HD-Torrents as '" + username + "'. Did not receive expected login cookies!");
        }
        
        Log.d(LOG_TAG, "Successfully logged in to HD-Torrents");
    }

    // =========================================================
    // SEARCH LOGIC
    // =========================================================

    protected List<SearchResult> parseHtml(String html, int maxResults) throws Exception {
        Log.d(LOG_TAG, "Parsing search results");

        List<SearchResult> results = new ArrayList<SearchResult>();

        final DateFormat parseDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

        int resultStart = html.indexOf(START_STRING) + START_STRING.length();

        while (resultStart >= 0 && results.size() < maxResults) {
            int resultEnd = html.indexOf(END_STRING, resultStart);

            while (resultEnd - resultStart > 5000) {
                resultStart = html.indexOf(START_STRING, resultStart + START_STRING.length() + 2) + START_STRING.length();
                resultEnd = html.indexOf(END_STRING, resultStart);
            }

            String itemString = html.substring(resultStart, resultEnd + END_STRING.length());

            String title = null;
            try {
                title = itemString.substring(0, itemString.indexOf("</A>"));
            } catch (Exception e) {}

            String downloadUrl = null;
            try {
                int downloadStart = itemString.indexOf("=download") + 1;
                int downloadEnd = itemString.indexOf(TORRENT_STRING) + TORRENT_STRING.length();
                downloadUrl = URL_PREFIX + itemString.substring(downloadStart, downloadEnd);
            } catch (Exception e) {}

            Date date = null;
            int seeders = -1;
            int leechers = -1;
            String size = null;
            try {
                int dateSearchStart = itemString.indexOf(DATE_START_SEARCH_STRING) + DATE_START_SEARCH_STRING.length();
                String dateSearchString = itemString.substring(dateSearchStart, itemString.length());
                int dateEnd =  dateSearchString.indexOf(DATE_END_STRING);
                String dateString = dateSearchString.substring(dateSearchString.indexOf(DATE_START_STRING) + DATE_START_STRING.length(), dateEnd);
                date = parseDateFormat.parse(dateString);

                int sizeStart = itemString.indexOf(SIZE_START_STRING, dateSearchStart + dateEnd) + SIZE_START_STRING.length();
                int sizeEnd = itemString.indexOf(SIZE_END_STRING, sizeStart);
                size = itemString.substring(sizeStart, sizeEnd);

                int seedStart = itemString.indexOf(PEER_START_STRING, sizeEnd) + PEER_START_STRING.length();
                int seedEnd = itemString.indexOf(PEER_END_STRING, seedStart);
                int leechStart = itemString.indexOf(PEER_START_STRING, seedEnd) + PEER_START_STRING.length();
                int leechEnd = itemString.indexOf(PEER_END_STRING, leechStart);
                String leechersString = itemString.substring(leechStart, leechEnd);
                String seedersString = itemString.substring(seedStart, seedEnd);
                seeders = Integer.parseInt(seedersString);
                leechers = Integer.parseInt(leechersString);
            } catch (Exception e) {}

            String imdbString = null;
            try {
                int imbdStart = itemString.indexOf(IMDB_START_STRING);
                if (imbdStart >= 10) {
                    imdbString = itemString.substring(imbdStart, itemString.indexOf(IMDB_END_STRING, imbdStart));
                }
            } catch (Exception e) {}

            if (title != null && downloadUrl != null) {
                SearchResult result = new SearchResult(title, downloadUrl, imdbString, size, date, seeders, leechers);
                results.add(result);
            }

            int nextResultStart = html.indexOf(START_STRING, resultEnd) + START_STRING.length();
            if (nextResultStart < resultStart) {
                resultStart = -1;
            }
            else {
                resultStart = nextResultStart;
            }
        }
        
        return results;
    }
}

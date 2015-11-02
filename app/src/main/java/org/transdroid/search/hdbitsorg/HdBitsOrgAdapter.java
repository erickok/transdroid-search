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
package org.transdroid.search.hdbitsorg;

import java.io.InputStream;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

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
 * An adapter that provides access to hdbits.org searches by parsing the raw HTML output.
 * 
 * @author John Conrad
 */
public class HdBitsOrgAdapter implements ISearchAdapter {
    private static final String LOG_TAG = HdBitsOrgAdapter.class.getName();

    private static final String LOGIN_FORM_URL = "https://hdbits.org/login";
    private static final String LOGIN_URL = "https://hdbits.org/login/doLogin";

    private static final String LOGIN_TOKEN_REGEX = "<input[^>]*name=\"lol\"[^>]*value=\"([^\"]+)\"[^>]*>"; 
    // without escapes                               <input[^>]*name="lol"[^>]*value="([^"]+)"[^>]*>

    private static final String LOGIN_POST_USERNAME = "uname";
    private static final String LOGIN_POST_PASSWORD = "password";
    private static final String LOGIN_POST_TOKEN = "lol";
        
    
    
    private static final String SEARCH_URL = "http://hdbits.org/browse.php?search=%1$s";
    private static final String SEARCH_SORT_BY_SEEDERS_SUFFIX = "&sort=seeders&d=DESC";
    
    private static final String SEARCH_REGEX = "<tr id='t[^']+'[^>]*>.*?href=\"/(details.php?[^\"]+)\"[^>]*?>([^<]*?)<.*?href=\"(download.php/[^\"]*?)\".*?<td[^>]*>(\\d*)\\W(day|month).*?<br />(\\d*)\\W*(day|hour).*?>([^<]*)<br>(GB|MB).*?toseeders=1\"><[^>]*>([^<]*).*?<td[^>]+.*?>(\\d+)"; 
    // without escapes:                         <tr id='t[^']+'[^>]*>.*?href="/(details.php?[^"]+)"[^>]*?>([^<]*?)<.*?href="(download.php/[^"]*?)".*?<td[^>]*>(\d*)\W(day|month).*?<br />(\d*)\W*(day|hour).*?>([^<]*)<br>(GB|MB).*?toseeders=1"><[^>]*>([^<]*).*?<td[^>]+.*?>(\d+)


    
    private static final String URL_PREFIX = "https://hdbits.org/";
    private static final int CONNECTION_TIMEOUT = 8000;

    // =========================================================
    // ISearchAdapter
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
    public boolean usesToken() {
        return false;
    }

    @Override
    public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {
        
        DefaultHttpClient client = prepareRequest(context);

        // build search query
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String url = String.format(SEARCH_URL, encodedQuery);
        if (order == SortOrder.BySeeders) url += SEARCH_SORT_BY_SEEDERS_SUFFIX;

        // make request
        Log.d(LOG_TAG, "Executing search request from: " + url);
        HttpResponse response = client.execute(new HttpGet(url));

        // parse HTML response into a list of torrents
        String html = HttpHelper.convertStreamToString(response.getEntity().getContent());
        return parseHtml(html, maxResults);

    }

    @Override
    public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
        // no rss based search for hdbits. there is a live RSS feed for all
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
        String username = SettingsHelper.getSiteUser(context, TorrentSite.HdBitsOrg);
        String password = SettingsHelper.getSitePass(context, TorrentSite.HdBitsOrg);

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
        return client;
    }

    /**
     * Retrieves a hidden token from the hdbits.org login form.
     */
    private String grabToken(DefaultHttpClient client) throws Exception {
        Log.d(LOG_TAG, "Retrieving login token.");

        // grab html
        HttpResponse response = client.execute(new HttpGet(LOGIN_FORM_URL));
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            throw new Exception("Failed to retrieve hdbits.org login form.");

        // try to find the hidden parameter on the login form
        String html = HttpHelper.convertStreamToString(response.getEntity().getContent());
        Pattern tokenRegexParser = Pattern.compile(LOGIN_TOKEN_REGEX);
        Matcher match = tokenRegexParser.matcher(html);
        boolean success = match.find();
        if (!success) throw new Exception("Unable to find hdbits.org login token. Has website HTML changed?");

        // success!
        return match.group(1);
    }

    /**
     * Attempts to log in to hdbits.org with the given credentials. On success
     * the given DefaultHttpClient should hold all required cookies to access
     * the site.
     */
    private void login(DefaultHttpClient client, String username, String password, String token) throws Exception {
        Log.d(LOG_TAG, "Attempting to login.");

        HttpPost request = new HttpPost(LOGIN_URL);
        request.setEntity(new UrlEncodedFormEntity(Arrays
                .asList(new BasicNameValuePair[] {
                        new BasicNameValuePair(LOGIN_POST_USERNAME, username),
                        new BasicNameValuePair(LOGIN_POST_PASSWORD, password),
                        new BasicNameValuePair(LOGIN_POST_TOKEN, token),
                        new BasicNameValuePair("returnto", "%2F") })));

        client.execute(request);

        // verify we have the cookies needed to log in
        boolean success = false, uid = false, pass = false, hash = false;
        for (Cookie cookie : client.getCookieStore().getCookies()) {
            if ("uid".equals(cookie.getName())) uid = true;
            if ("pass".equals(cookie.getName())) pass = true;
            if ("hash".equals(cookie.getName())) hash = true;            
        }
        
        // if we don't have the correct cookies, login failed. notify user with a toast and toss an exception.
        success = uid && pass && hash;
        if (!success) {
        	Log.e(LOG_TAG, "Failed to log into hdbits.org as '" + username + "'. Did not receive expected login cookies!");
            throw new LoginException("Failed to log into hdbits.org as '" + username + "'. Did not receive expected login cookies!");            
        }
        
        Log.d(LOG_TAG, "Successfully logged in to hdbits.org.");
    }

    // =========================================================
    // SEARCH LOGIC
    // =========================================================

    protected List<SearchResult> parseHtml(String html, int maxResults) throws Exception {
        Log.d(LOG_TAG, "Parsing search results.");        
        
        List<SearchResult> results = new ArrayList<SearchResult>();
        int matchCount = 0;
        int errorCount = 0;
        
        Pattern regex = Pattern.compile(SEARCH_REGEX, Pattern.DOTALL);
        Matcher match = regex.matcher(html);
        while (match.find() && matchCount < maxResults) {
            matchCount++;
            if (match.groupCount() != 11) {
                errorCount++;
                continue;
            }
            
            String detailsUrl = URL_PREFIX + match.group(1);
            String title      = match.group(2);
            String torrentUrl = URL_PREFIX + match.group(3);
            String size       = match.group(8) + match.group(9); // size + unit
            int seeders       = Integer.parseInt(match.group(10));
            int leechers      = Integer.parseInt(match.group(11));
            
            int time1         = Integer.parseInt(match.group(4));
            String timeUnit1  = match.group(5);
            int time2         = Integer.parseInt(match.group(6));
            String timeUnit2  = match.group(7);
            
            // hdbits.org lists "added date" in a relative format (i.e. 8 months 7 days ago)
            // we roughly calculate the number of MS elapsed then subtract that from "now"
            // could be a day or two off depending on month lengths, it's just imprecise data
            long elapsedTime = 0;
            if (timeUnit1.startsWith("month")) elapsedTime += time1 * 1000L * 60L * 60L * 24L * 30L;
            if (timeUnit1.startsWith("day"))   elapsedTime += time1 * 1000L * 60L * 60L * 24L;
            if (timeUnit2.startsWith("day"))   elapsedTime += time2 * 1000L * 60L * 60L * 24L;
            if (timeUnit2.startsWith("hour"))  elapsedTime += time2 * 1000L * 60L * 60L;

            Date addedDate = new Date();
            addedDate.setTime(addedDate.getTime() - elapsedTime);

            // build our search result
            SearchResult torrent = new SearchResult(title, torrentUrl, detailsUrl, size, addedDate, seeders, leechers);
            results.add(torrent);                
        }
        
        Log.d(LOG_TAG, "Found " + matchCount + " matches and successfully parsed " + (matchCount - errorCount) + " of those matches.");
        return results;
    }
}

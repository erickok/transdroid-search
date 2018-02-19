package org.transdroid.search.adapters.privatetrackers;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.security.auth.login.LoginException;

/**
 * Created by Márk on 2015.11.03..
 */
public class NcoreAdapter implements ISearchAdapter {

    private static final String LOGIN_USER = "nev";
    private static final String LOGIN_PASS = "pass";
    private static final String LOGIN_STAYLOGGEDIN = "ne_leptessen_ki";

    private static final String LOGINURL = "https://ncore.cc/login.php";
    private static final String QUERYURL = "https://ncore.cc/torrents.php?mire=%1$s&miben=name&tipus=all";
    private static final String SORT_COMPOSITE = "";
    private static final String SORT_SEEDS = "&miszerint=seeders&hogyan=DESC";
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private HttpClient prepareRequest(SharedPreferences prefs) throws Exception {

        String username = SettingsHelper.getSiteUser(prefs, TorrentSite.Ncore);
        String password = SettingsHelper.getSitePass(prefs, TorrentSite.Ncore);
        if (username == null || password == null) {
            throw new InvalidParameterException("No username or password was provided, while this is required for this private site.");
        }

        // First log in
        HttpClient httpclient = HttpHelper.buildDefaultSearchHttpClient(false);
        HttpPost loginPost = new HttpPost(LOGINURL);
        loginPost.setEntity(new UrlEncodedFormEntity(
                Arrays.asList(
                        new BasicNameValuePair(LOGIN_USER, username),
                        new BasicNameValuePair(LOGIN_STAYLOGGEDIN, "1"),
                        new BasicNameValuePair(LOGIN_PASS, password)
                )));
        HttpResponse loginResult = httpclient.execute(loginPost);
        if (loginResult.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            // Failed to sign in
            throw new LoginException("Login failure for Ncore with user " + username);
        }
        String loginHtml = HttpHelper.convertStreamToString(loginResult.getEntity().getContent());
        final String LOGIN_ERROR = "Login failed!";
        if (loginHtml == null || loginHtml.contains(LOGIN_ERROR)) {
            // Failed to sign in
            throw new LoginException("Login failure for Ncore with user " + username);
        }

        return httpclient;

    }

    @Override
    public List<SearchResult> search(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws Exception {

        HttpClient httpclient = prepareRequest(prefs);

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
    public InputStream getTorrentFile(SharedPreferences prefs, String url) throws Exception {

        // Provide an authenticated file handle to the requested url
        HttpClient httpclient = prepareRequest(prefs);
        HttpResponse response = httpclient.execute(new HttpGet(url));
        return response.getEntity().getContent();

    }

    protected List<SearchResult> parseHtml(String html, int maxResults) throws Exception {

        // Texts to find subsequently
        final String NOMATCH = "lista_mini_error";
        final String RESULTS = "<div class=\"box_torrent_all\">";
        final String TORRENT = "<div class=\"box_nagy";
        final String TORRENT_END = "<div style=\"clear:both;\">";

        // Parse the search results from HTML by looking for the identifying texts
        List<SearchResult> results = new ArrayList<>();
        if (html.contains(NOMATCH)) {
            return results; // Success, but no result for this query
        }
        int resultsStart = html.indexOf(RESULTS) + RESULTS.length();

        int torStart = html.indexOf(TORRENT, resultsStart);
        while (torStart >= 0 && results.size() < maxResults) {
            int nextTorrentIndex = html.indexOf(TORRENT, torStart + TORRENT.length());
            int endTorrentIndex = html.indexOf(TORRENT_END, torStart);
            if (nextTorrentIndex >= 0) {
                results.add(parseHtmlItem(html.substring(torStart + TORRENT.length(), endTorrentIndex)));
            } else {
                results.add(parseHtmlItem(html.substring(torStart + TORRENT.length())));
            }
            torStart = nextTorrentIndex;
        }
        return results;

    }

    private SearchResult parseHtmlItem(String htmlItem) {

        // Texts to find subsequently
        final String ID_TORRENT = "konyvjelzo('";
        final String ID_END = "')";
        final String NAME = "title=\"";
        final String NAME_END = "\">";
        final String DATE = "<div class=\"box_feltoltve2\">";
        final String DATE_END = "</div>";
        final String SIZE = "<div class=\"box_meret2\">";
        final String SIZE_END = "</div>";
        final String SEEDERS = "#peers\">";
        final String SEEDERS_END = "</a>";
        final String LEECHERS = "#peers\">";
        final String LEECHERS_END = "</a>";
        final String PREF_SITE = "https://ncore.cc/torrents.php?action=";
        final String PREF_DOWNLOAD = "download&id=";
        final String PREF_DETAILS= "details&id=";



        int idStart = htmlItem.indexOf(ID_TORRENT, 0) + ID_TORRENT.length();
        String id = htmlItem.substring(idStart, htmlItem.indexOf(ID_END, idStart));

        String details = PREF_SITE + PREF_DETAILS + id;
        String link    = PREF_SITE + PREF_DOWNLOAD+ id;

        int nameStart = htmlItem.indexOf(NAME, 0) + NAME.length();
        String name = htmlItem.substring(nameStart, htmlItem.indexOf(NAME_END, nameStart));

        int dateStart = htmlItem.indexOf(DATE, nameStart) + DATE.length();
        String dateText = htmlItem.substring(dateStart, htmlItem.indexOf(DATE_END, dateStart));
        String dateTextWellFormed = dateText.replace("<br>", " ");
        dateTextWellFormed = dateTextWellFormed.replace("\"", "");


        Date date = null;
        try {
            date = df.parse(dateTextWellFormed);
        } catch (ParseException e) {
            // Not parsable; just leave it at null
        }



        int sizeStart = htmlItem.indexOf(SIZE, dateStart) + SIZE.length();
        String size = htmlItem.substring(sizeStart, htmlItem.indexOf(SIZE_END, sizeStart));
        size = size.replace("<br>", "");

        int seedersStart = htmlItem.indexOf(SEEDERS, sizeStart) + SEEDERS.length();
        int seeders;
        String seedersText = htmlItem.substring(seedersStart, htmlItem.indexOf(SEEDERS_END, seedersStart));
        seeders = Integer.parseInt(seedersText);


        int leechersStart = htmlItem.indexOf(LEECHERS, seedersStart);
        int leechers;
        leechersStart += LEECHERS.length();
        String leechersText = htmlItem.substring(leechersStart, htmlItem.indexOf(LEECHERS_END, leechersStart));
        leechers = Integer.parseInt(leechersText);


        return new SearchResult(name, link, details, size, date, seeders, leechers);

    }

    @Override
    public String buildRssFeedUrlFromSearch(SharedPreferences prefs, String query, SortOrder order) {
        // Only provide a generic open RSS feed, otherwise an ID for searched serie/movie/etc has
        // to be obtained on http://finderss.it.cx/ and my not exist.
        return null;
    }

    @Override
    public String getSiteName() {
        return "Ncore";
    }

    public AuthType getAuthType() {
        return AuthType.USERNAME;
    }

    public String[] getRequiredCookies() {
        return null;
    }

}

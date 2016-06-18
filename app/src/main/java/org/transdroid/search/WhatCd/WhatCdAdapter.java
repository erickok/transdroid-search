/*
    thismachinechills / Alex
    License: GPLv3 and greater

    Thanks to other Transdroid plugin developers for
    providing a good portion of this
 */

package org.transdroid.search.WhatCd;

import java.io.InputStream;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.security.auth.login.LoginException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.gui.SettingsHelper;
import org.transdroid.util.HttpHelper;

import android.content.Context;
import android.util.Log;   


/*
 api doc @ https://github.com/WhatCD/Gazelle/wiki/JSON-API-Documentation
*/

public class WhatCdAdapter implements ISearchAdapter {
    private static final String LOG_TAG = WhatCdAdapter.class.getName();
    private static final String SITE_NAME = "What.cd";
    private static final String LOGIN_URL = "https://what.cd/login.php";
    private static final String SEARCH_URL = "https://what.cd/ajax.php?action=browse&searchstr=";
    private static final String INDEX_URL = "https://what.cd/ajax.php?action=index";


    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String LOGIN = "Log In";
    private static final String LOGIN_ERROR = "Your username or password was incorrect.";
    private static SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-dd-MM kk:mm:ss", Locale.ENGLISH);
    private static final int MEGABYTES_IN_BYTES = 1024 * 1024;

    private String authkey;
    private String passkey;


    private class SearchResultComparator implements Comparator<SearchResult> {
        public int compare(SearchResult result1, SearchResult result2) {
            return result1.getSeeds() - result2.getSeeds();
        }
    }

    private HttpClient prepareRequest(Context context) throws Exception {
        String username = SettingsHelper.getSiteUser(context, TorrentSite.WhatCd);
        String password = SettingsHelper.getSitePass(context, TorrentSite.WhatCd);

        if (username == null || password == null) {
            throw new InvalidParameterException("No username or password was provided, while this is required for this private site.");
        }

        HttpClient client = HttpHelper.buildDefaultSearchHttpClient(false);
        login(client, username, password);
        getKeys(client);

        return client;
    }

    public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {
        HttpClient httpclient = prepareRequest(context);
        String searchString = URLEncoder.encode(query, "UTF-8");
        HttpGet queryGet = new HttpGet(SEARCH_URL + searchString);

        HttpResponse queryResult = httpclient.execute(queryGet);

        if (queryResult.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new Exception("Unsuccessful query to the What.cd JSON API");
        }

        JSONObject json = getJSON(queryResult);

        if (json == null) {
            return new ArrayList<SearchResult>();
        }

        List<SearchResult> results = new ArrayList<>();
        JSONArray jsonResults = json.getJSONObject("response")
                                    .getJSONArray("results");

        //Log.d(LOG_TAG, jsonResults.toString());

        jsonResults = getTorrentsFromResults(jsonResults);

        for (int i = 0; i < jsonResults.length(); i++) {
            JSONObject torrent = jsonResults.getJSONObject(i);

            SearchResult result = new SearchResult(
                    torrent.getString("groupName"),
                    getTorrentUrl(torrent.getString("torrentId")),
                    getDetailsUrl(torrent),
                    bytesToMBytes(torrent.getString("size")),
                    getDate(torrent),
                    torrent.getInt("seeders"),
                    torrent.getInt("leechers"));
            //Log.d(LOG_TAG, result.getTorrentUrl());

            results.add(result);

        }

        Collections.sort(results, new SearchResultComparator());
        Collections.reverse(results);

        return results;
    }

    private String getDetailsUrl(JSONObject torrent) throws Exception {
        return "https://what.cd/torrents.php?id=" + torrent.getLong("groupId") +
                "&torrentid=" + torrent.getLong("torrentId");
    }

    private String getTorrentUrl(String torrentId) {
        return "https://what.cd/torrents.php?action=download&id=" + torrentId +
                "&authkey=" + authkey +
                "&torrent_pass=" + passkey;
    }

    private JSONArray getTorrentsFromResults(JSONArray results) throws Exception {
        JSONArray jsonTorrents = new JSONArray();

        for (int index = 0; index < results.length(); index++) {
            JSONObject item = results.getJSONObject(index);
            boolean isMusic = item.has("artist");

            //Log.d(LOG_TAG, item.toString());

            if (isJsonTorrent(item)) {
                jsonTorrents.put(item);

            } else {
                long groupId = item.getLong("groupId");
                String name = item.getString("groupName");

                if (isMusic) {
                    name = item.getString("artist") + " - " + name;
                }

                JSONArray jorrentJson = item.getJSONArray("torrents");
                JSONArray torrents = getTorrentsFromResults(jorrentJson);

                for (int torrentIndex = 0; torrentIndex < torrents.length(); torrentIndex++) {
                    JSONObject torrent = torrents.getJSONObject(torrentIndex);
                    String torrentTitle = name;

                    if (isJsonTorrent(torrent)) {
                        if (isMusic) {
                            torrentTitle += " - " + torrent.getString("format") +
                                    " - " + torrent.getString("encoding");
                        }

                        torrent.put("groupName", torrentTitle);
                        torrent.put("groupId", groupId);
                        jsonTorrents.put(torrent);
                    }

                }
            }
        }

        //Log.d(LOG_TAG, jsonTorrents.toString());

        return jsonTorrents;
    }


    private boolean isJsonTorrent(JSONObject json) {
        return json.has("torrentId");
    }

    private String bytesToMBytes(String bytesString) {
        /*
            Thanks to StrikeSearchAdapter developer
         */

        long nbBytes = 0;

        try {
            nbBytes = Long.parseLong(bytesString);
        } catch (NumberFormatException e) {
            return bytesString;
        }

        long nbMegaBytes = nbBytes / MEGABYTES_IN_BYTES;
        long lastingBytesTruncated = (nbBytes % MEGABYTES_IN_BYTES) / 10000;

        return nbMegaBytes + "." + lastingBytesTruncated + "MB";
    }

    private Date getDate(JSONObject torrent) {
        /*
            Thanks to StrikeSearchAdapter developer
         */

        Date date;
        try {
            date = DATE_FMT.parse(torrent.getString("time"));
        } catch (ParseException e) {
            date = new Date();
        } catch (JSONException e) {
            date = new Date();
        }
        return date;
    }

    @Override
    public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
        return null;
    }


    @Override
    public String getSiteName() {
        return SITE_NAME;
    }


    public boolean isPrivateSite() {
        return true;
    }


    public boolean usesToken() {
        return false;
    }


    @Override
    public InputStream getTorrentFile(Context context, String url) throws Exception {
        HttpClient client = prepareRequest(context);
        HttpResponse response = client.execute(new HttpGet(url));
        return response.getEntity().getContent();

    }

    private void getKeys(HttpClient client) throws Exception {
        HttpResponse response = client.execute(new HttpGet(INDEX_URL));
        JSONObject json = getJSON(response).getJSONObject("response");

        authkey = json.getString("authkey");
        passkey = json.getString("passkey");

        //Log.d(LOG_TAG, "keys: " + authkey + " " + passkey);
    }

    private JSONObject getJSON(HttpResponse result) throws Exception {
        InputStream instream = result.getEntity().getContent();
        String json = HttpHelper.convertStreamToString(instream);
        instream.close();

        return new JSONObject(json);
    }

    private void login(HttpClient client, String username, String password) throws Exception {
        Log.d(LOG_TAG, "Attempting to login.");

        HttpPost loginPost = new HttpPost(LOGIN_URL);
        loginPost.setEntity(new UrlEncodedFormEntity(
                Arrays.asList(
                        new BasicNameValuePair(USERNAME, username),
                        new BasicNameValuePair(PASSWORD, password),
                        new BasicNameValuePair(LOGIN, LOGIN)
                )));
        HttpResponse loginResult = client.execute(loginPost);
        String loginHtml = HttpHelper.convertStreamToString(loginResult.getEntity().getContent());

        if (loginHtml == null || loginHtml.contains(LOGIN_ERROR)) {
            throw new LoginException("Login failure for What.cd with user " + username);
        }

        Log.d(LOG_TAG, "Successfully logged in to What.cd");
    }
}

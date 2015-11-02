package org.transdroid.search.StrikeSearch;

import android.content.Context;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.util.HttpHelper;

import java.io.InputStream;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
* Adapter for Strike Search. See https://getstrike.net/api
* */
public class StrikeSearchAdapter implements ISearchAdapter{
    private static final String QUERYURL = "https://getstrike.net/api/v2/torrents/search/?phrase=";
    private static final int CONNECTION_TIMEOUT = 8000;
    private static final int MEGABYTES_IN_BYTES = 1024*1024;
    private static SimpleDateFormat addedFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);

    @Override
    public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {
        DefaultHttpClient httpclient = prepareRequest(context);

        // Start synchronous search via the JSON API
        String searchString =  URLEncoder.encode(query, "UTF-8");
        HttpGet queryGet = new HttpGet(QUERYURL + searchString);

        HttpResponse queryResult = httpclient.execute(queryGet);
        if (queryResult.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new Exception("Unsuccessful query to the StrikeSearch JSON API");
        }

        // Read JSON response
        InputStream instream = queryResult.getEntity().getContent();
        String json = HttpHelper.convertStreamToString(instream);
        instream.close();
        JSONObject structure = null;
        try {
            structure = new JSONObject(json);
        } catch (JSONException e) {
            return new ArrayList<SearchResult>();
        }

        // Construct the list of search results
        List<SearchResult> results = new ArrayList<>();
        JSONArray torrents = structure.getJSONArray("torrents");
        for (int i = 0; i < torrents.length(); i++) {
            JSONObject torrent = torrents.getJSONObject(i);
            Date dateAdded = getDate(torrent);
            results.add(new SearchResult(
                    torrent.getString("torrent_title"),
                    torrent.getString("magnet_uri"),
                    torrent.getString("page"),
                    bytesToMBytes(torrent.getString("size")),
                    dateAdded,
                    torrent.getInt("seeds"),
                    torrent.getInt("leeches")));
        }
        return results;
    }

    private Date getDate(JSONObject torrent) {
        Date date;
        try {
            date = addedFormat.parse(torrent.getString("upload_date"));
        } catch (ParseException e) {
            date = new Date();
        } catch (JSONException e) {
            date = new Date();
        }
        return date;
    }

    private String bytesToMBytes (String bytesString) {
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

    private DefaultHttpClient prepareRequest(Context context) throws Exception {
        // Setup http client
        HttpParams httpparams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT);
        DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);

        return httpclient;

    }

    @Override
    public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
        // No RSS feeds
        return null;
    }

    @Override
    public String getSiteName() {
        return "Strike Search";
    }

    @Override
    public boolean isPrivateSite() {
        return false;
    }

    @Override
    public boolean usesToken() {
        return false;
    }

    @Override
    public InputStream getTorrentFile(Context context, String url) throws Exception {
        return null;
    }
}

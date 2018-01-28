package org.transdroid.search.adapters.publictrackers;

import android.content.SharedPreferences;

import org.apache.http.HttpResponse;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * An adapter that provides access to YTS searches via their official JSON api.
 * Ref: https://yts.ag/api
 * <p/>
 * Created by Issinoho on 19/11/2015.
 */
public class YtsAdapter implements ISearchAdapter {

    private static final String BASE_URL = "https://yts.immunicity.world/api/v2/list_movies.json";
    private static final int CONNECTION_TIMEOUT = 10000;

    /**
     * Return a properly formatted string denoting file size with units.
     *
     * @param sizeBytes Integer representation of file size in bytes.
     * @return String representation of file size with units.
     */
    private static String getFormattedSize(long sizeBytes) {
        String size;
        if (sizeBytes > 1024 * 1024 * 1024) {
            size = String.format(Locale.getDefault(), "%1$.1f GB", sizeBytes / (1024D * 1024D * 1024D));
        } else if (sizeBytes > 1024 * 1024) {
            size = String.format(Locale.getDefault(), "%1$.1f MB", sizeBytes / (1024D * 1024D));
        } else if (sizeBytes > 1024) {
            size = String.format(Locale.getDefault(), "%1$.1f kB", sizeBytes / 1024D);
        } else {
            size = String.format(Locale.getDefault(), "%1$.1f B", (double) sizeBytes);
        }

        return size;
    }

    @Override
    public List<SearchResult> search(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws Exception {
        return performSearch(query, order);
    }

    private List<SearchResult> performSearch(String query, SortOrder order) throws IOException, JSONException {
        // Ask for search results
        String orderKey = order == SortOrder.BySeeders? "seeds": "date_added";
        String q = String.format(Locale.US, "?query_term=%1$s&sort_by=%2$s", URLEncoder.encode(query, "UTF-8"), orderKey);
        HttpParams httpparams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT);
        DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);
        //httpclient.getParams().setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        HttpResponse response = httpclient.execute(new HttpGet(BASE_URL + (query.length() > 0 ? q : "")));

        // Read JSON response
        InputStream instream = response.getEntity().getContent();
        String json = HttpHelper.convertStreamToString(instream);
        instream.close();

        // Parse results
        JSONObject structure = new JSONObject(json);
        List<SearchResult> results = new ArrayList<>();
        JSONObject data = structure.getJSONObject("data");
        JSONArray movies = data.getJSONArray("movies");
        for (int i = 0; i < movies.length(); i++) {
            // Dequeue the next movie
            JSONObject movie = movies.getJSONObject(i);

            // Get the torrents for this movie
            JSONArray torrents = movie.getJSONArray("torrents");
            for (int t = 0; t < torrents.length(); t++) {
                // Dequeue the next torrent
                JSONObject torrent = torrents.getJSONObject(t);

                // Construct title as longer movie title + quality
                String title = movie.getString("title_long") + " " + torrent.getString("quality");

                // Get the file size
                long sizeBytes = torrent.getLong("size_bytes");
                String size = getFormattedSize(sizeBytes);

                // Get the uploaded date
                Date date = null;
                try {
                    date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(torrent.getString("date_uploaded"));
                } catch (JSONException | ParseException e) {
                    System.out.println(e.getMessage());
                }

                // Add to collection
                results.add(new SearchResult(title, torrent.getString("url"), movie.getString("url"), size, date, torrent.getInt("seeds"), torrent
                        .getInt("peers")));
            }
        }

        // Return results
        return results;
    }

    @Override
    public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
        return null;
    }

    @Override
    public String getSiteName() {
        return "YTS (proxied)";
    }

    public AuthType getAuthType() {
        return AuthType.NONE;
    }

    public String[] getRequiredCookies() {
        return null;
    }

    @Override
    public InputStream getTorrentFile(SharedPreferences prefs, String url) throws Exception {
        return null;
    }

}

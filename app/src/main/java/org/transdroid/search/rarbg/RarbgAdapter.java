package org.transdroid.search.rarbg;

import android.content.Context;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONObject;
import org.transdroid.search.BuildConfig;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.util.HttpHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * An adapter that provides access to RARBG searches via their official rarbg/torrentapi JSON api. Project: https://github.com/rarbg/torrentapi Doc:
 * https://torrentapi.org/apidocs_v2.txt
 */
public class RarbgAdapter implements ISearchAdapter {

	private static final String BASE_URL = "https://torrentapi.org/pubapi_v2.php";

	// Access is provided using a token, requested via get_token and valid for up to 15 minutes
	private static String accessToken;
	private HttpClient httpclient;

	@Override
	public String getSiteName() {
		return "RARBG";
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
	public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {

		if (httpclient == null) {
			httpclient = HttpHelper.buildDefaultSearchHttpClient(false);
			httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Torrent Search (by Transdroid) " + BuildConfig.VERSION_NAME);
		}
		if (accessToken == null) {
			requestAccessToken();
		}
		List<SearchResult> results = performSearch(query, order);

		if (results == null) {
			// Special case: our access token was invalid; request a new token and try again
			accessToken = null;
			requestAccessToken();
			results = performSearch(query, order);
			if (results == null)
				throw new IOException("RARBG returned a problem with out access token, even after requesting a new one and retrying");
		}

		return results;
	}

	private void requestAccessToken() throws Exception {

		// Ask for a new token
		HttpResponse response = httpclient.execute(new HttpGet(BASE_URL + "?get_token=get_token"));
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			InputStream instream = response.getEntity().getContent();
			String error = HttpHelper.convertStreamToString(instream);
			instream.close();
			throw new IOException(
					"RARBG did not supply us with a token to their torrentapi: HTTP " + response.getStatusLine().getStatusCode() + ": " + error);
		}

		// Read JSON response
		InputStream instream = response.getEntity().getContent();
		String json = HttpHelper.convertStreamToString(instream);
		instream.close();
		JSONObject structure = new JSONObject(json);

		if (structure.has("token")) {
			accessToken = structure.getString("token");
			return;
		}

		throw new IOException("RARBG did not supply us with a token to their torrentapi: instead we got: " + json);

	}

	private List<SearchResult> performSearch(String query, SortOrder order) throws Exception {

		// Ask for extended search results
		String q = String.format(Locale.US, "?mode=search&search_string=%1$s&ranked=0&sort=%2$s&format=json_extended&token=%3$s",
				URLEncoder.encode(query, "UTF-8"), (order == SortOrder.BySeeders ? "seeders" : "last"), accessToken);
		HttpResponse response = httpclient.execute(new HttpGet(BASE_URL + q));

		// Read JSON response
		InputStream instream = response.getEntity().getContent();
		String json = HttpHelper.convertStreamToString(instream);
		instream.close();
		JSONObject structure = new JSONObject(json);

		// Check for error reponses
		if (structure.has("error_code") && structure.getInt("error_code") == 2) {
			// Null: we need to refresh our access token
			return null;
		}
		if (structure.has("error_code") && structure.getInt("error_code") == 20) {
			// No results found
			return new ArrayList<>();
		}
		if (structure.has("error")) {
			// Critical error: throw exception with the message
			throw new IOException("RARBG error: " + structure.getString("error"));
		}

		// Parse results
		List<SearchResult> results = new ArrayList<>();
		JSONArray array = structure.getJSONArray("torrent_results");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);
		for (int i = 0; i < array.length(); i++) {
			JSONObject item = array.getJSONObject(i);
			long sizeBytes = item.getLong("size");
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
			Date date = null;
			try {
				date = dateFormat.parse(item.getString("pubdate"));
			} catch (Exception e) {
				// Ignore; we rather have no date and results than stop on this error
			}
			results.add(new SearchResult(item.getString("title"), item.getString("download"), null, size, date, item.getInt("seeders"),
					item.getInt("leechers")));
		}
		return results;

	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// Not supported by RARABG
		return null;
	}

	@Override
	public InputStream getTorrentFile(Context context, String url) throws Exception {
		// Only for private sites
		return null;
	}
}

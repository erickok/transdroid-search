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
package org.transdroid.search.BTN;

import android.content.Context;

import org.alexd.jsonrpc.JSONRPCException;
import org.alexd.jsonrpc.JSONRPCParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.gui.SettingsHelper;

import org.alexd.jsonrpc.JSONRPCClient;
import org.transdroid.util.FileSizeConverter;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.login.LoginException;

/**
 * An adapter that provides access to BTN searches by leveraging the JSON RPC.
 * This is the same API that sickbeard uses.
 */
public class BTNAdapter implements ISearchAdapter {

	private static final String API_URL = "http://api.btnapps.net";
	private static final String THETVDB_BASE_URL = "http://thetvdb.com/?tab=series&id=";
	private static final String API_SEARCH = "getTorrents";

	@Override
	public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {

		// transdroid fudge - the user can enter the API key in either field
		String username = SettingsHelper.getSiteUser(context, TorrentSite.BTN);
		String password = SettingsHelper.getSitePass(context, TorrentSite.BTN);
		String apikey = (username == null ? password : username);

		if (username == null && password == null) {
			throw new LoginException("The BTN user API key was not provided, please configure BTN site settings");
		}

		// Try and get the search results - if we can't, assume invalid API key
		JSONObject apiSearchResults;
		try {
			// Generous time-out for mobile connections
			JSONRPCClient client = JSONRPCClient.create(API_URL, JSONRPCParams.Versions.VERSION_1);
			client.setConnectionTimeout(10000);
			client.setSoTimeout(10000);
			apiSearchResults = (JSONObject) client.call(API_SEARCH, apikey, query, maxResults);
		}
		catch (JSONRPCException e) {
			throw new LoginException("The BTN user API key was invalid, please check your entry");
		}

		List<SearchResult> results = new ArrayList<>();
		try {
			JSONObject apiSearchResultsTorrents = apiSearchResults.getJSONObject("torrents");
			Iterator<String> searchResultSetKeys = apiSearchResultsTorrents.keys();
			while (searchResultSetKeys.hasNext())
			{
				String resultKey = searchResultSetKeys.next();
				try {
					JSONObject resultEntry = apiSearchResultsTorrents.getJSONObject(resultKey);

					String name = resultEntry.optString("ReleaseName");
					String series = resultEntry.optString("Series");
					String groupName = resultEntry.optString("GroupName");
					String resolution = resultEntry.optString("Resolution");
					String source = resultEntry.optString("Source");
					String codec = resultEntry.optString("Codec");
					String link = resultEntry.getString("DownloadURL");
					String tvdbID = resultEntry.optString("TvdbID");
					String size = FileSizeConverter.getSize(resultEntry.getLong("Size"));
					Date date = new Date(resultEntry.getLong("Time") * 1000);
					int seeders = resultEntry.getInt("Seeders");
					int leechers = resultEntry.getInt("Leechers");

					// Ensure we have a title, in case the torrent has no release name
					if (name.equals(""))
					{
						StringBuilder sb = new StringBuilder();
						sb.append(series);
						if (!sb.toString().equals(""))
							sb.append(".");
						sb.append(groupName);
						if (!sb.toString().equals(""))
							sb.append(".");
						sb.append(resolution);
						if (!sb.toString().equals(""))
							sb.append(".");
						sb.append(source);
						if (!sb.toString().equals(""))
							sb.append(".");
						sb.append(codec);
						if (!sb.toString().equals(""))
							name = sb.toString();

						name = name.replace(" ", ".");
					}

					String details = "";
					if (tvdbID != null)
						details = THETVDB_BASE_URL + tvdbID;

					results.add(
						new SearchResult(name, link, details, size, date, seeders, leechers)
					);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			if (order == SortOrder.BySeeders)
			{
				 Collections.sort(results, new TorrentSeedsComparator());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			return results;
		}
	}

	@Override
	public InputStream getTorrentFile(Context context, String link) throws Exception {
		URL url = new URL(link);
		URLConnection urlConnection = url.openConnection();
		return new BufferedInputStream(urlConnection.getInputStream());
	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// Not implemented
		return null;
	}

	@Override
	public String getSiteName() {
		return "BTN";
	}

	@Override
	public boolean isPrivateSite() {
		return true;
	}

	@Override
	public boolean usesToken() {
		return true;
	}

	class TorrentSeedsComparator implements Comparator<SearchResult> {
		public int compare(SearchResult tor1, SearchResult tor2) {
			return tor2.getSeeds() - tor1.getSeeds();
		}
	}

}

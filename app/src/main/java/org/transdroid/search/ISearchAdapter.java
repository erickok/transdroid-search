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
package org.transdroid.search;

import java.io.InputStream;
import java.util.List;

import android.content.Context;

/**
 * Interface with the required methods for a search adapter that synchronously gets torrent search results from some
 * website.
 * @author Eric Kok
 */
public interface ISearchAdapter {

	/**
	 * Implementing search providers should synchronously perform the search for torrents matching the given query
	 * string.
	 * @param context The Android activity/provider context from which the shared preferences can be accessed
	 * @param query The raw (non-urlencoded) query to search for
	 * @param order The preferred order in which results are sorted
	 * @param maxResults Maximum number of results to return
	 * @return The list of found torrents on the site matching the search query
	 * @throws Exception When an exception occurred during the loading or parsing of the search results
	 */
	List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception;

	/**
	 * Implementing search providers should provide the URL of an RSS feed matching the search a specific query.
	 * @param query The raw (non-urlencoded) query for which the RSS feed should provide torrents
	 * @param order The preferred order in which the RSS items are sorted
	 * @return The RSS feed URL, or null if this is not supported by the site
	 */
	String buildRssFeedUrlFromSearch(String query, SortOrder order);

	/**
	 * Implementing search providers should return the real name of the site they work on.
	 * @return The name of the torrent site
	 */
	String getSiteName();

	/**
	 * Implementing search providers should return whether this is a private site, that is, whether this site requires
	 * user credentials before it can be searched.
	 * @return True if this is an adapter to a private site, false otherwise.
	 */
	boolean isPrivateSite();

	/**
	 * Implementing search providers should return whether the site uses a token authentication system.
	 * @return True is a session token is used in lieu of a username/password login combination
	 */
	boolean usesToken();

	/**
	 * Implement search providers should set up an HTTP request for the specified torrent file uri and, possibly after
	 * setting authentication credentials, return a handle to the file content stream.
	 * @param context The Android activity/provider context from which the shared preferences can be accessed
	 * @param url The full url of the torrent file to download
	 * @return An InputStream handle to the requested file so it can be further downloaded, or null if no connection is
	 *         possible (like when the device is offline or when the user is not authorized)
	 * @throws Exception When an exception occurred during the retrieval of the request url
	 */
	InputStream getTorrentFile(Context context, String url) throws Exception;

}

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

import java.util.List;

/**
 * Interface with the required methods for a search adapter that
 * synchronously gets torrent search results from some website.
 * 
 * @author Eric Kok
 */
public interface ISearchAdapter {

	/**
	 * Implementing search providers should synchronously perform the search for torrents matching the given query string.
	 * @param query The raw (non-urlencoded) query to search for
	 * @param order The preferred order in which results are sorted
	 * @param maxResults Maximum number of results to return
	 * @return The list of found torrents on the site matching the search query
	 * @throws Exception When an exception occurred during the loading or parsing of the search results
	 */
	public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception;

	/**
	 * Implementing search providers should provide the URL of an RSS feed matching the search a specific query.
	 * @param query The raw (non-urlencoded) query for which the RSS feed should provide torrents
	 * @param order The preferred order in which the RSS items are sorted
	 * @return The RSS feed URL, or null if this is not supported by the site
	 */
	public String buildRssFeedUrlFromSearch(String query, SortOrder order);

	/**
	 * Implementing search providers should return the real name of the site they work on.
	 * @return The name of the torrent site
	 */
	public String getSiteName();
	
}

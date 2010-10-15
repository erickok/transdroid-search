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
package org.transdroid.search.RssFeedSearch;

import java.util.ArrayList;
import java.util.List;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;

/**
 * An abstract class providing functionality to easily build search engine support for torrent sites that use RSS feeds.
 * The site should allow for a custom feed per search and the results should include at least the title and direct URL.
 * 
 * @author Eric Kok
 */
public abstract class RssFeedSearchAdapter implements ISearchAdapter {

	/**
	 * Returns a SearchResult object from a single RSS item. Typically, you return a 
	 * new SearchResult object filled with the title and URL's from the raw RSS item.
	 * @param item The RSS item element, containing title, URL, description, etc.
	 * @return A result object that is properly filled with data
	 */
	protected abstract SearchResult fromRssItemToSearchResult(Item item);

	/**
	 * Returns the RSS feed URL for a search query. Typically, you return URL-encode
	 * the query and return the URL (possibly with a search parameter). 
	 * @param query The plain search term the user gave
	 * @param order 
	 * @return The full URL of the custom search RSS feed
	 */
	protected abstract String getUrl(String query, SortOrder order);

	/**
	 * Returns the RSS parser to use. When not overriden the default parser will be
	 * used, but when additional tags need to be parsed, a custom parser can be used.
	 * @param url The url to parse
	 * @return An RssParser-based parser instance
	 */
	protected RssParser getRssParser(String url) {
		return new RssParser(url);
	}
	
	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		return getUrl(query, order);
	}

	@Override
	public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {

		// Parse the RSS feeds
		RssParser parser = getRssParser(getUrl(query, order));
		parser.parse();
		List<Item> items = parser.getChannel().getItems();
		
		// Create a list of SearchResults and send it back
		List<SearchResult> results = new ArrayList<SearchResult>();
		int i = 0;
		if (items != null) {
			for (Item item : items) {
				if (i >= maxResults) {
					break;
				}
				results.add(fromRssItemToSearchResult(item));
				i++;
			}
		}
		return results;
			
	}
	
}

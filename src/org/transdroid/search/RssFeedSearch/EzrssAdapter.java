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

import java.net.URLEncoder;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.util.FileSizeConverter;

/**
 * Search adapter for the EzRss torrent site (based on custom search RSS feeds)
 * This adapter does not support sorting but instead always orders by release date (descending)
 * 
 * @author Eric Kok
 */
public class EzrssAdapter extends RssFeedSearchAdapter {

	protected SearchResult fromRssItemToSearchResult(Item item) {

		EzrssItem theItem = (EzrssItem) item;
		return new SearchResult(
				item.getTitle(),
				item.getLink(),
				theItem.comments,
				FileSizeConverter.getSize(item.getEnclosureLength()), 
				item.getPubdate(), 
				0, 
				0);
	}

	@Override
	protected String getUrl(String query, SortOrder order) {
		return "http://ezrss.it/search/index.php?simple&show_name=" + URLEncoder.encode(query) + "&mode=rss";
	}

	@Override
	protected RssParser getRssParser(String url) {
		return new EzrssRssParser(url);
	}
	
	/**
	 * Custom Item with addition torrentLink. size, seeders and leechers data properties
	 */
	public class EzrssItem extends Item {
		public String comments;
	}
	
	/**
	 * Custom parser to parse the additional comments data property
	 */
	public class EzrssRssParser extends RssParser {

		public EzrssRssParser(String url) {
			super(url);
		}
		
		public Item createNewItem() {
			return new EzrssItem();
		}

	    public void addAdditionalData(String localName, Item item, String text) {
	    	EzrssItem theItem = (EzrssItem) item;
	    	if (localName.equalsIgnoreCase("comments")) {
	    		theItem.comments = text.trim();
	    	}
	    }
	}

	@Override
	public String getSiteName() {
		return "EzRss";
	}
	
}

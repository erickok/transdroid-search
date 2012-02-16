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
 * Search adapter for the ExtraTorrent torrent site (based on custom search RSS feeds)
 * 
 * @author Eric Kok
 */
public class ExtraTorrentAdapter extends RssFeedSearchAdapter {

	protected SearchResult fromRssItemToSearchResult(Item item) {
		ExtraTorrentsItem theItem = (ExtraTorrentsItem) item;
		return new SearchResult(
				item.getTitle(), 
				item.getEnclosureUrl(),
				item.getLink(),
				theItem.getSize() == -1? "": FileSizeConverter.getSize(theItem.getSize()),  
				item.getPubdate(),
				theItem.getSeeders(), 
				theItem.getLeechers());
	}

	@Override
	protected String getUrl(String query, SortOrder order) {
		// Note: doesn't support different list sortings
		return "http://extratorrent.ws/rss.xml?type=search&search=" + URLEncoder.encode(query);
	}

	@Override
	protected RssParser getRssParser(String url) {
		return new ExtraTorrentsRssParser(url);
	}
	
	/**
	 * Custom Item with addition size, seeders and leechers data properties
	 */
	public class ExtraTorrentsItem extends Item {
		private long size;
		private int seeders;
		private int leechers;
		public void setSize(long size) { this.size = size; }
		public void setSeeders(int seeders) { this.seeders = seeders; }
		public void setLeechers(int leechers) { this.leechers = leechers; }
		public long getSize() { return size; }
		public int getSeeders() { return seeders; }
		public int getLeechers() { return leechers; }
	}
	
	/**
	 * Custom parser to parse the additional size, seeders and leechers data properties
	 */
	public class ExtraTorrentsRssParser extends RssParser {

		public ExtraTorrentsRssParser(String url) {
			super(url);
		}
		
		public Item createNewItem() {
			return new ExtraTorrentsItem();
		}

	    public void addAdditionalData(String localName, Item item, String text) {
	    	ExtraTorrentsItem theItem = (ExtraTorrentsItem) item;
	    	if (localName.equalsIgnoreCase("size")) {
	    		try {
	    			theItem.setSize(Long.parseLong(text.trim()));
	    		} catch (Exception e) {
	    			theItem.setSize(-1);
				}
	    	}
	    	if (localName.equalsIgnoreCase("seeders")) {
	    		try {
	    			theItem.setSeeders(Integer.parseInt(text.trim()));
	    		} catch (Exception e) {
	    			theItem.setSeeders(0);
				}
	    	}
	    	if (localName.equalsIgnoreCase("leechers")) {
	    		try {
	    			theItem.setLeechers(Integer.parseInt(text.trim()));
	    		} catch (Exception e) {
	    			theItem.setSeeders(0);
				}
	    	}
	    }
	}

	@Override
	public String getSiteName() {
		return "ExtraTorrent";
	}
	
}

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

import org.apache.http.impl.client.DefaultHttpClient;
import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.util.FileSizeConverter;

/**
 * Search adapter for the Bitsnoop torrent site (based on custom search
 * RSS feeds)
 * 
 * @author Gabor Tanka
 */
public class BitSnoopAdapter extends RssFeedSearchAdapter {

	@Override
	protected SearchResult fromRssItemToSearchResult(Item item) {
		BitSnoopItem theItem = (BitSnoopItem) item;
		return new SearchResult(
				item.getTitle(), 
				theItem.getEnclosureUrl(),
				item.getLink(),
				FileSizeConverter.getSize(theItem.getSize()), 
				item.getPubdate(), 
				theItem.getSeeders(), 
				theItem.getLeechers());
	}
	
	@Override
	public String getSiteName() {
		return "BitSnoop";
	}

	@Override
	protected String getUrl(String query, SortOrder order) {
		return "http://bitsnoop.com/search/all/" + URLEncoder.encode(query)
				+ "/c/d/1/?fmt=rss";
	}

	@Override
	protected RssParser getRssParser(String url) {
		return new BitSnoopRssParser(url);
	}

	/**
	 * Custom Item with addition size, seeders and leechers data
	 * properties
	 */
	public class BitSnoopItem extends Item {
		private long size;
		private int seeders;
		private int leechers;
		public void setSize(long size) { this.size = size; }
		public void setSeeders(int seeders) { this.seeders = seeders; }
		public void setLeechers(int leechers) {	this.leechers = leechers; }
		public long getSize() {	return size; }
		public int getSeeders() { return seeders; }
		public int getLeechers() { return leechers;	}
	}

	public class BitSnoopRssParser extends RssParser {

		public BitSnoopRssParser(String url) {
			super(url);
		}

		public Item createNewItem() {
			return new BitSnoopItem();
		}

		public void addAdditionalData(String localName, Item item, String text) {
			BitSnoopItem theItem = (BitSnoopItem) item;
			if (localName.equalsIgnoreCase("size")) {
				theItem.setSize(Long.parseLong(text.trim()));
			}
			if (localName.equalsIgnoreCase("numSeeders")) {
				theItem.setSeeders(Integer.parseInt(text.trim()));
			}
			if (localName.equalsIgnoreCase("numLeechers")) {
				theItem.setLeechers(Integer.parseInt(text.trim()));
			}
		}
		
		@Override
		protected DefaultHttpClient initialise() {
			DefaultHttpClient client = super.initialise();	
			// Spoof Firefox user agent to force a result from BitSnoop
	        client.getParams().setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
	        return client;
		}

	}

}

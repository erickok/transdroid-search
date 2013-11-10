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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.util.FileSizeConverter;

/**
 * Search adapter for the KickassTorrents torrent site (based on custom search RSS feeds)
 * 
 * @author Eric Kok
 */
public class KickassTorrentsAdapter extends RssFeedSearchAdapter {

	protected SearchResult fromRssItemToSearchResult(Item item) {
		KickassTorrentsItem theItem = (KickassTorrentsItem) item;
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
	protected String getUrl(String query, SortOrder order) {
		try {
			return "http://kickass.to/search/" + URLEncoder.encode(query, "UTF-8").replace("+", "%20") + "/?rss=1" + (order == SortOrder.BySeeders? "&field=seeders&sorder=desc": "");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError("UTF-8 not supported");
		}
	}

	@Override
	protected RssParser getRssParser(String url) {
		return new KickassTorrentsRssParser(url);
	}
	
	/**
	 * Custom Item with addition torrentLink. size, seeders and leechers data properties
	 */
	public class KickassTorrentsItem extends Item {
		private String torrentLink;
		private long size;
		private int seeders;
		private int leechers;
		public void setTorrentLink(String torrentLink) { this.torrentLink = torrentLink; }
		public void setSize(long size) { this.size = size; }
		public void setSeeders(int seeders) { this.seeders = seeders; }
		public void setLeechers(int leechers) { this.leechers = leechers; }
		public String getTorrentLink() { return torrentLink; }
		public long getSize() { return size; }
		public int getSeeders() { return seeders; }
		public int getLeechers() { return leechers; }
	}
	
	/**
	 * Custom parser to parse the additional torrentLink. size, seeders and leechers data properties
	 */
	public class KickassTorrentsRssParser extends RssParser {

		public KickassTorrentsRssParser(String url) {
			super(url);
		}
		
		public Item createNewItem() {
			return new KickassTorrentsItem();
		}

	    public void addAdditionalData(String localName, Item item, String text) {
	    	KickassTorrentsItem theItem = (KickassTorrentsItem) item;
	    	if (localName.equalsIgnoreCase("torrentLink")) {
	    		theItem.setTorrentLink(text.trim());
	    	}
	    	if (localName.equalsIgnoreCase("contentLength")) {
	    		theItem.setSize(Long.parseLong(text.trim()));
	    	}
	    	if (localName.equalsIgnoreCase("seeds")) {
	    		theItem.setSeeders(Integer.parseInt(text.trim()));
	    	}
	    	if (localName.equalsIgnoreCase("peers")) {
	    		theItem.setLeechers(Integer.parseInt(text.trim()));
	    	}
	    }
	}

	@Override
	public String getSiteName() {
		return "KickAssTorrents";
	}
	
}

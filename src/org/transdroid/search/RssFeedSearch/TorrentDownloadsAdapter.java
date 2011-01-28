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
 * Search adapter for the Torrent Downloads torrent site (based on custom search RSS feeds)
 * 
 * @author Eric Kok
 */
public class TorrentDownloadsAdapter extends RssFeedSearchAdapter {
	
	protected SearchResult fromRssItemToSearchResult(Item item) {
		// Direct .torrent file download in style http://www.torrentdownloads.net/torrent/<id>/<title>
		// Web links (as appearing in the RSS item) in style http://www.torrentdownloads.net/download/<id>/<title>
		TorrentDownloadsItem theItem = (TorrentDownloadsItem) item;
		return new SearchResult(
				item.getTitle(), 
				item.getLink().replace("/torrent/", "/download/"),
				item.getLink(),
				FileSizeConverter.getSize(theItem.getSize()),  
				item.getPubdate(),
				theItem.getSeeders(), 
				theItem.getLeechers());
	}

	@Override
	protected String getUrl(String query, SortOrder order) {
		// Note: doesn't support different list sortings
		return "http://www.torrentdownloads.net/rss.xml?type=search&search=" + URLEncoder.encode(query);
	}

	@Override
	protected RssParser getRssParser(String url) {
		return new TorrentDownloadsRssParser(url);
	}
	
	/**
	 * Custom Item with addition size, seeders and leechers data properties
	 */
	public class TorrentDownloadsItem extends Item {
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
	public class TorrentDownloadsRssParser extends RssParser {

		public TorrentDownloadsRssParser(String url) {
			super(url);
		}
		
		public Item createNewItem() {
			return new TorrentDownloadsItem();
		}

	    public void addAdditionalData(String localName, Item item, String text) {
	    	TorrentDownloadsItem theItem = (TorrentDownloadsItem) item;
	    	if (localName.equalsIgnoreCase("size")) {
	    		theItem.setSize(Long.parseLong(text.trim()));
	    	}
	    	if (localName.equalsIgnoreCase("seeders")) {
	    		theItem.setSeeders(Integer.parseInt(text.trim()));
	    	}
	    	if (localName.equalsIgnoreCase("leechers")) {
	    		theItem.setLeechers(Integer.parseInt(text.trim()));
	    	}
	    }
	}

	@Override
	public String getSiteName() {
		return "Torrent Downloads";
	}
	
}

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

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.util.FileSizeConverter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Search adapter for the NyaaTorrents torrent site (based on custom search RSS feeds)
 * 
 * @author Eric Kok, Mario Franco
 */
public class NyaaTorrentsAdapter extends RssFeedSearchAdapter {

	protected SearchResult fromRssItemToSearchResult(Item item) {
		NyaaTorrentsItem theItem = (NyaaTorrentsItem) item;
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
			return "http://www.nyaa.se/?page=rss&term=" + URLEncoder.encode(query, "UTF-8").replace("+", "%20") + "" + (order == SortOrder.BySeeders? "&sort=2": "");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected RssParser getRssParser(String url) {
		return new NyaaTorrentsRssParser(url);
	}
	
	/**
	 * Custom Item with addition torrentLink. size, seeders and leechers data properties
	 */
	public class NyaaTorrentsItem extends Item {
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
	public class NyaaTorrentsRssParser extends RssParser {

		public NyaaTorrentsRssParser(String url) {
			super(url);
		}
		
		public Item createNewItem() {
			return new NyaaTorrentsItem();
		}

	    public void addAdditionalData(String localName, Item item, String text) {
	    	NyaaTorrentsItem theItem = (NyaaTorrentsItem) item;
            if (item != null) {
                if (localName.equalsIgnoreCase("link")) {
                    theItem.setEnclosureUrl(text.trim());
                }
                if (localName.equalsIgnoreCase("guid")) {
                    theItem.setTorrentLink(text.trim());
                }
                if (localName.equalsIgnoreCase("description")) {
                    String[] data = text.trim().split("[, -]");
					if (data != null && data.length > 0) {
						theItem.setSeeders(Integer.parseInt(data[0].trim()));
						theItem.setLeechers(Integer.parseInt(data[3].trim()));

						double size = Double.parseDouble(data[10].trim());
						if (data[11].trim().equals("MiB")) {
							size = size * 1024 * 1024;
						} else if (data[11].trim().equals("GiB")) {
							size = size * 1024 * 1024 * 1024;
						}
						theItem.setSize((long) size);
					}
                }
            }
	    }
	}

	@Override
	public String getSiteName() {
		return "NyaaTorrents";
	}

	@Override
	public boolean isPrivateSite() {
		return false;
	}

	@Override
	public boolean usesToken() {
		return false;
	}

}

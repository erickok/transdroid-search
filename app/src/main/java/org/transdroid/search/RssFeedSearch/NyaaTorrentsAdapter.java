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
				theItem.getTorrentLink(),
				item.getLink(),
				theItem.getSize(),
				item.getPubdate(), 
				theItem.getSeeders(), 
				theItem.getLeechers());
	}

	@Override
	protected String getUrl(String query, SortOrder order) {
		try {
			return "https://nyaa.si/?page=rss&q=" + URLEncoder.encode(query, "UTF-8").replace("+", "%20") + "" + (order == SortOrder.BySeeders? "&o=desc": "");
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
		private String size;
		private int seeders;
		private int leechers;
		private int downloads;

		public void setTorrentLink(String torrentLink) { this.torrentLink = torrentLink; }
		public void setSize(String size) { this.size = size; }
		public void setSeeders(int seeders) { this.seeders = seeders; }
		public void setLeechers(int leechers) { this.leechers = leechers; }
		public void setDownloads(int downloads) { this.leechers = downloads; }
		public String getTorrentLink() { return torrentLink; }
		public String getSize() { return size; }
		public int getSeeders() { return seeders; }
		public int getLeechers() { return leechers; }
		public int getDownloads() { return downloads; }
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
					theItem.setTorrentLink(text.trim());
                }
                if (localName.equalsIgnoreCase("guid")) {
					theItem.setEnclosureUrl(text.trim());
					theItem.setLink(text.trim());
                }
				if (localName.equalsIgnoreCase("seeders")) {
					theItem.setSeeders(Integer.parseInt(text.trim()));

				}
				if (localName.equalsIgnoreCase("leechers")) {
					theItem.setLeechers(Integer.parseInt(text.trim()));
				}
				if (localName.equalsIgnoreCase("downloads")) {
					theItem.setDownloads(Integer.parseInt(text.trim()));
				}
				if (localName.equalsIgnoreCase("size")) {
					theItem.setSize(text.trim());
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

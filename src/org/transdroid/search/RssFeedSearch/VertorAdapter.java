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
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;

/**
 * Search adapter for the Vertor torrent site (based on custom search RSS feeds)
 * 
 * @author Eric Kok
 */
public class VertorAdapter extends RssFeedSearchAdapter {
	
	protected SearchResult fromRssItemToSearchResult(Item item) {
		// Web links in format http://www.vertor.com/torrents/<torrent-id>/<torrent-name>
		int torIDStart = "http://www.vertor.com/torrents/".length();
		int torIDEnd = item.getLink().indexOf("/", torIDStart);
		String torID = item.getLink().substring(torIDStart,torIDEnd);
		String d = item.getDescription();
		// Description includes size...
		int sizeStart = d.indexOf("Size: ") + "Size: ".length();
		String size = d.substring(sizeStart, d.indexOf(" ", sizeStart));
		size = size.replace("&nbsp;", "");
		// ... and seeders/leechers
		int statusStart = d.indexOf("Status: ") + "Status: ".length();
		int seeders = Integer.parseInt(d.substring(statusStart, d.indexOf(" ", statusStart)));
		int leechersStart = d.indexOf("seeders, ", statusStart) + "seeders, ".length();
		int leechers = Integer.parseInt(d.substring(leechersStart, d.indexOf(" ", leechersStart)));
		return new SearchResult(
				item.getTitle(), 
				"http://www.vertor.com/index.php?mod=download&id=" + torID, 
				"http://www.vertor.com/index.php?mod=view&id=" + torID, 
				size,  
				item.getPubdate(), 
				seeders, 
				leechers);
	}

	@Override
	protected String getUrl(String query, SortOrder order) {
		return "http://www.vertor.com/index.php?mod=rss_search&words=" + URLEncoder.encode(query) + "&search=1" + (order == SortOrder.BySeeders? "&orderby=a.seeds": "");
	}

	@Override
	public String getSiteName() {
		return "Vertor";
	}
	
}

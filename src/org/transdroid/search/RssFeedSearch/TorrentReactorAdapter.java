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
 * Search adapter for the Torrentreactor.to torrent site (based on custom search RSS feeds)
 * 
 * @author Eric Kok
 */
public class TorrentReactorAdapter extends RssFeedSearchAdapter {

	protected SearchResult fromRssItemToSearchResult(Item item) {
		// For a weblink: http://www.torrentreactor.net/torrents/<id>/<title>
		// Torrent at:    http://dl5.torrentreactor.net/download.php?id=<id>&name=<title>
		String link = item.getLink();
		int iID = "http://www.torrentreactor.net/torrents/".length(); 
		String sID = link.substring(iID, link.indexOf("/", iID));
		// Description includes size...
		String d = item.getDescription();
		int sizeStart = d.indexOf("Size: ") + "Size: ".length();
		String size = d.substring(sizeStart, d.indexOf(" MB", sizeStart) + " MB".length());
		// ... and seeders/leechers
		int statusStart = d.indexOf("Status: ") + "Status: ".length();
		int seeders = Integer.parseInt(d.substring(statusStart, d.indexOf(" ", statusStart)));
		int leechersStart = d.indexOf("seeders, ", statusStart) + "seeders, ".length();
		int leechers = Integer.parseInt(d.substring(leechersStart, d.indexOf(" ", leechersStart)));
		
		return new SearchResult(
				item.getTitle(), 
				"http://dl5.torrentreactor.net/download.php?id=" + sID, 
				item.getLink(),  
				size,
				item.getPubdate(),
				seeders,
				leechers);
	}

	@Override
	protected String getUrl(String query, SortOrder order) {
		return "http://www.torrentreactor.net/rss.php?search=" + URLEncoder.encode(query) + (order == SortOrder.BySeeders? "&orderby=a.seeds": "");
	}

}

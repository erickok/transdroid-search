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
 * Search adapter for the Monova torrent site (based on custom search RSS feeds)
 * 
 * @author Eric Kok
 */
public class MonovaAdapter extends RssFeedSearchAdapter {

	protected SearchResult fromRssItemToSearchResult(Item item) {
		// Description in format 'Seeds: <num> Peers: <num> Filesize: <sizestring>'
		String d = item.getDescription();
		// Size
		String size = "";
		int sizeExist = d.indexOf("Filesize: ");
		if (sizeExist >= 0) {
			int sizeStart = sizeExist + "Filesize: ".length();
			size = d.substring(sizeStart);
		}
		// Seeds
		int seedsExist = d.indexOf("Seeds: ");
		int seeders = 0;
		if (seedsExist >= 0) {
			int seedsStart = seedsExist + "Seeds: ".length();
			try {
				seeders = Integer.parseInt(d.substring(seedsStart, d.indexOf(" ", seedsStart)));
			} catch (Exception e) { }
		}
		// Leechers
		int leechers = 0;
		int leechersExist = d.indexOf("Peers: ");
		if (leechersExist >= 0) {
			int leechersStart = leechersExist + "Peers: ".length();
			try {
				leechers = Integer.parseInt(d.substring(leechersStart, d.indexOf(" ", leechersStart)));
			} catch (Exception e) { }
		}
		return new SearchResult(
				item.getTitle(), 
				item.getEnclosureUrl(), 
				item.getLink(), 
				size,  
				item.getPubdate(), 
				seeders, 
				leechers);
	}

	@Override
	protected String getUrl(String query, SortOrder order) {
		return "http://www.monova.org/rss.php?type=search&term=" + URLEncoder.encode(query) + (order == SortOrder.BySeeders? "&order=seeds": "");
	}

	@Override
	public String getSiteName() {
		return "Monova";
	}
	
}

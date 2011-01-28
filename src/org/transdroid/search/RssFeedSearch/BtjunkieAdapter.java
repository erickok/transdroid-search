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
 * Search adapter for the BTJunkie torrent site (based on custom search RSS feeds)
 * 
 * @author Eric Kok
 */
public class BtjunkieAdapter extends RssFeedSearchAdapter {

	protected SearchResult fromRssItemToSearchResult(Item item) {
		int size = item.getDescription().indexOf("Size: ") + 6;
		String t = item.getTitle(); // The title in format 'name  [seeders/leechers]'
		int i = t.lastIndexOf("["); // Where the [seeders/leechers] string starts
		String seeders = t.substring(i + 1, t.indexOf("/", i)); // The seeders text
		String leechers = t.substring(t.indexOf("/", i) + 1, t.indexOf("]", i)); // The seeders text
		return new SearchResult(
				t.substring(0, i - 2), 
				item.getLink() + "/download.torrent", 
				item.getLink(), 
				item.getDescription().substring(size), 
				item.getPubdate(),
				(seeders.equals("X")? 0: Integer.parseInt(seeders)),
				(leechers.equals("X")? 0: Integer.parseInt(leechers)));
	}

	@Override
	protected String getUrl(String query, SortOrder order) {
		return "http://btjunkie.org/rss.xml?query=" + URLEncoder.encode(query) + (order == SortOrder.BySeeders? "&o=22": "");
	}

	@Override
	public String getSiteName() {
		return "BTJunkie";
	}
	
}

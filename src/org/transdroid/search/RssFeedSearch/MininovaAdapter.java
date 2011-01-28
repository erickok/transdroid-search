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
 * Search adapter for the Mininova torrent site (based on custom search RSS feeds)
 * 
 * @author eric
 *
 */
public class MininovaAdapter extends RssFeedSearchAdapter {

	protected SearchResult fromRssItemToSearchResult(Item item) {
		String desc = item.getDescription();
		int size = desc.indexOf("Size: ") + 6;
		int ratio = desc.indexOf("Ratio: ") + 7;
		int seeds = desc.indexOf("seeds", ratio) + 7;
		return new SearchResult(item.getTitle(), item.getLink().replace("/tor/", "/get/"), item.getLink(), 
				desc.substring(size, desc.indexOf("<", size)).replace("&nbsp;", " "),  
				item.getPubdate(),
				Integer.parseInt(desc.substring(ratio, desc.indexOf(" ", ratio))),
				Integer.parseInt(desc.substring(seeds, desc.indexOf(" ", seeds))));
	}

	@Override
	protected String getUrl(String query, SortOrder order) {
		return "http://www.mininova.org/rss/" + URLEncoder.encode(query) + (order == SortOrder.BySeeders? "/seeds": "");
	}

	@Override
	public String getSiteName() {
		return "Mininova";
	}
	
}

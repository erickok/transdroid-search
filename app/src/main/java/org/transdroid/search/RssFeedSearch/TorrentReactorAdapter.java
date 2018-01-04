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
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;

/**
 * Search adapter for the Torrentreactor.net torrent site (based on custom search RSS feeds).
 *
 * NOTE: Currently doesn't provides a direct .torrent link, so it is disabled.
 *
 * @author Eric Kok
 */
public class TorrentReactorAdapter extends RssFeedSearchAdapter {

	protected SearchResult fromRssItemToSearchResult(Item item) {
		// Description includes size...
		String d = item.getDescription();
		int sizeStart = d.indexOf("Size: ") + "Size: ".length();
		String size = d.substring(sizeStart, d.indexOf(",", sizeStart) + ",".length());
		// ... and seeders/leechers
		int statusStart = d.indexOf("Status: ") + "Status: ".length();
		int seeders = Integer.parseInt(d.substring(statusStart, d.indexOf(" ", statusStart)));
		int leechersStart = d.indexOf("seeder, ", statusStart) + "seeder, ".length();
		int leechers = Integer.parseInt(d.substring(leechersStart, d.indexOf(" ", leechersStart)));

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
		// NOTE: Torrent Reactor doesn't support sorting in the RSS feed
		try {
			return "http://www.torrentreactor.net/rss.php?search=" + URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getSiteName() {
		return "TorrentReactor";
	}

	public AuthType getAuthType() {
		return AuthType.NONE;
	}

	public String[] getRequiredCookies() {
		return null;
	}

}

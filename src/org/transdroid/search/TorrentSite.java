/*
 *	This file is part of Transdroid Torrent Search 
 *	<http://code.google.com/p/transdroid-search/>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
package org.transdroid.search;

import java.util.List;

import org.transdroid.search.Isohunt.IsohuntAdapter;
import org.transdroid.search.RssFeedSearch.BtjunkieAdapter;
import org.transdroid.search.RssFeedSearch.ExtraTorrentAdapter;
import org.transdroid.search.RssFeedSearch.KickassTorrentsAdapter;
import org.transdroid.search.RssFeedSearch.MininovaAdapter;
import org.transdroid.search.RssFeedSearch.MonovaAdapter;
import org.transdroid.search.RssFeedSearch.TorrentDownloadsAdapter;
import org.transdroid.search.RssFeedSearch.TorrentReactorAdapter;
import org.transdroid.search.RssFeedSearch.VertorAdapter;
import org.transdroid.search.ThePirateBay.ThePirateBayAdapter;

/**
 * Provides factory-like access to all the torrent site search adapters.
 * 
 * @author Eric Kok
 */
public enum TorrentSite {

	Btjunkie {
		public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
			return new BtjunkieAdapter().search(query, order, maxResults);
		}
	},
	ExtraTorrent {
		public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
			return new ExtraTorrentAdapter().search(query, order, maxResults);
		}
	},
	Isohunt {
		public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
			return new IsohuntAdapter().search(query, order, maxResults);
		}
	},
	KickassTorents {
		public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
			return new KickassTorrentsAdapter().search(query, order, maxResults);
		}
	},
	Mininova {
		public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
			return new MininovaAdapter().search(query, order, maxResults);
		}
	},
	Monova {
		public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
			return new MonovaAdapter().search(query, order, maxResults);
		}
	},
	ThePirateBay {
		public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
			return new ThePirateBayAdapter().search(query, order, maxResults);
		}
	},
	TorrentDownloads {
		public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
			return new TorrentDownloadsAdapter().search(query, order, maxResults);
		}
	},
	TorrentReactor {
		public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
			return new TorrentReactorAdapter().search(query, order, maxResults);
		}
	},
	Vertor {
		public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
			return new VertorAdapter().search(query, order, maxResults);
		}
	};
	
	public abstract List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception;

	/**
	 * Returns the TorrentSite corresponding to the Enum type name it 
	 * has, e.g. <code>TorrentSite.fromCode("Mininova")</code> returns 
	 * the <code>TorrentSite.Isohunt</code> enumeration value
	 * @param siteCode The name of the enum type value
	 * @return The corresponding enum type value of a torrent site
	 */
	public static TorrentSite fromCode(String siteCode) {
		try {
			return Enum.valueOf(TorrentSite.class, siteCode);
		} catch (Exception e) {
			return null;
		}
	}
	
}

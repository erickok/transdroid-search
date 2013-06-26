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
package org.transdroid.search;

import java.util.List;

import org.transdroid.search.Fenopy.FenopyAdapter;
import org.transdroid.search.Isohunt.IsohuntAdapter;
import org.transdroid.search.RssFeedSearch.BitSnoopAdapter;
import org.transdroid.search.RssFeedSearch.ExtraTorrentAdapter;
import org.transdroid.search.RssFeedSearch.KickassTorrentsAdapter;
import org.transdroid.search.RssFeedSearch.LimeTorrentsAdapter;
import org.transdroid.search.RssFeedSearch.MininovaAdapter;
import org.transdroid.search.RssFeedSearch.MonovaAdapter;
import org.transdroid.search.RssFeedSearch.TorrentDownloadsAdapter;
import org.transdroid.search.RssFeedSearch.TorrentReactorAdapter;
import org.transdroid.search.RssFeedSearch.VertorAdapter;
import org.transdroid.search.ThePirateBay.ThePirateBayAdapter;
import org.transdroid.search.ThePirateBay.ThePirateBayMirrorAdapter;

/**
 * Provides factory-like access to all the torrent site search adapters.
 * 
 * @author Eric Kok
 */
public enum TorrentSite {
	BitSnoop {
		@Override
		public ISearchAdapter getAdapter() {
			return new BitSnoopAdapter();
		}
	},
	ExtraTorrent {
		@Override
		public ISearchAdapter getAdapter() {
			return new ExtraTorrentAdapter();
		}
	},
	Fenopy {
		@Override
		public ISearchAdapter getAdapter() {
			return new FenopyAdapter();
		}
	},
	Isohunt {
		@Override
		public ISearchAdapter getAdapter() {
			return new IsohuntAdapter();
		}
	},
	KickassTorents {
		@Override
		public ISearchAdapter getAdapter() {
			return new KickassTorrentsAdapter();
		}
	},
	LimeTorrents {
		@Override
		public ISearchAdapter getAdapter() {
			return new LimeTorrentsAdapter();
		}
	},
	Mininova {
		@Override
		public ISearchAdapter getAdapter() {
			return new MininovaAdapter();
		}
	},
	Monova {
		@Override
		public ISearchAdapter getAdapter() {
			return new MonovaAdapter();
		}
	},
	ThePirateBay {
		@Override
		public ISearchAdapter getAdapter() {
			return new ThePirateBayAdapter();
		}
	},
	ThePirateBayMirror {
		@Override
		public ISearchAdapter getAdapter() {
			return new ThePirateBayMirrorAdapter();
		}
	},
	TorrentDownloads {
		@Override
		public ISearchAdapter getAdapter() {
			return new TorrentDownloadsAdapter();
		}
	},
	TorrentReactor {
		@Override
		public ISearchAdapter getAdapter() {
			return new TorrentReactorAdapter();
		}
	},
	Vertor {
		@Override
		public ISearchAdapter getAdapter() {
			return new VertorAdapter();
		}
	};

	/**
	 * Directly and synchronously perform the search for torrents matching the given query string.
	 * @param query The raw (non-urlencoded) query to search for
	 * @param order The preferred order in which results are sorted
	 * @param maxResults Maximum number of results to return
	 * @return The list of found torrents on the site matching the search query
	 * @throws Exception When an exception occurred during the loading or parsing of the search results
	 */
	public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
		return getAdapter().search(query, order, maxResults);
	}

	public abstract ISearchAdapter getAdapter();

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

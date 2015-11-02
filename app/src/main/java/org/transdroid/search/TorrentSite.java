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

import android.content.Context;

import org.transdroid.search.AsiaTorrents.AsiaTorrentsAdapter;
import org.transdroid.search.BTN.BTNAdapter;
import org.transdroid.search.BitHdtv.BitHdtvAdapter;
import org.transdroid.search.Danishbits.DanishbitsAdapter;
import org.transdroid.search.HoundDawgs.HoundDawgsAdapter;
import org.transdroid.search.StrikeSearch.StrikeSearchAdapter;
import org.transdroid.search.RssFeedSearch.BitSnoopAdapter;
import org.transdroid.search.RssFeedSearch.ExtraTorrentAdapter;
import org.transdroid.search.RssFeedSearch.KickassTorrentsAdapter;
import org.transdroid.search.RssFeedSearch.LimeTorrentsAdapter;
import org.transdroid.search.RssFeedSearch.MininovaAdapter;
import org.transdroid.search.RssFeedSearch.NyaaTorrentsAdapter;
import org.transdroid.search.RssFeedSearch.TorrentDownloadsAdapter;
import org.transdroid.search.ScambioEtico.ScambioEtico;
import org.transdroid.search.ThePirateBay.ThePirateBayAdapter;
import org.transdroid.search.TorrentDay.TorrentDayAdapter;
import org.transdroid.search.TorrentLeech.TorrentLeechAdapter;
import org.transdroid.search.hdbitsorg.HdBitsOrgAdapter;
import org.transdroid.search.hdtorrents.HdTorrentsAdapter;
import org.transdroid.search.rarbg.RarbgAdapter;
import org.transdroid.search.revolutiontt.RevolutionTTAdapter;

import java.io.InputStream;
import java.util.List;

/**
 * Provides factory-like access to all the torrent site search adapters.
 * @author Eric Kok
 */
public enum TorrentSite {
	AsiaTorrents {
		@Override
		public ISearchAdapter getAdapter() {
			return new AsiaTorrentsAdapter();
		}
	},
	BitSnoop {
		@Override
		public ISearchAdapter getAdapter() {
			return new BitSnoopAdapter();
		}
	},
	BitHdtv {
		@Override
		public ISearchAdapter getAdapter() {
			return new BitHdtvAdapter();
		}
	},
	BTN {
		@Override
		public ISearchAdapter getAdapter() {
			return new BTNAdapter();
		}
	},
	Danishbits {
		@Override
		public ISearchAdapter getAdapter() {
			return new DanishbitsAdapter();
		}
	},
	ExtraTorrent {
		@Override
		public ISearchAdapter getAdapter() {
			return new ExtraTorrentAdapter();
		}
	},
	HdBitsOrg {
		@Override
		public ISearchAdapter getAdapter() {
			return new HdBitsOrgAdapter();
		}
	},
	HdTorrents {
		@Override
		public ISearchAdapter getAdapter() {
			return new HdTorrentsAdapter();
		}
	},
	HoundDawgs {
		@Override
		public ISearchAdapter getAdapter() {
			return new HoundDawgsAdapter();
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
	NyaaTorrents {
		@Override
		public ISearchAdapter getAdapter() {
			return new NyaaTorrentsAdapter();
		}
	},
	ThePirateBay {
		@Override
		public ISearchAdapter getAdapter() {
			return new ThePirateBayAdapter();
		}
	},
	ScambioEtico {
		@Override
		public ISearchAdapter getAdapter() {
			return new ScambioEtico();
		}
	},
	StrikeSearch {
		@Override
		public ISearchAdapter getAdapter() {
			return new StrikeSearchAdapter();
		}
	},
	RevolutionTT {
		@Override
		public ISearchAdapter getAdapter() {
			return new RevolutionTTAdapter();
		}
	},
	Rarbg {
		@Override
		public ISearchAdapter getAdapter() {
			return new RarbgAdapter();
		}
	},
	TorrentDay {
		@Override
		public ISearchAdapter getAdapter() {
			return new TorrentDayAdapter();
		}
	},
	TorrentDownloads {
		@Override
		public ISearchAdapter getAdapter() {
			return new TorrentDownloadsAdapter();
		}
	},
	TorrentLeech {
		@Override
		public ISearchAdapter getAdapter() {
			return new TorrentLeechAdapter();
		}
	};

	/**
	 * Directly and synchronously perform the search for torrents matching the given query string.
	 * @param context The Android activity/provider context from which the shared preferences can be accessed
	 * @param query The raw (non-urlencoded) query to search for
	 * @param order The preferred order in which results are sorted
	 * @param maxResults Maximum number of results to return
	 * @return The list of found torrents on the site matching the search query
	 * @throws Exception When an exception occurred during the loading or parsing of the search results
	 */
	public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {
		return getAdapter().search(context, query, order, maxResults);
	}

	/**
	 * Synchronously set up a connection to download a specific torrent file and return an input stream to this. Authentication and authorization is
	 * off-loaded to the implementing torrent site.
	 * @param context The Android activity/provider context from which the shared preferences can be accessed
	 * @param url The full url of the torrent file to download
	 * @return An InputStream handle to the requested file so it can be further downloaded, or null if no connection is possible (like when the device
	 * is offline or when the user is not authorized)
	 * @throws Exception When an exception occurred during the retrieval of the request url
	 */
	public InputStream getTorrentFile(Context context, String url) throws Exception {
		return getAdapter().getTorrentFile(context, url);
	}

	public abstract ISearchAdapter getAdapter();

	/**
	 * Returns the TorrentSite corresponding to the Enum type name it has, e.g. <code>TorrentSite.fromCode("Mininova")</code> returns the
	 * <code>TorrentSite.Mininova</code> enumeration value
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

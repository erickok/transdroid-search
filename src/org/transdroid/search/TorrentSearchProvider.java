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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

/**
 * Main entry point for Android applications that want to query for torrent search results.
 * 
 * @author Eric Taix
 * @author Eric Kok
 */
public class TorrentSearchProvider extends ContentProvider {

	public static final String PROVIDER_NAME = "org.transdroid.search.torrentsearchprovider";

	/**
	 * The content URI to use. Useful if the application have access to this class. Otherwise it must build
	 * the URI like<br/>
	 * <code>Uri uri = Uri.parse("content://org.transdroid.search.torrentsearchprovider/search/ubuntu");</code>
	 * <br/>
	 * And within an activity then call:<br/>
	 * <code>Cursor cur = managedQuery(uri, null, null, null, null);</code>
	 **/
	public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/search");
	public static final String SELECTION_SITE = "SITE = ?";

	private static final int SEARCH_TERM = 1;

	// Static intialization of the URI matcher
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "search/*", SEARCH_TERM);
	}

	/*
	 * Not supported by this content provider
	 */
	@Override
	public int delete(Uri uriP, String selectionP, String[] selectionArgsP) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uriP) {
		switch (uriMatcher.match(uriP)) {
		case SEARCH_TERM:
			return "vnd.android.cursor.dir/vnd.transdroid.torrent";
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uriP);
		}
	}

	/*
	 * Not supported by this content provider
	 */
	@Override
	public Uri insert(Uri uriP, ContentValues valuesP) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String,
	 * java.lang.String[], java.lang.String)
	 */
	@Override
	public Cursor query(Uri uriP, String[] projectionP, String selectionP, String[] selectionArgsP, String sortOrderP) {

		// The available columns; note that an _ID is a
		// ContentProvider-requirement
		String[] columnNames = new String[] { "_ID", "NAME", "TORRENTURL", "DETAILSURL", "SIZE", "ADDED", "SEEDERS",
			"LEECHERS" };
		MatrixCursor cursor = new MatrixCursor(columnNames);

		String term = "";
		SortOrder order = SortOrder.BySeeders; // Use BySeeders as default
		final int maxResults = 30;
		TorrentSite site = TorrentSite.Mininova; // Use Mininova as default

		// Retrieve the search term, site and order
		if (uriMatcher.match(uriP) == SEARCH_TERM) {
			term = uriP.getPathSegments().get(1);
		}
		if (selectionP != null && selectionP.equals(SELECTION_SITE) && selectionArgsP != null
			&& selectionArgsP.length > 0) {
			site = TorrentSite.fromCode(selectionArgsP[0]);
			if (site == null) {
				throw new RuntimeException(selectionArgsP[0] + " is not a valid torrent site. " + 
					"To get the available sites, use " + TorrentSitesProvider.PROVIDER_NAME);
			}
		}
		if (sortOrderP != null) {
			order = SortOrder.fromCode(sortOrderP);
			if (site == null) {
				throw new RuntimeException(sortOrderP + " is not a valid sort order. " + 
					"Only BySeeders and Combined are supported.");
			}
		}

		Log.d(TorrentSearchProvider.class.getName(), 
			"Term: '" + term + "' Site: " + site.toString() + " Order: " + order.toString());
		if (!term.equals("")) {

			// Perform the actual search
			try {
				List<SearchResult> results = site.search(term, order, maxResults);
				// Return the results as MatrixCursor
				int id = 0;
				for (SearchResult result : results) {
					Object[] values = new Object[8];
					values[0] = id++;
					values[1] = result.getTitle();
					values[2] = result.getTorrentUrl();
					values[3] = result.getDetailsUrl();
					values[4] = result.getSize();
					values[5] = result.getAddedDate() != null? result.getAddedDate().getTime(): -1;
					values[6] = result.getSeeds();
					values[7] = result.getLeechers();
					cursor.addRow(values);
				}
			} catch (Exception e) {
				// Log the error and stack trace, but also throw an explicit run-time exception for clarity 
				Log.d(TorrentSearchProvider.class.getName(), e.toString());
				for (StackTraceElement stack : e.getStackTrace()) {
					Log.d(TorrentSearchProvider.class.getName(), stack.toString());
				}
				throw new RuntimeException(e.toString());
			}

		}
		
		// Register the content URI for changes (although update() isn't supported)
		cursor.setNotificationUri(getContext().getContentResolver(), uriP);
		return cursor;
		
	}

	/*
	 * Not supported by this content provider
	 */
	@Override
	public int update(Uri uriP, ContentValues valuesP, String selectionP, String[] selectionArgsP) {
		throw new UnsupportedOperationException();
	}

}

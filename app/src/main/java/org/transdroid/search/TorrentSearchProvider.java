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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.security.auth.login.LoginException;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

/**
 * Main entry point for Android applications that want to query for torrent search results.
 * @author Eric Taix
 * @author Eric Kok
 */
public class TorrentSearchProvider extends ContentProvider {

	public static final String PROVIDER_NAME = "org.transdroid.search.torrentsearchprovider";

	/**
	 * The content URI to use. Useful if the application have access to this class. Otherwise it must build the URI like<br/>
	 * <code>Uri uri = Uri.parse("content://org.transdroid.search.torrentsearchprovider/search/ubuntu");</code> <br/>
	 * And within an activity then call:<br/>
	 * <code>Cursor cur = managedQuery(uri, null, null, null, null);</code> The torrent files to which the search
	 * results point to, i.e. the url in the TORRENTURL column, can be retrieved by calling
	 * {@link ContentResolver#openInputStream(Uri)} on this provider. The URI to be provided looks like:
	 * <code>Uri uri = Uri.parse("content://org.transdroid.search.torrentsearchprovider/get/Mininova/http%3A%2F%2Fwww.mininova.org%2Ftor%2F3190129%2F0");</code>
	 * <br />
	 * Note that the first selection segment contains the torrent side code and the last segment in the URI is the
	 * URL-encoded file url to download.
	 **/
	public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/search");
	public static final String SELECTION_SITE = "SITE = ?";

	private static final int SEARCH_TERM = 1;
	private static final int ENCODED_TORRENTURL = 2;

	// Static intialization of the URI matcher
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "search/*", SEARCH_TERM);
		uriMatcher.addURI(PROVIDER_NAME, "get/*/*", ENCODED_TORRENTURL);
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
		case ENCODED_TORRENTURL:
			return "vnd.android.cursor.dir/vnd.transdroid.torrentfile";
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

		if (uriMatcher.match(uriP) != SEARCH_TERM) {
			Log.e(TorrentSearchProvider.class.getName(), "query() does not support the " + uriP + " url type");
			throw new RuntimeException("query() is only possible with the /search/* action; to download a file using "
					+ "/get*/* use the ContentResolver's openInputStream(Uri) function.");
		}
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
			// TODO: Support searching multiple sites at once
			site = TorrentSite.fromCode(selectionArgsP[0]);
			if (site == null) {
				throw new RuntimeException(selectionArgsP[0] + " is not a valid torrent site. "
						+ "To get the available sites, use " + TorrentSitesProvider.PROVIDER_NAME);
			}
		}
		if (sortOrderP != null) {
			order = SortOrder.fromCode(sortOrderP);
			if (order == null) {
				throw new RuntimeException(sortOrderP + " is not a valid sort order. "
						+ "Only BySeeders and Combined are supported.");
			}
		}

		Log.d(TorrentSearchProvider.class.getName(), "Term: '" + term + "' Site: " + site.toString() + " Order: "
				+ order.toString());
		if (!term.equals("")) {

			// Perform the actual search
			try {
				List<SearchResult> results = site.search(getContext(), term, order, maxResults);
				// Return the results as MatrixCursor
				int id = 0;
				for (SearchResult result : results) {
					Object[] values = new Object[8];
					values[0] = id++;
					values[1] = result.getTitle();
					values[2] = result.getTorrentUrl();
					values[3] = result.getDetailsUrl();
					values[4] = result.getSize();
					values[5] = result.getAddedDate() != null ? result.getAddedDate().getTime() : -1;
					values[6] = result.getSeeds();
					values[7] = result.getLeechers();
					cursor.addRow(values);
				}
			} catch (LoginException e) {
	            // this toast really shouldn't be implemented here, but main app doesn't currently notify user
	            // of a login failure, it simply says no search results.
				backgroundToast(R.string.login_failure);
				
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

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		if (uriMatcher.match(uri) == ENCODED_TORRENTURL) {

			// Get the torrent site and url to download from the Uri specifier (which is URL-decoded by the Uri class)
			String url = uri.getPathSegments().get(2);
			TorrentSite site = TorrentSite.fromCode(uri.getPathSegments().get(1));

			// Download the requested file and store it locally
			InputStream input = null;
			File tempFile = new File("/not/yet/set");
			try {

				// Request the file handle from the search site adapter, which takes case or user credentials too
				input = site.getTorrentFile(getContext(), url);

				// Write a temporary file with the torrent contents
				tempFile = File.createTempFile("transdroidsearch_", ".torrent", getContext().getCacheDir());
				FileOutputStream output = new FileOutputStream(tempFile);
				try {
					final byte[] buffer = new byte[1024];
					int read;
					while ((read = input.read(buffer)) != -1)
						output.write(buffer, 0, read);
					output.flush();
				} finally {
					output.close();
				}
			} catch (IOException e) {
				Log.e(TorrentSearchProvider.class.getSimpleName(), "Can't write input stream for " + url + " to "
						+ tempFile.toString() + ": " + e.toString());
				throw new RuntimeException("The requested url " + url + " (via " + site.getAdapter().getSiteName()
						+ ") could not be downloaded, as either we cannot store files locally.");
			} catch (Exception e) {
				Log.e(TorrentSearchProvider.class.getSimpleName(), "Can't write input stream for " + url + " to "
						+ tempFile.toString() + ": " + e.toString());
				throw new RuntimeException("The requested url " + url + " (via " + site.getAdapter().getSiteName()
						+ ") could not be downloaded, as either we have no access to that url or it does not exist.");

			} finally {
				try {
					if (input != null)
						input.close();
				} catch (IOException e) {
					Log.e(TorrentSearchProvider.class.getSimpleName(),
							"Error closing the input stream of " + tempFile.toString() + " for " + url + " : "
									+ e.toString());
				}
			}

			return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
		}
		throw new RuntimeException("Files can only be opened using the /get/*/* uri type.");
	}
	
    // =========================================================
    // UTILITY METHODS
    // =========================================================
    
    private void backgroundToast(final int resourceId) {
        new Thread() {
            @Override public void run() {                                    
            	final Context context = TorrentSearchProvider.this.getContext();
            	
            	Looper.prepare();
                Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(context, context.getString(resourceId), Toast.LENGTH_LONG).show();
                    }
                });                
                Looper.loop();
                Looper.myLooper().quit();
            }
        }.start();
    }

}

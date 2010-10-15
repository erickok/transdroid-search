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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/**
 * Main entry point for Android applications that want to query for torrent
 * search results.
 * 
 * @author Eric Taix
 * @author Eric Kok
 */
public class TorrentSearchProvider extends ContentProvider {

  public static final String PROVIDER_NAME = "org.transdroid.search.torrentsearchprovider";

  /** The content URI to use. Useful if the application have access to this class. Otherwise it must build the URI like<br/>
   <code>Uri uri = Uri.parse("content://oorg.transdroid.search.torrentsearchprovider/search/mininova/eric%20Taix");</code><br/>
   And within an activity then call:<br/>
   <code>Cursor cur = managedQuery(uri, null, null, null, null);</code>
   **/
  public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/search");

  private static final int SEARCH_TERM = 1;
  private static final int SITE_AND_SEARCH_TERM = 2;

  // Static intialization of the URI matcher
  private static final UriMatcher uriMatcher;
  static {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(PROVIDER_NAME, "search/*", SEARCH_TERM);
    uriMatcher.addURI(PROVIDER_NAME, "search/*/*", SITE_AND_SEARCH_TERM);
  }

  /*
   * (non-Javadoc)
   * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
   */
  @Override
  public int delete(Uri uriP, String selectionP, String[] selectionArgsP) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
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
   * (non-Javadoc)
   * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
   */
  @Override
  public Uri insert(Uri uriP, ContentValues valuesP) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * @see android.content.ContentProvider#onCreate()
   */
  @Override
  public boolean onCreate() {
    return false;
  }

  /*
   * (non-Javadoc)
   * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String,
   * java.lang.String[], java.lang.String)
   */
  @Override
  public Cursor query(Uri uriP, String[] projectionP, String selectionP, String[] selectionArgsP, String sortOrderP) {
    
	String[] columnNames = new String[] { "NAME", "TORRENTURL", "DETAILSURL", "SIZE", "ADDED", "SEEDERS", "LEECHERS" };
    MatrixCursor curs = new MatrixCursor(columnNames);
    
    String term = "";
    // TODO: Allow the sort order as parameter
    final SortOrder order = SortOrder.BySeeders;
    final int maxResults = 30;
    TorrentSite site = TorrentSite.Mininova; // Use Mininova as default
    
    // Retrieve the search term and possibly site
    if (uriMatcher.match(uriP) == SITE_AND_SEARCH_TERM) {
    	String siteCode = uriP.getPathSegments().get(1);
    	site = TorrentSite.fromCode(siteCode);
    	term = uriP.getPathSegments().get(2);
    }
    if (uriMatcher.match(uriP) == SEARCH_TERM) {
  	  term = uriP.getPathSegments().get(1);
    }
    
    if (!term.equals("") && site != null) {
	
      // Perform the actual search
      try {
    	  List<SearchResult> results = site.search(term, order, maxResults);
	      // Return the results as MatrixCursor
	      for (SearchResult result : results) {
	    	Object[] values = new Object[5];
	        values[0] = result.getTitle();
	        values[1] = result.getTorrentUrl();
	        values[2] = result.getDetailsUrl();
	        values[3] = result.getSize();
	        values[4] = result.getAddedDate().toString(); // TODO: Can we add this as DateTime?
	        values[5] = result.getSeeds();
	        values[6] = result.getLeechers();
	        curs.addRow(values);
	      }
	  } catch (Exception e) {
		// TODO: We have to do something with this error, but I'm not sure how ContentProiders are supposed to communicate back errors to using classes
		e.printStackTrace();
	  }
      
    }
    // Register to watch a content URI for changes (don't really know what it means ?)
    curs.setNotificationUri(getContext().getContentResolver(), uriP);
    return curs;
  }

  /*
   * (non-Javadoc)
   * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String,
   * java.lang.String[])
   */
  @Override
  public int update(Uri uriP, ContentValues valuesP, String selectionP, String[] selectionArgsP) {
    throw new UnsupportedOperationException();
  }

}

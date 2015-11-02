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
package org.transdroid.search.gui;

import org.transdroid.search.TorrentSite;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * A helper class to access user settings form the {@link SharedPreferences} and gives access to the used preferences
 * keys for other classes that store/read the user preferences directly.
 * 
 * @author Eric Kok
 */
public class SettingsHelper {

	// Used to store if a site is enabled/disabled (with the site name appended to this key)
	static final String PREF_SITE_ENABLED = "pref_key_site_";
	// Used to store a private site's user credentials (with the site name appended to this key)
	static final String PREF_SITE_USER = "pref_key_user_";
	static final String PREF_SITE_PASS = "pref_key_pass_";
	static final String PREF_SITE_TOKEN = "pref_key_token_";

	/**
	 * Determines if a torrent site is currently enabled by the user, based on the user settings. Public sites are
	 * simply enabled/disabled with a check box but private sites are only enabled when proper user credentials have
	 * been supplied.
	 * @param context The android activity or prodiver context to access shared preferences from
	 * @param site The site for which to determine if it is enabled
	 * @return True if the site is enabled and should be available in the search and torrent site providers; false
	 *         otherwise
	 */
	public static boolean isSiteEnabled(Context context, TorrentSite site) {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		// For public sites use the PREF_SITE_ENABLED-based preference only
		if (!site.getAdapter().isPrivateSite())
			return prefs.getBoolean(PREF_SITE_ENABLED + site.name(), true);

		// For private sites see if a token or username and password are specified as well
		if (!prefs.getBoolean(PREF_SITE_ENABLED + site.name(), true))
			return false;
		if (site.getAdapter().usesToken()) {
			if (prefs.getString(PREF_SITE_TOKEN + site.name(), null) == null)
				return false;
		} else {
			if (prefs.getString(PREF_SITE_USER + site.name(), null) == null)
				return false;
			if (prefs.getString(PREF_SITE_PASS + site.name(), null) == null)
				return false;
		}
		return true;

	}

	/**
	 * Returns the name that the user specified in the settings as site-specific credentials.
	 * @param context The android activity or provider context to access shared preferences from
	 * @param site The site for which to retrieve the user name
	 * @return The name that the user entered in the settings as site-specific user name, or null if no user name was
	 *         entered
	 */
	public static String getSiteUser(Context context, TorrentSite site) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(PREF_SITE_USER + site.name(), null);
	}

	/**
	 * Returns the password that the user specified in the settings as site-specific credentials.
	 * @param context The android activity or provider context to access shared preferences from
	 * @param site The site for which to retrieve the password
	 * @return The password that the user entered in the settings as site-specific user pass, or null if no password was
	 *         entered
	 */
	public static String getSitePass(Context context, TorrentSite site) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(PREF_SITE_PASS + site.name(), null);
	}

}

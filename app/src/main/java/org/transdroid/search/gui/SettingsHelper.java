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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

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
	static final String PREF_SITE_COOKIE = "pref_key_cookie_";

	/**
	 * Determines if a torrent site is currently enabled by the user, based on the user settings. Public sites are
	 * simply enabled/disabled with a check box but private sites are only enabled when proper user credentials have
	 * been supplied.
	 * @param prefs The shared preferences to read from
	 * @param site The site for which to determine if it is enabled
	 * @return True if the site is enabled and should be available in the search and torrent site providers; false
	 *         otherwise
	 */
	public static boolean isSiteEnabled(SharedPreferences prefs, TorrentSite site) {

		// For public sites use the PREF_SITE_ENABLED-based preference only
		if (!site.getAdapter().isPrivateSite())
			return prefs.getBoolean(PREF_SITE_ENABLED + site.name(), true);

		// For private sites see if a token or username and password are specified as well
		if (!prefs.getBoolean(PREF_SITE_ENABLED + site.name(), true))
			return false;

		switch (site.getAdapter().getAuthType()) {
			case TOKEN:
				return prefs.getString(PREF_SITE_TOKEN + site.name(), null) != null;
			case USERNAME:
				return prefs.getString(PREF_SITE_USER + site.name(), null) != null
						&& prefs.getString(PREF_SITE_PASS + site.name(), null) != null;
			case COOKIES:
				for (String cookie : site.getAdapter().getRequiredCookies()) {
					final String cookieValue = getSiteCookie(prefs, site, cookie);
					if (cookieValue == null) {
						return false;
					}
				}
				return true;
		}
		return true;
	}

	/**
	 * Returns the name that the user specified in the settings as site-specific credentials.
	 * @param prefs The shared preferences to read from
	 * @param site The site for which to retrieve the user name
	 * @return The name that the user entered in the settings as site-specific user name, or null if no user name was
	 *         entered
	 */
	public static String getSiteUser(SharedPreferences prefs, TorrentSite site) {
		return prefs.getString(PREF_SITE_USER + site.name(), null);
	}

	/**
	 * Returns the password that the user specified in the settings as site-specific credentials.
	 * @param prefs The shared preferences to read from
	 * @param site The site for which to retrieve the password
	 * @return The password that the user entered in the settings as site-specific user pass, or null if no password was
	 *         entered
	 */
	public static String getSitePass(SharedPreferences prefs, TorrentSite site) {
		return prefs.getString(PREF_SITE_PASS + site.name(), null);
	}
	
	/**
	 * Returns the API token that the user specified in the settings as site-specific credentials.
	 * @param prefs The shared preferences to read from
	 * @param site The site for which to retrieve the API key/access token
	 * @return The API token that the user entered in the settings as site-specific user pass, or null if none
	 *         entered
	 */
	public static String getSiteToken(SharedPreferences prefs, TorrentSite site) {
		return prefs.getString(PREF_SITE_TOKEN + site.name(), null);
	}

	public static void setSiteCookie(Editor editor, TorrentSite site, String name, String value) {
		editor.putString(PREF_SITE_COOKIE + site.name() + "_" + name, value);
	}

	public static String getSiteCookie(SharedPreferences prefs, TorrentSite site, String name) {
		return prefs.getString(PREF_SITE_COOKIE + site.name() + "_" + name, null);
	}
}

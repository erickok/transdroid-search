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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Pair;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.adapters.custom.CustomSiteAdapter;

import java.util.ArrayList;
import java.util.List;

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
	static final String PREF_SITE_CUSTOM_NAME = "pref_key_custom_name_";
	static final String PREF_SITE_CUSTOM_URL = "pref_key_custom_url_";
	private static final String CODE_PREFIX_CUSTOM = "custom_";

	public static ISearchAdapter getSiteByCode(SharedPreferences prefs, String code) {
		if (code.startsWith(CODE_PREFIX_CUSTOM)) {
			final int index = Integer.parseInt(code.substring(code.length() - 1));
			final Pair<String, ISearchAdapter> customSite = getCustomSite(prefs, index);
			return customSite == null ? null : customSite.second;
		}
		return TorrentSite.fromCode(code).getAdapter();
	}

	public static List<Pair<String, ISearchAdapter>> getAllSites(SharedPreferences prefs) {
		List<Pair<String, ISearchAdapter>> all = new ArrayList<>();
		for (final TorrentSite site : TorrentSite.values()) {
			all.add(Pair.create(site.name(), site.getAdapter()));
		}
		int i = 0;
		while (prefs.contains(PREF_SITE_CUSTOM_URL + i)) {
			all.add(getCustomSite(prefs, i++));
		}
		return all;
	}

	/**
	 * Determines if a torrent site is currently enabled by the user, based on the user settings. Public sites are
	 * simply enabled/disabled with a check box but private sites are only enabled when proper user credentials have
	 * been supplied.
	 * @param prefs The shared preferences to read from
	 * @param siteCode The site code for which to determine if it is enabled
	 * @param adapter The search adapter for this site
	 * @return True if the site is enabled and should be available in the search and torrent site providers; false
	 *         otherwise
	 */
	public static boolean isSiteEnabled(SharedPreferences prefs, String siteCode, ISearchAdapter adapter) {
		switch (adapter.getAuthType()) {
			case CUSTOM:
				return true;
			case NONE:
				return prefs.getBoolean(PREF_SITE_ENABLED + siteCode, false);
			case TOKEN:
				return prefs.getString(PREF_SITE_TOKEN + siteCode, null) != null;
			case USERNAME:
				return prefs.getString(PREF_SITE_USER + siteCode, null) != null
						&& prefs.getString(PREF_SITE_PASS + siteCode, null) != null;
			case COOKIES:
				for (String cookie : adapter.getRequiredCookies()) {
					final String cookieValue = getSiteCookie(prefs, siteCode, cookie);
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

	public static void setSiteCookie(Editor editor, String siteCode, String name, String value) {
		editor.putString(PREF_SITE_COOKIE + siteCode + "_" + name, value);
	}

	public static String getSiteCookie(SharedPreferences prefs, String siteCode, String name) {
		return prefs.getString(PREF_SITE_COOKIE + siteCode + "_" + name, null);
	}

	static Pair<String, ISearchAdapter> getCustomSite(SharedPreferences prefs, int index) {
		if (!prefs.contains(PREF_SITE_CUSTOM_URL + index))
			return null;
		String siteUrl = prefs.getString(PREF_SITE_CUSTOM_URL + index, "");
		String siteDomain = Uri.parse(siteUrl).getHost();
		String siteName = prefs.getString(PREF_SITE_CUSTOM_NAME + index, siteDomain);
		if (siteName == null) siteName = siteUrl;
		ISearchAdapter adapter = new CustomSiteAdapter(siteName, siteUrl);
		return Pair.create(CODE_PREFIX_CUSTOM + index, adapter);
	}

	static String getCustomSiteName(SharedPreferences prefs, int index) {
		return prefs.getString(PREF_SITE_CUSTOM_NAME + index, null);
	}

	static String getCustomSiteUrl(SharedPreferences prefs, int index) {
		return prefs.getString(PREF_SITE_CUSTOM_URL + index, null);
	}

	static void removeCustomSite(SharedPreferences prefs, int sortOrder) {
		final Editor editor = prefs.edit();
		// Move all sites later in the list 'up' one place
		int i = sortOrder;
		while (prefs.contains(PREF_SITE_CUSTOM_URL + (i + 1))) {
			editor.putString(PREF_SITE_CUSTOM_NAME + i, prefs.getString(PREF_SITE_CUSTOM_NAME + (i + 1), null))
					.putString(PREF_SITE_CUSTOM_URL + i, prefs.getString(PREF_SITE_CUSTOM_URL + (i + 1), null));
			i++;
		}
		editor
				.remove(PREF_SITE_CUSTOM_NAME + i)
				.remove(PREF_SITE_CUSTOM_URL + i)
				.apply();
	}

}

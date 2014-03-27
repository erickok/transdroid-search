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

import java.util.Locale;

import org.transdroid.search.TorrentSite;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

/**
 * Represents a public site in as preference activity list item and allow enabling/disabling the site.
 * @author Eric Kok
 */
public class PublicSitePreference extends CheckBoxPreference {

	public PublicSitePreference(Context context, int sortOrder, TorrentSite torrentSite) {
		super(context);
		setOrder(sortOrder);
		setTitle(torrentSite.getAdapter().getSiteName());
		setKey(SettingsHelper.PREF_SITE_ENABLED + torrentSite.name());
		setDefaultValue(true);
	}

	@Override
	public int compareTo(Preference another) {
		// Override default Preference comparison to compare forcefully on the torrent site name
		return getTitle().toString().toLowerCase(Locale.getDefault())
				.compareTo(another.getTitle().toString().toLowerCase(Locale.getDefault()));
	}

}

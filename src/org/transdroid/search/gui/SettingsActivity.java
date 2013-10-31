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

import org.transdroid.search.R;
import org.transdroid.search.TorrentSite;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();

		// Load the preferences screen
		if (getPreferenceScreen() != null)
			getPreferenceScreen().removeAll();
		addPreferencesFromResource(R.xml.pref_settings);
		
		// Retrieve all torrent sites and build a preference object for them
		int publicCounter = 101;
		int privateCounter = 201;
		TorrentSite[] sites = TorrentSite.values();
		for (TorrentSite torrentSite : sites) {
			if (torrentSite.getAdapter().isPrivateSite())
				getPreferenceScreen().addPreference(new PrivateSitePreference(this, privateCounter++, torrentSite));
			else 
				getPreferenceScreen().addPreference(new PrivateSitePreference(this, publicCounter++, torrentSite));
		}

	}
	
}

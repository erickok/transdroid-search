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

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Pair;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.R;

import java.util.List;

import static org.transdroid.search.ISearchAdapter.AuthType.CUSTOM;
import static org.transdroid.search.ISearchAdapter.AuthType.NONE;

/**
 * Activity that shows all public and private torrent sites, enable/disable them or enter settings, and to add custom RSS search feeds.
 * @author Eric Kok
 */
public class SettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsActivity.SettingsFragment()).commit();
		}
	}

	public static class SettingsFragment extends PreferenceFragment implements CustomSitePreference.OnCustomSiteChanged {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			onCustomSiteChanged();
		}

		@Override
		public void onCustomSiteChanged() {
			// Load the preferences screen
			if (getPreferenceScreen() != null) {
				getPreferenceScreen().removeAll();
			}
			addPreferencesFromResource(R.xml.pref_settings);

			// Retrieve all torrent sites and build a preference object for them
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			int publicCounter = 101;
			int privateCounter = 201;
			int customIndex = 0;
			int customCounter = 301;
			List<Pair<String, ISearchAdapter>> sites = SettingsHelper.getAllSites(prefs);
			PreferenceCategory publicGroup = (PreferenceCategory) findPreference("header_publicsites");
			PreferenceCategory privateGroup = (PreferenceCategory) findPreference("header_privatesites");
			PreferenceCategory customGroup = (PreferenceCategory) findPreference("header_customsites");
			for (Pair<String, ISearchAdapter> torrentSite : sites) {
				if (torrentSite.second.getAuthType() == CUSTOM) {
					customGroup.addPreference(new CustomSitePreference(getActivity(), customIndex++, customCounter++, this));
				} else if (torrentSite.second.getAuthType() != NONE) {
					privateGroup.addPreference(new PrivateSitePreference(getActivity(), privateCounter++, torrentSite));
				} else {
					publicGroup.addPreference(new PublicSitePreference(getActivity(), publicCounter++, torrentSite));
				}
			}
			customGroup.addPreference(new CustomSitePreference(getActivity(), customIndex, customCounter, this));
		}
	}

}

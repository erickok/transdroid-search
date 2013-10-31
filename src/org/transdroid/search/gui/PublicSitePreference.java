package org.transdroid.search.gui;

import java.util.Locale;

import org.transdroid.search.TorrentSite;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

public class PublicSitePreference extends CheckBoxPreference {

	public PublicSitePreference(Context context, int sortOrder, TorrentSite torrentSite) {
		super(context);
		setOrder(sortOrder);
		setTitle(torrentSite.getAdapter().getSiteName());
		setKey(SettingsHelper.PREF_SITE_ENABLED + torrentSite.name().toUpperCase(Locale.UK));
		setDefaultValue(true);
	}

	@Override
	public int compareTo(Preference another) {
		// Override default Preference comparison to compare forcefully on the torrent site name
		return getTitle().toString().compareTo(another.getTitle().toString());
	}
	
}

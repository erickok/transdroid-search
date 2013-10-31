package org.transdroid.search.gui;

import org.transdroid.search.TorrentSite;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceGroup;

public class PrivateSitePreference extends PreferenceGroup {

	public PrivateSitePreference(Context context, int sortOrder, TorrentSite torrentSite) {
		super(context, null);
		setOrder(sortOrder);
		setTitle(torrentSite.getAdapter().getSiteName());
	}

	@Override
	public int compareTo(Preference another) {
		// Override default Preference comparison to compare forcefully on the torrent site name
		return getTitle().toString().compareTo(another.getTitle().toString());
	}
	
}

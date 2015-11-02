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

import org.transdroid.search.R;
import org.transdroid.search.TorrentSite;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

/**
 * Represents a private site as preference activity list item and allows entering the token or username and password for
 * the site via a pop-up dialog.
 * @author Eric Kok
 */
public class PrivateSitePreference extends DialogPreference {

	private final TorrentSite torrentSite;
	private final boolean usesToken;
	private EditText userEdit, passEdit, tokenEdit;

	public PrivateSitePreference(Context context, int sortOrder, TorrentSite torrentSite) {
		super(context, null);
		this.torrentSite = torrentSite;
		this.usesToken = torrentSite.getAdapter().usesToken();

		// Set up the credentials dialog and the preference appearance
		setOrder(sortOrder);
		setTitle(torrentSite.getAdapter().getSiteName());
		if (usesToken) {
			setDialogLayoutResource(R.layout.dialog_token);
			setDialogTitle(R.string.pref_token);
		} else {
			setDialogLayoutResource(R.layout.dialog_credentials);
			setDialogTitle(R.string.pref_credentials);
			String currentUser = PreferenceManager.getDefaultSharedPreferences(getContext())
					.getString(SettingsHelper.PREF_SITE_USER + torrentSite.name(), null);
			if (currentUser != null)
				setSummary(currentUser);
		}

	}

	@Override
	public int compareTo(Preference another) {
		// Override default Preference comparison to compare forcefully on the torrent site name
		return getTitle().toString().toLowerCase(Locale.getDefault())
				.compareTo(another.getTitle().toString().toLowerCase(Locale.getDefault()));
	}

	@Override
	protected View onCreateDialogView() {
		View dialog = super.onCreateDialogView();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		if (usesToken) {
			tokenEdit = (EditText) dialog.findViewById(R.id.token);
			// Show token for easy modification
			tokenEdit.setText(prefs.getString(SettingsHelper.PREF_SITE_TOKEN + torrentSite.name(), ""));
		} else {
			userEdit = (EditText) dialog.findViewById(R.id.username);
			passEdit = (EditText) dialog.findViewById(R.id.password);
			// Show username for easy modification
			userEdit.setText(prefs.getString(SettingsHelper.PREF_SITE_USER + torrentSite.name(), ""));
		}
		return dialog;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult) {
			if (usesToken) {
				persistToken(tokenEdit.getText().toString());
			} else {
				persistUserAndPass(userEdit.getText().toString(), passEdit.getText().toString());
			}
		}
	}

	private void persistToken(String token) {
		if (TextUtils.isEmpty(token))
			token = null;
		Editor edit = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
		edit.putString(SettingsHelper.PREF_SITE_TOKEN + torrentSite.name(), token);
		edit.commit();
	}

	private void persistUserAndPass(String username, String password) {
		// Store the new username and password
		if (TextUtils.isEmpty(username))
			username = null;
		if (TextUtils.isEmpty(password))
			password = null;
		Editor edit = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
		edit.putString(SettingsHelper.PREF_SITE_USER + torrentSite.name(), username);
		edit.putString(SettingsHelper.PREF_SITE_PASS + torrentSite.name(), password);
		edit.commit();
		// Show the username in the preference activity
		setSummary(username);
	}

}

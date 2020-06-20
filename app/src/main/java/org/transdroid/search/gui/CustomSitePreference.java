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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.R;

/**
 * Represents a custom RSS search site as preference activity list item and allows entering the optional name and url
 * via a pop-up dialog.
 *
 * @author Eric Kok
 */
public class CustomSitePreference extends DialogPreference {

    private final SharedPreferences prefs;
    private final int index;
    private OnCustomSiteChanged onCustomSiteChanged;
    private EditText nameEdit, urlEdit;

    CustomSitePreference(Context context, int index, int sortOrder, OnCustomSiteChanged onCustomSiteChanged) {
        super(context, null);
        this.index = index;
        this.onCustomSiteChanged = onCustomSiteChanged;

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Pair<String, ISearchAdapter> currentSite = SettingsHelper.getCustomSite(prefs, index);
        String currentUrl = SettingsHelper.getCustomSiteUrl(prefs, index);

        setOrder(sortOrder);
        setTitle(currentSite == null ? context.getString(R.string.pref_customsites_add) : currentSite.second.getSiteName());
        setDialogLayoutResource(R.layout.dialog_custom);
        setDialogTitle(R.string.pref_hint_url);
        if (currentUrl != null) {
            String host = Uri.parse(currentUrl).getHost();
            setSummary(host != null ? host : currentUrl);
        }
    }

    @Override
    protected View onCreateDialogView() {
        final View dialog = super.onCreateDialogView();
        nameEdit = dialog.findViewById(R.id.name);
        urlEdit = dialog.findViewById(R.id.url);
        // Show name and url for easy modification
        nameEdit.setText(SettingsHelper.getCustomSiteName(prefs, index));
        urlEdit.setText(SettingsHelper.getCustomSiteUrl(prefs, index));
        // Show delete button
        ImageButton deleteButton = dialog.findViewById(R.id.delete);
        deleteButton.setOnClickListener(v -> {
            SettingsHelper.removeCustomSite(prefs, index);
            getDialog().dismiss();
            onCustomSiteChanged.onCustomSiteChanged();
        });
        return dialog;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String name = nameEdit.getText().toString();
            String url = urlEdit.getText().toString();
            if (TextUtils.isEmpty(url)) {
                prefs.edit()
                        .remove(SettingsHelper.PREF_SITE_CUSTOM_NAME + index)
                        .remove(SettingsHelper.PREF_SITE_CUSTOM_URL + index)
                        .apply();
            } else {
                if (TextUtils.isEmpty(name))
                    name = null;
                prefs.edit()
                        .putString(SettingsHelper.PREF_SITE_CUSTOM_NAME + index, name)
                        .putString(SettingsHelper.PREF_SITE_CUSTOM_URL + index, url)
                        .apply();
            }
            onCustomSiteChanged.onCustomSiteChanged();
        }
    }

    interface OnCustomSiteChanged {
        void onCustomSiteChanged();
    }

}

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
package org.transdroid.search.adapters.custom;

import android.util.Log;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.adapters.rss.RssFeedSearchAdapter;
import org.transdroid.util.FileSizeConverter;
import org.xml.sax.Attributes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class CustomSiteAdapter extends RssFeedSearchAdapter {

    private static final String LOG_TAG = CustomSiteAdapter.class.getSimpleName();

    private final String name;
    private final String url;

    public CustomSiteAdapter(String name, String url) {
        this.name = name;
        this.url = url;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.CUSTOM;
    }

    @Override
    public String getSiteName() {
        return name;
    }

    @Override
    protected String getUrl(String query, SortOrder order) {
        if (!url.startsWith("http") && !url.startsWith("https")) {
            Log.e(LOG_TAG, "Custom site '" + name + "' URL does not start with http(s), but is '" + url + "'");
        }
        if (!url.contains("%s")) {
            Log.e(LOG_TAG, "Custom site '" + name + "' URL does not contain '%s', but is '" + url + "'");
        }
        try {
            return String.format(Locale.US, url, URLEncoder.encode(query, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null; // UFT-8 always available on Android
        }
    }

    @Override
    protected RssParser getRssParser(String url) {
        return new CustomSiteParser(url);
    }

    @Override
    protected SearchResult fromRssItemToSearchResult(Item item) {

        CustomSiteItem theItem = (CustomSiteItem) item;
        return new SearchResult(
                item.getTitle(),
                item.getTheLink(),
                theItem.detailsLink,
                FileSizeConverter.getSize(theItem.size),
                item.getPubdate(),
                theItem.seeders,
                theItem.leechers);
    }

    static class CustomSiteItem extends Item {
        String detailsLink;
        int seeders, leechers;
        long size;
    }

    static class CustomSiteParser extends RssParser {

        CustomSiteParser(String url) {
            super(url);
        }

        @Override
        protected Item createNewItem() {
            return new CustomSiteItem();
        }

        @Override
        protected void addAdditionalData(String localName, String qName, Attributes attributes, Item item) {
            CustomSiteItem theItem = (CustomSiteItem) item;

            if (qName.equalsIgnoreCase("torznab:attr")
                    && attributes != null && attributes.getLength() > 0) {
                long asNumber = 0;
                try {
                    if (attributes.getValue("value") != null) {
                        asNumber = Long.parseLong(attributes.getValue("value"));
                    }
                } catch (NumberFormatException ignored) {
                }

                if (attributes.getValue("name").equalsIgnoreCase("seeders")) {
                    theItem.seeders = (int) asNumber;
                }
                if (attributes.getValue("name").equalsIgnoreCase("peers")) {
                    theItem.leechers = (int) asNumber;
                }
            }
        }

        @Override
        protected void addAdditionalData(String localName, Item item, String text) {
            CustomSiteItem theItem = (CustomSiteItem) item;
            long asNumber = 0;
            try {
                asNumber = Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
            }
            if (localName.equalsIgnoreCase("url")
                    || localName.equalsIgnoreCase("comments")
                    || localName.equalsIgnoreCase("guid")) {
                theItem.detailsLink = text.trim();
            }
            if (localName.equalsIgnoreCase("size")) {
                theItem.size = asNumber;
            }
            if (localName.equalsIgnoreCase("seeders")) {
                theItem.seeders = (int) asNumber;
            }
            if (localName.equalsIgnoreCase("leechers")) {
                theItem.leechers = (int) asNumber;
            }
            if (localName.equalsIgnoreCase("grabs")) {
                // Not accurate, but it's something
                theItem.seeders = (int) asNumber;
                theItem.leechers = (int) asNumber;
            }
        }
    }

}

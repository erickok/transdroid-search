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
package org.transdroid.search.adapters.rss.publictrackers;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.adapters.rss.RssFeedSearchAdapter;
import org.transdroid.util.FileSizeConverter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Search adapter for the LimeTorrents torrent site (based on custom search RSS feeds)
 * This adapter does not support sorting.
 *
 * @author Eric Kok
 */
public class LimeTorrentsAdapter extends RssFeedSearchAdapter {

    protected SearchResult fromRssItemToSearchResult(Item item) {

        LimeTorrentsItem theItem = (LimeTorrentsItem) item;
        return new SearchResult(
                item.getTitle(),
                item.getEnclosureUrl(),
                item.getLink(),
                FileSizeConverter.getSize(theItem.size),
                item.getPubdate(),
                theItem.seeders,
                theItem.leechers);
    }

    @Override
    protected String getUrl(String query, SortOrder order) {
        try {
            return "https://www.limetorrents.co/searchrss/" + URLEncoder.encode(query, "UTF-8") + "/";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected RssParser getRssParser(String url) {
        return new LimeTorrentsParser(url);
    }

    @Override
    public String getSiteName() {
        return "LimeTorrents";
    }

    public AuthType getAuthType() {
        return AuthType.NONE;
    }

    /**
     * Custom item with the torrent size and seeders and leechters numbers
     */
    public static class LimeTorrentsItem extends Item {
        public int seeders, leechers;
        public long size;
    }

    /**
     * Custom parser to parse the additional comments data property
     */
    public static class LimeTorrentsParser extends RssParser {

        public LimeTorrentsParser(String url) {
            super(url);
        }

        public Item createNewItem() {
            return new LimeTorrentsItem();
        }

        public void addAdditionalData(String localName, Item item, String text) {
            LimeTorrentsItem theItem = (LimeTorrentsItem) item;
            if (localName.equalsIgnoreCase("description")) {
                // Contains the seeders and leechers, which looks something like 'Seeds: 1 , Leechers 9'
                try {
                    String description = text.trim();
                    String seedersText = "Seeds: ";
                    int seedersStart = description.indexOf(seedersText);
                    int seedersEnd = description.indexOf(" ", seedersStart + seedersText.length());
                    if (seedersStart >= 0 && seedersEnd >= seedersStart) {
                        theItem.seeders = Integer.parseInt(description.substring(seedersStart + seedersText.length(), seedersEnd));
                    }
                    String leechersText = "Leechers ";
                    int leechersStart = description.indexOf(leechersText);
                    if (leechersStart >= 0) {
                        theItem.leechers = Integer.parseInt(description.substring(leechersStart + leechersText.length()));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            if (localName.equalsIgnoreCase("size")) {
                try {
                    theItem.size = Long.parseLong(text.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }
}

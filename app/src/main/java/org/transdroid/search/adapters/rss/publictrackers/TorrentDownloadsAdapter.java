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
 * Search adapter for the Torrent Downloads torrent site (based on custom search RSS feeds)
 *
 * @author Eric Kok
 */
public class TorrentDownloadsAdapter extends RssFeedSearchAdapter {

    private static final String DOMAIN = "https://www.torrentdownloads.me";

    protected SearchResult fromRssItemToSearchResult(Item item) throws UnsupportedEncodingException {
        // Direct .torrent file download in style http://www.torrentdownloads.me/torrent/<id>/<title>
        // Web links (as appearing in the RSS item) in style http://www.torrentdownloads.me/download/<id>/<title>
        TorrentDownloadsItem theItem = (TorrentDownloadsItem) item;
        return new SearchResult(
                item.getTitle(),
                "magnet:?xt=urn:btih:" + ((TorrentDownloadsItem) item)
                        .getInfoHash().toLowerCase() + "&dn=" +
                        URLEncoder.encode(item.getTitle(), "UTF-8"),
                DOMAIN + item.getLink(),
                FileSizeConverter.getSize(theItem.getSize()),
                item.getPubdate(),
                theItem.getSeeders(),
                theItem.getLeechers());
    }

    @Override
    protected String getUrl(String query, SortOrder order) {
        // Note: doesn't support different list sortings
        try {
            return DOMAIN + "/rss.xml?type=search&search=" + URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected RssParser getRssParser(String url) {
        return new TorrentDownloadsRssParser(url);
    }

    @Override
    public String getSiteName() {
        return "Torrent Downloads";
    }

    public AuthType getAuthType() {
        return AuthType.NONE;
    }

    /**
     * Custom Item with addition size, seeders and leechers data properties
     */
    public static class TorrentDownloadsItem extends Item {
        private long size;
        private int seeders;
        private int leechers;
        private String infoHash;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public int getSeeders() {
            return seeders;
        }

        public void setSeeders(int seeders) {
            this.seeders = seeders;
        }

        public int getLeechers() {
            return leechers;
        }

        public void setLeechers(int leechers) {
            this.leechers = leechers;
        }

        public String getInfoHash() {
            return infoHash;
        }

        public void setInfoHash(String infoHash) {
            this.infoHash = infoHash;
        }
    }

    /**
     * Custom parser to parse the additional size, seeders and leechers data properties
     */
    public static class TorrentDownloadsRssParser extends RssParser {

        public TorrentDownloadsRssParser(String url) {
            super(url);
        }

        public Item createNewItem() {
            return new TorrentDownloadsItem();
        }

        public void addAdditionalData(String localName, Item item, String text) {
            TorrentDownloadsItem theItem = (TorrentDownloadsItem) item;
            if (localName.equalsIgnoreCase("size")) {
                theItem.setSize(Long.parseLong(text.trim()));
            }
            if (localName.equalsIgnoreCase("seeders")) {
                theItem.setSeeders(Integer.parseInt(text.trim()));
            }
            if (localName.equalsIgnoreCase("leechers")) {
                theItem.setLeechers(Integer.parseInt(text.trim()));
            }
            if (localName.equalsIgnoreCase("info_hash")) {
                theItem.setInfoHash(text.trim());
            }
        }
    }

}

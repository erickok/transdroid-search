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
package org.transdroid.search.RssFeedSearch;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Search adapter for the Sky Torrents torrent site (based on custom search RSS feeds)
 *
 * @author Eric Kok
 * @author Thomas Riccardi
 */
public class SkyTorrentsAdapter extends RssFeedSearchAdapter {

  // Example: '4 seeder(s), 1 leecher(s), 18 file(s) 204.4 MB'
  private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("(\\d+) seeder\\(s\\), (\\d+) leecher\\(s\\), (\\d+) file\\(s\\) (.*)");

  protected SearchResult fromRssItemToSearchResult(Item item) {
    SkyTorrentsItem theItem = (SkyTorrentsItem) item;
    return new SearchResult(
        item.getTitle(),
        item.getLink(),
        theItem.getGUID(),
        theItem.getSize(),
        item.getPubdate(),
        theItem.getSeeders(),
        theItem.getLeechers());
  }

  @Override
  protected String getUrl(String query, SortOrder order) {
    // Note: doesn't support different list sortings
    try {
      return "https://www.skytorrents.in/rss/all/ed/1/" + URLEncoder.encode(query, "UTF-8").replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  protected RssParser getRssParser(String url) {
    return new SkyTorrentsRssParser(url);
  }

  /**
   * Custom Item with addition size, seeders and leechers data properties
   */
  public class SkyTorrentsItem extends Item {
    private String size;
    private int seeders;
    private int leechers;
    private String GUID;
    public void setSize(String size) { this.size = size; }
    public void setSeeders(int seeders) { this.seeders = seeders; }
    public void setLeechers(int leechers) { this.leechers = leechers; }
    public void setGUID(String GUID) { this.GUID = GUID; }
    public String getSize() { return size; }
    public int getSeeders() { return seeders; }
    public int getLeechers() { return leechers; }
    public String getGUID() { return GUID; }
  }

  /**
   * Custom parser to parse the additional size, seeders and leechers data properties
   */
  public class SkyTorrentsRssParser extends RssParser {

    public SkyTorrentsRssParser(String url) {
      super(url);
    }

    public Item createNewItem() {
      return new SkyTorrentsItem();
    }

    public void addAdditionalData(String localName, Item item, String text) {
      if (item == null) {
        return;
      }
      SkyTorrentsItem theItem = (SkyTorrentsItem) item;
      if (localName.equalsIgnoreCase("description")) {
        // Contains the seeders, leechers and size, which looks something like '4 seeder(s), 1 leecher(s), 18 file(s) 204.4 MB'
        Matcher matcher = DESCRIPTION_PATTERN.matcher(text.trim());
        if (matcher == null || !matcher.matches()) {
          throw new IllegalStateException(
                  "Impossible to parse Sky Torrents description.");
        }

        theItem.setSeeders(Integer.parseInt(matcher.group(1)));
        theItem.setLeechers(Integer.parseInt(matcher.group(2)));
        //theItem.setFilesCount(matcher.group(3));
        theItem.setSize(matcher.group(4));

      }
      if (localName.equalsIgnoreCase("guid")) {
        try {
          theItem.setGUID(text.trim());
        } catch (Exception e) {
          theItem.setGUID("");
        }
      }
    }
  }

  @Override
  public String getSiteName() {
    return "Sky Torrents";
  }

  @Override
  public boolean isPrivateSite() {
    return false;
  }

  @Override
  public boolean usesToken() {
    return false;
  }

}

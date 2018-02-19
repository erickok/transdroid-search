package org.transdroid.search.adapters.html.privatetrackers;

import android.content.SharedPreferences;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.transdroid.search.adapters.html.AbstractHtmlAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class TorrentDayAdapter extends AbstractHtmlAdapter {
  private static final String BASE_URL = "https://www.torrentday.com/";
  private static final String QUERY_URL = BASE_URL + "browse.php?search=%1$s";
  private static final String COOKIE_UID = "uid";
  private static final String COOKIE_PASS = "pass";

  @Override
  protected String getLoginUrl() {
    return BASE_URL;
  }

  @Override
  protected String getSearchUrl(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws UnsupportedEncodingException {
    final String encodedQuery = URLEncoder.encode(query, "UTF-8");
    return String.format(QUERY_URL, encodedQuery);
  }

  @Override
  protected Elements selectTorrentElements(Document document) {
    return document.select("tr.browse");
  }

  @Override
  protected boolean isAuthenticationRequiredForTorrentLink() {
    return true;
  }

  @Override
  protected SearchResult buildSearchResult(Element torrentElement) {
    final Elements torrentNameElement = torrentElement.select("a.torrentName");
    final String title = torrentNameElement.text();
    final String torrentUrl = BASE_URL + torrentElement.select("td.dlLinksInfo > a").attr("href");
    final String detailsUrl = BASE_URL + torrentNameElement.attr("href");
    final String size = torrentElement.select("td.sizeInfo").text();
    final int seeds = Integer.valueOf(torrentElement.select("td.seedersInfo").text());
    final int leechers = Integer.valueOf(torrentElement.select("td.leechersInfo").text());

    return new SearchResult(title, torrentUrl, detailsUrl, size, null, seeds, leechers);
  }

  @Override
  protected TorrentSite getTorrentSite() {
    return TorrentSite.TorrentDay;
  }

  @Override
  public String buildRssFeedUrlFromSearch(SharedPreferences prefs, String query, SortOrder order) {
    // not implemented
    return null;
  }

  @Override
  public String getSiteName() {
    return "TorrentDay";
  }

  public AuthType getAuthType() {
    return AuthType.COOKIES;
  }

  public String[] getRequiredCookies() {
    return new String[]{COOKIE_UID, COOKIE_PASS};
  }
}

package org.transdroid.search.TorrentDay;

import android.content.SharedPreferences;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.gui.SettingsHelper;

public class TorrentDayAdapter implements ISearchAdapter {
  private static final String QUERY_URL = "https://www.torrentday.com/browse.php?search=%1$s";

  public static void main(String[] args) throws Exception {
    new TorrentDayAdapter().search(null, "ufc", null, 10);
  }

  @Override
  public List<SearchResult> search(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws Exception {
    final String encodedQuery = URLEncoder.encode(query, "UTF-8");
    final String url = String.format(QUERY_URL, encodedQuery);

    final String uid = SettingsHelper.getSiteCookie(prefs, TorrentSite.TorrentDay, "uid");
    String pass = SettingsHelper.getSiteCookie(prefs, TorrentSite.TorrentDay, "pass");

    final Document doc = Jsoup.connect(url)
        .cookie("uid", uid)
        .cookie("pass", pass)
        .get();

    final ArrayList<SearchResult> results = new ArrayList<>();
    for (Element element : doc.select("tr.browse")) {
      final Elements torrentNameElement = element.select("a.torrentName");
      final String title = torrentNameElement.text();
      final String torrentUrl = element.select("td.dlLinksInfo > a").attr("href");
      final String detailsUrl = torrentNameElement.attr("href");
      final String size = element.select("td.sizeInfo").text();
      final int seeds = Integer.valueOf(element.select("td.seedersInfo").text());
      final int leechers = Integer.valueOf(element.select("td.leechersInfo").text());
      results.add(new SearchResult(title, torrentUrl, detailsUrl, size, null, seeds, leechers));
      if (results.size() == maxResults) {
        break;
      }
    }
    return results;
  }

  @Override
  public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
    return null;
  }

  @Override
  public String getSiteName() {
    return "TorrentDay";
  }

  @Override
  public boolean isPrivateSite() {
    return true;
  }

  public AuthType getAuthType() {
    return AuthType.COOKIES;
  }

  public String[] getRequiredCookies() {
    return new String[]{"uid", "pass"};
  }

  @Override
  public InputStream getTorrentFile(SharedPreferences prefs, String url) throws Exception {
    return null;
  }
}

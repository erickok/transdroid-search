package org.transdroid.search.TorrentDay;

import android.content.SharedPreferences;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.gui.SettingsHelper;
import org.transdroid.util.HttpHelper;

public class TorrentDayAdapter implements ISearchAdapter {
  private static final String BASE_URL = "https://www.torrentday.com/";
  private static final String QUERY_URL = BASE_URL + "browse.php?search=%1$s";
  private static final String COOKIE_UID = "uid";
  private static final String COOKIE_PASS = "pass";

  @Override
  public List<SearchResult> search(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws Exception {
    final String encodedQuery = URLEncoder.encode(query, "UTF-8");
    final String url = String.format(QUERY_URL, encodedQuery);

    final String uid = SettingsHelper.getSiteCookie(prefs, TorrentSite.TorrentDay, COOKIE_UID);
    final String pass = SettingsHelper.getSiteCookie(prefs, TorrentSite.TorrentDay, COOKIE_PASS);

    final Document doc = Jsoup.connect(url)
        .cookie(COOKIE_UID, uid)
        .cookie(COOKIE_PASS, pass)
        .get();

    final ArrayList<SearchResult> results = new ArrayList<>();
    for (Element element : doc.select("tr.browse")) {
      final Elements torrentNameElement = element.select("a.torrentName");
      final String title = torrentNameElement.text();
      final String torrentUrl = BASE_URL + element.select("td.dlLinksInfo > a").attr("href");
      final String detailsUrl = BASE_URL + torrentNameElement.attr("href");
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

  @Override
  public InputStream getTorrentFile(SharedPreferences prefs, String url) throws Exception {
    final DefaultHttpClient httpClient = new DefaultHttpClient();
    final HttpGet request = new HttpGet(url);

    final String uid = SettingsHelper.getSiteCookie(prefs, TorrentSite.TorrentDay, COOKIE_UID);
    final String pass = SettingsHelper.getSiteCookie(prefs, TorrentSite.TorrentDay, COOKIE_PASS);

    request.setHeader("Cookie", String.format("uid=%s; pass=%s", uid, pass));
    final HttpResponse response = httpClient.execute(request);
    return response.getEntity().getContent();
  }
}

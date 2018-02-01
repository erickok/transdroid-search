package org.transdroid.search.adapters.html.privatetrackers;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.adapters.html.AbstractHtmlAdapter;
import org.transdroid.util.DateUtils;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter for AudioBookBay Private site.
 */
public class AudioBookTorrentsAdapter extends AbstractHtmlAdapter {
  private static final String BASE_URL = "https://audiobookbay.nl";
  private static final String SEARCH_URL = BASE_URL + "/?s=%s";
  private static final String LOGIN_URL = BASE_URL + "/member/login.php";
  private static final Pattern CONTENT_PATTERN = Pattern.compile("Posted: (.*) Piece Size: .* File Size: (.*)");
  @SuppressLint("SimpleDateFormat")
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy");

  @Override
  protected String getLoginUrl() {
    return LOGIN_URL;
  }

  @Override
  protected String getSearchUrl(SharedPreferences prefs, String query, SortOrder order, int maxResults)
      throws UnsupportedEncodingException {
    // Site does not support sorting
    return String.format(SEARCH_URL, URLEncoder.encode(query, "UTF-8"));
  }

  @Override
  protected Elements selectTorrentElements(Document document) {
    return document.select("div.post");
  }

  @Override
  protected SearchResult buildSearchResult(Element torrentElement) {
    final Element titleElement = torrentElement.selectFirst("div.postTitle a");
    final String title = titleElement.text();
    final String detailsUrl = BASE_URL + titleElement.attr("href");

    final String contentText = torrentElement.selectFirst("div.postContent > p:last-child").text();
    final Matcher matcher = CONTENT_PATTERN.matcher(contentText);
    final Date added;
    final String size;
    if (matcher.matches()) {
      added = DateUtils.parseDate(DATE_FORMAT, matcher.group(1));
      size = matcher.group(2);
    } else {
      added = null;
      size = null;
    }

    // There is no proper torrent download link on the search results page so use the datailUrl
    // instead. Will fetch the actual torrentUrl in getTorrentFile()
    // Site does not support seeders/leechers
    return new SearchResult(title, detailsUrl, detailsUrl, size, added, 0, 0);
  }


  @Override
  protected boolean isAuthenticationRequiredForTorrentLink() {
    // Technically not needed because we handle getTorrentFile() ourselves but added for completeness.
    return true;
  }

  /**
   * The search result page does not provide a torrent download link. It's hidden in the detail url.
   * We need to open the detail page and extract it.
   */
  @Override
  public InputStream getTorrentFile(SharedPreferences prefs, String url) throws Exception {
    final DefaultHttpClient httpClient = new DefaultHttpClient();
    authenticateHttpClient(httpClient, prefs);

    HttpResponse response;
    HttpGet request;

    // First open detail page and parse it into a Jsoup object
    request = new HttpGet(url);
    response = httpClient.execute(request);
    final HttpEntity entity = response.getEntity();
    final Document document = Jsoup.parse(entity.getContent(), null, "");
    entity.consumeContent();

    // Find torrent URL in detail page
    final String torrentUrl = BASE_URL + document.selectFirst("a[href^='/download.php?f=']").attr("href");

    // Download torrent file
    request = new HttpGet(torrentUrl);
    response = httpClient.execute(request);
    return response.getEntity().getContent();
  }

  @Override
  protected TorrentSite getTorrentSite() {
    return TorrentSite.AudioBookBay;
  }

  @Override
  public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
    // not implemented
    return null;
  }

  @Override
  public String getSiteName() {
    return "AudioBookBay";
  }

  public AuthType getAuthType() {
    return AuthType.USERNAME;
  }
}

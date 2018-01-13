package org.transdroid.search.Nebulance;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.transdroid.search.AbstractHtmlAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.util.DateUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter for Nebulance Private site.
 */
public class NebulanceAdapter extends AbstractHtmlAdapter {
    private static final String BASE_URL = "https://nebulance.io/";
    private static final String LOGIN_URL = BASE_URL + "login.php";
    private static final String QUERY_URL = BASE_URL + "torrents.php?order_by=time&order_way=desc&searchtext=%s&search_type=0&taglist=%s&tags_type=0";
    private static final Pattern QUALITY_REGEX = Pattern.compile("\\b(\\d+p)\\b");
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd yyyy, HH:mm");

    @Override
    protected String getLoginUrl() {
        return LOGIN_URL;
    }

    @Override
    protected String getSearchUrl(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws UnsupportedEncodingException {
        final Matcher matcher = QUALITY_REGEX.matcher(query);
        final String quality;
        final boolean found = matcher.find();
        if (found) {
            quality = matcher.group(1);
            query = matcher.replaceAll(" ").trim();
        } else {
            quality = "";
        }
        return String.format(QUERY_URL, URLEncoder.encode(query, "UTF-8"), quality);
    }

    @Override
    protected Elements selectTorrentElements(Document document) {
        return document.select("tr[class='torrent rowa'],tr[class='torrent rowb']");
    }

    @Override
    protected SearchResult buildSearchResult(Element torrentElement) {
        final Elements detailLink = torrentElement.select("a[data-browse-id]");
        final Elements downloadLink = torrentElement.select("a[title='Download Torrent']");
        final String quality = torrentElement.select("div.tags > div > a:matches(\\d+p)").text();
        final String title = detailLink.text() + " " + quality;

        final String torrentUrl = BASE_URL + detailLink.attr("href");
        final String detailsUrl = BASE_URL + downloadLink.attr("href");
        final String size = torrentElement.select("td.nobr > div").first().text();
        final Date date = getTorrentDate(torrentElement);
        final Elements children = torrentElement.children();
        final int numChildren = children.size();

        final int seeds = Integer.valueOf(children.get(numChildren - 2).text());
        final int leechers = Integer.valueOf(children.get(numChildren - 1).text());

        return new SearchResult(title, torrentUrl, detailsUrl, size, date, seeds, leechers);
    }

    private Date getTorrentDate(Element torrentElement) {
        final String timeText = torrentElement.select("td.nobr > span.time").text();
        final String timeTooltip = torrentElement.select("td.nobr > span.time").attr("title");
        final Date date = DateUtils.parseDate(DATE_FORMAT, timeTooltip);
        return date == null ? DateUtils.parseDate(DATE_FORMAT, timeText) : date;
    }

    @Override
    protected TorrentSite getTorrentSite() {
        return TorrentSite.Nebulance;
    }

    @Override
    public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
        // not implemented
        return null;
    }

    @Override
    public String getSiteName() {
        return "Nebulance";
    }

    public AuthType getAuthType() {
        return AuthType.USERNAME;
    }
}

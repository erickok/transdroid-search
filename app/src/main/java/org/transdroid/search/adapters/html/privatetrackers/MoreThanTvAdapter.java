package org.transdroid.search.adapters.html.privatetrackers;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.adapters.html.AbstractHtmlAdapter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Adapter for MoreThanTV private site.
 */
public class MoreThanTvAdapter extends AbstractHtmlAdapter {
    private final static String BASE_URL = "https://www.morethan.tv/";
    private static final String LOGIN_URL = BASE_URL + "login.php";
    private static final String SEARCH_URL = BASE_URL + "torrents.php?searchstr=%s&tags_type=1&order_by=%s&order_way=desc&group_results=1&action=basic&searchsubmit=1";

    private static final String ORDER_TIME = "time";
    private static final String ORDER_SEEDERS = "seeders";

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd yyyy, HH:mm");

    @Override
    protected String getSearchUrl(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws UnsupportedEncodingException {
        return String.format(SEARCH_URL, URLEncoder.encode(query, "UTF-8"), order == SortOrder.BySeeders ? ORDER_SEEDERS : ORDER_TIME);
    }

    @Override
    protected Elements selectTorrentElements(Document document) {
        return document.select("tr[class^=torrent]");
    }

    @Override
    protected SearchResult buildSearchResult(Element torrentElement) {
        final Element groupInfo = torrentElement.selectFirst("div[class^=group_info]");
        final Element nameElement = groupInfo.selectFirst("div > a");
        final String title = nameElement.text();
        final String torrentUrl = BASE_URL + groupInfo.selectFirst("div > span > a").attr("href");
        final String detailsUrl = BASE_URL + nameElement.attr("href");


        Date added;
        try {
            added = DATE_FORMAT.parse(torrentElement.selectFirst("span[class='time tooltip']").attr("title"));
        } catch (ParseException e) {
            added = null;
        }
        final Elements numberColumns = torrentElement.select("td[class^='number_column']");
        final String size = numberColumns.get(0).text();
        final int seeds = Integer.parseInt(numberColumns.get(2).text());
        final int leechers = Integer.parseInt(numberColumns.get(3).text());

        return new SearchResult(title, torrentUrl, detailsUrl, size, added, seeds, leechers);
    }

    @Override
    protected String getLoginUrl() {
        return LOGIN_URL;
    }

    @Override
    public String buildRssFeedUrlFromSearch(SharedPreferences prefs, String query, SortOrder order) {
        return null;
    }

    @Override
    public String getSiteName() {
        return "MoreThanTV";
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.USERNAME;
    }

    @Override
    protected TorrentSite getTorrentSite() {
        return TorrentSite.MoreThanTv;
    }
}

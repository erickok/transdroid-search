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
package org.transdroid.search.adapters.html.publictrackers;

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
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An adapter that provides access to The Pirate Bay torrent searches by parsing the raw HTML output.
 * @author Eric Kok
 */
public class ThePirateBayAdapter extends AbstractHtmlAdapter {

	private static final String DOMAIN = "https://proxtpb.art/";
	private static final String QUERYURL = DOMAIN + "/search/%1$s/0/%2$s/0";
	private static final String SORT_COMPOSITE = "3";
	private static final String SORT_SEEDS = "7";
	private static final Pattern PATTERN_DESCRIPTION = Pattern.compile("^Uploaded ([^,]+), Size ([^,]+),.*");

	@Override
	protected String getSearchUrl(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws UnsupportedEncodingException {
		return String.format(QUERYURL, URLEncoder.encode(query, "UTF-8"), (order == SortOrder.BySeeders ? SORT_SEEDS : SORT_COMPOSITE));
	}

	@Override
	protected Elements selectTorrentElements(Document document) {
		return document.select("table[id=searchResult] > tbody > tr");
	}

	@Override
	protected SearchResult buildSearchResult(Element torrentElement) {
		final Elements linkElement = torrentElement.select("a.detLink");
		final String title = linkElement.text();
		final String torrentUrl = torrentElement.select("a[href^=magnet:]").attr("href");
		final String detailsUrl = DOMAIN + linkElement.attr("href");

		final String description = torrentElement.select("font.detDesc").text();
		final Matcher matcher = PATTERN_DESCRIPTION.matcher(description);
		final Date added;
		final String size;
		if (matcher.matches()) {
			added = parseDate(matcher.group(1));
			size = matcher.group(2);
		} else {
			added = null;
			size = "";
		}

		final Elements children = torrentElement.children();
		final int numChildren = children.size();

		final int seeds = Integer.valueOf(children.get(numChildren - 2).text());
		final int leechers = Integer.valueOf(children.get(numChildren - 1).text());
		return new SearchResult(title, torrentUrl, detailsUrl, size, added, seeds, leechers);
	}

	@Override
	protected TorrentSite getTorrentSite() {
		return TorrentSite.ThePirateBay;
	}

	@Override
	public String buildRssFeedUrlFromSearch(SharedPreferences prefs, String query, SortOrder order) {
		return null;
	}

	@Override
	public String getSiteName() {
		return "The Pirate Bay (proxied)";
	}

	private static Date parseDate(String dateString) {
		try {
			dateString = dateString.replace("&nbsp;", " ");
			final Calendar calendar = Calendar.getInstance();
			final String[] split = dateString.split(" ");
			final String[] split1 = split[0].split("-");
			final String[] split2 = split[1].split(":");
			if (split2.length == 2) {
				calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(split2[0]));
				calendar.set(Calendar.MINUTE, Integer.valueOf(split2[1]));
				if (split[0].equals("Y-day")) {
					calendar.add(Calendar.DAY_OF_MONTH, -1);
				} else if (!split[0].equals("Today")) {
					calendar.set(Calendar.MONTH, Integer.valueOf(split1[0]) - 1);
					calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(split1[1]));
				}
			} else {
				calendar.set(Calendar.YEAR, Integer.valueOf(split[1]));
				calendar.set(Calendar.MONTH, Integer.valueOf(split1[0]) - 1);
				calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(split1[1]));
			}
			return calendar.getTime();
		} catch (RuntimeException e) {
			return null;
		}
	}
}

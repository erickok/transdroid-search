package org.transdroid.search.adapters.html.publictrackers;

import android.content.SharedPreferences;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.search.TorrentSite;
import org.transdroid.search.adapters.html.AbstractHtmlAdapter;
import org.transdroid.util.HttpHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScambioEticoAdapter extends AbstractHtmlAdapter {

	@Override
	public List<SearchResult> search(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws Exception {
		final DefaultHttpClient client = HttpHelper.buildDefaultSearchHttpClient(false);
		authenticateHttpClient(client, prefs);

		final String searchUrl = getSearchUrl(prefs, query, order, maxResults);
		final HttpPost post = new HttpPost(searchUrl);
		// NOTE No support for sorting
		post.setEntity(new UrlEncodedFormEntity(Collections.singletonList(new BasicNameValuePair("srcrel", query))));
		final HttpResponse response = client.execute(post);
		final HttpEntity entity = response.getEntity();
		final Document document = Jsoup.parse(entity.getContent(), null, "");
		entity.consumeContent();

		final Elements torrentElements = selectTorrentElements(document);
		final ArrayList<SearchResult> searchResults = new ArrayList<>();
		for (Element torrentElement : torrentElements) {
			searchResults.add(buildSearchResult(torrentElement));
			if (searchResults.size() == maxResults) {
				break;
			}
		}
		return searchResults;
	}

	@Override
	protected String getSearchUrl(SharedPreferences prefs, String query, SortOrder order, int maxResults) {
		return "http://www.tntvillage.scambioetico.org/src/releaselist.php";
	}

	@Override
	protected Elements selectTorrentElements(Document document) {
		return document.select("tr:has(a)");
	}

	@Override
	protected SearchResult buildSearchResult(Element torrentElement) {
		final String torrentUrl = torrentElement.selectFirst("td a[href]").text();
		final String detailsUrl = torrentElement.selectFirst("td:eq(2) a[href]").text();
		final String leechers = torrentElement.selectFirst("td:eq(3)").text();
		final String seeders = torrentElement.selectFirst("td:eq(4)").text();
		final String title = torrentElement.selectFirst("td:eq(6)").text();

		return new SearchResult(title, torrentUrl, detailsUrl, "", null, Integer.parseInt(seeders), Integer.parseInt(leechers));
	}

	@Override
	public String buildRssFeedUrlFromSearch(SharedPreferences prefs, String query, SortOrder order) {
		// Scambio Etico doesn't support RSS feed-based searches
		return null;
	}

	@Override
	protected TorrentSite getTorrentSite() {
		return TorrentSite.ScambioEtico;
	}

	@Override
	public String getSiteName() {
		return "Scambio Etico";
	}

}

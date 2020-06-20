package org.transdroid.search.adapters.html;

import android.content.SharedPreferences;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A base class for HTML scraping adapters. It handles most of the boilerplate code.
 */
public abstract class AbstractHtmlAdapter implements ISearchAdapter {

    @Override
    public List<SearchResult> search(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws Exception {
        // todo: All adapters that use this method call with ignoreSslIssues=false. Perhaps it needs
        // If an adapter that need ignoreSslIssues=true comes around, we can extract an abstract
        // method to rpovide it.
        final DefaultHttpClient client = HttpHelper.buildDefaultSearchHttpClient(false);
        authenticateHttpClient(client, prefs);

        final String searchUrl = getSearchUrl(prefs, query, order, maxResults);
        final HttpGet get = new HttpGet(searchUrl);
        final HttpResponse response = client.execute(get);
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
    public InputStream getTorrentFile(SharedPreferences prefs, String url) throws Exception {
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        if (isAuthenticationRequiredForTorrentLink()) {
            authenticateHttpClient(httpClient, prefs);
        }
        final HttpGet request = new HttpGet(url);

        final HttpResponse response = httpClient.execute(request);
        return response.getEntity().getContent();
    }

    /**
     * @return true is this site generated authenticated link. false if the links require manual
     * authentication.
     */
    @SuppressWarnings("WeakerAccess")
    protected boolean isAuthenticationRequiredForTorrentLink() {
        return false;
    }

    /**
     * Build a SearchResult object from an HTML Element.
     *
     * @param torrentElement An HTML element representing a search result.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract SearchResult buildSearchResult(Element torrentElement);

    /**
     * Find the HTML elements containing the results of the search
     *
     * @param document A Jsoup document to select from.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract Elements selectTorrentElements(Document document);

    /**
     * Authenticates an HttpClient. Derived Adapters can override this method to perform more
     * complicated authentication procedures.
     *
     * @param client an HttpClient to authenticate.
     * @param prefs  The apps shared preferences object.
     */
    @SuppressWarnings("WeakerAccess")
    protected void authenticateHttpClient(DefaultHttpClient client, SharedPreferences prefs) throws Exception {
        switch (getAuthType()) {
            case NONE:
                // Nothing to do
                return;
            case TOKEN:
                // Not supported yet
                throw new Exception("Not supported");
            case USERNAME:
                final String loginUrl = getLoginUrl();
                final HttpPost post = new HttpPost(loginUrl);
                post.setEntity(new UrlEncodedFormEntity(Arrays
                        .asList(
                                new BasicNameValuePair(getUsernameFieldName(), SettingsHelper.getSiteUser(prefs, getTorrentSite())),
                                new BasicNameValuePair(getPasswordFieldName(), SettingsHelper.getSitePass(prefs, getTorrentSite())))));
                client.execute(post).getEntity().consumeContent();
                break;
            case COOKIES:
                final BasicCookieStore cookieStore = new BasicCookieStore();
                final String domain = new URL(getLoginUrl()).getHost();
                for (String cookieName : getRequiredCookies()) {
                    final BasicClientCookie cookie = new BasicClientCookie(cookieName,
                            SettingsHelper.getSiteCookie(prefs, getTorrentSite().name(), cookieName));
                    cookie.setDomain(domain);
                    cookieStore.addCookie(cookie);
                }
                client.setCookieStore(cookieStore);
                break;
        }
    }

    /**
     * Provides the site enum type.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract TorrentSite getTorrentSite();

    /**
     * Provides the name of the form input field for a username.
     */
    @SuppressWarnings("WeakerAccess")
    protected String getPasswordFieldName() {
        return "password";
    }

    /**
     * Provides the name of the form input field for a username.
     */
    @SuppressWarnings("WeakerAccess")
    protected String getUsernameFieldName() {
        return "username";
    }

    /**
     * Provides the login url. For sites with cookie auth, this should probably just return the base
     * url so we can extract a domain from it.
     */
    protected String getLoginUrl() {
        return null;
    }

    /**
     * Provides the search url.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract String getSearchUrl(SharedPreferences prefs, String query, SortOrder order, int maxResults) throws UnsupportedEncodingException;

    @Override
    public AuthType getAuthType() {
        return AuthType.NONE;
    }

    @Override
    public String[] getRequiredCookies() {
        return null;
    }
}

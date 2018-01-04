package org.transdroid.search;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;
import static org.transdroid.search.ISearchAdapter.AuthType.NONE;

@RunWith(AndroidJUnit4.class)
public class TorrentSiteTest {

	private static final String QUERY = "music";
	private static final SortOrder ORDER = SortOrder.Combined;
	private static final int RESULTS = 10;
	private static final long THE_YEAR_2000 = 946684800000L;

	private Context context;
	private SharedPreferences prefs;
	private String packageName;

	@Before
	public void createProvider() {
		context = InstrumentationRegistry.getContext();
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		packageName = context.getPackageName();
	}

	@Test
	public void search_AsiaTorrents() throws Exception {
		searchSite(TorrentSite.AsiaTorrents);
	}

	@Test
	public void search_BitHdtv() throws Exception {
		searchSite(TorrentSite.BitHdtv);
	}

	@Test
	public void search_BTN() throws Exception {
		searchSite(TorrentSite.BTN);
	}

	@Test
	public void search_Danishbits() throws Exception {
		searchSite(TorrentSite.Danishbits);
	}

	@Test
	public void search_ExtraTorrent() throws Exception {
		searchSite(TorrentSite.ExtraTorrent);
	}

	@Test
	public void search_HdBitsOrg() throws Exception {
		searchSite(TorrentSite.HdBitsOrg);
	}

	@Test
	public void search_HdTorrents() throws Exception {
		searchSite(TorrentSite.HdTorrents);
	}

	@Test
	public void search_HoundDawgs() throws Exception {
		searchSite(TorrentSite.HoundDawgs);
	}

	@Test
	public void search_LimeTorrents() throws Exception {
		searchSite(TorrentSite.LimeTorrents);
	}

	@Test
	public void search_Ncore() throws Exception {
		searchSite(TorrentSite.Ncore);
	}

	@Test
	public void search_ThePirateBay() throws Exception {
		searchSite(TorrentSite.ThePirateBay);
	}

	@Test
	public void search_Pretome() throws Exception {
		searchSite(TorrentSite.Pretome);
	}

	@Test
	public void search_ScambioEtico() throws Exception {
		searchSite(TorrentSite.ScambioEtico);
	}

	@Test
	public void search_RevolutionTT() throws Exception {
		searchSite(TorrentSite.RevolutionTT);
	}

	@Test
	public void search_Rarbg() throws Exception {
		searchSite(TorrentSite.Rarbg);
	}

	@Test
	public void search_SkyTorrents() throws Exception {
		searchSite(TorrentSite.SkyTorrents);
	}

	@Test
	public void search_TorrentDownloads() throws Exception {
		searchSite(TorrentSite.TorrentDownloads);
	}

	@Test
	public void search_TorrentLeech() throws Exception {
		searchSite(TorrentSite.TorrentLeech);
	}

	private void searchSite(TorrentSite torrentSite) throws Exception {
		// Set test user and password
		if (torrentSite.getAdapter().getAuthType() != NONE) {
			String user = getResourceString(torrentSite.name() + "_user");
			String pass = getResourceString(torrentSite.name() + "_pass");
			String token = getResourceString(torrentSite.name() + "_token");
			if (!(has(user, pass) || has(token)))
				assert_().fail(torrentSite.name() + " is private but no credentials found: untestable");
			prefs.edit()
					.putString("pref_key_user_" + torrentSite.name(), user)
					.putString("pref_key_pass_" + torrentSite.name(), pass)
					.putString("pref_key_token_" + torrentSite.name(), token)
					.commit();
		}

		List<SearchResult> results = torrentSite.search(prefs, QUERY, ORDER, RESULTS);

		assertThat(results).isNotEmpty();
		for (SearchResult result : results) {
			assertThat(result.getTitle()).isNotEmpty();
			assertThat(result.getTorrentUri()).isNotNull();
			assertThat(result.getTorrentUri().toString()).isNotEmpty();
			assertThat(result.getDetailsUrl()).isNotEmpty();
			assertThat(result.getSize()).isNotEmpty();
			assertThat(result.getAddedDate()).isNotNull();
			assertThat(result.getAddedDate().getTime()).isGreaterThan(THE_YEAR_2000);
			assertThat(result.getSeeds()).isAtLeast(0);
			assertThat(result.getLeechers()).isAtLeast(0);
		}
	}

	private boolean has(String... fields) {
		for (String field : fields) {
			if (field == null || field.length() == 0)
				return false;
		}
		return true;
	}

	@NonNull
	private String getResourceString(@NonNull String resourceName) {
		int id = context.getResources().getIdentifier(resourceName, "string", packageName);
		if (id == 0)
			return "";
		return context.getString(id);
	}

}

package org.transdroid.search;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public class TorrentSearchProviderTest {

	private ContentProviderClient provider;

	@Before
	public void createProvider() {
		Context context = InstrumentationRegistry.getInstrumentation().getContext();
		provider = context.getContentResolver().acquireContentProviderClient(TorrentSearchProvider.CONTENT_URI);
	}

	@Test
	public void query_basicIntegration() throws Exception {
		Cursor cursor = provider.query(Uri.parse("content://org.transdroid.search.torrentsearchprovider/search/music"), null, null, null, null);
		assertThat(cursor).isNotNull();
		assertThat(cursor.getCount()).isGreaterThan(0);
		assertThat(cursor.getColumnCount()).isEqualTo(8);
		assertThat(cursor.moveToFirst()).isTrue();
		int id = 0;
		do {
			assertThat(cursor.getInt(0)).isEqualTo(id); // Incremental id
			assertThat(cursor.getString(1)).isNotEmpty(); // Title
			assertThat(cursor.getString(2)).isNotEmpty(); // Torrent url
			assertThat(cursor.getString(3)).isNotEmpty(); // Details url
			assertThat(cursor.getString(4)).isNotEmpty(); // Size
			assertThat(cursor.getLong(5)).isGreaterThan(0L); // Timestamp
			assertThat(cursor.getInt(6)).isAtLeast(0); // Seeders
			assertThat(cursor.getInt(7)).isAtLeast(0); // Leechers
			id++;
		} while (cursor.moveToNext());
	}

	@Test
	public void getType_searchTerm() throws Exception {
		String type = provider.getType(Uri.parse("content://org.transdroid.search.torrentsearchprovider/search/query"));
		assertThat(type).isEqualTo("vnd.android.cursor.dir/vnd.transdroid.torrent");
	}

	@Test
	public void getType_torrentUrl() throws Exception {
		String type = provider.getType(Uri.parse("content://org.transdroid.search.torrentsearchprovider/get/site/url"));
		assertThat(type).isEqualTo("vnd.android.cursor.dir/vnd.transdroid.torrentfile");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void delete_unsupported() throws Exception {
		provider.delete(TorrentSearchProvider.CONTENT_URI, null, null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void insert_unsupported() throws Exception {
		provider.insert(TorrentSearchProvider.CONTENT_URI, null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void update_unsupported() throws Exception {
		provider.update(TorrentSearchProvider.CONTENT_URI, null, null, null);
	}

}
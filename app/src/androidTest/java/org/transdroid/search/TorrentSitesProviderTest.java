package org.transdroid.search;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.Range;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public class TorrentSitesProviderTest {

	private ContentProviderClient provider;

	@Before
	public void createProvider() {
		Context context = InstrumentationRegistry.getInstrumentation().getContext();
		provider = context.getContentResolver().acquireContentProviderClient(TorrentSitesProvider.CONTENT_URI);
	}

	@Test
	public void query_hasSitesWithCodeAndName() throws Exception {
		Cursor cursor = provider.query(TorrentSitesProvider.CONTENT_URI, null, null, null, null);
		int authTypeCount = ISearchAdapter.AuthType.values().length;
		assertThat(cursor).isNotNull();
		assertThat(cursor.getCount()).isGreaterThan(0);
		assertThat(cursor.getColumnCount()).isEqualTo(5);
		assertThat(cursor.moveToFirst()).isTrue();
		int id = 0;
		do {
			assertThat(cursor.getInt(0)).isEqualTo(id); // Incremental id
			assertThat(cursor.getString(1)).isNotEmpty(); // Site code
			assertThat(cursor.getString(2)).isNotEmpty(); // Site name
			assertThat(cursor.getInt(4)).isIn(Range.closed(0, authTypeCount)); // Auth type
			id++;
		} while (cursor.moveToNext());
	}

	@Test
	public void getType_static() throws Exception {
		String type = provider.getType(TorrentSitesProvider.CONTENT_URI);
		assertThat(type).isEqualTo("vnd.android.cursor.dir/vnd.transdroid.torrentsite");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void delete_unsupported() throws Exception {
		provider.delete(TorrentSitesProvider.CONTENT_URI, null, null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void insert_unsupported() throws Exception {
		provider.insert(TorrentSitesProvider.CONTENT_URI, null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void update_unsupported() throws Exception {
		provider.update(TorrentSitesProvider.CONTENT_URI, null, null, null);
	}

}

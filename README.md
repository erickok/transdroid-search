transdroid-search
=================

Transdroid Torrent Search is an Android application that provides torrent search results to other Android apps by exposing `ContentProvider`s. Originally part of [Transdroid](http://www.transdroid.org), it now supplies torrent links for 10+ torrent sites to various torrent-related Android applications.

The latest .apk is available via [transdroid.org/latest-search](http://transdroid.org/latest-search) and code is available under the Lesser GPL v3 license.

Usage
=====

Transdroid Torrent Search provides access to torrent searches on a variety of sites. Instead of providing an interface itself, it allows Android application to access the data via a content provider.

Getting search results
----------------------

Acquiring search results for a specific query can be as easy as as two lines of code:
```
Uri uri = Uri.parse("content://org.transdroid.search.torrentsearchprovider/search/" + query);
Cursor results = managedQuery(uri, null, null, null, null);
```

The returned `Cursor` can be used in a `ListActivity` or elsewhere. The following fields are available in the returned cursor:
```
String[] fields = new String[] { "_ID", "NAME", "TORRENTURL", "DETAILSURL", "SIZE", "ADDED", "SEEDERS", "LEECHERS" };
```

*Important:* Querying the content providers is an synchronous operation. If done from your application's UI thread this operation will stall the interface. Use, for example, an `AsyncTask` to implement proper threading.

Customizing search results
--------------------------

You have control over the search results that are returned. A specific site may be queried and the preferred sort order can be given:
```
Uri uri = Uri.parse("content://org.transdroid.search.torrentsearchprovider/search/" + query);
Cursor results = managedQuery(uri, null, "SITE = ?", new String[] { siteCode }, sortOrder)
```
Here, `siteCode` is the code of one of the supported torrent sites. The default is `Mininova`. The `orderCode` is either BySeeders (default) or Combined. Note that no errors are returned when a site or sort order doesn't exist (although they are written to LogCat); an null `Cursor` is returned instead. (This is a limitation of `ContentResolver`s.)

Supported torrent sites
-----------------------

To get a listing of (the codes of) the support torrent sites, you may use another provider:
```
uri = Uri.parse("content://org.transdroid.search.torrentsitesprovider/sites");
Cursor sites = managedQuery(uri, null, null, null, null);
```

The returned `Cursor` contains the following fields:
```
String[] fields = new String[] { "_ID", "CODE", "NAME", "RSSURL" };
```

Developed By
============

* Eric Kok (Original developer) <eric@2312.nl>
* Steve Garon
* Gabor Tanka
* Eric Taix

License
=======


    Copyright 2010-2013 Eric Kok et al.
    
    Transdroid Torrent Search is free software: you can redistribute 
    it and/or modify it under the terms of the GNU Lesser General 
    Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later 
    version.

    Transdroid Torrent Search is distributed in the hope that it will 
    be useful, but WITHOUT ANY WARRANTY; without even the implied 
    warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Lesser General Public License for more details.
    
    You should have received a copy of the GNU Lesser General Public 
    License along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.

Some code/libraries are used in the project:

* [RssParser](http://github.com/digitalspaghetti/learning-android) (learning-android) by Tane Piper (Public Domain)

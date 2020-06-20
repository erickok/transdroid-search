transdroid-search
=================

Transdroid Torrent Search is an Android application that provides torrent search results to other Android apps by exposing `ContentProvider`s. Originally part of [Transdroid](https://www.transdroid.org/), it now supplies torrent links for 15+ public and private torrent sites to various torrent-related Android applications.

The latest .apk is available via [transdroid.org/latest-search](https://transdroid.org/latest-search) and code is available under the Lesser GPL v3 license.

<a href="https://transdroid.org/latest-search">
    <img src="https://transdroid.org/images/getontransdroid.png"
    alt="Get it on transdroid.org"
    height="80">
</a>
<a href="https://f-droid.org/packages/org.transdroid.search/">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">
</a>

Usage
=====

Transdroid Torrent Search provides access to torrent searches on a variety of sites. Instead of providing a search interface itself, it allows Android application to access the data via a content provider.

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

Customizing search results
--------------------------

A specific site may be queried and the preferred sort order can be given:
```
Uri uri = Uri.parse("content://org.transdroid.search.torrentsearchprovider/search/" + query);
Cursor results = managedQuery(uri, null, "SITE = ?", new String[] { siteCode }, sortOrder)
```
Here, `siteCode` is the code of one of the supported torrent sites. The default is `RARBG`. The `orderCode` is either BySeeders (default) or Combined. Note that no errors are returned when a site or sort order doesn't exist (although they are written to LogCat); a null `Cursor` is returned instead. (This is a limitation of `ContentResolver`s.)

Supported torrent sites
-----------------------

To get a listing of (the codes of) the support torrent sites, including custom RSS sites defined, you may use another provider:
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
* Alon Albert
* John Conrad
* Toon Schoenmakers
* Gabor Foldvari
* Marco Furlando
* Mário Franco
* Martin Piffault
* Colby Brown
* Thomas Riccardi
* and others...

License
=======

    Copyright 2010-2019 Eric Kok et al.

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
* [android-json-rpc](https://code.google.com/archive/p/android-json-rpc/) by alexd (MIT License)
* [Volley](https://github.com/google/volley) by Google (Apache License 2.0)

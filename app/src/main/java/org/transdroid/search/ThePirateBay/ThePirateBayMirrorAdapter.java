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
package org.transdroid.search.ThePirateBay;


/**
 * An adapter that provides access to The Pirate Bay via a mirror site,
 *  
 * @author Eric Kok
 */
public class ThePirateBayMirrorAdapter extends ThePirateBayAdapter {

	private static final String QUERYURL = "http://pirateproxy.net/search/%s/%s/%s/100,200,300,400,600/";

	@Override
	public String getSiteName() {
		return "The Pirate Bay (mirror)";
	}
	
	@Override
	protected String getQueryUrl() {
		return QUERYURL;
	}
	
}

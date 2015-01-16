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
package org.transdroid.search;

/**
 * The preferred type of torrent search result sorting
 * 
 * @author Eric Kok
 */
public enum SortOrder {
	Combined,
	BySeeders;

	/**
	 * Returns the SortOrder corresponding to the Enum type name it 
	 * has, e.g. <code>SortOrder.fromCode("BySeeders")</code> returns 
	 * the <code>SortOrder.BySeeders</code> enumeration value
	 * @param orderCode The name of the enum type value
	 * @return The corresponding enum type value of sort order
	 */
	public static SortOrder fromCode(String orderCode) {
		try {
			return Enum.valueOf(SortOrder.class, orderCode);
		} catch (Exception e) {
			return null;
		}
	}
	
}

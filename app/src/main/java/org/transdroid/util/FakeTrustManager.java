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
package org.transdroid.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class FakeTrustManager implements X509TrustManager {

        private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[] {};

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public boolean isClientTrusted(X509Certificate[] chain) {
                return true;
        }

        public boolean isServerTrusted(X509Certificate[] chain) {
                return true;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
                return _AcceptedIssuers;
        }

}

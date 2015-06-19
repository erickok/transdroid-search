/*
 * Copyright 2010-2013 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.util;

import android.annotation.TargetApi;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.util.Log;

import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 * Implements an HttpClient socket factory with extensive support for SSL. Many thanks to http://blog.dev001.net/post/67082904181/android-using-sni-and-tlsv1-2-with-apache-httpclient
 * for the base implementation.
 * <p/>
 * All SSL protocols that a particular Android version support will be enabled (according to http://developer.android.com/reference/javax/net/ssl/SSLSocket.html).
 * This currently includes SSL v3 and TLSv1.0, v1.1 and v1.2.
 * <p/>
 * SNI is supported for host name verification. For Android 4.2+, which supports it natively, the default (strict) hostname verifier is used. For
 * Android 4.1 and earlier it is possibly supported through reflexion on the same methods.
 */
public class TlsSniSocketFactory implements LayeredSocketFactory {

	private final static HostnameVerifier hostnameVerifier = new StrictHostnameVerifier();

	public TlsSniSocketFactory() {
	}

	// Plain TCP/IP (layer below TLS)

	@Override
	public Socket connectSocket(Socket s, String host, int port, InetAddress localAddress, int localPort, HttpParams params) throws IOException {
		return null;
	}

	@Override
	public Socket createSocket() throws IOException {
		return null;
	}

	@Override
	public boolean isSecure(Socket s) throws IllegalArgumentException {
		if (s instanceof SSLSocket) {
			return s.isConnected();
		}
		return false;
	}

	// TLS layer

	@Override
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public Socket createSocket(Socket plainSocket, String host, int port, boolean autoClose) throws IOException {
		if (autoClose) {
			// we don't need the plainSocket
			plainSocket.close();
		}

		SSLCertificateSocketFactory sslSocketFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);

		// create and connect SSL socket, but don't do hostname/certificate verification yet
		SSLSocket ssl = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getByName(host), port);

		// enable TLSv1.1/1.2 if available
		ssl.setEnabledProtocols(ssl.getSupportedProtocols());

		// set up SNI before the handshake
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			sslSocketFactory.setHostname(ssl, host);
		} else {
			try {
				java.lang.reflect.Method setHostnameMethod = ssl.getClass().getMethod("setHostname", String.class);
				setHostnameMethod.invoke(ssl, host);
			} catch (Exception e) {
				Log.d(TlsSniSocketFactory.class.getSimpleName(), "SNI not usable: " + e);
			}
		}

		// verify hostname and certificate
		SSLSession session = ssl.getSession();
		if (!hostnameVerifier.verify(host, session)) {
			throw new SSLPeerUnverifiedException("Cannot verify hostname: " + host);
		}

		return ssl;
	}

}

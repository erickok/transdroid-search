/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
 package org.transdroid.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * Provides a set of general helper methods that can be used in web-based communication.
 * 
 * @author erickok
 *
 */
public class HttpHelper {

    /*
     * To convert the InputStream to String we use the BufferedReader.readLine()
     * method. We iterate until the BufferedReader return null which means
     * there's no more data to read. Each line will appended to a StringBuilder
     * and returned as String.
     * 
     * Taken from http://senior.ceng.metu.edu.tr/2009/praeda/2009/01/11/a-simple-restful-client-at-android/
     */
    public static String ConvertStreamToString(InputStream is, String encoding) throws UnsupportedEncodingException {
    	InputStreamReader isr;
    	if (encoding != null) {
    		isr = new InputStreamReader(is, encoding);
    	} else {
    		isr = new InputStreamReader(is);
    	}
    	BufferedReader reader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
    
    public static String ConvertStreamToString(InputStream is) {
    	try {
			return ConvertStreamToString(is, null);
		} catch (UnsupportedEncodingException e) {
			// Since this is going to use the default encoding, it is never going to crash on an UnsupportedEncodingException
			e.printStackTrace();
			return null;
		}
    }
    
}
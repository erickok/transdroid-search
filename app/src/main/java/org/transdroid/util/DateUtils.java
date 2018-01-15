package org.transdroid.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * Date Time related utilities
 *
 * @author alonalbert
 */
public class DateUtils {
    public static Date parseDate(DateFormat df, String str) {
        try {
            return df.parse(str);
        } catch (ParseException e) {
            return null;
        }
    }
}

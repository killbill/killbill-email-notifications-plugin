package org.killbill.billing.plugin.notification.util;

import javax.annotation.Nullable;
import java.util.Locale;

public class LocaleUtils {

    private LocaleUtils() {
    }

    public static String localeString(final Locale locale, @Nullable String prefix) {
        final StringBuilder tmp = new StringBuilder();
        if (prefix != null) {
            tmp.append(prefix);
            if (!prefix.endsWith("_")) {
                tmp.append("_");
            }
        }
        tmp.append(locale.getLanguage())
                .append("_")
                .append(locale.getCountry());
        return tmp.toString();

    }

    // From commons-lang
    public static Locale toLocale(final String str) {
        if (str == null) {
            return null;
        }
        final int len = str.length();
        if (len != 2 && len != 5 && len < 7) {
            throw new IllegalArgumentException("Invalid locale format: " + str);
        }
        final char ch0 = str.charAt(0);
        final char ch1 = str.charAt(1);
        if (ch0 < 'a' || ch0 > 'z' || ch1 < 'a' || ch1 > 'z') {
            throw new IllegalArgumentException("Invalid locale format: " + str);
        }
        if (len == 2) {
            return new Locale(str, "");
        } else {
            if (str.charAt(2) != '_') {
                throw new IllegalArgumentException("Invalid locale format: " + str);
            }
            final char ch3 = str.charAt(3);
            if (ch3 == '_') {
                return new Locale(str.substring(0, 2), "", str.substring(4));
            }
            final char ch4 = str.charAt(4);
            if (ch3 < 'A' || ch3 > 'Z' || ch4 < 'A' || ch4 > 'Z') {
                throw new IllegalArgumentException("Invalid locale format: " + str);
            }
            if (len == 5) {
                return new Locale(str.substring(0, 2), str.substring(3, 5));
            } else {
                if (str.charAt(5) != '_') {
                    throw new IllegalArgumentException("Invalid locale format: " + str);
                }
                return new Locale(str.substring(0, 2), str.substring(3, 5), str.substring(6));
            }
        }
    }
}

package io.tezrok.api.util;

public class StringUtil {
    public static final String NEWLINE = System.getProperty("line.separator");
    public static final char TAB = '\t';

    public static String tab(final int count) {
        return nchar(count, TAB);
    }

    public static String nchar(final int count, final char ch) {
        StringBuilder result = new StringBuilder();
        int counter = 0;

        while (++counter <= count) {
            result.append(ch);
        }

        return result.toString();
    }

    public static String alignLine(String prefix, String text, String suffix, int length) {
        StringBuilder sb = new StringBuilder(prefix);
        sb.append(text);

        while ((sb.length() + suffix.length()) < length) {
            sb.append(" ");
        }

        sb.append(suffix);

        return sb.toString();
    }

    public static int calcLines(String text) {
        int counts = 0;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                counts++;
            }
        }

        return counts;
    }
}

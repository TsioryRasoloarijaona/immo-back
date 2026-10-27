package com.tsix.immocrm.utils;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tri naturel des codes : les suites de chiffres se comparent en nombre ({@code A-2} avant {@code A-10}), casse ignorée. */
public final class OrdreNaturel {

    public static final Comparator<String> CODES = OrdreNaturel::comparer;

    private static final Pattern MORCEAUX = Pattern.compile("\\d+|\\D+");

    private OrdreNaturel() {
    }

    private static int comparer(String a, String b) {
        Matcher ma = MORCEAUX.matcher(a);
        Matcher mb = MORCEAUX.matcher(b);
        while (ma.find()) {
            if (!mb.find()) {
                return 1;
            }
            int c = morceau(ma.group(), mb.group());
            if (c != 0) {
                return c;
            }
        }
        return mb.find() ? -1 : 0;
    }

    private static int morceau(String x, String y) {
        if (Character.isDigit(x.charAt(0)) && Character.isDigit(y.charAt(0))) {
            String nx = x.replaceFirst("^0+(?=\\d)", "");
            String ny = y.replaceFirst("^0+(?=\\d)", "");
            return nx.length() != ny.length() ? Integer.compare(nx.length(), ny.length()) : nx.compareTo(ny);
        }
        return String.CASE_INSENSITIVE_ORDER.compare(x, y);
    }
}

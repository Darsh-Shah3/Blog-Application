package com.threadly.community.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * URL-safe slug for communities.
 * Isolated from services so string rules can be unit-tested without Spring/DB.
 */
@Component
public class SlugGenerator {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    /**
     * Converts display name to lowercase hyphenated slug (NFD strip for accents).
     */
    public String fromName(String input) {
        String nowhitespace = WHITESPACE.matcher(input.trim().toLowerCase(Locale.ENGLISH)).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        return NON_LATIN.matcher(normalized).replaceAll("").replaceAll("-{2,}", "-");
    }
}

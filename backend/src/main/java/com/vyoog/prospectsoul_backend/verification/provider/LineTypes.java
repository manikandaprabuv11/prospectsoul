package com.vyoog.prospectsoul_backend.verification.provider;

import java.util.Set;

/**
 * Line types returned by Twilio Lookup v2 Line Type Intelligence, taken from
 * the provider's documented response contract rather than guessed
 * (docs/dev_docs/14 §7).
 */
public final class LineTypes {

    private LineTypes() {}

    public static final String MOBILE = "mobile";
    public static final String LANDLINE = "landline";
    public static final String FIXED_VOIP = "fixedvoip";
    public static final String NON_FIXED_VOIP = "nonfixedvoip";
    public static final String PERSONAL = "personal";
    public static final String TOLL_FREE = "tollfree";
    public static final String PREMIUM = "premium";
    public static final String SHARED_COST = "sharedcost";
    public static final String UAN = "uan";
    public static final String VOICEMAIL = "voicemail";
    public static final String PAGER = "pager";
    public static final String UNKNOWN = "unknown";

    /** Every documented value, normalized the way {@link #normalize} returns them. */
    public static final Set<String> ALL = Set.of(
            MOBILE, LANDLINE, FIXED_VOIP, NON_FIXED_VOIP, PERSONAL, TOLL_FREE,
            PREMIUM, SHARED_COST, UAN, VOICEMAIL, PAGER, UNKNOWN);

    /**
     * Twilio spells these in camelCase ({@code nonFixedVoip}); comparisons are
     * done on the lower-cased form so a casing change upstream cannot silently
     * turn a mobile number into a non-mobile failure.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toLowerCase();
    }

    public static boolean isMobile(String raw) {
        return MOBILE.equals(normalize(raw));
    }
}

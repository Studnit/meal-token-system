package com.mealtoken.util;

import com.mealtoken.exception.BusinessException;

import java.util.regex.Pattern;

/** Shared input rules: a 10 digit Indian mobile number (starts with 6-9) and a well-formed email address. */
public final class Validators {
    public static final String MOBILE_REGEX = "^[6-9][0-9]{9}$";
    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9]([A-Za-z0-9\\-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9\\-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$";
    /** 3-30 characters, starts with a letter, so a username can never be mistaken for a mobile number or an email. */
    public static final String USERNAME_REGEX = "^[A-Za-z][A-Za-z0-9._-]{2,29}$";
    private static final Pattern USERNAME = Pattern.compile(USERNAME_REGEX);
    private static final Pattern MOBILE = Pattern.compile(MOBILE_REGEX);
    private static final Pattern EMAIL = Pattern.compile(EMAIL_REGEX);

    private Validators() {}

    public static boolean isMobile(String s) { return s != null && MOBILE.matcher(s.trim()).matches(); }
    public static boolean isEmail(String s) { return s != null && s.trim().length() <= 190 && EMAIL.matcher(s.trim()).matches(); }

    public static boolean isUsername(String s) { return s != null && USERNAME.matcher(s.trim()).matches(); }

    public static String requireUsername(String s) {
        if (!isUsername(s)) throw new BusinessException("error.username.invalid");
        return s.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /** Suggests a valid username from an email's local part (used when none is given, e.g. staff created by an owner). */
    public static String usernameBase(String email) {
        String local = email == null ? "" : email.split("@", 2)[0].toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        if (local.length() > 24) local = local.substring(0, 24);
        return USERNAME.matcher(local).matches() ? local : "user";
    }

    /** Blank becomes null; anything else must be a valid 10 digit mobile number. */
    public static String mobileOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        if (!isMobile(s)) throw new BusinessException("error.phone.invalid");
        return s.trim();
    }

    public static String requireEmail(String s) {
        if (!isEmail(s)) throw new BusinessException("error.email.invalid");
        return s.trim();
    }
}

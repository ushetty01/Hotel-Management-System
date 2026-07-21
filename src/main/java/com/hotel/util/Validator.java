// File: src/main/java/com/hotel/util/Validator.java
package com.hotel.util;

/**
 * Simple input validation utilities.
 */
public class Validator {

    /** Returns true if the string is null or blank. */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Returns true if the string can be parsed as a valid positive integer. */
    public static boolean isPositiveInt(String value) {
        if (isEmpty(value)) return false;
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Returns true if the string can be parsed as a valid positive double. */
    public static boolean isPositiveDouble(String value) {
        if (isEmpty(value)) return false;
        try {
            return Double.parseDouble(value.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Returns true if the phone number is 10 digits. */
    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) return false;
        return phone.trim().matches("\\d{10}");
    }
}

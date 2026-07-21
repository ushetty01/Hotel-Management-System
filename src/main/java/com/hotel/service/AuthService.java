// File: src/main/java/com/hotel/service/AuthService.java
package com.hotel.service;

/**
 * Handles login authentication.
 * Returns role string: "admin" | "receptionist" | null on failure.
 */
public class AuthService {

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "1234";

    private static final String RECEP_USER = "user";
    private static final String RECEP_PASS = "1111";

    /**
     * @return "admin", "receptionist", or null if credentials are wrong.
     */
    public String authenticate(String username, String password) {
        if (ADMIN_USER.equals(username) && ADMIN_PASS.equals(password)) {
            return "admin";
        }
        if (RECEP_USER.equals(username) && RECEP_PASS.equals(password)) {
            return "receptionist";
        }
        return null;
    }
}

package app.dto;

import jakarta.annotation.Nullable;

import java.util.HashMap;

public class Cookies {
    public static HashMap<String, String> parse(@Nullable String inputCookies) {
        HashMap<String, String> cookieMap = new HashMap<>();

        if (inputCookies == null || inputCookies.isBlank()) {
            return cookieMap;
        }

        String[] cookies = inputCookies.split(";");

        for (String cookie : cookies) {
            String[] parts = cookie.trim().split("=", 2);

            if (parts.length != 2) continue;

            cookieMap.put(parts[0], parts[1]);
        }

        return cookieMap;
    }
}

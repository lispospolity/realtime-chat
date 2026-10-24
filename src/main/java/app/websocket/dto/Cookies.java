package app.websocket.dto;

import java.util.HashMap;

public class Cookies {
    public static HashMap<String, String> parse(String inputCookies) {
        String[] cookies = inputCookies.split("; ");
        HashMap<String, String> cookieMap = new HashMap<>();

        for (String cookie : cookies) {
            String[] parts = cookie.split("=", 2);
            cookieMap.put(parts[0], parts[1]);
        }
        return cookieMap;
    }
}

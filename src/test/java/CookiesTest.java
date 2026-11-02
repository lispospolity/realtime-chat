import app.dto.Cookies;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class CookiesTest {
    private final String token = "abc123";
    private final String sessionId = "xyz789";

    @Test
    void correctCookie() {
        String correctCookie = "session_token=abc123; session_id=xyz789;";

        HashMap<String, String> cookies = Cookies.parse(correctCookie);

        Assertions.assertEquals(token, cookies.get("session_token"));
        Assertions.assertEquals(sessionId, cookies.get("session_id"));
    }

    @Test
    void halfCookie() {
        String halfCookie = "session_token; =xyz789;";

        HashMap<String, String> cookies = Cookies.parse(halfCookie);

        Assertions.assertNotEquals(token, cookies.get("session_token"));
        Assertions.assertNull(cookies.get("session_token"));

        Assertions.assertNotEquals(sessionId, cookies.get("session_id"));
        Assertions.assertNull(cookies.get("session_id"));
    }

    @Test
    void oneCookie() {
        String oneCookie = "session_token=abc123";

        HashMap<String, String> cookies = Cookies.parse(oneCookie);

        Assertions.assertEquals(token, cookies.get("session_token"));

        Assertions.assertNotEquals(sessionId, cookies.get("session_id"));
        Assertions.assertNull(cookies.get("session_id"));
    }

    @Test
    void emptyCookie() {
        String emptyCookie = "session_token=";

        HashMap<String, String> cookies = Cookies.parse(emptyCookie);

        Assertions.assertNotEquals(token, cookies.get("session_token"));
        Assertions.assertTrue(cookies.get("session_token").isBlank());

        Assertions.assertNotEquals(sessionId, cookies.get("session_id"));
        Assertions.assertNull(cookies.get("session_id"));
    }

    @Test
    void nullCookies() {
        String nullCookies = null;

        HashMap<String, String> cookies = Cookies.parse(nullCookies);

        Assertions.assertNotEquals(token, cookies.get("session_token"));
        Assertions.assertNull(cookies.get("session_token"));

        Assertions.assertNotEquals(sessionId, cookies.get("session_id"));
        Assertions.assertNull(cookies.get("session_id"));
    }
}

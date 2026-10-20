import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import app.utils.TokenUtils;
import static app.utils.TokenUtils.hashToken;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class TokenUtilsTest {

    @Test
    void generateTokenLengthCorrect() {
        String token = TokenUtils.generateToken();

        Assertions.assertEquals(43, token.length());
    }

    @Test
    void generateTokenProducesDifferentTokens() {
        String token1 = TokenUtils.generateToken();
        String token2 = TokenUtils.generateToken();

        Assertions.assertNotEquals(token1, token2);
    }

    @Test
    void checkHashTokenImplementation() throws NoSuchAlgorithmException {
        String token = "n7QxV2mK9pL4wR8zT1aB6cD0eF5gH3jN8sU2vYkM4oA";

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] expectedHash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

        Assertions.assertArrayEquals(expectedHash, hashToken(token));
    }

}

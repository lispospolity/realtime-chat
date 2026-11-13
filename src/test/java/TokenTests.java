import app.session.Token;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TokenTests {

    @Test
    void generateTokenLengthCorrect() throws NoSuchAlgorithmException {
        Token token = Token.createToken();

        Assertions.assertEquals(43, token.getValue().length());
    }

    @Test
    void generateTokenProducesDifferentTokens() throws NoSuchAlgorithmException {
        Token token1 = Token.createToken();
        Token token2 = Token.createToken();

        Assertions.assertNotEquals(token1, token2);
    }

    @Test
    void checkHashTokenImplementation() throws NoSuchAlgorithmException {
        String token = "n7QxV2mK9pL4wR8zT1aB6cD0eF5gH3jN8sU2vYkM4oA";

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] expectedHash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

        Assertions.assertArrayEquals(expectedHash, new Token(token).getHashedValue());
    }

    @Test
    void checkIfAcceptsNull() {
        Assertions.assertThrows(NullPointerException.class, () -> new Token(null));
    }

}

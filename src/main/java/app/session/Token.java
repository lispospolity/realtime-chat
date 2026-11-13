package app.session;

import jakarta.validation.constraints.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class Token {
    private final String value;
    public String getValue() { return value; }

    private final byte[] hashedToken;
    public byte[] getHashedValue() { return hashedToken; }

    private final static SecureRandom secureRandom = new SecureRandom();

    public Token(String token) throws NoSuchAlgorithmException {
        this.value = token;
        this.hashedToken = hashToken(token);
    }

    public static Token createToken() throws NoSuchAlgorithmException {
        String token = generateToken();
        return new Token(token);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Token token)) return false;
        return Arrays.equals(hashedToken, token.hashedToken);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hashedToken);
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
    private static byte[] hashToken(@NotNull String token) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(token.getBytes(StandardCharsets.UTF_8));
    }
}

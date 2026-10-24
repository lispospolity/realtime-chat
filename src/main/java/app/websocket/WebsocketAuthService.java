package app.websocket;

import app.auth.SessionRepository;
import app.logging.Debug;
import app.utils.TokenUtils;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

@Service
public class WebsocketAuthService {
    private final SessionRepository sessionRepo;

    public WebsocketAuthService(SessionRepository sessionRepository) {
        sessionRepo = sessionRepository;
    }

    public boolean tokenIncorrect(String token) {
        Long timeNow = System.currentTimeMillis();
        try {
            byte[] tokenHash = TokenUtils.hashToken(token);

            return sessionRepo.tokenExpired(tokenHash, timeNow);
        } catch (NoSuchAlgorithmException | SQLException ex) {
            Debug.log("WebsocketAuthService: tokenCorrect(): "+ex);
            return false;
        }
    }

    public String getUserId(String token) {
        try {
            byte[] tokenHash = TokenUtils.hashToken(token);

            return sessionRepo.getUserIdBySessionKey(tokenHash);
        } catch (NoSuchAlgorithmException | SQLException ex) {
            Debug.log("WebsocketAuthService: tokenCorrect(): "+ex);
            return null;
        }
    }

    public long sessionExpiryMillis(String token) {
        try {
            byte[] tokenHash = TokenUtils.hashToken(token);

            //return sessionRepo.getTokenExpiry(tokenHash);
            return 0;
        } catch (NoSuchAlgorithmException ex) {
            Debug.log("WebsocketAuthService: tokenCorrect(): "+ex);
            return 0;
        }
    }
}

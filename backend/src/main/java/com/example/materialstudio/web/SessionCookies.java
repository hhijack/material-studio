package com.example.materialstudio.web;

import com.example.materialstudio.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 会话 Cookie（HttpOnly / SameSite=Lax / 180 天）。
 * R4 起 Cookie 值为「userId.HMAC-SHA256(userId, secret)」——未持密钥方无法伪造他人 userId
 * （原为裸 UUID，见 README §8 的取舍说明与收紧路径）。secret 未配置时启动随机生成（重启后旧会话失效）。
 */
@Component
public class SessionCookies {

    private static final Logger log = LoggerFactory.getLogger(SessionCookies.class);

    private final AppProperties props;
    private final byte[] secret;

    public SessionCookies(AppProperties props) {
        this.props = props;
        String configured = props.session().secret();
        if (configured == null || configured.isBlank()) {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.secret = random;
            log.warn("app.session.secret 未配置：已生成随机签名密钥（重启后既有会话将失效）。"
                    + "compose/生产环境请设置 MS_SESSION_SECRET。");
        } else {
            this.secret = configured.getBytes(StandardCharsets.UTF_8);
        }
    }

    /** 签发会话 Cookie。 */
    public ResponseCookie sessionCookie(String userId) {
        return ResponseCookie.from(props.session().cookieName(), signed(userId))
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(props.session().maxAgeSeconds())
                .build();
    }

    public String cookieName() {
        return props.session().cookieName();
    }

    /** 从请求解析会话：Cookie 存在且签名校验通过时返回 userId，否则 null（视同未登录）。 */
    public String parse(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (props.session().cookieName().equals(cookie.getName())
                    && verify(cookie.getValue())) {
                return unwrap(cookie.getValue());
            }
        }
        return null;
    }

    private String signed(String userId) {
        return userId + "." + hmac(userId);
    }

    private String unwrap(String value) {
        int dot = value.lastIndexOf('.');
        return dot <= 0 ? value : value.substring(0, dot);
    }

    private boolean verify(String value) {
        int dot = value.lastIndexOf('.');
        if (dot <= 0) {
            return false;
        }
        String userId = value.substring(0, dot);
        String mac = value.substring(dot + 1);
        byte[] expected = hmac(userId).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(mac.getBytes(StandardCharsets.US_ASCII), expected);
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 初始化失败", e);
        }
    }
}

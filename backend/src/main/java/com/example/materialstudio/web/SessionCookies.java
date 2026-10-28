package com.example.materialstudio.web;

import com.example.materialstudio.config.AppProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** 会话 Cookie 构造（HttpOnly / SameSite=Lax / 180 天）。 */
@Component
public class SessionCookies {

    private final AppProperties props;

    public SessionCookies(AppProperties props) {
        this.props = props;
    }

    public ResponseCookie sessionCookie(String userId) {
        return ResponseCookie.from(props.session().cookieName(), userId)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(props.session().maxAgeSeconds())
                .build();
    }

    public String cookieName() {
        return props.session().cookieName();
    }
}

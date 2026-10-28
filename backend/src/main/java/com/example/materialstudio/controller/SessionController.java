package com.example.materialstudio.controller;

import com.example.materialstudio.model.User;
import com.example.materialstudio.service.SessionService;
import com.example.materialstudio.web.CurrentUser;
import com.example.materialstudio.web.SessionCookies;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 匿名会话：获取/修改昵称/切换身份。 */
@RestController
@RequestMapping("/api/session")
public class SessionController {

    public record SessionResponse(String id, String nickname, boolean created) {
    }

    public record UpdateNicknameRequest(@NotBlank @Size(min = 1, max = 24) String nickname) {
    }

    private final SessionService sessionService;
    private final SessionCookies cookies;

    public SessionController(SessionService sessionService, SessionCookies cookies) {
        this.sessionService = sessionService;
        this.cookies = cookies;
    }

    @GetMapping
    public ResponseEntity<SessionResponse> get(HttpServletRequest request) {
        SessionService.SessionResult result = sessionService.getOrCreate(readCookie(request));
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.created()) {
            builder.header("Set-Cookie", cookies.sessionCookie(result.user().id()).toString());
        }
        return builder.body(toResponse(result.user(), result.created()));
    }

    @PatchMapping
    public SessionResponse update(@CurrentUser User user, @RequestBody UpdateNicknameRequest body) {
        User updated = sessionService.updateNickname(user.id(), body.nickname());
        return toResponse(updated, false);
    }

    /** 切换身份 = 重置为全新用户并重写 Cookie，用于演示个人记录隔离。 */
    @DeleteMapping
    public ResponseEntity<SessionResponse> switchIdentity() {
        User user = sessionService.switchIdentity();
        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.sessionCookie(user.id()).toString())
                .body(toResponse(user, true));
    }

    private String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookies.cookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private SessionResponse toResponse(User user, boolean created) {
        return new SessionResponse(user.id(), user.nickname(), created);
    }
}

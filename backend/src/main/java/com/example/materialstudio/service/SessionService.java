package com.example.materialstudio.service;

import com.example.materialstudio.mapper.UserMapper;
import com.example.materialstudio.model.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 匿名身份：Cookie 携带用户 UUID；首次访问建立用户记录。 */
@Service
public class SessionService {

    private final UserMapper userMapper;

    public SessionService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public record SessionResult(User user, boolean created) {
    }

    @Transactional
    public SessionResult getOrCreate(String cookieValue) {
        if (cookieValue != null && !cookieValue.isBlank()) {
            User existing = userMapper.findById(cookieValue);
            if (existing != null) {
                return new SessionResult(existing, false);
            }
        }
        return new SessionResult(createUser(), true);
    }

    /** 切换身份：新建用户，由控制器重写 Cookie。 */
    @Transactional
    public User switchIdentity() {
        return createUser();
    }

    @Transactional
    public User updateNickname(String userId, String nickname) {
        userMapper.updateNickname(userId, nickname.trim());
        return userMapper.findById(userId);
    }

    private User createUser() {
        String id = UUID.randomUUID().toString();
        String nickname = "访客" + id.substring(0, 4).toUpperCase();
        userMapper.insert(id, nickname);
        return new User(id, nickname, LocalDateTime.now());
    }
}

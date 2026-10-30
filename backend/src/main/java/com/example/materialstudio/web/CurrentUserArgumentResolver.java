package com.example.materialstudio.web;

import com.example.materialstudio.mapper.UserMapper;
import com.example.materialstudio.model.User;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserMapper userMapper;
    private final SessionCookies sessionCookies;

    public CurrentUserArgumentResolver(UserMapper userMapper, SessionCookies sessionCookies) {
        this.userMapper = userMapper;
        this.sessionCookies = sessionCookies;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // Cookie 需通过 HMAC 签名校验才算有效会话（伪造 userId 会被拒）
        jakarta.servlet.http.HttpServletRequest request = webRequest.getNativeRequest(jakarta.servlet.http.HttpServletRequest.class);
        String userId = request == null ? null : sessionCookies.parse(request);
        if (userId != null) {
            User user = userMapper.findById(userId);
            if (user != null) {
                return user;
            }
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "NO_SESSION", "会话不存在或已失效，请刷新页面重建会话");
    }
}

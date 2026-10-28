package com.example.materialstudio.web;

import com.example.materialstudio.config.AppProperties;
import com.example.materialstudio.mapper.UserMapper;
import com.example.materialstudio.model.User;
import jakarta.servlet.http.HttpServletRequest;
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
    private final String cookieName;

    public CurrentUserArgumentResolver(UserMapper userMapper, AppProperties props) {
        this.userMapper = userMapper;
        this.cookieName = props.session().cookieName();
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request != null && request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    User user = userMapper.findById(cookie.getValue());
                    if (user != null) {
                        return user;
                    }
                }
            }
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "NO_SESSION", "会话不存在或已失效，请刷新页面重建会话");
    }
}

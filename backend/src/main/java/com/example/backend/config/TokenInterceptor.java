package com.example.backend.config;

import com.example.backend.tools.JwtTool;
import com.example.backend.tools.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTool jwtTool;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                Claims claims = jwtTool.parseToken(token);
                Long userId = claims.get("userId", Long.class);
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                UserContext.set(new UserContext.UserContextInfo(userId, username, role));
            } catch (Exception e) {
                // Invalid token, ignore or throw exception
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.remove();
    }
}

package com.example.attendancemanagementsystem.user.loginandprofile.handler;

import java.io.IOException;
import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, 
        HttpServletResponse response, 
        Authentication authentication) throws IOException, ServletException {

        String targetUrl = determineTargetUrl(authentication);
        response.sendRedirect(targetUrl);
    }

    protected String determineTargetUrl(Authentication authentication) {
        
        String targetUrl = "/"; 
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();

            // ★修正ポイント: ROLE_ADMIN または ROLE_SUPER_ADMIN なら管理者画面へ
            if (role.equals("ROLE_ADMIN") || role.equals("ROLE_SUPER_ADMIN")) {
                targetUrl = "/admin/timetable"; 
                break;
            } else if (role.equals("ROLE_STUDENT")) {
                targetUrl = "/student/main_calendar"; 
                break;
            }
        }
        return targetUrl;
    }
}
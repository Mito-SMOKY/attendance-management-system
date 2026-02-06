package com.example.attendancemanagementsystem.user.loginandprofile.handler;

import java.io.IOException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired; // 追加
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.example.attendancemanagementsystem.common.entity.UsersEntity; // 追加
import com.example.attendancemanagementsystem.common.repository.UsersRepository; // 追加

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // DB確認用にRepositoryを注入
    @Autowired
    private UsersRepository usersRepository;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, 
        HttpServletResponse response, 
        Authentication authentication) throws IOException, ServletException {

        // メールアドレス未登録チェック 
        String loginId = authentication.getName();
        UsersEntity user = usersRepository.findByLoginId(loginId).orElse(null);

        // メールが登録されていない場合は強制的に登録画面へ
        if (user != null && user.getEmail() == null) {
            response.sendRedirect("/email/auth");
            return; 
        }

        String targetUrl = determineTargetUrl(authentication);
        response.sendRedirect(targetUrl);
    }

    protected String determineTargetUrl(Authentication authentication) {
        
        String targetUrl = "/"; 
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();

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
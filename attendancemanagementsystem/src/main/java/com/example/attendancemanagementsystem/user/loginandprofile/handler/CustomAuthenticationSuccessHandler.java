package com.example.attendancemanagementsystem.user.loginandprofile.handler;

import java.io.IOException;
import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ログイン成功時にユーザーのロール（権限）に応じてリダイレクト先を振り分けるハンドラー
 */
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, 
        HttpServletResponse response, 
        Authentication authentication) throws IOException, ServletException {

        // 振り分けられたターゲットURLを取得
        String targetUrl = determineTargetUrl(authentication);

        // 取得したURLへリダイレクトを実行
        response.sendRedirect(targetUrl);
    }

    /**
     * ユーザーが持つ権限（GrantedAuthority）からリダイレクト先を決定する
     */
    protected String determineTargetUrl(Authentication authentication) {
        
        // デフォルトのリダイレクト先（どのロールにも該当しない場合）
        String targetUrl = "/"; 

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();

            // UserDetailsServiceImplで設定したロールと比較
            if (role.equals("ROLE_ADMIN")) {
                // 管理者（UserTypeID=2）用のメインメニュー
                targetUrl = "/admin/home"; 
                break;
            } else if (role.equals("ROLE_STUDENT")) {
                // 生徒（UserTypeID=1）用のメインメニュー
                targetUrl = "/student/home"; 
                break;
            }
        }
        return targetUrl;
    }
}
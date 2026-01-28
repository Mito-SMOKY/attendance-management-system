package com.example.attendancemanagementsystem.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.example.attendancemanagementsystem.user.loginandprofile.handler.CustomAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. APIへのアクセス制限を解除 (Pythonからのアクセスを通すため)
            .authorizeHttpRequests(auth -> auth
                // 静的リソース
                .requestMatchers("/css/**", "/js/**", "/image/**", "/error").permitAll()
                
                // "/api/issue/**" (PC登録用) と "/api/attendance/**" (ラズパイ出席用)
                // これらはプログラムからのアクセスなので、ログインなしで許可する
                .requestMatchers("/api/issue/**", "/api/attendance/record/**").permitAll()
                
                // 画面系のアクセス制御 (既存の設定)

                // テスト用
                .requestMatchers("/test/**").permitAll()
                .requestMatchers("/login", "/first-login").permitAll()
                .requestMatchers("/email/**").permitAll()
                .requestMatchers("/password/**").permitAll()
                // .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/student/**").hasRole("STUDENT")
                
                // それ以外は認証必須
                .anyRequest().authenticated()
            )
            
            // 2. CSRF対策を無効化 (API用)
            // PythonからPOSTする際にブロックされないようにする
            // .csrf(csrf -> csrf
                
            //     //テスト用
            //     .ignoringRequestMatchers("/test/**")
            //     .ignoringRequestMatchers("/api/issue/**", "/api/attendance/record/**")
            // )

            .csrf(csrf -> csrf.disable()) // 全面無効化（本番環境では適切に設定すること）
            
            // 3. ログイン画面の設定
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(customAuthenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")        
                .logoutSuccessUrl("/login?logout") 
                .invalidateHttpSession(true)      
                .deleteCookies("JSESSIONID")  
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("secretKey")
                .tokenValiditySeconds(86400 * 14)
            );

        return http.build();
    }

    // --- 共通Bean定義 ---
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }
}
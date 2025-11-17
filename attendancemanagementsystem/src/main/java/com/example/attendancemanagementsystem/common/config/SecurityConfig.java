package com.example.attendancemanagementsystem.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import com.example.attendancemanagementsystem.user.loginandprofile.handler.CustomAuthenticationSuccessHandler; 

@Configuration
@Profile("!dummy")
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // ログイン、CSS、JSは全員許可
                        .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                        
                        // ★ 必須の修正点: ロール（権限）に基づいたアクセス許可を追加
                        // /admin/で始まるURLにはROLE_ADMINが必要
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // /student/で始まるURLにはROLE_STUDENTが必要
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        
                        // その他のリクエストは認証済みであれば許可
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customAuthenticationSuccessHandler()) 
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")
                );

        return http.build();
    }
}
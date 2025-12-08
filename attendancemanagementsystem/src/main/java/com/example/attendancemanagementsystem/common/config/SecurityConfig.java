package com.example.attendancemanagementsystem.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.attendancemanagementsystem.common.security.ApiKeyAuthFilter;
import com.example.attendancemanagementsystem.user.loginandprofile.handler.CustomAuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@Profile("!dummy")
public class SecurityConfig {

    // --- 共通Bean定義 ---
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }
    // ----------------------


    /**
     * @Order(1) APIキー認証用のFilterChain (優先度: 高)
     * PythonやRaspiからのアクセス (/api/attendance/**) 専用です。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(
            HttpSecurity http,
            @Value("${nfc.auth.api-key}") String apiKey
    ) throws Exception {

        http
            //"/api/**" から "/api/attendance/**" に限定します。
            ///api/issue/** はここをスルーして下の設定(Order 2)に行きます。
            .securityMatcher("/api/attendance/**") 
            
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().write("Unauthorized: API Key Required or Invalid");
                })
            )
            .authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
            )
            .addFilterBefore(new ApiKeyAuthFilter(apiKey),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /**
     * @Order(2) Web UI用のFilterChain (優先度: 低)
     * ブラウザからのアクセス (/api/issue/** や画面表示) 用です。
     */
    @Bean
    @Order(2) 
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(authorize -> authorize
                        // 画面側API (/api/issue/**) は管理者権限または上位管理者権限が必要
                        // 修正箇所1: ADMIN または SUPER_ADMIN を許可
                        .requestMatchers("/api/issue/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        
                        // attendance以外のAPIが万が一ここに来たら拒否
                        .requestMatchers("/api/attendance/**").denyAll() 
                        
                        .requestMatchers("/login", "/css/**", "/js/**", "/image/**", "/error").permitAll()
                        
                        // 修正箇所2: /admin/** は ADMIN または SUPER_ADMIN を許可
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customAuthenticationSuccessHandler()) 
                        .permitAll() 
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") 
                        .logoutSuccessUrl("/login?logout") 
                        .permitAll()
                );

        return http.build();
    }
}
package com.example.attendancemanagementsystem.common.config;

import com.example.attendancemanagementsystem.common.security.ApiKeyAuthFilter;
import com.example.attendancemanagementsystem.user.loginandprofile.handler.CustomAuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletResponse;

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
     * /api/** のパスのみに適用されます。
     * @param http HttpSecurity
     * @param apiKey application.propertiesから注入されるAPIキー
     * @return SecurityFilterChain
     * @throws Exception 
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(
            HttpSecurity http,
            @Value("${nfc.auth.api-key}") String apiKey
    ) throws Exception {

        http
            .securityMatcher("/api/**") // APIパスのみに適用
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().write("Unauthorized: API Key Required or Invalid");
                })
            )
            .authorizeHttpRequests(authz -> authz
                // /api/attendance/はAPIキーがあればアクセス許可
                .requestMatchers("/api/attendance/**").authenticated() 
                // その他の /api/ で始まるリクエストはすべて拒否 (セキュリティ強化)
                .anyRequest().denyAll() 
            )
            .addFilterBefore(new ApiKeyAuthFilter(apiKey),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /**
     * @Order(2) Web UI用のFilterChain (優先度: 低)
     * /api/ を除くすべてのWebアクセス(フォームログイン、/admin, /studentなど)に適用されます。
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception
     */
    @Bean
    @Order(2) // APIフィルタチェーンの後に処理されるように順序を明示
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //CSRF 無効化
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(authorize -> authorize
                        // /api/へのアクセスは全てAPIフィルタチェーンに任せる
                        .requestMatchers("/api/**").denyAll() 
                        
                        // ログイン、CSS、JSは全員許可
                        .requestMatchers("/login", "/css/**", "/js/**", "/error").permitAll()
                        
                        // ロール（権限）に基づいたアクセス許可
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        
                        // その他のリクエストは認証済みであれば許可
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customAuthenticationSuccessHandler()) 
                        .permitAll() // ログインフォーム関連のURLを許可
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // 明示的なログアウトURL
                        .logoutSuccessUrl("/login?logout") // ログアウト後の遷移先
                        .permitAll()
                );

        return http.build();
    }
}
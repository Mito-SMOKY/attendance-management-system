package com.example.attendancemanagementsystem.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dummy") // ★ 「dummy」プロファイルの時にだけ、この設定を読み込む
@EnableWebSecurity
public class DummySecurityConfig {

    // パスワード暗号化の道具 (これは本物と同じものを使う)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ★★★ ここがダミーデータの核心 ★★★
    // DBの代わりに、Javaのメモリ内にダミーユーザーを定義する
    @Bean
    public UserDetailsService userDetailsService() {
        // "password123" を暗号化
        String encodedPassword = passwordEncoder().encode("password123");

        // ダミーの管理者ユーザー
        UserDetails admin = User.builder()
                .username("admin@test.com") // これがログインID(メールアドレス)
                .password(encodedPassword)   // これがログインパスワード
                .roles("ADMIN")              // 権限
                .build();

        // ダミーの学生ユーザー
        UserDetails student = User.builder()
                .username("student@test.com")
                .password(encodedPassword)
                .roles("STUDENT")
                .build();

        // 2人のダミーユーザーを Spring Security に登録
        return new InMemoryUserDetailsManager(admin, student);
    }

    // セキュリティのルール設定 (H2コンソール以外は本物とほぼ同じ)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // "/login" と "/css/**" などはログイン不要でアクセスOK
                        .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                        // それ以外のURLはすべてログインが必要
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        // ログインフォームのURLは "/login"
                        .loginPage("/login")
                        // ログイン成功したら "/" (ホーム) に飛ぶ
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        // ログアウト成功したら "/login" に飛ぶ
                        .logoutSuccessUrl("/login")
                )
                .csrf(csrf -> csrf.disable()); // DBを使わないのでCSRFを一旦無効化

        return http.build();
    }
}
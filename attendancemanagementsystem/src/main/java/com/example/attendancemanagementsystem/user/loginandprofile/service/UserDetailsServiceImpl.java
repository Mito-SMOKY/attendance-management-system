package com.example.attendancemanagementsystem.user.loginandprofile.service;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
@Profile("!dummy")
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsersRepository usersRepository;

    public UserDetailsServiceImpl(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String loginIdOrEmail) throws UsernameNotFoundException {

        // 1. まずLoginIDで検索を試みる
        UsersEntity user = usersRepository.findByLoginId(loginIdOrEmail)
                // 2. もしLoginIDで見つからなければ、次にEmailで検索を試みる
                .or(() -> usersRepository.findByEmail(loginIdOrEmail))
                // 3. どちらでも見つからなければ例外をスローする
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + loginIdOrEmail));
        
        //ユーザーが見つかったらそのユーザーの権限を確認する
        //UserTypeIdが1なら学生、2なら管理者として扱う（DB.UserTypes参照）
        String role = "ROLE_USER";
        if (user.getUserTypeId() == 1) {
            role = "ROLE_STUDENT";
        } else if (user.getUserTypeId() == 2) {
            role = "ROLE_ADMIN";
        }

        //ロール（権限）の情報を持つリストの作成
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        return new User(
                user.getLoginId(), 
                user.getPassword(),
                authorities
        );
    }
}
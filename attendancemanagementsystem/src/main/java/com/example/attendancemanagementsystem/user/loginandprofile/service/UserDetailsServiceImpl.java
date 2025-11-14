package com.example.attendancemanagementsystem.user.loginandprofile.service;

import java.util.Collections;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

        return new User(
                user.getLoginId(), 
                user.getPassword(),
                Collections.emptyList()
        );
    }
}
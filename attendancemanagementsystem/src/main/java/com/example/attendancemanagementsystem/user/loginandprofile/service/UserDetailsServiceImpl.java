package com.example.attendancemanagementsystem.user.loginandprofile.service;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.attendancemanagementsystem.common.entity.AdministratorEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AdministratorRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
@Profile("!dummy")
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsersRepository usersRepository;
    private final AdministratorRepository administratorRepository; // 追加

    // コンストラクタで両方のRepositoryを注入
    public UserDetailsServiceImpl(UsersRepository usersRepository, AdministratorRepository administratorRepository) {
        this.usersRepository = usersRepository;
        this.administratorRepository = administratorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String loginIdOrEmail) throws UsernameNotFoundException {

        // 1. まずLoginIDで検索を試みる
        UsersEntity user = usersRepository.findByLoginId(loginIdOrEmail)
                // 2. もしLoginIDで見つからなければ、次にEmailで検索を試みる
                .or(() -> usersRepository.findByEmail(loginIdOrEmail))
                // 3. どちらでも見つからなければ例外をスローする
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + loginIdOrEmail));
        
        String role = "ROLE_USER";

        // ★デバッグログ: 取得したユーザー情報を確認
        System.out.println("--------------------------------------------------");
        System.out.println("★[Debug] ログイン試行ユーザー: " + user.getName() + " (ID: " + user.getUserId() + ")");
        System.out.println("★[Debug] UserTypeID: " + user.getUserTypeId());

        if (user.getUserTypeId() == 1) {
            role = "ROLE_STUDENT";
        } else if (user.getUserTypeId() == 2) {
            // 管理者の場合、administratorテーブルから詳細を取得
            AdministratorEntity adminDetails = administratorRepository.findById(user.getUserId()).orElse(null);

            // ★デバッグログ: administratorテーブルの検索結果確認
            if (adminDetails == null) {
                // System.out.println("★[Debug] adminDetails is NULL (administratorテーブルにデータが見つかりません)");
                // データがない場合は安全のため通常の管理者に倒す
                role = "ROLE_ADMIN";
            } else {
                System.out.println("★[Debug] AdminLevelID: " + adminDetails.getAdminLevelId());

                if (adminDetails.getAdminLevelId() == 1) {
                    role = "ROLE_SUPER_ADMIN";
                    // System.out.println("★[Debug] 判定結果: ROLE_SUPER_ADMIN (上位管理者)");
                } else {
                    role = "ROLE_ADMIN";
                    // System.out.println("★[Debug] 判定結果: ROLE_ADMIN (通常管理者)");
                }
            }
        }

        // System.out.println("★[Debug] 最終決定ロール: " + role);
        // System.out.println("--------------------------------------------------");

        //ロール（権限）の情報を持つリストの作成
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        // CustomUserDetails を使い、userId を渡す
        return new CustomUserDetails(
                user.getLoginId(), 
                user.getPassword(),
                authorities,
                user.getUserId()
        );
    }
}
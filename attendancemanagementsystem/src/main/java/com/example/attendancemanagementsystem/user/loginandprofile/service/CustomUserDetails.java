package com.example.attendancemanagementsystem.user.loginandprofile.service;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
// カスタムユーザ詳細クラス
public class CustomUserDetails extends User {

    private static final long serialVersionUID = 1L;

    //ユーザーエンティティを取得しているためなくても良いが、互換性を持たせるために残す
    private final Integer userId;

    //新規メソッドに使用するために追加
    private final UsersEntity usersEntity;

    /**
    //コンストラクタ
     * @param user DBから取得したユーザーエンティティ
     * @param authorities 権限リスト
     */
    public CustomUserDetails(UsersEntity user, Collection<? extends GrantedAuthority> authorities) {
        // スーパークラスのコンストラクタを呼び出す
        super(user.getLoginId(), user.getPassword(), authorities);

        // フィールドにセット
        this.userId = user.getUserId();
        this.usersEntity = user;
    }

    // ユーザID取得メソッド
    public Integer getUserId() {
        return userId;
    }
    // 画面(Thymeleaf)からユーザー情報(名前など)を取得するためのゲッター
    public UsersEntity getUsersEntity() {
        return usersEntity;
    }
}
package com.example.attendancemanagementsystem.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * APIキー (Bearer Token) による認証を行うカスタムフィルター。
 * SecurityConfig.java で /api/** パスに適用されます。
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    // コンフィグから渡された、正しいAPIキー（秘密の合言葉）
    private final String apiKey;

    /**
     * コンストラクタ
     * @param apiKey application.propertiesから注入されるAPIキー
     */
    public ApiKeyAuthFilter(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * リクエストごとに一度だけ実行されるフィルター処理のメインロジック。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. リクエストヘッダーから "Authorization" の値を取得する
        String header = request.getHeader("Authorization");

        // 2. Authorization ヘッダーが存在しない、または "Bearer " で始まらない場合
        if (header == null || !header.startsWith("Bearer ")) {
            // APIキー認証を行わず、次のフィルターへ処理を渡す
            // SecurityConfigで/api/**がauthenticated()を要求しているため、次の処理で401になる
            filterChain.doFilter(request, response);
            return;
        }

        // 3. "Bearer " の部分 (7文字) を取り除き、APIキー本体（トークン）を取得する
        String token = header.substring(7).trim();

        // 4. トークンが設定ファイルに定義された正しいAPIキーと一致しない場合
        if (!token.equals(apiKey)) {
            // 認証失敗として 401 Unauthorized を返し、レスポンスボディにエラーメッセージを書き込む
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid API Key");
            return; // 処理を中断し、次のフィルターへ進まない
        }

        // 5. APIキーが正しければ、認証オブジェクトを作成する
        // ユーザー名: "api-key-user" (仮のユーザー名、ロールや権限は不要)
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("api-key-user", null, null);

        // 6. リクエストの詳細情報を認証オブジェクトに設定する
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 7. SecurityContextHolderに認証オブジェクトを設定し、認証済みとする
        // これにより、後続のSpring SecurityフィルターやControllerで、
        // このリクエストが認証されたものとして扱われるようになる。
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 8. 認証成功。次のフィルターへ処理を渡す
        filterChain.doFilter(request, response);
    }
}
document.addEventListener('DOMContentLoaded', function() {
    // HTML要素の取得
    const otpForm = document.getElementById('otpForm');
    const otpInput = document.getElementById('otpInput');
    const submitBtn = document.getElementById('submitBtn');
    const jsErrorMsg = document.getElementById('jsErrorMsg');

    // 再送信フォームからメールアドレスの値を取得（ユーザー識別用）
    // ※th:value="${session.resetEmail}" が入っている hidden input を探します
    const emailField = document.querySelector('input[name="email"]');
    const userEmail = emailField ? emailField.value : '';

    // 1. 画面ロード時の初期フォーカス
    otpInput.focus();

    // 2. 入力制限：数字以外を自動削除
    otpInput.addEventListener('input', function() {
        // 全角数字を半角に変換
        let value = this.value.replace(/[０-９]/g, function(s) {
            return String.fromCharCode(s.charCodeAt(0) - 0xFEE0);
        });
        
        // 数字以外を削除
        this.value = value.replace(/[^0-9]/g, '');

        // 入力中はエラー表示を消す
        if (jsErrorMsg.textContent) {
            jsErrorMsg.textContent = '';
            jsErrorMsg.style.display = 'none';
        }
    });

    // 3. 送信処理（非同期通信）
    otpForm.addEventListener('submit', async function(e) {
        // 通常のフォーム送信（画面遷移）をキャンセル
        e.preventDefault();

        const inputCode = otpInput.value.trim();

        // --- クライアント側バリデーション ---
        if (inputCode === '') {
            showError('認証コードを入力してください');
            return;
        }
        if (inputCode.length !== 6) {
            showError('6桁の認証コードを入力してください');
            return;
        }

        // --- ボタンを無効化（二重送信防止） ---
        submitBtn.disabled = true;
        submitBtn.textContent = '確認中...';

        try {
            // サーバーへ送信 (JSON形式)
            // HTMLの th:action="@{/password/verify-otp}" のURLを使用
            const actionUrl = otpForm.getAttribute('action');

            const response = await fetch(actionUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                    // CSRF対策が必要な場合はここに 'X-CSRF-TOKEN': ... を追加
                },
                body: JSON.stringify({
                    email: userEmail,
                    otp: inputCode
                })
            });

            // レスポンスがJSONでない場合（サーバーエラー等）のガード
            const contentType = response.headers.get("content-type");
            if (!contentType || !contentType.includes("application/json")) {
                throw new Error("サーバーからの応答が不正です");
            }

            const data = await response.json();

            // --- 結果判定 ---
            if (data.success) {
                // 【成功】
                // サーバーから指定されたURL（パスワード再設定画面など）へ遷移
                window.location.href = data.redirectUrl || '/password/reset'; 
            } else {
                // 【失敗】
                if (data.status === 'RESENT_LOCKED') {
                    // ★ 5回失敗時：アラートを出してリセット ★
                    alert('【重要】\n試行回数の上限を超えました。\nセキュリティのため、新しい認証コードをメールで再送信しました。\n\nメールを確認し、新しいコードを入力してください。');
                    
                    otpInput.value = ''; // 入力を空にする
                    showError('新しいコードを入力してください');
                    
                } else if (data.status === 'WRONG_CODE') {
                    // 通常の間違い
                    showError(data.message); // 「コードが違います。あとX回...」
                } else {
                    // その他のエラー（有効期限切れなど）
                    showError(data.message);
                }
            }

        } catch (error) {
            console.error('通信エラー:', error);
            showError('システムエラーが発生しました。時間をおいて再度お試しください。');
        } finally {
            // ボタンを元に戻す
            submitBtn.disabled = false;
            submitBtn.textContent = '送信';
        }
    });

    // エラーメッセージ表示用関数
    function showError(msg) {
        jsErrorMsg.textContent = msg;
        jsErrorMsg.style.display = 'block';
        otpInput.classList.add('input-error'); // CSSで赤枠などを定義推奨
        otpInput.focus();
        
        // 揺れるアニメーションなどを入れる場合はここに記述
    }
});
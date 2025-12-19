document.addEventListener('DOMContentLoaded', function() {
    // HTML要素の取得
    const otpForm = document.getElementById('otpForm');
    const otpInput = document.getElementById('otpInput');
    const submitBtn = document.getElementById('submitBtn');
    const jsErrorMsg = document.getElementById('jsErrorMsg');

    // メールアドレスの取得（hidden inputから）
    const emailField = otpForm.querySelector('input[name="email"]');
    const userEmail = emailField ? emailField.value : '';

    // 1. 初期フォーカス
    otpInput.focus();

    // 2. 入力制限（数字のみ）
    otpInput.addEventListener('input', function() {
        // 全角数字を半角に
        let value = this.value.replace(/[０-９]/g, function(s) {
            return String.fromCharCode(s.charCodeAt(0) - 0xFEE0);
        });
        // 数字以外削除
        this.value = value.replace(/[^0-9]/g, '');

        // エラー消去
        if (jsErrorMsg.style.display === 'block') {
            jsErrorMsg.textContent = '';
            jsErrorMsg.style.display = 'none';
            otpInput.classList.remove('input-error');
        }
    });

    // 3. 送信処理
    otpForm.addEventListener('submit', async function(e) {
        e.preventDefault(); // 通常送信をキャンセル

        const inputCode = otpInput.value.trim();

        // クライアントバリデーション
        if (inputCode === '') {
            showError('認証コードを入力してください');
            return;
        }
        if (inputCode.length !== 6) {
            showError('6桁の認証コードを入力してください');
            return;
        }

        // ボタン無効化
        submitBtn.disabled = true;
        submitBtn.textContent = '確認中...';

        try {
            const actionUrl = otpForm.getAttribute('action');

            const response = await fetch(actionUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                    // CSRFトークンが必要な場合は以下を有効化
                    // 'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                },
                body: JSON.stringify({
                    email: userEmail,
                    otp: inputCode
                })
            });

            const contentType = response.headers.get("content-type");
            if (!contentType || !contentType.includes("application/json")) {
                throw new Error("サーバー応答エラー");
            }

            const data = await response.json();

            if (data.success) {
                // 成功時：リダイレクト
                window.location.href = data.redirectUrl || '/password/reset'; 
            } else {
                // 失敗時：メッセージ表示
                showError(data.message);
            }

        } catch (error) {
            console.error('Error:', error);
            showError('システムエラーが発生しました。');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = '確定';
        }
    });

    function showError(msg) {
        jsErrorMsg.innerHTML = msg; // HTMLタグ(改行等)を含む可能性があるためinnerHTML
        jsErrorMsg.style.display = 'block';
        otpInput.classList.add('input-error');
        otpInput.focus();
    }
});
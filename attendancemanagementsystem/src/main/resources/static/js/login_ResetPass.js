document.addEventListener('DOMContentLoaded', function() {
    
    const form = document.getElementById('resetPassForm');
    const passwordInput = document.getElementById('password');
    const confirmInput = document.getElementById('confirmPassword');
    const submitBtn = document.getElementById('submitBtn');
    const jsErrorMsg = document.getElementById('jsErrorMsg');

    // -------------------------------------------------
    // ★ 入力制限機能（全角 ＆ 禁止記号 ブロック）
    // -------------------------------------------------
    function sanitizeInput(e) {
        const input = e.target;
        let val = input.value;

        // 【処理1】半角英数字・記号(ASCII)以外をすべて削除
        // これで全角文字（あいうえお、ＡＢＣ、１２３）が消えます
        val = val.replace(/[^\x20-\x7E]/g, '');

        // 【処理2】さらに、指定された特定の禁止記号を削除
        // 対象: , ' " ` ; @ # % \ & { } < >
        val = val.replace(/[, "'`@#%\\&{};<>]/g, '');

        // 値が書き換わっていたら反映
        if (input.value !== val) {
            input.value = val;
        }
    }

    // 入力イベントに登録
    if (passwordInput) passwordInput.addEventListener('input', sanitizeInput);
    if (confirmInput) confirmInput.addEventListener('input', sanitizeInput);


    // -------------------------------------------------
    // エラー表示処理
    // -------------------------------------------------
    function showError(message) {
        if (jsErrorMsg) {
            jsErrorMsg.textContent = message;
            jsErrorMsg.style.display = 'block';
        }
    }

    function clearError() {
        if (jsErrorMsg) {
            jsErrorMsg.textContent = '';
            jsErrorMsg.style.display = 'none';
        }
    }

    // -------------------------------------------------
    // 送信時のチェック
    // -------------------------------------------------
    if (form) {
        form.addEventListener('submit', function(e) {
            clearError();
            const pass = passwordInput.value;
            const confirm = confirmInput.value;

            // 1. 一致チェック
            if (pass !== confirm) {
                e.preventDefault();
                showError('パスワードが一致しません。もう一度確認してください。');
                return;
            }

            // 2. 文字数チェック (8文字以上 24文字以下)
            if (pass.length < 8 || pass.length > 24) {
                e.preventDefault();
                showError('パスワードは8文字以上、24文字以下で設定してください。');
                return;
            }

            // 3. 大文字・小文字の混合チェック
            const hasLowerCase = /[a-z]/.test(pass);
            const hasUpperCase = /[A-Z]/.test(pass);

            if (!hasLowerCase || !hasUpperCase) {
                e.preventDefault();
                showError('パスワードには大文字と小文字をそれぞれ最低1文字含めてください。');
                return;
            }

            // OKなら送信
            submitBtn.disabled = true;
            submitBtn.textContent = '処理中...';
        });
    }

    // 入力時にエラーメッセージを消す
    if (passwordInput && confirmInput) {
        [passwordInput, confirmInput].forEach(input => {
            input.addEventListener('input', clearError);
        });
    }
});
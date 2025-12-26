document.addEventListener('DOMContentLoaded', function() {
    
    const form = document.getElementById('emailForm');
    const newEmail = document.getElementById('newEmail');
    const confirmEmail = document.getElementById('confirmEmail');
    const matchError = document.getElementById('matchError');
    const submitBtn = document.getElementById('submitBtn');
    const jsErrorMsg = document.getElementById('jsErrorMsg');

    // -------------------------------------------------
    // 1. 入力制限（メールアドレス用サニタイズ）
    // -------------------------------------------------
    function sanitizeEmail(e) {
        const input = e.target;
        let val = input.value;

        // 全角文字を削除
        val = val.replace(/[^\x20-\x7E]/g, '');

        // メールアドレスに通常使わない記号を削除
        // 許可: 英数字, @, ., -, _
        val = val.replace(/[^a-zA-Z0-9@._-]/g, '');

        if (input.value !== val) {
            input.value = val;
        }
    }

    if (newEmail) newEmail.addEventListener('input', sanitizeEmail);
    if (confirmEmail) confirmEmail.addEventListener('input', sanitizeEmail);


    // -------------------------------------------------
    // 2. 送信時のチェック
    // -------------------------------------------------
    if (form) {
        form.addEventListener('submit', function(e) {
            
            // エラーリセット
            if (jsErrorMsg) jsErrorMsg.style.display = 'none';
            if (matchError) matchError.style.display = 'none';
            if (confirmEmail) confirmEmail.classList.remove('input-error');

            const email1 = newEmail.value;
            const email2 = confirmEmail.value;

            // 未入力チェック（HTMLのrequiredでも防げますが念のため）
            if (!email1 || !email2) {
                e.preventDefault();
                showError('メールアドレスを入力してください。');
                return;
            }

            // 一致チェック
            if (email1 !== email2) {
                e.preventDefault();
                // 個別エラーメッセージを表示
                if (matchError) matchError.style.display = 'block';
                if (confirmEmail) confirmEmail.classList.add('input-error');
                return;
            }

            // 問題なければボタン無効化
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = '送信中...';
            }
        });
    }

    // -------------------------------------------------
    // 3. 入力中に一致エラーを消す処理
    // -------------------------------------------------
    if (confirmEmail) {
        confirmEmail.addEventListener('input', function() {
            if (newEmail.value === confirmEmail.value) {
                if (matchError) matchError.style.display = 'none';
                confirmEmail.classList.remove('input-error');
            }
        });
    }

    // 汎用エラー表示関数
    function showError(msg) {
        if (jsErrorMsg) {
            jsErrorMsg.textContent = msg;
            jsErrorMsg.style.display = 'block';
        }
    }
});
document.addEventListener('DOMContentLoaded', function() {
    
    const form = document.getElementById('authForm');
    const loginIdInput = document.getElementById('loginId');
    const passwordInput = document.getElementById('password');
    const submitBtn = document.getElementById('submitBtn');
    const jsErrorMsg = document.getElementById('jsErrorMsg');
    const togglePassword = document.getElementById('togglePassword');

    // -------------------------------------------------
    // 1. 入力制限（リアルタイム削除）
    // -------------------------------------------------

    // ★ログインID用（全角禁止、許可記号 @ . - _ 以外禁止）
    function sanitizeLoginId(e) {
        const input = e.target;
        let val = input.value;

        // 全角削除
        val = val.replace(/[^\x20-\x7E]/g, '');
        // 許可記号以外削除
        val = val.replace(/[^a-zA-Z0-9@._-]/g, '');

        if (input.value !== val) {
            input.value = val;
        }
    }

    // ★パスワード用（全角禁止、禁止記号削除）
    function sanitizePassword(e) {
        const input = e.target;
        let val = input.value;

        // 全角削除
        val = val.replace(/[^\x20-\x7E]/g, '');
        // 禁止記号削除
        val = val.replace(/[, "'`@#%\\&{};<>]/g, '');

        if (input.value !== val) {
            input.value = val;
        }
    }

    if (loginIdInput) loginIdInput.addEventListener('input', sanitizeLoginId);
    if (passwordInput) passwordInput.addEventListener('input', sanitizePassword);


    // -------------------------------------------------
    // 2. パスワード表示切替
    // -------------------------------------------------
    if (togglePassword && passwordInput) {
        togglePassword.addEventListener('click', function () {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            this.classList.toggle('fa-eye');
            this.classList.toggle('fa-eye-slash');
        });
    }

    // -------------------------------------------------
    // 3. エラー表示関数
    // -------------------------------------------------
    function showError(message) {
        if (jsErrorMsg) {
            jsErrorMsg.textContent = message;
            jsErrorMsg.style.display = 'block';
        } else {
            alert(message);
        }
    }
    
    function clearError() {
        if (jsErrorMsg) {
            jsErrorMsg.style.display = 'none';
            jsErrorMsg.textContent = '';
        }
    }

    // -------------------------------------------------
    // 4. 送信時のチェック
    // -------------------------------------------------
    if (form) {
        form.addEventListener('submit', function(e) {
            clearError();
            const pass = passwordInput ? passwordInput.value : '';

            // 文字数チェック (8文字以上 24文字以下)
            if (pass.length < 8 || pass.length > 24) {
                e.preventDefault();
                showError('パスワードは8文字以上、24文字以下で入力してください。');
                return;
            }

            // 大文字・小文字の混合チェック
            const hasLowerCase = /[a-z]/.test(pass);
            const hasUpperCase = /[A-Z]/.test(pass);

            if (!hasLowerCase || !hasUpperCase) {
                e.preventDefault();
                showError('パスワードには大文字と小文字をそれぞれ1文字以上含めてください。');
                return;
            }

            // OKならボタンを無効化
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = '処理中...';
            }
        });
    }
    
    // 入力時にエラーを消す
    if (passwordInput) passwordInput.addEventListener('input', clearError);
});
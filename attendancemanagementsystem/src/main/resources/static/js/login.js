document.addEventListener('DOMContentLoaded', function() {
    console.log("Login script loaded."); // ★F12キーのコンソールでこれが表示されるか確認

    // 要素の取得
    const form = document.getElementById('loginForm');
    const passwordInput = document.getElementById('password');
    const usernameInput = document.getElementById('username');
    const submitBtn = document.getElementById('submitBtn');
    const jsErrorMsg = document.getElementById('jsErrorMsg');
    const togglePassword = document.getElementById('togglePassword');

    // -------------------------------------------------
    // 1. 入力制限（打った瞬間に消す処理）
    // -------------------------------------------------
    function sanitizeInput(e) {
        const input = e.target;
        let val = input.value;

        // ① 全角文字などを削除 (半角英数字・記号以外を消す)
        val = val.replace(/[^\x20-\x7E]/g, '');

        // ② 特定の禁止記号を削除 ( , ' " ` ; @ # % \ & { } < > )
        val = val.replace(/[, "'`@#%\\&{};<>]/g, '');

        // 値が変わっていたら反映
        if (input.value !== val) {
            input.value = val;
        }
    }

    // パスワード欄に入力制限を適用
    if (passwordInput) {
        passwordInput.addEventListener('input', sanitizeInput);
    }
    
    // ユーザーID欄は全角のみ禁止（メールアドレスの記号は許可）
    if (usernameInput) {
        usernameInput.addEventListener('input', function(e) {
            const input = e.target;
            let val = input.value;
            // 全角文字のみ削除（半角のみ許可）
            val = val.replace(/[^\x20-\x7E]/g, '');
            if (input.value !== val) {
                input.value = val;
            }
        });
    }

    // -------------------------------------------------
    // 2. 送信時のチェック（ボタンを押したとき）
    // -------------------------------------------------
    if (form) {
        form.addEventListener('submit', function(e) {
            
            // エラーをリセット
            if (jsErrorMsg) {
                jsErrorMsg.textContent = '';
                jsErrorMsg.style.display = 'none';
            }

            
            // 問題なければボタンを無効化（連打防止）
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'ログイン中...';
            }
        });
    } else {
        console.error("loginFormが見つかりません。HTMLのformタグにid='loginForm'があるか確認してください。");
    }

    function showError(msg) {
        if (jsErrorMsg) {
            jsErrorMsg.textContent = msg;
            jsErrorMsg.style.display = 'block';
        } else {
            alert(msg);
        }
    }

    // -------------------------------------------------
    // 3. パスワード表示切替
    // -------------------------------------------------
    if (togglePassword && passwordInput) {
        togglePassword.addEventListener('click', function () {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            this.classList.toggle('fa-eye');
            this.classList.toggle('fa-eye-slash');
        });
    }
});

window.addEventListener('load', () => {
    const body = document.body;
    const splash = document.getElementById('splash');

    // 1. セッションに「再生済みフラグ」があるか確認
    const hasPlayed = sessionStorage.getItem('animationPlayed');

    if (hasPlayed) {
        // --- 【スキップ処理】フラグがある場合は、すべてを即座に表示 ---
        if (splash) splash.style.display = 'none';
        body.classList.remove('loading');
        body.classList.add('header-active', 'loaded', 'show-static', 'animation-done');
        
    } else {
        // --- 【通常処理】フラグがない場合は、いつものアニメーションを実行 ---
        
        // ヘッダー表示
        setTimeout(() => body.classList.add('header-active'), 100); 

        // 波紋待機後の移動開始
        setTimeout(() => {
            body.classList.add('loaded'); 

            setTimeout(() => {
                body.classList.add('show-static');

                setTimeout(() => {
                    body.classList.add('animation-done'); 
                    
                    if(splash) splash.style.opacity = '0';

                    setTimeout(() => {
                        if(splash) splash.style.display = 'none';
                        body.classList.remove('loading');
                        
                        // ★アニメーションが最後まで終わったら「再生済み」フラグをセット
                        sessionStorage.setItem('animationPlayed', 'true');

                    },500);
                },); // 静止時間
            }, 1200);
        }, 2200);
    }
});
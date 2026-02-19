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
    const splashLogoContainer = document.querySelector('.splash-logo-container');
    const staticLogo = document.querySelector('.static-logo');

    // const hasPlayed = sessionStorage.getItem('animationPlayed');

    // if (hasPlayed) {
    //     if (splash) splash.style.display = 'none';
    //     body.classList.remove('loading');
    //     body.classList.add('header-active', 'loaded', 'show-static', 'animation-done');
        
    // } else {
        // 1. ヘッダー表示
        setTimeout(() => body.classList.add('header-active'), 100); 

        // 2. 波紋待機後の移動開始
        setTimeout(() => {
            // --- 座標計算ロジック ---
            if (staticLogo && splashLogoContainer) {
                //スタティックロゴの現在の位置を取得
                const rect = staticLogo.getBoundingClientRect();
                
                //画面の中心座標
                const centerX = window.innerWidth / 2;
                const centerY = window.innerHeight / 2;

                //中心から目的地までの正確な距離を計算
                const moveX = (rect.left + rect.width / 2) - centerX;
                const moveY = (rect.top + rect.height / 2) - centerY;

                //座標を指定
                splashLogoContainer.style.transform = `translate(calc(-50% + ${moveX}px), calc(-50% + ${moveY}px)) scale(1)`;
            }

            body.classList.add('loaded'); 

            // 3. 移動完了後（1.5秒後）にスタティックロゴを裏で表示
            setTimeout(() => {
                body.classList.add('show-static');

                setTimeout(() => {
                    body.classList.add('animation-done'); 
                    
                    if(splash) splash.style.opacity = '0';

                    setTimeout(() => {
                        if(splash) splash.style.display = 'none';
                        body.classList.remove('loading');
                        sessionStorage.setItem('animationPlayed', 'true');
                    }, 500); // 消えるアニメーション時間
                },); 
            }, 1300); // 移動にかかる時間（CSSの1.5sと合わせる）
        }, 2500); // 最初の波紋待機
    }
);
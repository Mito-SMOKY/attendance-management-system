document.addEventListener('DOMContentLoaded', function() {
    // ---------------------------------------------------------
    // 1. 要素の取得
    // ---------------------------------------------------------
    const otpForm = document.getElementById('otpForm');
    const otpInput = document.getElementById('otpInput');
    const submitBtn = document.getElementById('submitBtn');
    const jsErrorMsg = document.getElementById('jsErrorMsg');
    
    // HTML側で用意されているサーバーエラー表示エリアを隠す制御用
    const serverErrorContainer = document.querySelector('div[th\\:if="${param.error}"]') || 
                                document.querySelector('div[style*="color: red"]');

    // 再送信関連
    const resendForm = document.getElementById('resendForm');
    const resendBtn = document.getElementById('resendBtn');
    const successMsg = document.querySelector('.success-msg');

    // ---------------------------------------------------------
    // 2. 入力制御（数字のみ・6桁制限）
    // ---------------------------------------------------------
    if (otpInput) {
        // 初期フォーカス
        otpInput.focus();

        otpInput.addEventListener('input', function(e) {
            let val = e.target.value;

            // 半角数字以外を削除
            val = val.replace(/[^0-9]/g, '');

            if (e.target.value !== val) {
                e.target.value = val;
            }

            // エラー表示リセット
            if (jsErrorMsg) {
                jsErrorMsg.style.display = 'none';
                jsErrorMsg.textContent = '';
            }
            if (otpInput.classList.contains('input-error')) {
                otpInput.classList.remove('input-error');
            }
        });
    }

    // ---------------------------------------------------------
    // 3. 送信時のバリデーション
    // ---------------------------------------------------------
    if (otpForm) {
        otpForm.addEventListener('submit', function(e) {
            
            const val = otpInput.value;

            // 未入力チェック
            if (!val) {
                e.preventDefault();
                showError('認証コードを入力してください。');
                return;
            }

            // 桁数チェック
            if (val.length < 6) {
                e.preventDefault();
                showError('認証コードは6桁で入力してください。');
                return;
            }

            // OKならボタン無効化（連打防止）
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = '送信中...';
            }
        });
    }

    // ---------------------------------------------------------
    // 4. エラー表示処理
    // ---------------------------------------------------------
    function showError(htmlMsg) {
        // JSエラーを出すときはサーバー側のエラー表示を隠す
        if (serverErrorContainer) serverErrorContainer.style.display = 'none';

        if (!jsErrorMsg) return;

        jsErrorMsg.innerHTML = htmlMsg;
        jsErrorMsg.style.display = 'block';
        
        if (otpInput) {
            otpInput.classList.add('input-error');
            otpInput.focus();
        }
    }

    // ---------------------------------------------------------
    // 5. 再送信タイマー（60秒）
    // ---------------------------------------------------------
    // 成功メッセージが出ている＝再送信直後なのでタイマー開始
    if (successMsg && resendBtn) {
        startResendTimer(60);
    }
    
    if (resendForm) {
        resendForm.addEventListener('submit', function() {
            if (resendBtn) {
                resendBtn.disabled = true;
                resendBtn.textContent = '送信中...';
            }
        });
    }

    function startResendTimer(seconds) {
        if (!resendBtn) return;
        
        resendBtn.disabled = true;
        let count = seconds;
        resendBtn.textContent = `再送信 (${count})`;
        
        const timer = setInterval(() => {
            count--;
            resendBtn.textContent = `再送信 (${count})`;
            
            if (count <= 0) {
                clearInterval(timer);
                resendBtn.textContent = '認証コードを再送信する';
                resendBtn.disabled = false;
            }
        }, 1000);
    }
});
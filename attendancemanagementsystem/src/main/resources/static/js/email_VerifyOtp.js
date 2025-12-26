document.addEventListener('DOMContentLoaded', function() {
    
    const otpInput = document.getElementById('otpInput');
    const submitBtn = document.getElementById('submitBtn');
    const otpForm = document.getElementById('otpForm');
    const jsErrorMsg = document.getElementById('jsErrorMsg');

    // 初期化：エラーメッセージをクリア
    if (jsErrorMsg) jsErrorMsg.textContent = '';

    // -------------------------------------------------
    // 1. 入力制限（数字のみ許可）
    // -------------------------------------------------
    if (otpInput) {
        otpInput.addEventListener('input', function(e) {
            let val = e.target.value;

            // 半角数字以外を削除
            val = val.replace(/[^0-9]/g, '');

            if (e.target.value !== val) {
                e.target.value = val;
            }
            
            // 入力中はエラーを消す
            if (jsErrorMsg) jsErrorMsg.textContent = '';
        });
    }

    // -------------------------------------------------
    // 2. 送信時のチェック
    // -------------------------------------------------
    if (otpForm) {
        otpForm.addEventListener('submit', function(e) {
            
            const val = otpInput.value;

            // 未入力チェック
            if (!val) {
                e.preventDefault();
                showError('認証コードを入力してください。');
                return;
            }

            // 桁数チェック（6桁固定の場合）
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

    function showError(msg) {
        if (jsErrorMsg) {
            jsErrorMsg.textContent = msg;
            jsErrorMsg.style.display = 'block';
        } else {
            alert(msg);
        }
    }
});
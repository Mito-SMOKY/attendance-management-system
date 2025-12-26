document.addEventListener('DOMContentLoaded', function() {
    // ---------------------------------------------------------
    // 1. 要素の取得
    // ---------------------------------------------------------
    const otpForm = document.getElementById('otpForm');
    const otpInput = document.getElementById('otpInput');
    const submitBtn = document.getElementById('submitBtn');
    const jsErrorMsg = document.getElementById('jsErrorMsg');
    
    // HTML側で用意されているサーバーエラー表示エリア（Thymeleafが表示するもの）
    // JSでエラーを出すときは、こっちが重複して表示されないように消す必要がある
    const serverErrorContainer = document.querySelector('div[th\\:if="${param.error}"]') || 
                                document.querySelector('.text-danger') || 
                                document.querySelector('div[style*="color: red"]');

    // 再送信関連
    const resendForm = document.getElementById('resendForm');
    const resendBtn = document.getElementById('resendBtn');
    const successMsg = document.querySelector('.success-msg');

    // CSRF関連
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    const csrfToken = csrfTokenMeta ? csrfTokenMeta.content : '';
    const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.content : 'X-CSRF-TOKEN';

    // ---------------------------------------------------------
    // 2. 入力制御（鉄壁ガード：そもそも打たせない・6文字制限）
    // ---------------------------------------------------------
    if (otpInput) {
        otpInput.focus();

        // キーを押した瞬間のガード
        otpInput.addEventListener('keydown', function(e) {
            const allowedKeys = ['Backspace', 'Delete', 'Tab', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Enter', 'Home', 'End'];
            
            if (allowedKeys.includes(e.key) || e.ctrlKey || e.metaKey) return;
            if (e.key === 'Process' || e.isComposing) return;

            // 数字以外ブロック
            if (!/^[0-9]$/.test(e.key)) {
                e.preventDefault();
                return;
            }

            // 6文字以上ブロック
            const currentVal = e.target.value;
            const selectionLen = e.target.selectionEnd - e.target.selectionStart;
            if (currentVal.length >= 6 && selectionLen === 0) {
                e.preventDefault();
            }
        });

        // 貼り付けガード
        otpInput.addEventListener('paste', function(e) {
            e.preventDefault();
            let pasteData = (e.clipboardData || window.clipboardData).getData('text');
            pasteData = pasteData.replace(/[０-９]/g, s => String.fromCharCode(s.charCodeAt(0) - 0xFEE0));
            pasteData = pasteData.replace(/[^0-9]/g, '');
            
            const input = e.target;
            const currentVal = input.value;
            const start = input.selectionStart;
            const end = input.selectionEnd;
            
            let newVal = currentVal.substring(0, start) + pasteData + currentVal.substring(end);
            if (newVal.length > 6) newVal = newVal.slice(0, 6);
            
            input.value = newVal;
            clearError(); 
        });

        // 入力値クリーニング
        const cleanInput = (target) => {
            let value = target.value;
            value = value.replace(/[０-９]/g, s => String.fromCharCode(s.charCodeAt(0) - 0xFEE0));
            value = value.replace(/[^0-9]/g, '');
            if (value.length > 6) value = value.slice(0, 6);

            if (target.value !== value) target.value = value;
            clearError(); 
        };

        otpInput.addEventListener('input', function(e) { cleanInput(e.target); });
        otpInput.addEventListener('compositionend', function(e) { cleanInput(e.target); });
    }

    // エラーを消す関数
    function clearError() {
        if (jsErrorMsg) {
            jsErrorMsg.innerHTML = '';
            jsErrorMsg.style.display = 'none';
        }
        if (otpInput) otpInput.classList.remove('input-error');
        if (serverErrorContainer) serverErrorContainer.style.display = 'none';
    }

    // ---------------------------------------------------------
    // 3. 送信処理 (HTMLのエラー文言に合わせて表示する)
    // ---------------------------------------------------------
    if (otpForm) {
        otpForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            // 直前クリーニング
            if(otpInput) {
                let val = otpInput.value;
                val = val.replace(/[０-９]/g, s => String.fromCharCode(s.charCodeAt(0) - 0xFEE0));
                val = val.replace(/[^0-9]/g, '');
                if (val.length > 6) val = val.slice(0, 6);
                otpInput.value = val;
            }

            const emailInput = otpForm.querySelector('input[name="email"]');
            const userEmail = emailInput ? emailInput.value : '';
            const inputCode = otpInput.value.trim();

            if (inputCode === '') {
                showError('認証コードを入力してください');
                return;
            }
            if (inputCode.length !== 6) {
                showError('6桁の認証コードを入力してください');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.textContent = '確認中...';

            try {
                const actionUrl = otpForm.getAttribute('action');
                const params = new URLSearchParams();
                params.append('email', userEmail);
                params.append('otp', inputCode);

                const response = await fetch(actionUrl, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        [csrfHeader]: csrfToken
                    },
                    body: params
                });
                
                if (response.status === 403) {
                    throw new Error("セッションが無効です。画面を更新してください。");
                }

                // サーバーがHTML(リダイレクト)を返してきた場合
                // (JSONじゃなくてHTMLが返ってきた＝エラー画面へのリダイレクトの可能性が高い)
                const contentType = response.headers.get("content-type");
                if (contentType && contentType.includes("text/html")) {
                    // JSONを期待してるのにHTMLが来た場合、無理にパースせず
                    // 「認証コードが違います」の可能性が高いのでそう表示するか、
                    // もしくは window.location.reload() してサーバー側の表示に任せる手もある。
                    // ここでは「HTMLの記述に合わせて」とのことなので、メッセージをマッピングする。
                    showMappedError('invalid'); 
                    return;
                }

                const data = await response.json();

                if (data.success) {
                    window.location.replace(data.redirectUrl || '/password/reset'); 
                } else {
                    // サーバーから返ってきた data.message (例: "invalid") を
                    // HTMLの記述に合わせた日本語に変換して表示
                    showMappedError(data.message);
                }

            } catch (error) {
                console.error('Error:', error);
                // 本当に通信エラーなどの場合だけシステムエラーと出す
                showError("システムエラーが発生しました。<br>しばらく待ってから再試行してください。");
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = '確定';
            }
        });
    }

    // ---------------------------------------------------------
    // ★ここが重要：エラーメッセージをHTMLの記述に合わせる関数★
    // ---------------------------------------------------------
    function showMappedError(serverMsg) {
        let displayHtml = '';

        // サーバーからのメッセージ(serverMsg)に含まれる単語で判定
        // ※サーバーが何を返してくるか不明な場合でも、これならある程度拾える
        if (!serverMsg) {
            displayHtml = '認証コードが正しくありません。<br>メールを確認して再入力してください。';
        } 
        else if (serverMsg.includes('invalid') || serverMsg.includes('correct') || serverMsg.includes('違います')) {
            // param.error[0] == 'invalid' の時のHTML
            displayHtml = '<p>認証コードが正しくありません。<br>メールを確認して再入力してください。</p>';
        } 
        else if (serverMsg.includes('expired') || serverMsg.includes('期限')) {
            // param.error[0] == 'expired' の時のHTML
            displayHtml = `
                <p style="font-weight: bold;">有効期限が切れています。</p>
                <a href="/password/forgot" style="color: #007bff; text-decoration: underline;">
                    認証コードを再発行する
                </a>`;
        } 
        else if (serverMsg.includes('soon') || serverMsg.includes('短すぎ')) {
            // param.error[0] == 'too_soon' の時のHTML
            displayHtml = '<p>再送信の間隔が短すぎます。<br>少し時間を置いてからお試しください。</p>';
        } 
        else {
            // それ以外はそのまま表示（サーバーが日本語を返している場合など）
            displayHtml = serverMsg;
        }

        showError(displayHtml);
    }

    // 汎用エラー表示関数
    function showError(htmlMsg) {
        if (serverErrorContainer) serverErrorContainer.style.display = 'none'; // サーバー側の表示は隠す

        if (!jsErrorMsg) return;

        jsErrorMsg.innerHTML = htmlMsg; // HTMLタグを使えるように innerHTML
        jsErrorMsg.style.display = 'block';
        
        if (otpInput) {
            otpInput.classList.add('input-error');
            otpInput.focus();
        }
    }

    // ---------------------------------------------------------
    // 5. 再送信タイマー
    // ---------------------------------------------------------
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
                resendBtn.textContent = '再送信';
                resendBtn.disabled = false;
            }
        }, 1000);
    }
});
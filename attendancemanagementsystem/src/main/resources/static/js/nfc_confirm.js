// nfc_confirm.js: 書き込み画面用スクリプト

document.addEventListener('DOMContentLoaded', () => {
    
    const authBtn = document.getElementById('btn-auth');
    const userIdInput = document.getElementById('input-user-id');
    const userNameInput = document.getElementById('input-user-name');

    // 名前を取得して表示する関数
    const updateUserName = async () => {
        const loginId = userIdInput.value; // 変数名をわかりやすく loginId とみなす
        
        if (!loginId) {
            userNameInput.value = "";
            return;
        }

        try {
            // 修正: パラメータ名を userId から loginId に変更（バックエンド側も合わせる必要あり）
            // または、バックエンドが userId というパラメータ名で LoginId を受け取るならそのままでも可
            // ここでは明示的に loginId として送る形を推奨します
            const res = await fetch(`/api/issue/user-info?loginId=${encodeURIComponent(loginId)}`);
            const data = await res.json();

            if (data.status === 'success') {
                userNameInput.value = data.name;
                userNameInput.style.color = "black";
            } else {
                userNameInput.value = "未登録のユーザーです";
                userNameInput.style.color = "red";
            }
        } catch (e) {
            console.error("API Error:", e);
            userNameInput.value = "取得エラー";
        }
    };

    userIdInput.addEventListener('change', updateUserName);

    if (userIdInput.value) {
        updateUserName();
    }
    
    authBtn.addEventListener('click', async () => {
        const loginId = userIdInput.value;
        const cardId = document.getElementById('input-card-id').value;

        if (!loginId) {
            alert("学籍番号(ログインID)を入力してください");
            return;
        }

        document.getElementById('view-input').classList.add('hidden');
        document.getElementById('view-loading').classList.remove('hidden');

        try {
            const res = await fetch('/api/issue/write', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ 
                    login_id: loginId, // 修正: user_id(数値) ではなく login_id(文字列) を送る
                    card_id: cardId
                })
            });

            if (res.ok) {
                window.location.href = '/admin/nfc/result?status=success';
            } else {
                const txt = await res.text();
                window.location.href = `/admin/nfc/result?status=error&msg=${encodeURIComponent(txt)}`;
            }
        } catch (e) {
            console.error(e);
            window.location.href = '/admin/nfc/result?status=error&msg=通信エラー';
        }
    });
});
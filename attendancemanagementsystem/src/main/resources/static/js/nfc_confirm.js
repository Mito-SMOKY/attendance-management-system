// confirm.js: 書き込み画面用スクリプト

document.addEventListener('DOMContentLoaded', () => {
    
    const authBtn = document.getElementById('btn-auth');
    const userIdInput = document.getElementById('input-user-id');
    const userNameInput = document.getElementById('input-user-name');

   // 名前を取得して表示する関数（共通化）
    const updateUserName = async () => {
        const userId = userIdInput.value;
        
        // 入力が空なら名前欄もクリア
        if (!userId) {
            userNameInput.value = "";
            return;
        }

        try {
            // APIを呼んで名前を取得
            const res = await fetch(`/api/issue/user-info?userId=${userId}`);
            const data = await res.json();

            if (data.status === 'success') {
                userNameInput.value = data.name; // 名前を表示
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

    // 学籍番号を手入力して変更したとき
    userIdInput.addEventListener('change', updateUserName);

    // 画面を開いた瞬間（すでにIDが入っている場合）
    if (userIdInput.value) {
        updateUserName();
    }
    
    // ボタンクリック時の処理
    authBtn.addEventListener('click', async () => {
        const userIdInput = document.getElementById('input-user-id');
        const userId = userIdInput.value;
        const cardIdInput = document.getElementById('input-card-id');
        const cardId = cardIdInput.value;

        if (!userId) {
            alert("ユーザーIDを入力してください");
            return;
        }

        // 画面切り替え (入力フォームを隠して、ローディングを表示)
        document.getElementById('view-input').classList.add('hidden');
        document.getElementById('view-loading').classList.remove('hidden');

        try {
            // 書き込みAPI実行
            const res = await fetch('/api/issue/write', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ 
                    user_id: parseInt(userId),
                    card_id: cardId
                })
            });

            if (res.ok) {
                // 成功 -> 結果画面(success)へ
                window.location.href = '/admin/nfc/result?status=success';
            } else {
                // 失敗 -> 結果画面(error)へ
                const txt = await res.text();
                window.location.href = `/admin/nfc/result?status=error&msg=${encodeURIComponent(txt)}`;
            }
        } catch (e) {
            window.location.href = '/admin/nfc/result?status=error&msg=通信エラー';
        }
    });
});
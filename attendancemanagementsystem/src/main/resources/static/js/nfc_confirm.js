// confirm.js: 書き込み画面用スクリプト

document.addEventListener('DOMContentLoaded', () => {
    
    const authBtn = document.getElementById('btn-auth');
    
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
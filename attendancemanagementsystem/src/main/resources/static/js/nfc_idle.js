// idle.js: 待機画面用スクリプト

let lastTimestamp = 0;

// 1秒ごとにポーリングを実行
setInterval(async () => {
    try {
        const res = await fetch('/api/issue/poll');
        const data = await res.json();

        // タイムスタンプが更新されたら画面遷移
        if (data.timestamp > lastTimestamp) {
            lastTimestamp = data.timestamp;
            
            // カード検知！ -> 書き込み画面へ移動
            if (data.status === 'SCANNED') {
                const cardId = encodeURIComponent(data.cardId || '');
                const userId = encodeURIComponent(data.userId || '');
                // リダイレクト
                window.location.href = `/admin/nfc/confirm?cardId=${cardId}&userId=${userId}`;
            } 
            // エラー検知 -> 結果画面(失敗)へ移動
            else if (data.status === 'ERROR') {
                const msg = encodeURIComponent(data.error || 'エラー');
                window.location.href = `/admin/nfc/result?status=error&msg=${msg}`;
            }
        }
    } catch (e) {
        console.error("Connection error", e);
    }
}, 1000);
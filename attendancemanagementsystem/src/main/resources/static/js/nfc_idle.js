// nfc_idle.js: 待機画面用スクリプト

let lastTimestamp = 0;

// 1秒ごとにポーリングを実行
setInterval(async () => {
    try {
        // キャッシュ対策で時間を付与
        const res = await fetch('/api/issue/poll?t=' + new Date().getTime());
        const data = await res.json();

        // ログ出力(確認用)
        // console.log("Server Status:", data.status);

        // タイムスタンプが更新されたら画面遷移
        if (data.timestamp > lastTimestamp) {
            
            // 初回ロード時はタイムスタンプ同期のみ
            if (lastTimestamp === 0) {
                lastTimestamp = data.timestamp;
                return;
            }
            lastTimestamp = data.timestamp;

            // URLパラメータを付けずにリダイレクトする
            if (data.status === 'SCANNED') {
                window.location.href = '/admin/nfc/confirm';
            } 
            else if (data.status === 'ERROR') {
                // エラーメッセージをURLエンコードして付与
                const msg = encodeURIComponent(data.error || 'エラー');
                window.location.href = `/admin/nfc/result?status=error&msg=${msg}`;
            }
        }
    } catch (e) {
        console.error("Connection error", e);
    }
}, 1000);
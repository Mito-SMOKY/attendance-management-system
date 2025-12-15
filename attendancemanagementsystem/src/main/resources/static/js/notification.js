/**
 * 検索キーワードとフィルタタイプを取得し、APIを呼び出して結果を再描画する
 */
function fetchNotifications() {
    const keyword = document.getElementById('keywordSearch').value;
    const type = document.getElementById('notificationTypeFilter').value;
    const tableBody = document.getElementById('notificationList');
    
    // パラメータをURLクエリ形式に整形
    const params = new URLSearchParams({
        keyword: keyword,
        type: type
        // page, size などのページネーション情報も追加する
    });

    const apiUrl = `/api/notifications?${params.toString()}`;

    fetch(apiUrl)
        .then(response => response.json())
        .then(notifications => {
            // 既存のリストをクリア
            tableBody.innerHTML = ''; 
            
            // 取得したデータを基にテーブルの行を生成
            notifications.forEach(notification => {
                const row = tableBody.insertRow();
                
                // 日付（例: 12/02(月) のように整形）
                row.insertCell(0).textContent = formatNotificationDate(notification.date);
                
                // 時間（例: 00:00）
                row.insertCell(1).textContent = notification.time; 
                
                // 通知タイトル
                const detailCell = row.insertCell(2);
                const titleLink = document.createElement('a');
                titleLink.href = notification.detailUrl; // 詳細画面へのリンク
                titleLink.textContent = notification.title;
                detailCell.appendChild(titleLink);
            });
        })
        .catch(error => {
            console.error('通知データの取得に失敗しました:', error);
            tableBody.innerHTML = '<tr><td colspan="3">通知データの読み込みに失敗しました。</td></tr>';
        });
}

// 画面読み込み時に一度実行
document.addEventListener('DOMContentLoaded', fetchNotifications);
document.addEventListener('DOMContentLoaded', function() {
    
    // --- 1. 変数定義 ---
    const endBtn = document.getElementById('endSessionBtn');
    // ThymeleafからセッションIDを取得
    const sessionId = endBtn.getAttribute('data-session-id');
    const tableBody = document.getElementById('attendeeList');
    const countBadge = document.getElementById('studentCount');
    const lastUpdatedLabel = document.getElementById('lastUpdated');

    // --- 2. リアルタイム更新機能 (Poll) ---
    
    window.fetchAttendees = function() {
        if(!sessionId) return;

        fetch(`/session/api/attendees/${sessionId}`)
            .then(response => {
                if (!response.ok) throw new Error("Network response was not ok");
                return response.json();
            })
            .then(data => {
                updateTable(data);
                updateTimestamp();
            })
            .catch(error => {
                console.error('Polling error:', error);
                // エラーが出ても画面を壊さないように静かに無視するか、コンソールに出すだけにする
            });
    };

    function updateTable(data) {
        // データが空の場合
        if (data.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">まだ入室者はいません</td></tr>`;
            countBadge.textContent = "0名";
            return;
        }

        // テーブルの中身を再構築
        let html = '';
        data.forEach(row => {
            html += `
                <tr class="fade-in">
                    <td>${row.studentId}</td>
                    <td class="fw-bold">${row.name}</td>
                    <td>${row.entryTime}</td>
                    <td><span class="badge bg-info text-dark">${row.status}</span></td>
                </tr>
            `;
        });
        
        // 既存のHTMLと比較して変更がある場合だけ書き換えるのがベストですが、
        // 簡易実装としてinnerHTMLを上書きします
        tableBody.innerHTML = html;
        countBadge.textContent = `${data.length}名`;
    }

    function updateTimestamp() {
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        lastUpdatedLabel.textContent = `最終更新: ${timeStr}`;
    }

    // ★ 3秒ごとに自動更新を開始 ★
    // (setIntervalはウィンドウが閉じられると自動で止まります)
    setInterval(fetchAttendees, 3000);

    // 初回実行
    fetchAttendees();


    // --- 3. 終了ボタンの処理 (以前と同じ) ---
    endBtn.addEventListener('click', function() {
        if (!confirm('本当に授業を終了しますか？\n終了すると出席扱いが確定します。')) {
            return;
        }

        // UIのロック
        document.getElementById('loadingOverlay').style.display = 'flex';
        endBtn.disabled = true;

        fetch('/session/end', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ sessionId: parseInt(sessionId) })
        })
        .then(response => {
            if (response.ok) {
                alert('お疲れ様でした！授業を終了しました。');
                window.close(); // ウィンドウを閉じる
            } else {
                throw new Error('サーバーエラーが発生しました');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('終了処理に失敗しました。');
            document.getElementById('loadingOverlay').style.display = 'none';
            endBtn.disabled = false;
        });
    });
});
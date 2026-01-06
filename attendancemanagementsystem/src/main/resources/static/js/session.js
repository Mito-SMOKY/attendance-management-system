/**
 * 授業実施中 画面制御ロジック
 */
(function() {
    // HTML側で定義された設定を取得
    const config = window.SESSION_CONFIG || { sessionId: 0, statusOptions: [] };
    const sessionId = config.sessionId;
    const ALL_STATUS_OPTIONS = config.statusOptions;

    // 先生が手動で変更可能なステータス
    const SELECTABLE_STATUS_IDS = [1, 2, 3];

    const attendeeListBody = document.getElementById('attendeeListBody');
    const lastUpdatedLabel = document.getElementById('lastUpdated');

    // 手動変更の一時保存用オブジェクト
    let localChanges = {}; 

    /**
     * 出席者データの取得
     */
    function fetchAttendees() {
        if (!sessionId) return;
        fetch(`/session/api/attendees/${sessionId}`)
            .then(res => res.json())
            .then(data => {
                // セッションステータスチェック (1:授業中 以外なら終了)
                if (data.status !== undefined && data.status !== 1) {
                    alert('この授業は終了、または破棄されました。\n画面を閉じます。');
                    window.close(); 
                    return;
                }

                // リスト描画 (新旧API構造に対応)
                const list = data.attendees ? data.attendees : data;
                renderTable(list);
                updateTimestamp();
            })
            .catch(err => console.error("データ取得エラー:", err));
    }

    /**
     * テーブルのレンダリング
     */
    function renderTable(attendees) {
        if(!Array.isArray(attendees)) return;

        let html = '';
        let counts = { 1:0, 2:0, 3:0, 4:0, 5:0, 6:0, total: attendees.length };

        attendees.forEach(student => {
            let displayStatusId = student.statusId;
            let isChanged = false;

            // ローカル（未保存）の変更があればそちらを優先
            if (localChanges.hasOwnProperty(student.userId)) {
                displayStatusId = localChanges[student.userId];
                isChanged = true;
            }

            if (counts.hasOwnProperty(displayStatusId)) counts[displayStatusId]++;

            // 行のデザイン判定
            let rowClass = isChanged ? 'status-changed ' : '';
            if (displayStatusId === 1) rowClass += 'status-present';
            else if (displayStatusId === 2) rowClass += 'status-absent';
            else if (displayStatusId === 3) rowClass += 'status-late';
            else if (displayStatusId === 4) rowClass += 'status-official';
            else if (displayStatusId === 6) rowClass += 'status-suspended';

            // ステータス選択プルダウンの生成
            let selectHtml = `<select class="form-select form-select-sm" 
                                    onchange="APP.changeStatus(${student.userId}, this.value)"
                                    style="background-color: rgba(255,255,255,0.7); cursor: pointer;">`;
            
            ALL_STATUS_OPTIONS.forEach(opt => {
                if (SELECTABLE_STATUS_IDS.includes(opt.statusId) || opt.statusId === displayStatusId) {
                    const selected = (opt.statusId === displayStatusId) ? 'selected' : '';
                    selectHtml += `<option value="${opt.statusId}" ${selected}>${opt.statusName}</option>`;
                }
            });
            selectHtml += `</select>`;

            const statusName = getStatusName(displayStatusId);

            html += `
                <tr class="${rowClass}">
                    <td>${student.userId}</td>
                    <td class="fw-bold">${student.studentName}</td>
                    <td>${student.entryTime ? student.entryTime : '<span class="text-muted">-</span>'}</td>
                    <td>${selectHtml}</td>
                    <td>
                        <span class="badge ${getBadgeClass(displayStatusId)}">${statusName}</span>
                        ${isChanged ? '<small class="text-muted ms-1">未保存</small>' : ''}
                    </td>
                </tr>
            `;
        });
        attendeeListBody.innerHTML = html;
        updateStats(counts);
    }

    /**
     * ステータス名取得ヘルパー
     */
    function getStatusName(id) {
        const found = ALL_STATUS_OPTIONS.find(o => o.statusId === id);
        return found ? found.statusName : '不明';
    }

    /**
     * バッジクラス判定
     */
    function getBadgeClass(id) {
        switch(id) {
            case 1: return 'bg-success';      
            case 2: return 'bg-danger';       
            case 3: return 'bg-warning text-dark'; 
            case 4: return 'bg-info text-dark';
            case 5: return 'bg-light text-dark border';
            case 6: return 'bg-secondary';
            default: return 'bg-secondary';
        }
    }

    /**
     * 統計数値の更新
     */
    function updateStats(counts) {
        document.getElementById('countTotal').textContent = counts.total;
        document.getElementById('countPresent').textContent = counts[1]; 
        document.getElementById('countLate').textContent = counts[3];    
        document.getElementById('countAbsent').textContent = counts[2];  
        const officialCount = (counts[4] || 0) + (counts[5] || 0) + (counts[6] || 0);
        document.getElementById('countOfficial').textContent = officialCount;
    }

    function updateTimestamp() {
        const now = new Date();
        lastUpdatedLabel.textContent = `最終更新: ${now.toLocaleTimeString()}`;
    }

    // 外部から呼び出せるようにグローバルな名前空間に公開
    window.APP = {
        changeStatus: function(userId, newStatusId) {
            localChanges[userId] = parseInt(newStatusId);
            fetchAttendees(); // 再描画
        }
    };

    // --- イベントリスナー登録 ---

    // 破棄ボタン
    document.getElementById('cancelSessionBtn').addEventListener('click', function() {
        if (!confirm('この授業セッションを破棄しますか？\n出席データは保存されず、この操作は取り消せません。')) return;
        
        document.getElementById('loadingOverlay').style.display = 'flex';
        this.disabled = true;

        fetch('/session/cancel', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: sessionId })
        })
        .then(res => {
            if(res.ok) {
                alert('セッションを破棄しました。');
                window.close();
            } else { throw new Error('破棄処理に失敗しました'); }
        })
        .catch(err => {
            alert(err.message);
            document.getElementById('loadingOverlay').style.display = 'none';
            this.disabled = false;
        });
    });

    // 授業終了ボタン
    document.getElementById('endSessionBtn').addEventListener('click', function() {
        if (!confirm('授業を終了しますか？\nここまでの変更内容を保存して確定します。')) return;

        document.getElementById('loadingOverlay').style.display = 'flex';
        this.disabled = true;

        fetch('/session/end', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sessionId: sessionId,
                changes: localChanges,
                endStatus: 2 // 2=正常終了
            })
        })
        .then(res => {
            if(res.ok) {
                alert('出席を確定しました。');
                window.close();
            } else { throw new Error('確定処理に失敗しました'); }
        })
        .catch(err => {
            alert(err.message);
            document.getElementById('loadingOverlay').style.display = 'none';
            this.disabled = false;
        });
    });

    // 初期実行と定期更新
    fetchAttendees();
    setInterval(fetchAttendees, 3000);

})();
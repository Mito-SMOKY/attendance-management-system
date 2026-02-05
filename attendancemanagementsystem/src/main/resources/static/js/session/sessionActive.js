//定数定義
const sessionIdInput = document.getElementById('sessionId');
const sessionId = sessionIdInput ? parseInt(sessionIdInput.value) : 0;
const API_ATTENDEES_URL = '/session/api/attendees/' + sessionId;
const STORAGE_KEY = 'attendance_changes_' + sessionId;
const EDITABLE_STATUS_IDS = [1, 3, 7, 2];


const STATUS_MAP = {
    1: { name: '出席', cls: 'status-btn-1' },
    2: { name: '欠席', cls: 'status-btn-2' },
    3: { name: '遅刻', cls: 'status-btn-3' },
    4: { name: '公欠', cls: 'status-btn-4' }, 
    7: { name: '早退', cls: 'status-btn-7' }
};

//状態管理
let localChanges = {};
try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
        localChanges = JSON.parse(saved);
    }
} catch (e) {
    console.error('Failed to load local changes', e);
    localChanges = {};
}

// ローカル保存関数
function saveLocalChanges() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(localChanges));
}


// 定期更新処理
function fetchAttendees() {
    fetch(API_ATTENDEES_URL)
        .then(res => res.json())
        .then(data => {
            renderTable(data);
            updateStats(data);
            document.getElementById('lastUpdated').innerText = '更新: ' + new Date().toLocaleTimeString();
        })
        .catch(err => console.error('Fetch error:', err));
}

// テーブル描画ロジック
function renderTable(attendees) {
    const tbody = document.getElementById('attendeesTableBody');
    tbody.innerHTML = '';
    
    let hasAutoUpdate = false;

    attendees.forEach(student => {
        const tr = document.createElement('tr');
        
        // 現在のステータスIDを決定（手動変更があればそちらを優先）
        let currentStatusId = localChanges[student.userId] || student.statusId;

        // 自動判定: エントリー時間があり、ステータス未設定なら「出席」にする
        if (!localChanges[student.userId] && !student.statusId && student.entryTime && student.entryTime !== '--:--') {
            currentStatusId = 1;
            localChanges[student.userId] = 1; 
            hasAutoUpdate = true;
        }

        // 名前カラム
        const nameTd = document.createElement('td');
        nameTd.innerHTML = `<strong>${student.studentName}</strong><br><small class="text-muted">${student.gradeClass}</small>`;
        
        // 時間カラム
        const timeTd = document.createElement('td');
        timeTd.innerText = student.entryTime || '--:--';

        // アクションカラム
        const actionTd = document.createElement('td');
        
        // ボタングループコンテナ
        const btnGroup = document.createElement('div');
        btnGroup.className = 'btn-group';

        // ボタン生成（定義順：出席・遅刻・早退・欠席）
        EDITABLE_STATUS_IDS.forEach(sId => {
            const btn = document.createElement('button');
            const info = STATUS_MAP[sId];
            
            btn.type = 'button';
            btn.className = `btn btn-sm ${info.cls}`;
            btn.innerText = info.name;

            // ロックによる無効化処理を削除
            
            // 現在選択中ならアクティブ化
            if (sId === currentStatusId) {
                btn.classList.add('status-active');
            }

            // クリック時の挙動
            btn.onclick = () => {
                localChanges[student.userId] = sId;
                saveLocalChanges(); 
                renderTable(attendees);
                updateStats(attendees); 
            };
            btnGroup.appendChild(btn);
        });
        actionTd.appendChild(btnGroup);

        // ステータスを「文字」で表示するロジック
        const statusTextTd = document.createElement('td');

        if (currentStatusId && STATUS_MAP[currentStatusId]) {
            const info = STATUS_MAP[currentStatusId];
            const statusText = document.createElement('span');
            statusText.className += ' text-primary';
            statusText.innerText = `${info.name}`;
            
            statusTextTd.appendChild(statusText);
        }

        tr.appendChild(nameTd);
        tr.appendChild(timeTd);
        tr.appendChild(actionTd);
        tr.appendChild(statusTextTd);

        tbody.appendChild(tr);

    });

    // 自動判定で変更があった場合、ブラウザに保存する
    if (hasAutoUpdate) {
        saveLocalChanges();
    }
}

// 集計ロジック
function updateStats(attendees) {
    let present = 0, absent = 0, late = 0, early = 0;
    
    // 出席状況をカウント
    attendees.forEach(s => {
        let sid = localChanges[s.userId] || s.statusId;
        if (sid === 1) present++;
        else if (sid === 2) absent++;
        else if (sid === 3) late++;
        else if (sid === 7) early++;
    });

    // 結果を表示
    document.getElementById('countTotal').innerText = attendees.length;
    document.getElementById('countPresent').innerText = present;
    document.getElementById('countAbsent').innerText = absent;
    document.getElementById('countLate').innerText = late;
    document.getElementById('countEarly').innerText = early;
}

// 親ウィンドウ更新＆閉じる
function refreshParentAndClose() {
    if (window.opener && !window.opener.closed) {
        window.opener.location.reload();
    }
    window.close();
}

// 初期化処理
document.addEventListener('DOMContentLoaded', () => {
    const cancelBtn = document.getElementById('cancelSessionBtn');

    // キャンセル処理
    if(cancelBtn) {
        cancelBtn.addEventListener('click', function() {
            if (!confirm('本当に授業を取り消しますか？\n出席データは削除されます。')) return;
            
            // ローディング表示
            document.getElementById('loadingOverlay').style.display = 'flex';
            this.disabled = true;

            // キャンセルAPI呼び出し
            fetch('/session/cancel', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sessionId: sessionId })
            })
            .then(res => {
                if(res.ok) {
                    localStorage.removeItem(STORAGE_KEY);
                    alert('授業をキャンセルしました。');
                    refreshParentAndClose();
                } else {
                    throw new Error('破棄処理に失敗しました');
                }
            })
            .catch(err => {
                alert(err.message);
                document.getElementById('loadingOverlay').style.display = 'none';
                this.disabled = false;
            });
        });
    }

    // 確定終了ボタン
    const endBtn = document.getElementById('endSessionBtn');

    // 終了処理
    if(endBtn) {
        endBtn.addEventListener('click', function() {
            if (!confirm('授業を終了しますか？\nここまでの変更内容を保存して確定します。')) return;

            // ローディング表示
            document.getElementById('loadingOverlay').style.display = 'flex';
            this.disabled = true;

            // 終了API呼び出し
            const payload = {
                sessionId: sessionId,
                changes: localChanges
            };
            fetch('/session/end', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            })
            .then(res => {
                if(res.ok) {
                    localStorage.removeItem(STORAGE_KEY);
                    alert('出席を確定しました。');
                    refreshParentAndClose();
                } else {
                    throw new Error('確定処理に失敗しました');
                }
            })
            .catch(err => {
                alert(err.message);
                document.getElementById('loadingOverlay').style.display = 'none';
                this.disabled = false;
            });
        });
    }

    // 初回読み込み & ポーリング開始
    if (sessionId && sessionId > 0) {
        fetchAttendees();
        setInterval(fetchAttendees, 3000);
    } else {
        console.error("セッションIDが無効なため、通信を停止しました。");
        alert("授業データの読み込みに失敗しました。画面を閉じてやり直してください。");
    }
});

const channel = new BroadcastChannel('attendance_channel');

// メッセージ受信時の処理
channel.onmessage = (event) => {
    if (event.data.type === 'force_close' && event.data.sessionId === sessionId) {
        console.log("別画面からの強制終了信号を受信しました。");
        window.close();
        document.body.innerHTML = "<div class='p-5 text-center'>管理者により強制終了されました。</div>";
    }
};
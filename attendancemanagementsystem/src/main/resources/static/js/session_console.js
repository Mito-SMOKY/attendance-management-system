// CSRFトークンの取得 (Spring Security対策)
const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

let currentSessionId = null; // 開始後にIDを記憶しておく変数

// --- 授業開始 ---
async function startClass() {
    const timeTableId = document.getElementById('timetable-id').value;
    const classroomId = document.getElementById('classroom-id').value;
    const note = document.getElementById('note').value;

    if(!timeTableId || !classroomId) {
        alert("IDを入力してください");
        return;
    }

    try {
        // APIを呼び出す
        const response = await fetch(`/api/session/start?timeTableId=${timeTableId}&classroomId=${classroomId}&note=${note}`, {
            method: 'POST',
            headers: {
                [csrfHeader]: csrfToken
            }
        });

        if (response.ok) {
            const data = await response.json();
            currentSessionId = data.sessionId; // IDを保存
            
            // 画面の見た目を変更
            document.getElementById('status-display').textContent = "授業中";
            document.getElementById('status-display').classList.add('status-active');
            document.getElementById('btn-start').style.display = 'none';
            document.getElementById('btn-end').style.display = 'inline-block';
            
            document.getElementById('message-area').textContent = "授業を開始しました！ログ取得中...";
            document.getElementById('message-area').style.color = "green";
        } else {
            alert("開始に失敗しました");
        }
    } catch (error) {
        console.error('Error:', error);
        alert("通信エラーが発生しました");
    }
}

// --- 授業終了 ---
async function endClass() {
    if (!currentSessionId) {
        alert("開始されていません");
        return;
    }

    if(!confirm("授業を終了し、出席を集計しますか？")) return;

    try {
        const response = await fetch(`/api/session/end?sessionId=${currentSessionId}`, {
            method: 'POST',
            headers: {
                [csrfHeader]: csrfToken
            }
        });

        if (response.ok) {
            const data = await response.json();
            
            // 画面を元に戻す（あるいは終了完了状態にする）
            document.getElementById('status-display').textContent = "終了・集計済";
            document.getElementById('status-display').classList.remove('status-active');
            document.getElementById('status-display').classList.add('status-ended');
            
            document.getElementById('btn-end').style.display = 'none';
            document.getElementById('btn-start').style.display = 'inline-block'; // 次の授業用
            
            document.getElementById('message-area').textContent = data.message;
        } else {
            alert("終了処理に失敗しました");
        }
    } catch (error) {
        console.error('Error:', error);
        alert("通信エラー");
    }
}
/**
 * 受講情報詳細画面用スクリプト
 */

// ステータスとマークの変換マップ
const STATUS_MAP = {
    '出席': '○',
    '欠席': '×',
    '遅刻': '△',
    '早退': '早',
    '公欠': '公',
    '公欠候補': '候',
    '出席停止': '停'
};

// マークからステータス名を取得
function getStatusNameByMark(mark) {
    return Object.keys(STATUS_MAP).find(key => STATUS_MAP[key] === mark) || '出席';
}

// 編集モード開始
function toggleEditMode() {
    // ボタンの切り替え
    document.getElementById('editBtn').style.display = 'none';
    document.getElementById('saveBtn').style.display = 'inline-block';
    document.getElementById('cancelBtn').style.display = 'inline-block';

    const cells = document.querySelectorAll('.editable-cell');
    
    cells.forEach(cell => {
        const currentMark = cell.textContent.trim();
        
        // データがないセル（"-"）は編集不可
        if (currentMark === '-') return; 

        const currentStatus = getStatusNameByMark(currentMark);

        // 安全な要素作成 (DOM操作)
        const select = document.createElement('select');
        select.className = 'status-select';
        select.style.width = '100%';

        Object.keys(STATUS_MAP).forEach(status => {
            const option = document.createElement('option');
            option.value = status;
            option.textContent = `${STATUS_MAP[status]} (${status})`;
            
            if (status === currentStatus) {
                option.selected = true;
            }
            select.appendChild(option);
        });

        cell.textContent = ''; 
        cell.appendChild(select);
    });
}

// 編集キャンセル
function cancelEdit() {
    location.reload(); 
}

// 保存処理
async function saveChanges() {
    // HTML内のhidden inputからIDを取得
    const studentIdInput = document.getElementById('studentIdHidden');
    if (!studentIdInput) {
        console.error("生徒IDが見つかりません");
        return;
    }
    const studentId = studentIdInput.value;

    const cells = document.querySelectorAll('.editable-cell');
    const updates = [];

    cells.forEach(cell => {
        const select = cell.querySelector('select');
        // selectが存在する（＝編集モードの）セルだけ処理
        if (select) {
            const rawDate = cell.dataset.date;
            // 日付フォーマット変換 (yyyy/MM/dd -> yyyy-MM-dd)
            const dateMatch = rawDate.match(/^(\d{4})\/(\d{2})\/(\d{2})/);
            
            if (dateMatch) {
                const formattedDate = `${dateMatch[1]}-${dateMatch[2]}-${dateMatch[3]}`;
                
                updates.push({
                    date: formattedDate,
                    period: parseInt(cell.dataset.period),
                    status: select.value
                });
            }
        }
    });

    if (updates.length === 0) {
        alert("変更可能なデータがありません。");
        cancelEdit();
        return;
    }

    const payload = {
        studentId: parseInt(studentId),
        updates: updates
    };

    try {
        const headers = { 'Content-Type': 'application/json' };
        
        // CSRFトークンが必要になったらここで取得してheadersに追加

        const response = await fetch('/admin/student/student-info/update', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert('保存しました');
            location.reload();
        } else {
            const errorText = await response.text();
            console.error("Server Error:", errorText);
            alert('保存に失敗しました\n' + errorText);
        }
    } catch (e) {
        console.error(e);
        alert('通信エラーが発生しました');
    }
}
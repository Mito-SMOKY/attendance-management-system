/**
 * 生徒情報画面（カレンダー表示）用スクリプト
 */

// --- 月変更機能 ---
function changeMonth() {
    const monthPicker = document.getElementById('monthPicker');
    if (!monthPicker) return;

    const month = monthPicker.value;
    const currentUrl = new URL(window.location.href);
    
    // URLSearchParamsを使用して安全にパラメータを操作
    if (month) {
        currentUrl.searchParams.set('month', month);
    } else {
        currentUrl.searchParams.delete('month');
    }
    window.location.href = currentUrl.toString();
}

// --- 教科検索機能 ---
function searchSubject() {
    const searchInput = document.getElementById('subjectSearchInput');
    if (!searchInput) return;

    // トリムして空文字チェック（XSS対策の一環として、無駄な空白や制御文字を除去）
    const keyword = searchInput.value.trim();
    const currentUrl = new URL(window.location.href);

    if (keyword) {
        currentUrl.searchParams.set('searchSubject', keyword);
    } else {
        currentUrl.searchParams.delete('searchSubject');
    }
    window.location.href = currentUrl.toString();
}

// --- 編集モード切り替え機能 ---
let isEditing = false;

function toggleEditMode() {
    isEditing = !isEditing;
    
    const viewElements = document.querySelectorAll('.view-mode');
    const editElements = document.querySelectorAll('.edit-mode');
    const editBtn = document.getElementById('editBtn');
    const saveBtn = document.getElementById('saveBtn');

    if (isEditing) {
        // 編集モードON: 表示用を隠し、編集用を表示
        viewElements.forEach(el => el.style.display = 'none');
        editElements.forEach(el => el.style.display = 'block');
        
        editBtn.textContent = 'キャンセル';
        editBtn.style.backgroundColor = '#6c757d'; // bootstrap secondary color
        editBtn.style.color = '#fff';
        saveBtn.style.display = 'inline-block'; // blockだとレイアウトが崩れる可能性があるため
    } else {
        // 編集モードOFF
        viewElements.forEach(el => el.style.display = 'block');
        editElements.forEach(el => el.style.display = 'none');
        
        editBtn.textContent = '編集';
        editBtn.style.backgroundColor = '';
        editBtn.style.color = '';
        saveBtn.style.display = 'none';
    }
}

// --- 保存処理 (非同期通信) ---
async function saveChanges() {
    // ユーザー確認
    if (!confirm('変更を保存しますか？')) return;

    // 1. 生徒IDの取得と検証
    const studentIdInput = document.getElementById('studentIdHidden');
    if (!studentIdInput || !studentIdInput.value) {
        alert("生徒IDが不正です。画面をリロードしてください。");
        return;
    }
    const studentId = parseInt(studentIdInput.value, 10);
    if (isNaN(studentId)) {
        alert("生徒IDが数値ではありません。");
        return;
    }

    // 2. データの収集
    const selects = document.querySelectorAll('.status-select');
    const updates = [];

    selects.forEach(select => {
        // data属性から日付を取得 (例: "2026/01/15(木)")
        const rawDate = select.dataset.date;
        if (!rawDate) return;

        // 正規表現で厳密に yyyy/MM/dd を抽出
        // ^ と $ は使いませんが、桁数を厳密に指定
        const match = rawDate.match(/(\d{4})\s*\/\s*(\d{2})\s*\/\s*(\d{2})/);
        
        if (match) {
            const formattedDate = `${match[1]}-${match[2]}-${match[3]}`;
            
            // ステータスの値も一応チェック（空文字などは送らない）
            if (select.value) {
                updates.push({
                    date: formattedDate,
                    period: parseInt(select.dataset.period, 10),
                    status: select.value
                });
            }
        }
    });

    if (updates.length === 0) {
        alert("更新対象のデータが見つかりません（または日付形式が不正です）");
        return;
    }

    const payload = {
        studentId: studentId,
        updates: updates
    };

    try {
        // 3. CSRFトークン取得 (セキュリティ対策)
        // Spring Securityが有効な場合、metaタグからトークンを取得する
        // 現状はコメントアウトしていますが、本番では必須です。
        /*
        const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
        const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
        const csrfToken = csrfTokenMeta ? csrfTokenMeta.content : '';
        const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.content : 'X-CSRF-TOKEN';
        */

        const headers = {
            'Content-Type': 'application/json'
        };

        // CSRFトークンがあればヘッダーに追加
        // if (csrfToken) {
        //     headers[csrfHeader] = csrfToken;
        // }

        const response = await fetch('/admin/student/student-info/update', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert('保存しました');
            location.reload();
        } else {
            // サーバー側からのエラーメッセージを取得
            const errorText = await response.text();
            console.error("Server Error:", errorText);
            alert('保存に失敗しました:\n' + errorText);
        }

    } catch (error) {
        console.error('Network Error:', error);
        alert('通信エラーが発生しました。ネットワーク接続を確認してください。');
    }
}
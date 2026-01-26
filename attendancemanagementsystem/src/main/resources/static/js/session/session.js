// 定数定義
const API_BASE = '/session';

// 共通関数: プルダウンの初期化
function resetSelect(el, defaultText) {
    el.innerHTML = '';
    const def = document.createElement('option');
    def.text = defaultText;
    def.value = "";
    def.selected = true;
    def.disabled = true;
    el.add(def);
    el.disabled = true;
}

// アクティブセッションIDを取得
async function fetchActiveSessionId(userId) {
    try {
        const res = await fetch(`${API_BASE}/api/active/check?userId=${userId}`);
        if (res.ok) {
            const data = await res.json();
            return data.sessionId; 
        }
    } catch (e) { 
        console.error(e); 
        alert("アクティブセッション情報の取得に失敗しました"); 
    }
    return null;
}

// --- 連動プルダウン読み込み関数群 ---

// 学科 -> コース読み込み
async function loadCourses(majorId) {
    const courseSelect = document.getElementById('courseId');
    resetSelect(courseSelect, "読み込み中...");
    
    // 下位のプルダウンもリセット
    resetSelect(document.getElementById('grade'), "コースを選択してください");
    resetSelect(document.getElementById('departmentId'), "学年を選択してください");
    
    if (!majorId) { 
        resetSelect(courseSelect, "学科を選択してください"); 
        return; 
    }

    try {
        const res = await fetch(`${API_BASE}/api/options/courses?majorId=${majorId}`);
        const data = await res.json();
        
        resetSelect(courseSelect, "選択してください");
        data.forEach(item => {
            const op = document.createElement('option');
            op.value = item.courseId;
            op.text = item.courseName;
            courseSelect.add(op);
        });
        courseSelect.disabled = false;
    } catch (e) { 
        console.error(e); 
        alert("コース情報の取得に失敗しました"); 
    }
}

// コース -> 学年読み込み
async function loadGrades(courseId) {
    const gradeSelect = document.getElementById('grade');
    resetSelect(gradeSelect, "読み込み中...");
    
    // 下位のプルダウンもリセット
    resetSelect(document.getElementById('departmentId'), "学年を選択してください");
    
    if (!courseId) { 
        resetSelect(gradeSelect, "コースを選択してください"); 
        return; 
    }

    try {
        const res = await fetch(`${API_BASE}/api/options/grades?courseId=${courseId}`);
        const data = await res.json();
        
        resetSelect(gradeSelect, "選択してください");
        data.forEach(g => {
            const op = document.createElement('option');
            op.value = g;
            op.text = g + "年";
            gradeSelect.add(op);
        });
        gradeSelect.disabled = false;
    } catch (e) { 
        console.error(e); 
        alert("学年情報の取得に失敗しました"); 
    }
}

// 学年 -> クラス(Department)読み込み
async function loadClasses(grade) {
    const courseId = document.getElementById('courseId').value;
    const classSelect = document.getElementById('departmentId');
    
    resetSelect(classSelect, "読み込み中...");
    
    if (!courseId || !grade) { 
        resetSelect(classSelect, "学年を選択してください"); 
        return; 
    }

    try {
        const res = await fetch(`${API_BASE}/api/options/classes?courseId=${courseId}&grade=${grade}`);
        const data = await res.json();
        
        resetSelect(classSelect, "選択してください");
        data.forEach(item => {
            const op = document.createElement('option');
            op.value = item.departmentId;
            op.text = item.className;
            classSelect.add(op);
        });
        classSelect.disabled = false;
    } catch (e) { 
        console.error(e); 
        alert("クラス情報の取得に失敗しました"); 
    }
}

// --- イベントリスナー設定 ---
document.addEventListener('DOMContentLoaded', () => {
    // 学科変更時
    const majorEl = document.getElementById('majorId');
    if(majorEl) majorEl.addEventListener('change', function() { loadCourses(this.value); });

    // コース変更時
    const courseEl = document.getElementById('courseId');
    if(courseEl) courseEl.addEventListener('change', function() { loadGrades(this.value); });

    // 学年変更時
    const gradeEl = document.getElementById('grade');
    if(gradeEl) gradeEl.addEventListener('change', function() { loadClasses(this.value); });
});


// --- 時間割自動入力処理 ---
window.fillTimetable = async function() {
    const userId = document.getElementById('userId').value;
    const date = document.getElementById('date').value;
    const slotId = document.getElementById('slotId').value;
    
    if (!date || !slotId) { 
        alert("日付と時限を選択してください"); 
        return; 
    }

    try {
        // APIから時間割データを取得
        const res = await fetch(`${API_BASE}/api/timetable/get?userId=${userId}&date=${date}&slotId=${slotId}`);
        
        if (!res.ok) {
            // 200以外の場合はデータなしとみなす
            throw new Error("この日時の時間割は見つかりませんでした");
        }
        
        const data = await res.json();
        
        // 1. 科目と教室をセット
        if (data.subjectId) document.getElementById('subjectId').value = data.subjectId;
        if (data.classroomId) document.getElementById('classroomId').value = data.classroomId;

        // 2. 学科・コース・学年・クラスを順番にセット（awaitで完了を待つのが重要）
        if (data.majorId) {
            document.getElementById('majorId').value = data.majorId;
            
            // コース一覧を読み込み完了まで待機
            await loadCourses(data.majorId);
            
            if (data.courseId) {
                document.getElementById('courseId').value = data.courseId;
                
                // 学年一覧を読み込み完了まで待機
                await loadGrades(data.courseId);
                
                if (data.targetGrade) {
                    document.getElementById('grade').value = data.targetGrade;
                    
                    // クラス一覧を読み込み完了まで待機
                    await loadClasses(data.targetGrade);
                    
                    if (data.departmentId) {
                        document.getElementById('departmentId').value = data.departmentId;
                    }
                }
            }
        }
    } catch (e) { 
        console.warn(e);
        alert(e.message); 
    }
};


// --- 授業開始処理 ---
window.startSession = async function() {
    const data = {
        date: document.getElementById('date').value,
        slotId: document.getElementById('slotId').value,
        majorId: document.getElementById('majorId').value,
        courseId: document.getElementById('courseId').value,
        targetGrade: document.getElementById('grade').value,
        departmentId: document.getElementById('departmentId').value,
        subjectId: document.getElementById('subjectId').value,
        classroomId: document.getElementById('classroomId').value
    };

    // 必須チェック
    if (!data.date || !data.slotId || !data.departmentId || !data.subjectId || !data.classroomId) {
        alert("すべての項目を選択してください。");
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        // 正常時: 授業画面を開く
        if (response.ok) {
            const json = await response.json();
            const uniqueWindowName = 'SessionWindow_' + json.sessionId;
            window.open(
                `${API_BASE}/active/${json.sessionId}`, 
                uniqueWindowName, 
                'width=800,height=600'
            );
            return;
        }

        // エラーハンドリング
        let errorMsg = "エラーが発生しました";
        try {
            const errData = await response.json();
            if (errData.message) errorMsg = errData.message;
        } catch (e) {
            errorMsg = await response.text(); 
        }

        // 409 Conflict: 実施中の授業がある場合 -> モーダル表示
        if (response.status === 409) {
            const msgEl = document.getElementById('conflictMessage');
            if(msgEl) msgEl.innerText = errorMsg;

            const userId = document.getElementById('userId').value;
            let conflictId = await fetchActiveSessionId(userId);

            // API取得失敗時のバックアップ
            if (!conflictId) {
                const oldHidden = document.getElementById('activeSessionId');
                if(oldHidden) conflictId = oldHidden.value;
            }
            
            document.getElementById('conflictSessionId').value = conflictId || ''; 
            
            const modalEl = document.getElementById('conflictModal');
            const modal = new bootstrap.Modal(modalEl);
            modal.show();
            
        } else {
            // その他のエラー
            alert(errorMsg);
        }

    } catch (err) {
        alert("通信エラーが発生しました: " + err.message);
    }
};

// 未終了の授業を開く
window.openConflictSession = function() {
    const sessionId = document.getElementById('conflictSessionId').value;
    if (!sessionId || sessionId === "null" || sessionId === "") {
        alert("セッションIDが特定できませんでした。\n「強制終了」を選んでください。");
        return;
    }
    
    // モーダルを閉じる
    const modalEl = document.getElementById('conflictModal');
    const modal = bootstrap.Modal.getInstance(modalEl);
    modal.hide();

    // ウィンドウを開く
    const uniqueWindowName = 'SessionWindow_' + sessionId;
    window.open(
        `${API_BASE}/active/${sessionId}`, 
        uniqueWindowName, 
        'width=800,height=600'
    );
};

// 強制終了して新規作成
window.forceEndSession = async function() {
    if (!confirm("本当に強制終了しますか？\n（出席データは確定されず、授業は終了扱いになります）")) {
        return;
    }
    
    try {
        let sessionId = document.getElementById('conflictSessionId').value;
        const userId = document.getElementById('userId').value;

        if (!sessionId) {
            sessionId = await fetchActiveSessionId(userId);
        }

        const payload = { 
            sessionId: sessionId ? parseInt(sessionId) : null,
            userId: parseInt(userId)
        };
        
        const forceEndResponse = await fetch(`${API_BASE}/api/force-end`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        if (forceEndResponse.ok) {
            if (sessionId) {
                const channel = new BroadcastChannel('attendance_channel');
                channel.postMessage({ 
                    type: 'force_close', 
                    sessionId: parseInt(sessionId) 
                });
                setTimeout(() => channel.close(), 1000);
            }
            alert("強制終了しました。自動的に再試行します。");
            
            const modalEl = document.getElementById('conflictModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            modal.hide();
            document.getElementById('conflictSessionId').value = '';
            
            // 再試行
            startSession(); 
        } else {
            const text = await forceEndResponse.text();
            alert("強制終了できませんでした: " + text);
        }
    } catch (err) {
        alert("強制終了エラー: " + err.message);
    }
};
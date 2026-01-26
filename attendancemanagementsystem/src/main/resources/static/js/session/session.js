// 定数定義
const API_BASE = '/session';

function showModal(modalId) {
    const el = document.getElementById(modalId);
    if(el) el.classList.add('show');
}

function hideModal(modalId) {
    const el = document.getElementById(modalId);
    if(el) el.classList.remove('show');
}

// 共通関数
function resetSelect(el, defaultText) {
    // プルダウンを初期化
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
    // API呼び出し
    try {
        const res = await fetch(`${API_BASE}/api/active/check?userId=${userId}`);
        if (res.ok) {
            const data = await res.json();
            return data.sessionId; 
        }
    } catch (e) { console.error(e); alert("アクティブセッション情報の取得に失敗しました"); }
    return null;
}

//連動プルダウン読み込み関数
async function loadCourses(majorId) {
    const courseSelect = document.getElementById('courseId');
    resetSelect(courseSelect, "読み込み中...");
    resetSelect(document.getElementById('grade'), "コースを選択してください");
    resetSelect(document.getElementById('departmentId'), "学年を選択してください");
    if (!majorId) { resetSelect(courseSelect, "学科を選択してください"); return; }

    // API呼び出し
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
    } catch (e) { console.error(e); alert("コース情報の取得に失敗しました"); }
}

// 学年読み込み
async function loadGrades(courseId) {
    const gradeSelect = document.getElementById('grade');
    resetSelect(gradeSelect, "読み込み中...");
    resetSelect(document.getElementById('departmentId'), "学年を選択してください");
    if (!courseId) { resetSelect(gradeSelect, "コースを選択してください"); return; }

    // API呼び出し
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
    } catch (e) { console.error(e); alert("学年情報の取得に失敗しました"); }
}

// クラス読み込み
async function loadClasses(grade) {
    const courseId = document.getElementById('courseId').value;
    const classSelect = document.getElementById('departmentId');
    resetSelect(classSelect, "読み込み中...");
    if (!courseId || !grade) { resetSelect(classSelect, "学年を選択してください"); return; }

    // API呼び出し
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
    } catch (e) { console.error(e); alert("クラス情報の取得に失敗しました"); }
}


// プルダウン連動設定
document.addEventListener('DOMContentLoaded', () => {
    const majorEl = document.getElementById('majorId');
    if(majorEl) majorEl.addEventListener('change', function() { loadCourses(this.value); });

    const courseEl = document.getElementById('courseId');
    if(courseEl) courseEl.addEventListener('change', function() { loadGrades(this.value); });

    const gradeEl = document.getElementById('grade');
    if(gradeEl) gradeEl.addEventListener('change', function() { loadClasses(this.value); });
});

//時間割自動入力
window.fillTimetable = async function() {
    const userId = document.getElementById('userId').value;
    const date = document.getElementById('date').value;
    const slotId = document.getElementById('slotId').value;
    if (!slotId) { alert("時限を選択してください"); return; }

    // API呼び出し
    try {
        const res = await fetch(`${API_BASE}/api/timetable/get?userId=${userId}&date=${date}&slotId=${slotId}`);
        if (!res.ok) throw new Error("この日時の時間割は見つかりませんでした");
        
        // データ設定
        const data = await res.json();
        
        // 各フィールドに値を設定
        if (data.subjectId) document.getElementById('subjectId').value = data.subjectId;
        if (data.classroomId) document.getElementById('classroomId').value = data.classroomId;
        if (data.majorId) {
            document.getElementById('majorId').value = data.majorId;
            await loadCourses(data.majorId);
            if (data.courseId) {
                document.getElementById('courseId').value = data.courseId;
                await loadGrades(data.courseId);
                if (data.targetGrade) {
                    document.getElementById('grade').value = data.targetGrade;
                    await loadClasses(data.targetGrade);
                    if (data.departmentId) document.getElementById('departmentId').value = data.departmentId;
                }
            }
        }
    } catch (e) { alert(e.message); }
};


// 授業開始
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

    if (!data.date || !data.slotId || !data.departmentId || !data.subjectId || !data.classroomId) {
        alert("すべての項目を選択してください。");
        return;
    }

    // API呼び出し
    try {
        const response = await fetch(`${API_BASE}/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        // 正常時: 新規ウィンドウで授業画面を開く
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

        // --- エラーハンドリングの変更部分 ---
        
        // 1. エラーメッセージをJSONからきれいに取り出す
        let errorMsg = "エラーが発生しました";
        try {
            // クローンして読み取らないと、後で再読み込みできない場合があるが、ここでは1回きりでOK
            const errData = await response.json();
            if (errData.message) {
                errorMsg = errData.message; // メッセージ本文のみ抽出
            }
        } catch (e) {
            // JSONパース失敗時はテキストとして取得
            errorMsg = await response.text(); 
        }

        // 2. ステータスコードによる分岐
        if (response.status === 409) {
            // 409 Conflict: 実施中の授業がある場合のみモーダルを表示
            const msgEl = document.getElementById('conflictMessage');
            if(msgEl) msgEl.innerText = errorMsg; // きれいなメッセージを表示

            const userId = document.getElementById('userId').value;
            let conflictId = await fetchActiveSessionId(userId);

            // 古いhiddenフィールドからも取得を試みる
            if (!conflictId) {
                const oldHidden = document.getElementById('activeSessionId');
                if(oldHidden) conflictId = oldHidden.value;
            }
            
            // モーダルにセッションIDをセット
            const conflictInput = document.getElementById('conflictSessionId');
            if(conflictInput) conflictInput.value = conflictId || ''; 

            // モーダル表示
            const modalEl = document.getElementById('conflictModal');
            showModal('conflictModal');
            
        } else {
            // 400 Bad Request (重複) や 500 Error の場合はアラートのみ
            // モーダルは表示しない
            alert(errorMsg);
        }

    } catch (err) {
        // fetch自体の失敗など
        alert("通信エラーが発生しました: " + err.message);
    }
};

// 未終了の授業を開く
window.openConflictSession = function() {
    const sessionId = document.getElementById('conflictSessionId').value;
    if (!sessionId || sessionId === "null") {
        alert("セッションIDが特定できませんでした。\n「強制終了」を選んでください。");
        return;
    }
    const modalEl = document.getElementById('conflictModal');
    hideModal('conflictModal');

    // 一意のウィンドウ名で開く
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

        // セッションIDがなければ再取得
        if (!sessionId) {
            sessionId = await fetchActiveSessionId(userId);
        }

        // API呼び出し
        const payload = { 
            sessionId: sessionId ? parseInt(sessionId) : null,
            userId: parseInt(userId)
        };
        
        // 強制終了API呼び出し
        const forceEndResponse = await fetch(`${API_BASE}/api/force-end`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        // 正常時: ブロードキャストで通知し、再試行
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
            
            // モーダルを閉じる
            const modalEl = document.getElementById('conflictModal');
            hideModal('conflictModal');
            document.getElementById('conflictSessionId').value = '';
            
            // 再試行
            startSession(); 
        } else {
            const text = await forceEndResponse.text();
            if(forceEndResponse.status === 400) {
                alert("強制終了エラー(400): IDが特定できませんでした。");
            } else {
                throw new Error(text);
            }
        }
    } catch (err) {
        alert("強制終了エラー: " + err.message);
    }
};
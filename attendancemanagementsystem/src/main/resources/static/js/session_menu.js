/*<![CDATA[*/

/**
 * session_menu.js
 * 授業開始設定画面の制御ロジック
 */

// --- グローバル変数・設定 ---
// APP_CONFIGが存在しない場合（単体テスト等）のためのフォールバック
const config = window.APP_CONFIG || {};
const API_BASE = config.apiBase || '/session/api';
const SESSION_START_URL = config.startUrl || '/session/start';
const SESSION_ACTIVE_URL_BASE = config.activeUrlBase || '/session/active/';
const allDepartments = config.departmentList || [];
const activeSessionId = config.activeSessionId || null;

// 自動提案データ
const suggested = config.suggested || {};

// 学科・学年紐付け管理用
let currentMajorGrades = [];

// --- 初期化処理 ---
document.addEventListener('DOMContentLoaded', () => {
    initDepartmentSelectors();
    
    const form = document.getElementById('startSessionForm');
    if (form) {
        form.addEventListener('submit', handleFormSubmit);
    }

    // 自動提案ロジックの実行
    if (suggested.deptId) {
        applySuggestedData();
    }
});

/**
 * 学科プルダウン生成
 */
function initDepartmentSelectors() {
    const majorSelect = document.getElementById('majorSelect');
    if (!majorSelect) return;

    const uniqueMajors = [...new Set(allDepartments.map(d => d.major.majorName))];
    uniqueMajors.forEach(majorName => {
        const opt = document.createElement('option');
        opt.value = majorName;
        opt.textContent = majorName;
        majorSelect.appendChild(opt);
    });
}

/**
 * 学科変更時：学年リストをAPIから取得（非同期）
 */
async function onMajorChange() {
    const majorSelect = document.getElementById('majorSelect');
    const gradeSelect = document.getElementById('targetGrade');
    const classSelect = document.getElementById('classSelect');
    const subjectSelect = document.getElementById('subjectId');
    const selectedMajor = majorSelect.value;

    // リセット処理
    gradeSelect.innerHTML = '<option value="">読込中...</option>';
    gradeSelect.disabled = true;
    classSelect.innerHTML = '<option value="">学年を選んでください</option>';
    classSelect.disabled = true;
    subjectSelect.innerHTML = '<option value="">クラスを選択してください</option>';
    document.getElementById('targetDepartment').value = "";

    if (!selectedMajor) {
        gradeSelect.innerHTML = '<option value="">学科を選んでください</option>';
        return;
    }

    const filteredDepts = allDepartments.filter(d => d.major.majorName === selectedMajor);
    currentMajorGrades = [];

    try {
        // 各クラスの学年情報を取得
        const promises = filteredDepts.map(async (dept) => {
            const res = await fetch(`${API_BASE}/grades/${dept.departmentId}`);
            const grades = await res.json();
            return { deptId: dept.departmentId, className: dept.className, grade: grades[0] };
        });

        currentMajorGrades = await Promise.all(promises);

        // 重複を除いた学年リストを作成
        const uniqueGrades = [...new Set(currentMajorGrades.map(item => item.grade))].sort();
        
        gradeSelect.innerHTML = '<option value="">選択してください</option>';
        if (uniqueGrades.length === 0) {
            gradeSelect.innerHTML = '<option value="">学年なし</option>';
        } else {
            uniqueGrades.forEach(g => {
                const opt = document.createElement('option');
                opt.value = g;
                opt.textContent = g + '年';
                gradeSelect.appendChild(opt);
            });
            gradeSelect.disabled = false;
        }
    } catch (err) {
        console.error('Grades fetch error:', err);
        gradeSelect.innerHTML = '<option value="">取得失敗</option>';
    }
}

/**
 * 学年変更時：選択された学年に属するクラスを表示
 */


// ↓↓↓ここをコースにしてね　oncourseChangeとかにしてね
function onGradeChange() {
    const selectedGrade = document.getElementById('targetGrade').value;
    const classSelect = document.getElementById('classSelect');

    classSelect.innerHTML = '<option value="">選択してください</option>';
    
    if (!selectedGrade) {
        classSelect.disabled = true;
        return;
    }

    const matchedClasses = currentMajorGrades.filter(item => String(item.grade) === String(selectedGrade));

    matchedClasses.forEach(item => {
        const opt = document.createElement('option');
        opt.value = item.deptId;
        opt.textContent = item.className;
        classSelect.appendChild(opt);
    });
    classSelect.disabled = false;
    document.getElementById('subjectId').innerHTML = '<option value="">クラスを選択してください</option>';
}
/////////////↑↑↑↑//////////////



/**
 * クラス変更時：科目をAPIから取得
 */
async function onClassChange() {
    const classSelect = document.getElementById('classSelect');
    const deptId = classSelect.value;
    const subjectSelect = document.getElementById('subjectId');
    
    document.getElementById('targetDepartment').value = deptId;

    if (!deptId) {
        subjectSelect.innerHTML = '<option value="">クラスを選択してください</option>';
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/subjects/${deptId}`);
        const subjects = await res.json();
        
        if (subjects.length > 0) {
            subjectSelect.innerHTML = subjects.map(s => 
                `<option value="${s.subjectId}">${s.subjectName}</option>`
            ).join('');
        } else {
            subjectSelect.innerHTML = '<option value="">科目なし</option>';
        }
    } catch (err) {
        console.error('Subjects fetch error:', err);
        subjectSelect.innerHTML = '<option value="">科目取得失敗</option>';
    }
}

/**
 * 授業開始処理
 */
function handleFormSubmit(e) {
    e.preventDefault();
    
    const subjectSelect = document.getElementById('subjectId');
    if (subjectSelect.selectedIndex < 0 || !subjectSelect.value) {
        alert("科目を選択してください");
        return;
    }

    const data = {
        date: document.getElementById('date').value,
        time: document.getElementById('time').value,
        classroomId: document.getElementById('classroomId').value,
        targetDepartmentId: document.getElementById('targetDepartment').value,
        targetGrade: document.getElementById('targetGrade').value,
        subjectId: subjectSelect.value,
        subjectName: subjectSelect.options[subjectSelect.selectedIndex].text
    };

    if (!data.targetDepartmentId || !data.targetGrade) {
        alert("学科・学年・クラスをすべて選択してください");
        return;
    }

    fetch(SESSION_START_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    .then(response => {
        if (!response.ok) throw new Error('作成失敗');
        return response.json();
    })
    .then(data => {
        const url = SESSION_ACTIVE_URL_BASE + data.sessionId;
        const width = 1000, height = 800;
        const left = (window.screen.width - width) / 2;
        const top = (window.screen.height - height) / 2;
        window.open(url, 'SessionWindow_' + data.sessionId, `width=${width},height=${height},top=${top},left=${left},scrollbars=yes,resizable=yes`);
    })
    .catch(error => {
        console.error(error);
        alert('開始失敗: ' + error.message);
    });
}

/**
 * 強制終了処理 (endStatus: 9)
 */
function forceEndSession() {
    if (!activeSessionId) return;

    if (!confirm("前回の授業を終了し、出席を確定しますか？\n(ログに基づいて自動集計されます)")) return;

    fetch('/session/end', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
            sessionId: activeSessionId,
            changes: {}, 
            endStatus: 9
        })
    })
    .then(response => {
        if (!response.ok) throw new Error('終了処理失敗');
        return response.json();
    })
    .then(() => {
        alert("前回の授業を終了しました。");
        window.location.reload();
    })
    .catch(error => {
        console.error('Error:', error);
        alert("終了処理に失敗しました。");
    });
}

/**
 * 自動提案データの適用
 */
async function applySuggestedData() {
    const targetDept = allDepartments.find(d => d.departmentId === suggested.deptId);
    if (!targetDept) return;

    // 1. 学科を選択
    const majorSelect = document.getElementById('majorSelect');
    majorSelect.value = targetDept.major.majorName;
    
    // 2. 学科変更イベント（学年リスト取得）を待機
    await onMajorChange();
    
    // 3. 学年を選択
    if (suggested.grade) {
        document.getElementById('targetGrade').value = suggested.grade;
        onGradeChange();
    }
    
    // 4. クラスを選択
    document.getElementById('classSelect').value = targetDept.departmentId;
    
    // 5. クラス変更イベント（科目リスト取得）を待機
    await onClassChange();
    
    // 6. 科目を選択
    if (suggested.subjectId) {
        document.getElementById('subjectId').value = suggested.subjectId;
    }
}

/*]]>*/
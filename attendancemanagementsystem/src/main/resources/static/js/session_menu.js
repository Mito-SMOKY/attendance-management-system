let currentMajorGrades = [];

document.addEventListener('DOMContentLoaded', () => {
    initMajorSelect();
    const form = document.getElementById('startSessionForm');
    if (form) {
        form.addEventListener('submit', handleFormSubmit);
    }
});

function initMajorSelect() {
    const majorSelect = document.getElementById('majorSelect');
    if (!majorSelect) return;

    const departments = window.APP_CONFIG.departmentList;
    const uniqueMajors = [...new Set(departments.map(d => d.major.majorName))];

    uniqueMajors.forEach(majorName => {
        const opt = document.createElement('option');
        opt.value = majorName;
        opt.textContent = majorName;
        majorSelect.appendChild(opt);
    });
}

/**
 * 学科選択時：その学科に属する全クラスの学年をAPIで取得し保持する
 */
async function onMajorChange() {
    const selectedMajorName = document.getElementById('majorSelect').value;
    const gradeSelect = document.getElementById('targetGrade');
    const classSelect = document.getElementById('classSelect');

    gradeSelect.innerHTML = '<option value="">読み込み中...</option>';
    classSelect.innerHTML = '<option value="">学年を選択してください</option>';
    gradeSelect.disabled = true;
    classSelect.disabled = true;

    if (!selectedMajorName) return;

    const deptsInMajor = window.APP_CONFIG.departmentList.filter(d => d.major.majorName === selectedMajorName);
    const apiBase = window.APP_CONFIG.apiBase;
    currentMajorGrades = [];

    try {
        const promises = deptsInMajor.map(async (dept) => {
            const res = await fetch(`${apiBase}/grades/${dept.departmentId}`);
            const grades = await res.json();
            return { deptId: dept.departmentId, className: dept.className, grade: grades[0] };
        });

        currentMajorGrades = await Promise.all(promises);

        const uniqueGrades = [...new Set(currentMajorGrades.map(item => item.grade))].sort();
        gradeSelect.innerHTML = '<option value="">選択してください</option>';
        uniqueGrades.forEach(g => {
            const opt = document.createElement('option');
            opt.value = g;
            opt.textContent = g + '年';
            gradeSelect.appendChild(opt);
        });
        gradeSelect.disabled = false;
    } catch (e) {
        console.error(e);
        gradeSelect.innerHTML = '<option value="">取得失敗</option>';
    }
}

/**
 * 学年選択時：保持しているデータから該当する学年のクラスのみを表示する
 */
function onGradeChange() {
    const selectedGrade = document.getElementById('targetGrade').value;
    const classSelect = document.getElementById('classSelect');

    classSelect.innerHTML = '<option value="">選択してください</option>';
    classSelect.disabled = true;

    if (!selectedGrade) return;

    const matchedClasses = currentMajorGrades.filter(item => String(item.grade) === String(selectedGrade));

    matchedClasses.forEach(item => {
        const opt = document.createElement('option');
        opt.value = item.deptId;
        opt.textContent = item.className;
        classSelect.appendChild(opt);
    });
    classSelect.disabled = false;
}

function onClassChange() {
    const classSelect = document.getElementById('classSelect');
    const deptId = classSelect.value;
    document.getElementById('targetDepartment').value = deptId;

    if (deptId) {
        updateSubjectOnly(deptId);
    }
}

function updateSubjectOnly(deptId) {
    const apiBase = window.APP_CONFIG.apiBase;
    fetch(`${apiBase}/subjects/${deptId}`)
        .then(res => res.json())
        .then(subjects => {
            const select = document.getElementById('subjectId');
            select.innerHTML = subjects.map(s => 
                `<option value="${s.subjectId}">${s.subjectName}</option>`
            ).join('') || '<option value="">科目なし</option>';
        });
}

function handleFormSubmit(e) {
    e.preventDefault();
    const subjectSelect = document.getElementById('subjectId');
    const data = {
        date: document.getElementById('date').value,
        time: document.getElementById('time').value,
        classroomId: document.getElementById('classroomId').value,
        targetDepartmentId: document.getElementById('targetDepartment').value,
        targetGrade: document.getElementById('targetGrade').value,
        subjectId: subjectSelect.value,
        subjectName: subjectSelect.options[subjectSelect.selectedIndex]?.text
    };

    if (!data.targetDepartmentId || !data.subjectId) {
        alert("未入力の項目があります");
        return;
    }

    fetch(window.APP_CONFIG.startUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    .then(res => res.json())
    .then(resData => {
        const url = window.APP_CONFIG.activeUrlBase + resData.sessionId;
        window.open(url, '_blank', 'width=1000,height=800');
    })
    .catch(err => alert("開始に失敗しました"));
}

function forceEndSession() {
    const sessionId = window.APP_CONFIG.activeSessionId;
    if (!sessionId || !confirm("終了しますか？")) return;

    fetch('/session/end', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId: sessionId, changes: {} })
    })
    .then(() => window.location.reload());
}
document.addEventListener('DOMContentLoaded', function() {
    
    const sessionId = document.getElementById('sessionId').value;
    
    // CSRFトークン準備
    // const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    // const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    const headers = { 'Content-Type': 'application/json' };
    // if (csrfTokenMeta && csrfHeaderMeta) {
    //     headers[csrfHeaderMeta.content] = csrfTokenMeta.content;
    // }

    // ==========================================
    // 1. 授業情報の編集ロジック
    // ==========================================
    const editSessionBtn = document.getElementById('editSessionBtn');
    const saveSessionBtn = document.getElementById('saveSessionBtn');
    const cancelSessionBtn = document.getElementById('cancelSessionBtn');
    const sessionInfoArea = document.getElementById('sessionInfoArea');

    if (editSessionBtn && sessionInfoArea) {
        function toggleSessionEdit(isEdit) {
            const views = sessionInfoArea.querySelectorAll('.session-view');
            const edits = sessionInfoArea.querySelectorAll('.session-edit');

            if (isEdit) {
                views.forEach(el => el.style.display = 'none');
                edits.forEach(el => el.style.display = 'block');
                editSessionBtn.style.display = 'none';
                saveSessionBtn.style.display = 'inline-block';
                cancelSessionBtn.style.display = 'inline-block';
            } else {
                views.forEach(el => el.style.display = '');
                edits.forEach(el => el.style.display = 'none');
                editSessionBtn.style.display = 'inline-block';
                saveSessionBtn.style.display = 'none';
                cancelSessionBtn.style.display = 'none';
            }
        }

        editSessionBtn.addEventListener('click', () => toggleSessionEdit(true));
        cancelSessionBtn.addEventListener('click', () => location.reload());

        saveSessionBtn.addEventListener('click', () => {
            if (!confirm('授業情報を更新しますか？')) return;

            const updateData = {
                subjectId: document.getElementById('subjectId').value,
                teacherId: document.getElementById('teacherId').value,
                classroomId: document.getElementById('classroomId').value,
                timeSlotId: document.getElementById('timeSlotId').value,
                sessionDate: document.getElementById('sessionDate').value
            };

            fetch(`/admin/class/session/update/${sessionId}`, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(updateData)
            })
            .then(res => {
                if (res.ok) {
                    alert('授業情報を更新しました');
                    location.reload();
                } else {
                    alert('更新に失敗しました');
                }
            })
            .catch(err => alert('通信エラーが発生しました'));
        });
    }


    // ==========================================
    // 2. 生徒出欠の編集ロジック
    // ==========================================
    const editStudentBtn = document.getElementById('editStudentBtn');
    const saveStudentBtn = document.getElementById('saveStudentBtn');
    const cancelStudentBtn = document.getElementById('cancelStudentBtn');
    const studentTable = document.getElementById('studentTable');

    if (editStudentBtn && studentTable) {
        function toggleStudentEdit(isEdit) {
            const views = studentTable.querySelectorAll('.status-view');
            const selects = studentTable.querySelectorAll('.status-select');

            if (isEdit) {
                views.forEach(el => el.style.display = 'none');
                selects.forEach(el => el.style.display = 'block');
                editStudentBtn.style.display = 'none';
                saveStudentBtn.style.display = 'inline-block';
                cancelStudentBtn.style.display = 'inline-block';
            } else {
                views.forEach(el => el.style.display = '');
                selects.forEach(el => el.style.display = 'none');
                editStudentBtn.style.display = 'inline-block';
                saveStudentBtn.style.display = 'none';
                cancelStudentBtn.style.display = 'none';
            }
        }

        editStudentBtn.addEventListener('click', () => toggleStudentEdit(true));
        cancelStudentBtn.addEventListener('click', () => location.reload());

        saveStudentBtn.addEventListener('click', () => {
            if (!confirm('出欠状況を保存しますか？')) return;

            const rows = studentTable.querySelectorAll('tbody tr');
            const updateData = {}; 

            rows.forEach(row => {
                const userId = row.getAttribute('data-user-id');
                const select = row.querySelector('.status-select');
                if (userId && select && select.value) {
                    updateData[userId] = select.value;
                }
            });

            fetch(`/admin/class/attendance/update/${sessionId}`, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(updateData)
            })
            .then(res => {
                if (res.ok) {
                    alert('保存しました');
                    location.reload();
                } else {
                    alert('保存に失敗しました');
                }
            })
            .catch(err => alert('通信エラーが発生しました'));
        });
    }
});
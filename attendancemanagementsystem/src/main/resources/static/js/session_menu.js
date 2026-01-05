document.getElementById('startSessionForm').addEventListener('submit', function(e) {
    e.preventDefault();

    // フォームの値を取得
    const data = {
        date: document.getElementById('date').value,
        time: document.getElementById('time').value,
        courseName: document.getElementById('courseName').value,
        subjectId: document.getElementById('subjectId').value,
        classroomId: document.getElementById('classroomId').value
    };

    // サーバー送信
    fetch('/session/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    .then(response => {
        if (!response.ok) throw new Error('セッション作成に失敗しました');
        return response.json();
    })
    .then(data => {
        // ポップアップを開く
        const url = `/session/active/${data.sessionId}`;
        window.open(url, 'SessionWindow_' + data.sessionId, 'width=1000,height=800,scrollbars=yes,resizable=yes');
    })
    .catch(error => {
        console.error('Error:', error);
        alert('エラーが発生しました: ' + error.message);
    });
});
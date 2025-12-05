// attendanceDetail.js

document.addEventListener('DOMContentLoaded', () => {
    
    // 必要なDOM要素を取得
    const yearDropdown = document.getElementById('year-dropdown');
    const monthDropdown = document.getElementById('month-dropdown');
    
    // URLから subjectId を取得する関数
    const getSubjectId = () => {
        const input = document.getElementById('currentSubjectId');
        return input ? input.value : null;
    };

    // --- イベントリスナー ---
    
    /**
     * 年または月のプルダウンが変更された際に実行されるハンドラ
     */
    const handleDropdownChange = () => {
        const selectedYear = yearDropdown.value;
        const selectedMonth = monthDropdown.value;
        
        // データの取得と更新を実行
        fetchAttendanceData(selectedYear, selectedMonth);
    };

    // 年と月の両方の変更を監視
    if (yearDropdown && monthDropdown) {
        yearDropdown.addEventListener('change', handleDropdownChange);
        monthDropdown.addEventListener('change', handleDropdownChange);
    }


    // --- データ取得ロジック ---
    
    /**
     * サーバーから新しい月のデータを取得し、画面を更新する関数
     * @param {string} year - 選択された年
     * @param {string} month - 選択された月
     */
    function fetchAttendanceData(year, month) {
        const subjectId = getSubjectId();
        
        if (!subjectId) {
            console.error('Subject ID not found in URL');
            return;
        }

        // subjectId をクエリパラメータに追加
        const apiEndpoint = `/student/api/data?subjectId=${subjectId}&year=${year}&month=${month}`;

        fetch(apiEndpoint)
            .then(response => {
                if (!response.ok) {
                    throw new Error('サーバーからのデータ取得に失敗しました。HTTP Status: ' + response.status);
                }
                return response.json();
            })
            .then(data => {
                // 画面要素を新しいデータで更新
                updateSummary(data);
                updateCalendar(data.dailyAttendanceList);
            })
            .catch(error => {
                console.error('エラーが発生しました:', error);
                alert('データの読み込み中にエラーが発生しました。');
            });
    }

    // --- DOM更新ロジック ---
    
    /**
     * 左側と上部のサマリー情報を更新する
     * @param {object} data - SubjectAttendanceDetailDto オブジェクト
     */
    function updateSummary(data) {
        // 左側コンテナの更新
        const subjectNameEl = document.querySelector('.subject-name');
        if(subjectNameEl) subjectNameEl.textContent = data.subjectName;

        const classRoomEl = document.getElementById('classroom-value');
        if(classRoomEl) classRoomEl.textContent = data.classroom;

        const teacherEl = document.getElementById('teacher-value');
        if(teacherEl) teacherEl.textContent = data.teacherName;

        const requiredEl = document.getElementById('required-classes-value');
        if(requiredEl) requiredEl.textContent = data.requiredClasses;
        
        // 出席率の表示形式調整
        const rateDisplay = (data.currentAttendanceRate * 100).toFixed(1) + '%';
        const rateEl = document.getElementById('current-rate-value');
        if(rateEl) rateEl.textContent = rateDisplay;
        
        const maxAbsenceEl = document.getElementById('max-absence-value');
        if(maxAbsenceEl) maxAbsenceEl.textContent = data.maxAbsenceClasses;
        
        // リスクマークの更新
        const riskIcon = document.getElementById('risk-warning-icon');
        if (riskIcon) {
            riskIcon.classList.remove('visible', 'hidden');
            // 例: 欠席可能数が1以下なら表示
            riskIcon.classList.add(data.maxAbsenceClasses <= 1 ? 'visible' : 'hidden');
        }

        // 上部サマリーの更新
        updateTextById('present-classes-value', `${data.presentClasses}コマ`);
        updateTextById('absent-classes-value', `${data.absentClasses}コマ`);
        updateTextById('late-classes-value', `${data.lateClasses}コマ`);
        updateTextById('official-absent-value', `${data.officialAbsentClasses}コマ`);
        updateTextById('official-pending-value', `${data.officialPendingClasses}コマ`);
    }

    // ヘルパー関数: IDが存在する場合のみテキスト更新
    function updateTextById(id, text) {
        const el = document.getElementById(id);
        if (el) el.textContent = text;
    }

    /**
     * カレンダーテーブルの tbody を更新する
     * @param {Array<object>} dailyList - DailyDetail の配列
     */
    function updateCalendar(dailyList) {
        const tbody = document.getElementById('calendar-body');
        if (!tbody) return;

        tbody.innerHTML = ''; // 既存の行をクリア

        if (!dailyList || dailyList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6">授業データがありません</td></tr>';
            return;
        }

        dailyList.forEach(day => {
            const row = document.createElement('tr');
            
            // 1. 日付
            row.insertAdjacentHTML('beforeend', `<td class="date-day">${day.dateDay}</td>`);

            // 2. 各時限のステータス (classStatuses配列)
            if (day.classStatuses) {
                day.classStatuses.forEach(status => {
                    let statusClass = 'no-class';
                    if (status === '○') statusClass = 'present';
                    else if (status === '✕') statusClass = 'absent'; 
                    else if (status === '△') statusClass = 'late'; 
                    else if (status === '公') statusClass = 'attendance-public'; 
                    
                    row.insertAdjacentHTML('beforeend', 
                        `<td class="status-cell"><span class="${statusClass}">${status}</span></td>`);
                });
            }

            // 3. 教室
            row.insertAdjacentHTML('beforeend', `<td class="classroom-cell">${day.classroom}</td>`);

            tbody.appendChild(row);
        });
    }
});